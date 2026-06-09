# Config Resolution

本文件描述当前源码里的配置来源、IPC 边界与落盘方式。

## 读取优先级

当前读取顺序是：

1. 已刷新并落盘的 JSON
2. 进程内缓存（仅 `WEB_SETTINGS` 有内存缓存）
3. 空 JSON `{}`

## 主 App 初始化链路

主 App 当前在 `MainActivity.onCreate()` 中执行：

1. `ContextProvider.provide(applicationContext)` 提供 App 侧 `Context`。
2. `XRepo.init(applicationContext)` 初始化设置仓库。
3. `resolveStartupAssistantUi()` 判断启动 UI 类型。
4. `AppLaunchDecision.resolve(startupAssistantUi)` 决定进入 `StartupPage` 或 `HomePage`。

## 宿主进程读取链路

宿主侧入口读取走 `XRepo` 与 `RuntimeEnvironment`：

- `Entrance.onLoad()`：`XRepo.init(ctx)` -> `RuntimeEnvironment.install(createAppRuntimeBridge())` -> `XRepo.web.await()`
- `HookLocalSettings.update(ctx)`：刷新 Hook 侧本地设置缓存
- `createAppRuntimeBridge()`：为 runtime 提供设置读取网关

`XIpcBridge` 会根据 `XValues.getAppTypeOf(context)` 分流：

- 主 App 进程：直接访问 `XIpcStoreRepository`
- 宿主进程：通过 `ContentResolver.openInputStream(store.fileUri)` 读取 `SettingsContentProvider.openFile()` 暴露的只读文件流

当前 provider contract 里虽然保留了 `GET_WEB_SETTINGS` / `GET_LOCAL_SETTINGS`，但实际 host 读取主路径是**直接打开 Store 文件流**，不是把整段 JSON 放进 `Bundle` 返回。

## 持久化职责

- `XIpcBridge`：屏蔽主进程与宿主进程的读写差异。
- `SettingsContentProvider`：做 caller 校验，并提供 `call()` 与 `openFile()` 两个 IPC 入口。
- `XProviderDispatcher`：处理 PUT / MUTATE / NOTIFICATION 这类命令式请求。
- `XIpcStoreRepository`：按 Store 加锁，做读、写、局部 mutate。
- `ConfigPersistence`：把 `WEB_SETTINGS` 和 `LOCAL_SETTINGS` 写入 `filesDir` 下的 JSON 文件，使用 `AtomicFile` 原子落盘。

## Store 边界

| Store | 文件名 | 读取方式 | 写入方式 |
| --- | --- | --- | --- |
| `WEB_SETTINGS` | `web_settings.json` | 主进程直接读；宿主通过 `openInputStream(store.fileUri)` | `XIpcBridge.writeWebSettingsJson()` |
| `LOCAL_SETTINGS` | `local_settings.json` | 主进程直接读；宿主通过 `openInputStream(store.fileUri)` | `XIpcBridge.writeLocalSettingsJson()` / `mutateSetting()` |

- `WEB_SETTINGS` 在 `XIpcBridge` 有进程内缓存。
- `LOCAL_SETTINGS` 当前不做长期缓存，读路径始终走 uncached。
- 旧 SharedPreferences 路径不再是当前实现。

## Server 回退策略

本地服务实现位于：

- `server/server.py`

当前规则：

- 请求路径匹配 `/<packageName>/<versionCode>/config.json`
- 精确命中时直接返回文件
- 未命中时，在同包名目录下选择数值距离最近的版本目录回退
- 不使用宿主 alias

## 关键源码

### `app/src/main/java/com/niki914/nexus/agentic/app/`

- `MainActivity.kt`
- `EXT.kt`

### `app/src/main/java/com/niki914/nexus/agentic/mod/`

- `XService.kt`
- `HookLocalSettings.kt`

### `app/src/main/java/com/niki914/nexus/agentic/repo/`

- `XRepo.kt`
- `LocalSettingsCodec.kt`
- `LocalSettingsStore.kt`

### `ipc/src/main/java/com/niki914/nexus/ipc/`

- `XIpcBridge.kt`
- `XRes.kt`
- `cp/SettingsContentProvider.kt`
- `cp/XProviderDispatcher.kt`
- `store/XIpcStoreRepository.kt`
- `store/ConfigPersistence.kt`
