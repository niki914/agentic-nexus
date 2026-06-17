# Config Resolution

本文件描述当前源码里的配置来源、runtime settings 网关、IPC 边界与落盘方式。

## 配置分层

当前实现已经不是单一 `web_settings.json` / `local_settings.json` 双文件模型。

- `web_settings`：宿主版本相关的远端配置缓存，默认落到 `settings/hooks.json`。
- `local_settings`：仍保留的兼容 store，当前落到 `local_settings.json`。
- agent/runtime 配置：已经拆到 `settings/*` 目录下的多 store，由 `ipc/src/main/java/com/niki914/nexus/ipc/store/StoreDescriptorRegistry.kt` 统一注册。

静态 store 注册如下：

| Store ID | 相对路径 |
| --- | --- |
| `web_settings` | `settings/hooks.json` |
| `local_settings` | `local_settings.json` |
| `agent.main.config` | `settings/agents/main/config.json` |
| `agent.main.memory` | `settings/agents/main/memory.json` |
| `tools.builtin` | `settings/tools/builtin_tools.json` |
| `tools.custom` | `settings/tools/custom_tools.json` |
| `tools.mcp.servers` | `settings/tools/mcp/servers.json` |
| `rules.execution` | `settings/rules/execution_rules.json` |
| `rules.takeover` | `settings/rules/takeover_rules.json` |
| `app.state` | `settings/app_state.json` |

动态 MCP cache store 通过 `StoreDescriptorRegistry.mcpCacheStoreId(serverId)` 生成，实际文件路径为 `settings/tools/mcp/cache/<serverId>.json`。

## 主 App 初始化链路

主 App 启动时的配置相关初始化分成两层：

1. `app/src/main/java/com/niki914/nexus/agentic/app/App.kt`：`XRepo.init(applicationContext)` -> `RuntimeEnvironment.install(createAppRuntimeBridge())` -> 异步触发 `XRepo.web.await()` 和 `XRepo.tryPutDefaultSettings()`。
2. `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`：`ContextProvider.provide(applicationContext)` -> `XRepo.init(applicationContext)` -> `resolveStartupAssistantUi()` -> `AppLaunchDecision.resolve(...)`。

`app/src/main/java/com/niki914/nexus/agentic/runtime/AppRuntimeBridge.kt` 当前把 runtime settings 网关固定到 `XRepoRuntimeGateway()`；`agent-runtime` 侧统一通过 `RuntimeEnvironment.awaitSettingsGateway()` 访问配置，不直接碰 `XRepo`。

## 宿主进程读取链路

宿主侧入口仍从 `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt` 启动：

- `XRepo.init(ctx)`：初始化仓库入口。
- `RuntimeEnvironment.install(createAppRuntimeBridge())`：把同一套 runtime settings gateway 装进宿主进程。
- `HookLocalSettings.update(ctx)`：刷新 Hook 侧 `LocalSettings` 缓存。
- `XRepo.web.await()`：读取或刷新远端 web settings。

`app/src/main/java/com/niki914/nexus/agentic/repo/XRepoRuntimeGateway.kt` 是 runtime 到仓库的适配层：

- `readLlmConfig()` 读取 `agent.main.config` 并合并 `agent.main.memory`。
- `listBuiltinToolSettings()`、`listCustomTools()`、`listMcpServers()`、`listCachedTools(server)`、`listExecutionRules()` 全部走 `XRepo` 对应 API。
- `saveDiscoveredTools()`、`setBuiltinToolEnabled()`、`saveCustomTool()` 等写操作也回到 `XRepo`。

## IPC 与持久化职责

### 进程分流

`ipc/src/main/java/com/niki914/nexus/ipc/XIpcBridge.kt` 根据 `XValues.getAppTypeOf(context)` 分流：

- 主 App 进程：直接调用 `ipc/src/main/java/com/niki914/nexus/ipc/store/XIpcStoreRepository.kt`。
- 宿主进程：通过 `ContentResolver.openInputStream()` / `openOutputStream()` 访问 `SettingsContentProvider.openFile()` 暴露的 store 文件；命令式局部更新走 provider `call()`。

当前只有 `web_settings` 在 `XIpcBridge` 内做进程内字符串缓存；其他 store 读取都走 uncached 路径。

### Provider 协议

`ipc/src/main/java/com/niki914/nexus/ipc/XRes.kt` 当前同时保留旧方法和泛化方法：

- 旧兼容方法：`GET_WEB_SETTINGS`、`GET_LOCAL_SETTINGS`、`MUTATE_WEB_SETTINGS`、`MUTATE_LOCAL_SETTINGS`。
- 当前泛化方法：`GET_STORE`、`MUTATE_STORE`。

`ipc/src/main/java/com/niki914/nexus/ipc/cp/XProviderDispatcher.kt` 的现状是：

- `GET_STORE` 与旧的 `GET_WEB_SETTINGS` / `GET_LOCAL_SETTINGS` 都只返回 `STORE_URI`，不再把整段 JSON 放进 `Bundle`。
- `MUTATE_STORE` 调用 `XIpcStoreRepository.mutateJson(storeId, path, valueJson)`。
- `PUT_WEB_SETTINGS` 与 `PUT_LOCAL_SETTINGS` 当前没有实际分发逻辑；宿主写整段 JSON 的主路径已经改成 `openFile(..., "wt")`。

`ipc/src/main/java/com/niki914/nexus/ipc/cp/SettingsContentProvider.kt` 的职责是：

- 做 caller 校验。
- 在 `call()` 中把 `GET_STORE` / `MUTATE_STORE` / 兼容方法分发给 `XProviderDispatcher`。
- 在 `openFile()` 中按 `StoreDescriptorRegistry.resolveDynamic(storeId)` 打开真实 store 文件；写入时创建 pipe，再由后台线程调用 `XIpcStoreRepository.writeJson()` 做原子落盘。

### Store 仓库

`ipc/src/main/java/com/niki914/nexus/ipc/store/XIpcStoreRepository.kt` 当前按 store 维度维护 `Mutex`：

- `readJson()`：读取持久化 JSON；文件不存在或内容非法时回退到 `StoreDescriptor.defaultJson`。
- `writeJson()`：要求写入内容是 JSON object，再调用 `ConfigPersistence.writeJson()`。
- `mutateJson()`：基于 `IpcJsonMutator` 做局部路径更新后再落盘。

`ipc/src/main/java/com/niki914/nexus/ipc/store/ConfigPersistence.kt` 把 store 写到 `filesDir/<relativePath>`，通过 `AtomicFile` 保证原子写入。

## Web Settings 回退

当前仓库里同时存在客户端回退逻辑和本地测试服务实现：

- 客户端位于 `app/src/main/java/com/niki914/nexus/agentic/repo/WebSettingsApi.kt`。
- 本地测试服务位于 `server/server.py`。

当前客户端行为：

1. 先请求 `https://gitee.com/niki914/nexus-res/raw/main/<packageName>/<versionCode>/config.json`。
2. 若精确版本 404，则请求 `https://gitee.com/niki914/nexus-res/raw/main/<packageName>/versions.json`。
3. 通过 `app/src/main/java/com/niki914/nexus/agentic/repo/WebSettingsModels.kt` 中的 `WebSettingsVersionFallback.nearestVersionCode()` 选择距离最近的版本；距离相同优先较低版本。
4. 成功结果回写 `web_settings` store，并带上 `requested_version_code` 与 `resolved_version_code`。

本地 `server/server.py` 的规则与客户端约定一致：优先命中 `/<packageName>/<versionCode>/config.json`，未命中时在同包名目录下选择数值距离最近的版本目录回退。

## 关键源码

### `app/src/main/java/com/niki914/nexus/agentic/app/`

- `App.kt`
- `MainActivity.kt`

### `app/src/main/java/com/niki914/nexus/agentic/repo/`

- `XRepo.kt`
- `XRepoRuntimeGateway.kt`
- `DomainSettingsStore.kt`
- `WebSettingsApi.kt`
- `WebSettingsModels.kt`
- `AgentSettingsCodec.kt`
- `MemorySettingsCodec.kt`
- `ToolSettingsCodec.kt`
- `McpSettingsCodec.kt`
- `RuleSettingsCodec.kt`
- `AppStateSettingsCodec.kt`

### `app/src/main/java/com/niki914/nexus/agentic/runtime/`

- `AppRuntimeBridge.kt`

### `app/src/main/java/com/niki914/nexus/agentic/mod/`

- `HookLocalSettings.kt`
- `XService.kt`

### `ipc/src/main/java/com/niki914/nexus/ipc/`

- `XIpcBridge.kt`
- `XRes.kt`
- `cp/SettingsContentProvider.kt`
- `cp/XProviderDispatcher.kt`
- `store/StoreDescriptor.kt`
- `store/StoreDescriptorRegistry.kt`
- `store/XIpcStoreRepository.kt`
- `store/ConfigPersistence.kt`

### `server/`

- `server.py`
