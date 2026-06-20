# XiaoAi Domain

## 整体模型

XiaoAi 侧同样先做 takeover 分流，再决定是否进入 Nexus 注入：

1. `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/CaptureInputHook.kt` 捕获 query。
2. `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt` 根据 takeover 规则先写入当前轮次模式。
3. 只有 `InjectedLLM` 才会继续走“响应目标捕获 + Instruction 分片注入”；`NativeTakeover` 会保留宿主原生 `Instruction` 与 TTS 路径。

## 当前两条路径

- `InjectedLLM`
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt` 先启动 `LLMController.stream(query)`，再用 `shareIn(..., SharingStarted.Eagerly, replay = Int.MAX_VALUE)` 预热共享流。
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/CaptureResponseTargetHook.kt` 捕获宿主响应目标后，`targetReady.await()` 才会放行后续消费。
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/RenderTextStreamCardHook.kt` 计算 delta、构造宿主 `Instruction` 并完成注入。

- `NativeTakeover`
  - takeover 判定后，`AbstractAssistantHook.handleCapturedQuery(...)` 会调用 `LLMController.stopCurrentRound(keepCurrentTurn = false)` 并直接返回。
  - 当前轮次不会继续消费共享流，也不会进入 `renderStreamCard(...)`。
  - 原生 `Instruction` 与原生 TTS 会继续由宿主自己处理。

## 关键 Hook

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/`

- `AbstractAssistantHook.kt`：统一处理 query 捕获后的 takeover 分流、`ActiveTurnStore` 写入与 LLM 分发前置。

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`

- `XiaoaiChatHook.kt`：XiaoAi 主编排入口；安装输入、响应、生命周期 subhook，并协调目标捕获与流式消费。
- `XiaoaiConfigProvider.kt`：提供响应目标、白名单拦截、文字流注入、TTS 播放拦截所需的运行时类名和方法名。
- `XiaoaiRenderSession.kt`：记录当前渲染轮次的 session 状态。

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/`

- `CaptureInputHook.kt`：捕获 query。
- `CaptureResponseTargetHook.kt`：捕获后续注入用的响应目标对象。
- `BlockNativeInstructionByWhitelistHook.kt`：只在 `InjectedLLM` 模式下按白名单放行原生 `Instruction`，其余默认拦截。
- `BlockNativeTtsPlaybackHook.kt`：只在 `InjectedLLM` 模式下拦截原生 TTS 播放。
- `RenderTextStreamCardHook.kt`：计算 delta、构造宿主 `Instruction`、注入分片并管理当前 `XiaoaiRenderSession`。

## 当前数据流

1. `dispatchQueryToLLM()` 启动共享流并预热。
2. 等待 `CaptureResponseTargetHook` 完成响应目标捕获。
3. `collectAsChunk` 消费共享流，把累计文本交给 `RenderTextStreamCardHook.render()`。
4. `RenderTextStreamCardHook` 根据上次已渲染全文计算增量 `delta`。
5. 终帧额外注入 `finalChunkText`，随后清空当前 session。

## 边界与注意点

- 当前源码里没有单独的 `BlockNativeTextStreamHook` 或 `BlockNativeTtsStreamHook`；原生阻断已经收敛为 `Instruction` 白名单拦截加 TTS 播放拦截。
- `targetReady.await()` 依赖 `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/CaptureResponseTargetHook.kt` 先完成目标捕获；当前源码没有超时或降级分支，目标捕获失败时不会自动切到其他注入路径。
- `RenderTextStreamCardHook` 只维护一个活跃 `XiaoaiRenderSession`，session reset 时会被显式清空。
- `XiaoaiChatHook.onSessionReset()` 会同时重置 `LLMController`、清掉响应目标、重建 `targetReady` 并清空渲染 session。
- `XiaoaiChatHook.renderStreamCard()` 遇到非活跃 injected turn 会直接返回。

## 调试入口

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiConfigProvider.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiRenderSession.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/`
