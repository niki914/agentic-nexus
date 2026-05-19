# AX Contract

<project>
Nexus 是一个 Android Xposed 模块。它在受支持宿主进程中注入自定义 LLM 响应，并按宿主类型路由到对应 Hook 实现，覆盖或拦截原生回答链路。当前支持 `ColorOS / Breeno` 与 `HyperOS / XiaoAi` 两条宿主分支。
</project>

<modules>

| 目录 | 类型 | 职责 | 关键入口 |
|------|------|------|----------|
| `app/` | application | Xposed 入口、宿主路由、Compose 壳层 UI、业务 Hook 实现 | `Entrance.kt`, `MainActivity.kt`, `AbstractAssistantHook.kt` |
| `h/` | library | Xposed 框架层、Runtime 编排、反射调用、上下文捕获、日志与容错 | `IXposed.kt`, `Runtime.kt`, `Hook.kt`, `ContextHook.kt` |
| `composebase/` | library | Compose 主题与 ViewModel 基础设施 | `BaseTheme.kt`, `ComposeMVIViewModel.kt` |
| `ipc/` | library | 宿主进程与主 App 的跨进程配置/通知桥接 | `XIpcBridge.kt`, `SettingsContentProvider.kt`, `XRes.kt` |
| `server/` | Python | 本地静态配置服务，按包名与版本号提供 `config.json`，并支持最近版本回退 | `server.py` |

所有模块的构建入口是根目录 `build.gradle.kts` 与各子模块 `build.gradle.kts`。

</modules>

<host_matrix>

| HostApp | 包名 | 业务实现 | 配置目录 |
|---------|------|----------|----------|
| `Breeno` | `com.heytap.speechassist` | `BreenoChatHook` | `server/com.heytap.speechassist/<versionCode>/config.json` |
| `XiaoAi` | `com.miui.voiceassist` | `XiaoaiChatHook` | `server/com.miui.voiceassist/<versionCode>/config.json` |

</host_matrix>

<boot_sequence>

1. `Entrance.getTarget()` 通过 `XValues.appList` 过滤受支持宿主包名，而不是写死单个宿主。
2. `Entrance.onLoad()` 先调用 `HookSideLoader.load(scope, ContextHook(), params)`，尽早拦截 `Application.onCreate()` 捕获宿主 `Context`。
3. 同一入口还会调用 `HookSideLoader.load(scope, ActivityHook(), params)`，用于补充 Activity 生命周期观测，供浮窗 detach 与目标页面恢复的联动判定复用。
4. 后台协程等待 `ContextProvider.await()` 返回宿主 `Context`，然后执行 `HookLocalSettings.update(ctx)` 与 `XService.getWebSettings(ctx)`。
5. `HostApp.fromPackageName(params.packageName)` 决定当前宿主类型；只有 `targetPkg == params.packageName` 时才继续安装业务 Hook。
6. `Breeno` 路由到 `BreenoChatHook`，`XiaoAi` 路由到 `XiaoaiChatHook`。
7. `RuntimeBootstrap.installIfNeeded()` 为当前进程创建单例 `Runtime`；当前工程的业务 Hook 都走 `hook.onHook(params)` 同步安装。

</boot_sequence>

<turn_state>

`ConversationTurnState(roomId, turnId, lastQuery, mode)` 是当前通用会话状态模型。

- `mode` 只有两种：`InjectedLLM` 与 `NativeTakeover`
- 入口在 `AbstractAssistantHook.handleCapturedQuery(roomId, query)`
- `shouldTakeOver(query)` 为 `true` 时，当前轮次标记为 `NativeTakeover`，调用 `onTakeoverTriggered()` 后直接返回
- 否则进入 `dispatchQueryToLLM()`，由 `LLMController` 发起流式请求
- `onSessionReset(roomId)` 会清理上一房间的 `ConversationJournal` 并重置 `turnState`
- `turnId` 由 `TurnIdGenerator` 生成，使用 `AtomicLong + maxOf(previous + 1, System.currentTimeMillis())` 保证单调递增

</turn_state>

<hook_topology>

`AbstractAssistantHook.onHook()` 固定执行顺序：

1. `onBeforeInstallHooks()`
2. `installSessionHooks()`
3. `installResponseHooks()`
4. `installInputHooks()`

`BreenoChatHook` 子 Hook：

| Hook | 触发点 | 作用 |
|------|--------|------|
| `ResetSessionHook` | 宿主会话重置点 | 新会话创建或会话切换时重置当前 session |
| `BlockNativeCardHook` | `DataCenter#insertMessage` before | 在 `InjectedLLM` 模式下拦截原生回答卡片；Breeno 的响应注入与拦截都发生在卡片层 |
| `SuppressCleanupHook` | `OperationFactory#create` after | 将清理操作替换为 `DoNothingOperation`，避免系统移除注入卡片 |
| `CaptureInputHook` | `DataCenter#insertMessage` before | 捕获用户 query，并缓存 `DataCenter` 实例 |

`BreenoChatHook.installSessionHooks()` 还会安装浮窗 owner 的 detach Hook，并跟踪目标 Activity 的 `onResume()`；若浮窗 detach 后 700ms 内未回到目标页面，则触发 `onSessionReset()`。

`XiaoaiChatHook` 子 Hook：

| Hook | 触发点 | 作用 |
|------|--------|------|
| `ResetSessionHook` | 宿主会话重置点 | 清空当前 dialog 状态与渲染 session |
| `CaptureResponseTargetHook` | 响应目标创建链路 | 捕获后续文字流分片注入目标 |
| `BlockNativeTextStreamHook` | 原生文字流链路 | 屏蔽与注入轮次冲突的原生文本分片 |
| `BlockNativeTtsStreamHook` | 原生 TTS 指令链路 | 屏蔽与注入轮次冲突的原生 TTS 流 |
| `BlockNativeTtsPlaybackHook` | 原生 TTS 播放链路 | 屏蔽与注入轮次冲突的原生播报 |
| `RenderTextStreamCardHook` | 注入文字流链路 | 将 LLM 累计文本切成增量块，并转换为宿主可消费的 `Instruction` |
| `CaptureInputHook` | 用户输入链路 | 捕获用户 query |

</hook_topology>

<render_pipeline>

`BreenoChatHook.renderStreamCard()`：

1. 校验 `turnId` 与 `mode` 是否仍匹配当前活跃轮次。
2. 通过 `BreenoConfigProvider` 读取 `viewBeanClass`、`DataCenter` 方法名、mock 方法集与 localData。
3. 首帧创建 `viewBean` 并注入 `chatType`、`roomId`、`recordId`、mock 数据和反馈按钮 localData。
4. 每片用当前累计文本直接覆盖 `content`，并更新 `isFinal`、`isFirstSlice`。
5. 首帧调用 `dataCenter.insertMessage(bean)`，后续调用 `dataCenter.updateMessage(bean, false)`；同一条回答卡片被持续全量刷新。
6. 终帧反转 `hideFeedbackView` 并再次 `updateMessage()`，最后清空 `currentRenderSession`。

`XiaoaiChatHook.renderStreamCard()`：

1. 先校验当前轮次是否仍为活跃 `InjectedLLM` 轮次。
2. `RenderTextStreamCardHook` 维护单个 `XiaoaiRenderSession`，根据累计文本计算本次增量 `delta`；对宿主执行的是分片注入，而不是整段覆盖。
3. `dispatchQueryToLLM()` 会先等待 `CaptureResponseTargetHook` 完成目标捕获，再开始消费共享流。
4. 渲染阶段直接使用已捕获的响应目标，而不是重新查找目标对象。
5. 将每个增量块包装为宿主 `Instruction`，其 `namespace`、`name`、`idPrefix`、终帧补片文本都来自 `XiaoaiConfigProvider`。
6. 终帧额外注入 `renderTextStreamCardFinalChunkText`，然后清空当前渲染 session。

宿主差异总结：

- `Breeno` 走回答卡片层，注入模型是“单卡片全量刷新”，hook 点较靠上。
- `XiaoAi` 走响应目标 / Instruction 流层，注入模型是“增量文本分片注入”，hook 点更靠下，同时需要拦截文字流、TTS 流和播放链路。

</render_pipeline>

<llm_pipeline>

- `LLMController` 使用 `com.github.niki914:S3ss10n:2.0.0`
- 每次发送请求前，通过 `HookLocalSettings.refreshFromHookContext()` 获取最新本地配置
- `endpoint`、`apiKey`、`model`、`prompt` 均来自 `LocalSettings`
- `SessionEvent.RoundStarted` 映射为首帧
- `SessionEvent.TextDelta` 追加到 `StringBuilder` 后回调中间帧
- `SessionEvent.RoundCompleted` 回调终帧
- `SessionEvent.Error` 会把错误文本追加进当前输出
- `SessionEvent.ToolRunning / ToolSucceeded / ToolFailed` 当前被忽略

</llm_pipeline>

<config_resolution>

配置读取优先级是：远程配置刷新后的持久化结果 > 已缓存的持久化结果 > 空 JSON `{}`。

主 App 刷新链路：

1. `MainActivity.onResume()` 根据 `RootUtils.getOsFamily()` 与已安装包，选择优先宿主包名。
2. `XService.refreshWebSettings(context, packageName, versionCode)` 发起 HTTP GET：`http://127.0.0.1:8788/<packageName>/<versionCode>/config.json`
3. 远程 JSON 写入 `XIpcBridge.writeWebSettingsJson()`
4. `XIpcBridge` 根据当前 `Context` 决定是直接本地落盘，还是走 `SettingsContentProvider`
5. `XIpcStoreRepository` 最终调用 `ConfigPersistence` 将 JSON 落到本地文件

宿主进程读取链路：

1. `XService.getWebSettings(context)` / `getLocalSettings(context)` 通过 `XIpcBridge.read*Json()` 读取 JSON
2. `parseJsonObject()` 将 JSON 解析为 `JsonObject`
3. `WebSettings` 暴露 `config`
4. `LocalSettings` 暴露 `endpoint`、`apiKey`、`model`、`prompt`、`proxy`、`takeoverKeywords`
5. `BaseConfigProvider.getElement(path)` 用点号路径读取云配置

服务端行为：

- `server/server.py` 以包名目录作为一层路径，而不是 alias
- 请求路径匹配 `/<packageName>/<versionCode>/config.json`
- 若对应版本目录不存在，则回退到同包名下“距离最近”的版本目录

</config_resolution>

<runtime_constraints>

- `RuntimeBootstrap` 通过双重检查锁保证每个进程只安装一次 `Runtime`
- `BreenoChatHook` 使用 `Mutex + currentRenderSession` 管理单个流式渲染会话
- `ConversationJournal` 使用 `Mutex` 串行化读写
- `HookLocalSettings` 在内存中缓存最近一次读取到的本地配置
- `XValues.getAppTypeOf(context)` 只接受三类来源：主 App、受支持宿主、未知包

</runtime_constraints>

<navigation>

- **Xposed 入口**: `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`
- **通用 Hook 模板**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`
- **Breeno 实现**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`
- **XiaoAi 实现**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`
- **LLM 调度**: `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- **本地/远程配置门面**: `app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt`
- **IPC 桥接**: `ipc/src/main/java/com/niki914/nexus/ipc/XIpcBridge.kt`
- **宿主枚举与 IPC 常量**: `ipc/src/main/java/com/niki914/nexus/ipc/XRes.kt`
- **本地配置服务器**: `server/server.py`
- **Breeno 配置样例**: `server/com.heytap.speechassist/120803/config.json`
- **XiaoAi 配置样例**: `server/com.miui.voiceassist/507013003/config.json`
- **Gradle 自动任务**: `app/build.gradle.kts` 中的 `adbReverse`；`startServer` 已定义但默认未挂到 install/assemble

</navigation>

<todo_backlog>

- [ ] `RenderTextStreamCardHook`: 非常不符合预期的一个实现，使用了监视器锁，而且硬编码类名而没有上云，需要整改
- [ ] `MainActivity`: Use liquid glass~
- [ ] `BaseConfigProvider`: Dataclass to describe a hook spot
- [ ] `BaseConfigProvider`: 修改下游的读参方式，最好 suspend
- [ ] `AbstractAssistantHook`: 统一所有业务下的 subhooks、云 config、AbstractAssistantHook 方法命名
- [ ] `BreenoConfigProvider`: 修改读参方式，最好 suspend
- [ ] `BreenoChatHook`: Muti-Languages
- [ ] `LLMController`: 配置检查与快速失败
- [ ] `LLMController`: 字数测速

</todo_backlog>
