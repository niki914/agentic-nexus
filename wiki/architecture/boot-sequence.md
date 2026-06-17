# Boot Sequence

本文档描述从 Xposed 入口到宿主主 Hook 安装完成的启动链路，以及 Hook 安装后每轮 query 的分流前置条件。

## 启动时序

1. **入口匹配**
   - `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt` 通过 `getTarget()` 返回 `Target.filter(*XValues.appList.toTypedArray())`，只监听受支持宿主包名。

2. **基础 Hook 装载**
   - `Entrance.onLoad()` 先用 `HookSideLoader.load(scope, ContextHook(), params)` 装载 `h/src/main/java/com/niki914/nexus/h/util/ContextHook.kt`。
   - 入口里仍保留 `ActivityHook` 与 `FloatWindowHook` 的注释代码，但当前未启用，不属于实际启动链。

3. **上下文与设置初始化**
   - `h/src/main/java/com/niki914/nexus/h/util/ContextProvider.kt` 的 `ContextProvider.await()` 等待宿主 `Context` 就绪。
   - `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` 的 `XRepo.init(ctx)` 初始化进程内 repo 上下文。
   - `app/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt` 通过 `RuntimeEnvironment.install(createAppRuntimeBridge())` 安装 runtime settings gateway。
   - `app/src/main/java/com/niki914/nexus/agentic/mod/HookLocalSettings.kt` 的 `HookLocalSettings.update(ctx)` 刷新宿主进程本地设置缓存。

4. **WEB_SETTINGS 获取与 fallback 通知**
   - `app/src/main/java/com/niki914/nexus/agentic/repo/WebSettingsApi.kt` 的 `XRepo.web.await()` 先读 IPC 缓存；缓存不可用时再请求远端配置。
   - 若目标版本没有精确配置，`WebSettingsApi` 会按 `versions.json` 选择最近版本并返回 `isFallbackVersion = true` 的成功结果。
   - `Entrance.onLoad()` 在拿到 fallback 成功结果后，会调用 `app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt` 的 `XService.postNotification(...)` 发出“版本未支持，已选择默认版本”的提示；通知实际经由 `XIpcBridge.postNotification(...)` 下发。
   - 只有 `webSettingsResult.configOrNull()` 非空时，才会继续进入宿主路由；配置缺失时不会安装业务 Hook。

5. **宿主路由**
   - `Entrance.onSettingsFetched()` 用 `HostApp.fromPackageName(params.packageName)` 判断当前宿主。
   - 只有 `targetPkg == params.packageName` 时才继续安装，避免把别的宿主配置挂到当前进程。
   - `HostApp.Breeno` 路由到 `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`。
   - `HostApp.XiaoAi` 路由到 `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`。
   - 未命中时直接返回，不安装业务 Hook。

6. **Runtime 安装**
   - `h/src/main/java/com/niki914/nexus/h/core/runtime/RuntimeBootstrap.kt` 的 `RuntimeBootstrap.installIfNeeded()` 为当前宿主进程安装单例 `Runtime`。
   - `Runtime` 当前只挂载命中的主业务 Hook；主 Hook 再在 `onHook()` 里继续安装 session / response / input subhook。

## 每轮 query 的分流前置

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt` 是 Breeno 与 XiaoAi 共用的运行期入口。
- 宿主输入先由各自的 `CaptureInputHook` 捕获，再回调到 `handleCapturedQuery(roomId, query)`。
- `handleCapturedQuery(...)` 会先读取 `XRepo.takeoverRules.list()`，再交给 `TakeoverResolver.resolve(...)` 生成当前轮次的 takeover 决策。
- 决策结果会先写入 `ActiveTurnStore`：
  - `TurnMode.NativeTakeover`：调用 `LLMController.stopCurrentRound(keepCurrentTurn = false)` 后直接返回，后续保留宿主原生回答路径，不进入 Nexus 渲染注入。
  - `TurnMode.InjectedLLM`：才会继续调用 `dispatchQueryToLLM(...)`，后续渲染注入链路才有机会生效。

## 关键源码

### `app/src/main/java/`

- `a0/a0/a0/a0/a0/a0/Entrance.kt`
- `com/niki914/nexus/agentic/mod/HookLocalSettings.kt`
- `com/niki914/nexus/agentic/mod/XService.kt`
- `com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`
- `com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
- `com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`
- `com/niki914/nexus/agentic/repo/WebSettingsApi.kt`
- `com/niki914/nexus/agentic/repo/XRepo.kt`

### `h/src/main/java/com/niki914/nexus/h/`

- `core/runtime/RuntimeBootstrap.kt`
- `util/ContextHook.kt`
- `util/ContextProvider.kt`
