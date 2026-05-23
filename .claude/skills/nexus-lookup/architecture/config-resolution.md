# Config Resolution

本文件描述本地配置、远程配置、IPC 桥接、server 回退策略的完整读写链路。

## 配置读取优先级

配置读取严格遵循以下优先级（高到低）：
1. 远程配置刷新后的持久化结果
2. 已缓存或已落盘的本地持久化结果
3. 空 JSON `{}`

## 主 App 刷新链路

负责从本地 Python 服务获取最新配置并落盘。读写方向：`Server -> Main App -> IPC / Local Disk`。

1. **触发与系统判定**：`MainActivity.onResume()` 调用 `OsUtils.getCurr()` 获取当前系统类型。
2. **宿主选择**：`resolveTargetHostPackage(osFamily)` 根据系统类型和已安装包列表选择目标宿主包名。
3. **版本获取**：`getInstalledPackageVersionCode(targetPkg)` 读取目标宿主版本号。
4. **远程拉取**：调用 `XService.refreshWebSettings(context, packageName, versionCode)` 发起 HTTP GET 请求，目标地址为 `http://127.0.0.1:8788/<packageName>/<versionCode>/config.json`。
5. **分发写入**：拉取到 JSON 后，调用 `XIpcBridge.writeWebSettingsJson()`。
6. **IPC 路由**：`XIpcBridge` 根据传入的 `Context` 判断所在进程，主 App 进程直接写入，宿主进程通过 `SettingsContentProvider.call()` 转发写入。
7. **本地落盘**：最终由 `XIpcStoreRepository` 持 Store 级 `Mutex` 调用 `ConfigPersistence` 写入 JSON 文件。

## 宿主进程读取链路

供目标应用在 Hook 运行时动态读取参数与模型配置。读写方向：`Local Disk / IPC -> Host App -> Hook Logic`。

1. **发起读取**：Hook 业务逻辑调用 `XService.getWebSettings(context)` 或 `getLocalSettings(context)`，底层由 `XIpcBridge.read*Json()` 获取配置文本。
2. **文件流读取**：宿主进程读取 Store 时，`XIpcBridge` 通过 `ContentResolver.openInputStream(store.fileUri)` 打开 `SettingsContentProvider.openFile()` 暴露的只读文件流，不通过 `Bundle` 返回整段 JSON。
3. **JSON 解析**：通过 `parseJsonObject()` 将读取到的字符串反序列化为 `JsonObject`。
4. **领域对象暴露**：
   - `WebSettings` 暴露通用的 `config` 属性。
   - `LocalSettings` 暴露 LLM 特定配置，包含 endpoint、apiKey、model、prompt、proxy、takeoverKeywords、memoryPrompt、tools、MCP、commandTools 等字段。
5. **云端路径寻址**：具体业务 Hook 通过 `BaseConfigProvider.getElement(path)` 使用点号路径读取深层动态字段。

## 核心模块与持久化职责

- **`XIpcBridge`**：作为配置流转的核心路由，根据当前 `Context` 自动判断调用方处于主进程还是宿主进程，隐藏进程差异。
- **`IpcContract.Store`**：定义 `WEB_SETTINGS` 与 `LOCAL_SETTINGS` 的读写 method、payload field 与文件流 URI。当前文件流 URI 为 `content://com.niki914.nexus.ipc.provider/stores/web_settings` 和 `content://com.niki914.nexus.ipc.provider/stores/local_settings`。
- **`SettingsContentProvider`**：处理跨进程请求。`call()` 负责写入、mutate、通知等命令型操作；`openFile()` 负责按 Store URI 返回只读 `ParcelFileDescriptor`。两个入口共用 UID / package 鉴权。
- **`XProviderDispatcher`**：Provider call 分发器。GET 类 method 返回 `store_uri` 句柄，PUT / MUTATE 类 method 返回 success-only，不返回 Store JSON payload。
- **持久化层 (`XIpcStoreRepository` / `ConfigPersistence`)**：封装 Store 级并发控制与文件 I/O。`WEB_SETTINGS` 落盘到 `web_settings.json`，`LOCAL_SETTINGS` 落盘到 `local_settings.json`，写入使用 `AtomicFile`。

## Store 持久化与 IPC 边界

| Store | 文件名 | 宿主读取方式 | 写入方式 |
| --- | --- | --- | --- |
| `WEB_SETTINGS` | `web_settings.json` | `ContentResolver.openInputStream(WEB_SETTINGS.fileUri)` | `XIpcBridge.writeWebSettingsJson()` -> provider call 或本地 repository |
| `LOCAL_SETTINGS` | `local_settings.json` | `ContentResolver.openInputStream(LOCAL_SETTINGS.fileUri)` | `XIpcBridge.writeLocalSettingsJson()` / `mutateSetting()` -> provider call 或本地 repository |

- `WEB_SETTINGS` 在 `XIpcBridge` 中保留内存缓存，写入成功后更新缓存。
- `LOCAL_SETTINGS` 不做长期缓存，读取走 uncached 路径，以便设置页和 MCP discovery cache 更新后尽快可见。
- 旧的 LocalSettings SharedPreferences 持久化路径已经不再使用。

## Server 端策略

本地 Python 静态服务 (`server/server.py`) 负责下发配置：

- **路径匹配规则**：强制按包名作为一级目录结构，匹配规则为 `/<packageName>/<versionCode>/config.json`。不使用 alias。
- **最近版本回退**：若请求对应的 `<versionCode>` 目录不存在，服务会在同包名 (`<packageName>`) 目录下寻找版本号“距离最近”的配置并返回。

## 核心源码参考

- `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/HookLocalSettings.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/XIpcBridge.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/XRes.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/cp/SettingsContentProvider.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/cp/XProviderDispatcher.kt`
- `ipc/src/main/java/com/niki914/nexus/ipc/store/`
- `server/server.py`
