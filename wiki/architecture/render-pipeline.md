# Render Pipeline

本文件只描述 `TurnMode.InjectedLLM` 轮次下的响应注入链路。`TurnMode.NativeTakeover` 会在 `AbstractAssistantHook.handleCapturedQuery(...)` 提前返回，保留宿主原生回答路径，不进入这里的渲染注入流程。

## 共用前置条件

- 两个宿主都会先安装 response hooks，但实际是否拦截原生输出、是否消费 `LLMController.stream(query)`，取决于当前活跃轮次是否已经被写成 `InjectedLLM`。
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt` 会先完成 takeover 路由，再决定是否继续走 `dispatchQueryToLLM(...)`。
- 因此，“takeover 路由”是“render injection”之前的前置条件，不是注入链中的一个附属步骤。

## Breeno 渲染管线

### 注入模型

Breeno 走回答卡片层的**单卡片全量刷新**：

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt` 的 `dispatchQueryToLLM()` 直接消费 `LLMController.stream(query)`，并把累计文本交给 `renderStreamCard(...)`。
- 首帧创建 mock bean 后通过 `dataCenter.insertMessage()` 插入回答卡片。
- 后续分片和终帧通过 `dataCenter.updateMessage()` 持续刷新同一张卡片。
- `chunk` 传入的是累计文本，不是 delta。

### 前置捕获与原生阻断

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/CaptureInputHook.kt` 在宿主输入链路里捕获 query 与 roomId，同时缓存 `DataCenter` 实例。
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/BlockNativeCardHook.kt` 只在 `InjectedLLM` 模式下拦截原生回答卡片；`NativeTakeover` 或无活跃 turn 时直接放行。
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/SuppressCleanupHook.kt` 也只在 `InjectedLLM` 模式下把命中的清理操作替换为 `DoNothingOperation`，避免宿主把注入卡片清掉。

### 生命周期

- `BreenoChatHook` 用 `Mutex + currentRenderSession` 维持单个活跃渲染会话。
- `onTurnStateChanged(...)` 在 `NativeTakeover` 轮次会立即清空当前 render session，避免上一轮注入状态残留。
- `onSessionReset()` 会同时重置 `LLMController` 与当前 render session。
- 终帧会补做一次卡片刷新并恢复反馈区显示，然后清空当前会话。

## XiaoAi 渲染管线

### 注入模型

XiaoAi 走**响应目标捕获 + Instruction 分片注入**：

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt` 的 `dispatchQueryToLLM()` 先启动 `LLMController.stream(query)`，再用 `shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)` 预热共享流。
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/CaptureResponseTargetHook.kt` 捕获宿主响应目标后，`targetReady.await()` 才会放行后续消费。
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/RenderTextStreamCardHook.kt` 根据累计文本计算本次 `delta`，再构造宿主 `Instruction` 注入。

### 原生阻断

当前源码里实际安装的是：

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/BlockNativeInstructionByWhitelistHook.kt`：只在 `InjectedLLM` 模式下按白名单放行必要原生 `Instruction`，其余默认拦截。
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/BlockNativeTtsPlaybackHook.kt`：只在 `InjectedLLM` 模式下拦截原生 TTS 播放调用。

当前源码里没有单独的 `BlockNativeTextStreamHook` 或 `BlockNativeTtsStreamHook`；旧说法不再适用。

### 生命周期

- `RenderTextStreamCardHook` 内部用 `Mutex + currentSession` 维护单个 `XiaoaiRenderSession`。
- 若响应目标缺失，`RenderTextStreamCardHook.render()` 只会上报 `renderTargetMissing` 事件；终帧时会清空 session，但不会自动降级到别的注入路径。
- 终帧会额外注入 `XiaoaiConfigProvider.RenderTextStreamCard.finalChunkText`，随后清空 session。
- `XiaoaiChatHook.onSessionReset()` 会同时清掉响应目标、`targetReady` 与渲染 session。

## 原生 takeover 路径

- `TurnMode.NativeTakeover` 在 `AbstractAssistantHook.handleCapturedQuery(...)` 中就会调用 `LLMController.stopCurrentRound(keepCurrentTurn = false)` 并直接返回。
- Breeno 侧因此不会调用 `renderStreamCard(...)`，`BlockNativeCardHook` 与 `SuppressCleanupHook` 也会对当前轮次放行原生回答与原生清理逻辑。
- XiaoAi 侧因此不会继续消费共享流；`BlockNativeInstructionByWhitelistHook` 与 `BlockNativeTtsPlaybackHook` 也会对当前轮次放行原生 `Instruction` 与 TTS。

## 关键源码

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`

- `BreenoChatHook.kt`
- `BreenoConfigProvider.kt`
- `BreenoFeedbackAssembler.kt`
- `subhooks/BlockNativeCardHook.kt`
- `subhooks/CaptureInputHook.kt`
- `subhooks/ResetConversationSignalHook.kt`
- `subhooks/SuppressCleanupHook.kt`

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`

- `XiaoaiChatHook.kt`
- `XiaoaiConfigProvider.kt`
- `XiaoaiRenderSession.kt`
- `subhooks/BlockNativeInstructionByWhitelistHook.kt`
- `subhooks/BlockNativeTtsPlaybackHook.kt`
- `subhooks/CaptureInputHook.kt`
- `subhooks/CaptureResponseTargetHook.kt`
- `subhooks/RenderTextStreamCardHook.kt`
