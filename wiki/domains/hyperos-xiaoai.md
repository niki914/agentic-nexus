# HyperOS / XiaoAi Domain

## 概览

XiaoAi 模块是 Nexus 的另一个核心宿主实现。其核心心智是**捕获响应目标（ResponseTarget），并通过专用的 `RenderTextStreamCardHook` 以流式分片（Chunk）的方式直接向宿主注入渲染指令**。

## 注入入口与路由

- `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt` 在加载时判断包名，如果命中 XiaoAi 宿主，则实例化 `XiaoaiChatHook`。
- `RuntimeBootstrap` 将该 Hook 挂载。

## 生命周期与接管

与 Breeno 不同，XiaoAi 依赖于 `ResponseTarget` 的捕获和 `CompletableDeferred` 同步等待机制：

### 1. 目标与输入捕获
- **`CaptureResponseTargetHook`**：监听 XiaoAi 的响应分发点，一旦截获到 `target`，立即通过回调通知 `XiaoaiChatHook`，并调用 `targetReady.complete(Unit)` 放行后续流式注入。
- **`CaptureInputHook`**：捕获用户输入，送入 `AbstractAssistantHook` 的处理管线。

### 2. 接管判定 (`TakeoverResolver`)
- 与通用流程一致，由 `TakeoverResolver` 决定是 `NativeTakeover` 还是 `InjectedLLM`。
- 若由 LLM 接管，进入流式渲染环节。

### 3. 原生响应阻断
XiaoAi 采用了多重子 Hook 来保证注入内容不被打断：
- **`BlockNativeInstructionByWhitelistHook`**：使用白名单机制，阻断原生下发的不期望指令。
- **`BlockNativeTtsPlaybackHook`**：阻断原生 TTS 语音播报，避免与注入文本产生听觉冲突。

### 4. 渲染注入 (`XiaoaiChatHook.renderStreamCard`)
XiaoAi 的渲染机制由 `RenderTextStreamCardHook` 专门负责：
- 在调用 `LLMController.stream` 前，主 Hook 会重置并等待 `targetReady.await()`，确保 `ResponseTarget` 已被捕获。
- **分片注入**：LLM 返回的流被 `collectAsChunk` 收集（而不是全量文本），并将每个 `chunk` 以及 `isFirst`、`isFinal` 标志传给 `renderTextStreamCardHook.render`。
- **渲染器内部**：`RenderTextStreamCardHook` 根据标志位和目标对象，调用 XiaoAi 原生的相关接口，动态拼装和分发 UI 渲染指令，实现平滑的打字机效果。
- **结束清理**：当收到 `isFinal` 时或会话重置时（`onSessionReset`），清理状态并重置 `CompletableDeferred`，准备下一轮对话。

## 关键模型与并发控制

- **`capturedResponseTarget`**：拦截到的 XiaoAi 原生渲染上下文对象，后续所有的指令都发送给它。
- **`CompletableDeferred<Unit>` (targetReady)**：处理时序竞争的核心。因为用户输入（`CaptureInputHook`）和目标生成（`CaptureResponseTargetHook`）是异步的，必须确保 `ResponseTarget` 存在后，LLM 的流才能开始渲染，否则存在抛弃首包的风险。
- **流分发**：使用 `shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)`，确保在等待 `targetReady` 的期间，LLM 的首批返回被缓存，不丢失任何文本。