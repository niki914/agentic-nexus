# Config Resolution

本文件描述本地配置、远程配置、IPC 桥接、server 回退策略的完整读写链路。

## 配置读取优先级

配置读取严格遵循以下优先级（高到低）：
1. 远程配置刷新后的持久化结果
2. 已缓存的本地持久化结果
3. 空 JSON `{}`

## 主 App 刷新链路

负责从本地 Python 服务获取最新配置并落盘。读写方向：`Server -> Main App -> IPC / Local Disk`。

1. **触发与宿主判定**：`MainActivity.onResume()` 中，根据操作系统类型（`RootUtils.getOsFamily()`）与设备已安装包列表，选择当前优先适配的宿主包名。
2. **远程拉取**：调用 `XService.refreshWebSettings(context, packageName, versionCode)` 发起 HTTP GET 请求，目标地址为 `http://127.0.0.1:8788/<packageName>/<versionCode>/config.json`。
3. **分发写入**：拉取到 JSON 后，调用 `XIpcBridge.writeWebSettingsJson()`。
4. **IPC 路由**：`XIpcBridge` 根据传入的 `Context` 判断所在进程，决定是直接本地落盘还是通过 `SettingsContentProvider` 跨进程通讯。
5. **本地落盘**：最终由 `XIpcStoreRepository` 调用 `ConfigPersistence`，将 JSON 真正写入本地文件。

## 宿主进程读取链路

供目标应用在 Hook 运行时动态读取参数与模型配置。读写方向：`Local Disk / IPC -> Host App -> Hook Logic`。

1. **发起读取**：Hook 业务逻辑调用 `XService.getWebSettings(context)` 或 `getLocalSettings(context)`，底层由 `XIpcBridge.read*Json()` 负责获取配置文本。
2. **JSON 解析**：通过 `parseJsonObject()` 将读取到的字符串反序列化为 `JsonObject`。
3. **领域对象暴露**：
   - `WebSettings` 暴露通用的 `config` 属性。
   - `LocalSettings` 暴露 LLM 特定的属性，包含：`endpoint`、`apiKey`、`model`、`prompt`、`proxy`、`takeoverKeywords`。
4. **云端路径寻址**：具体业务 Hook（如渲染、流控）通过 `BaseConfigProvider.getElement(path)` 使用点号路径（Dot Notation）读取深度嵌套的动态字段。

## 核心模块与持久化职责

- **`XIpcBridge`**：作为配置流转的核心路由，根据当前 `Context` 自动判断调用方处于主进程还是宿主进程，隐藏进程差异。
- **`SettingsContentProvider`**：处理跨进程请求，为没有文件读写权限或路径不同的宿主进程提供访问桥梁。
- **持久化层 (`XIpcStoreRepository` / `ConfigPersistence`)**：封装基础 I/O 操作与存储媒介细节，执行文件的写入与缓存读取。

## Server 端策略

本地 Python 静态服务 (`server/server.py`) 负责下发配置：

- **路径匹配规则**：强制按包名作为一级目录结构，匹配规则为 `/<packageName>/<versionCode>/config.json`。不使用 alias。
- **最近版本回退**：若请求对应的 `<versionCode>` 目录不存在，服务会在同包名 (`<packageName>`) 目录下寻找版本号“距离最近”的配置并返回，以此提供一定的跨版本容错和向前/向后兼容。

## 核心源码参考

- `app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/HookLocalSettings.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/XIpcBridge.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/cp/SettingsContentProvider.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/store/`
- `server/server.py`
