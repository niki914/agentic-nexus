# Render Pipeline

本文件描述 Breeno 与 XiaoAi 两条响应注入链路的当前实现差异。

## Breeno 渲染管线

### 注入模型

Breeno 走**单卡片全量刷新**：

- 首帧通过 `dataCenter.insertMessage()` 插入回答卡片。
- 后续分片和终帧通过 `dataCenter.updateMessage()` 持续刷新同一张卡片。
- `chunk` 传入的是累计文本，不是 delta。

### 前置捕获与阻断

- `CaptureInputHook`：在 `DataCenter#insertMessage` 前捕获 query，并缓存 `DataCenter` 实例。
- `BlockNativeCardHook`：在 `InjectedLLM` 模式下拦截原生回答卡片。
- `SuppressCleanupHook`：把命中的清理操作替换为 `DoNothingOperation`，避免注入卡片被宿主清掉。

### 生命周期

- `BreenoChatHook` 用 `Mutex + currentRenderSession` 维持单个活跃渲染会话。
- takeover 模式或 session reset 时会清空当前 render session。
- 终帧时会补做一次卡片刷新并恢复反馈区显示。

## XiaoAi 渲染管线

### 注入模型

XiaoAi 走**Instruction 分片注入**：

- `LLMController.stream(query)` 先被 `shareIn(scope, SharingStarted.Eagerly, replay = Int.MAX_VALUE)` 预热。
- `CaptureResponseTargetHook` 捕获宿主响应目标后，`RenderTextStreamCardHook` 才开始向目标注入分片。
- `RenderTextStreamCardHook` 根据累计文本计算本次 `delta`，再构造宿主 `Instruction` 注入。

### 原生阻断

当前源码里实际安装的是：

- `BlockNativeInstructionByWhitelistHook`：在 `InjectedLLM` 模式下，除白名单外默认拦截原生 `Instruction`。
- `BlockNativeTtsPlaybackHook`：拦截原生 TTS 播放调用。

当前源码里**未见**单独的 `BlockNativeTextStreamHook` 或 `BlockNativeTtsStreamHook` 类；旧说法不再成立。

### 生命周期

- `RenderTextStreamCardHook` 内部用 `Mutex + currentSession` 维护单个 `XiaoaiRenderSession`。
- 首帧为空时只记录日志，等待后续 delta。
- 终帧会额外注入 `XiaoaiConfigProvider.RenderTextStreamCard.finalChunkText`，随后清空 session。
- `XiaoaiChatHook.onSessionReset()` 会同时清掉响应目标与渲染 session。

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
- `subhooks/CaptureInputHook.kt`
- `subhooks/CaptureResponseTargetHook.kt`
- `subhooks/BlockNativeInstructionByWhitelistHook.kt`
- `subhooks/BlockNativeTtsPlaybackHook.kt`
- `subhooks/RenderTextStreamCardHook.kt`
