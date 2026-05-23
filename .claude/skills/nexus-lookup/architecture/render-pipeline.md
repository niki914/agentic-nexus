# Render Pipeline

本文件描述 Breeno 与 XiaoAi 两条响应注入链路的架构模型及关键差异。

## Breeno 渲染管线

**注入模型**：单卡片全量刷新
Breeno 的注入发生在卡片渲染层。每次接收到 LLM 增量内容时，直接用累计全量文本覆盖卡片的 `content`，并通过 `DataCenter.updateMessage` 持续刷新同一卡片对象。

**前置捕获点**
- **用户输入**：通过 `CaptureInputHook` 拦截 `DataCenter#insertMessage` 捕获 Query。
- **实例缓存**：同步缓存 `DataCenter` 实例，为后续卡片注入提供句柄。

**原生阻断机制**
- **卡片拦截**：在 `InjectedLLM` 模式下，`BlockNativeCardHook` 拦截 `DataCenter#insertMessage` 中的原生回答卡片。
- **防清理**：`SuppressCleanupHook` 将系统清理操作替换为 `DoNothingOperation`，避免注入卡片被意外移除。

**生命周期与并发**
- **并发控制**：`BreenoChatHook` 使用 `Mutex + currentRenderSession` 保证单个流式渲染会话的唯一性。
- **终帧处理**：收到终帧后，更新卡片状态反转反馈视图，并清空当前 Render Session。
- **会话清理**：`ResetConversationSignalHook` 监听宿主重置信号；浮窗 Detach 事件结合 700ms 阈值判断退出并触发重置。

**源码参考**
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/`

## XiaoAi 渲染管线

**注入模型**：增量文本分片注入
XiaoAi 注入点位于底层指令流层。框架根据累计文本计算每次的增量 `delta`，将其封装为宿主 `Instruction` 分片进行逐块投递，而非全量覆盖。

**前置捕获点**
- **用户输入**：通过 `CaptureInputHook` 捕获 Query。
- **目标捕获**：在响应目标创建链路中，由 `CaptureResponseTargetHook` 捕获文字流分片注入的 `Target` 对象。LLM 调度会在捕获完成后再消费共享流。

**原生阻断机制**
在 `InjectedLLM` 模式下，全面阻断冲突链路：
- **文字流**：`BlockNativeTextStreamHook` 屏蔽原生文本分片。
- **TTS 流**：`BlockNativeTtsStreamHook` 屏蔽原生 TTS 指令流。
- **播放层**：`BlockNativeTtsPlaybackHook` 屏蔽原生 TTS 播报。

**生命周期与并发**
- **并发控制**：`RenderTextStreamCardHook` 维护单个 `XiaoaiRenderSession` 处理分片。
- **终帧处理**：计算并注入 `renderTextStreamCardFinalChunkText`，完成后清空 Render Session。
- **会话清理**：`XiaoaiChatHook.onSessionReset` 清空当前 Dialog 状态与渲染 Session。

**源码参考**
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/`
