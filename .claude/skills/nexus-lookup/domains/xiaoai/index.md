# XiaoAi Domain

## 整体心智模型

XiaoAi 宿主（`com.miui.voiceassist`）的注入模型是**增量文本分片注入**，走的是响应目标与 Instruction 流层，其 Hook 点更为底层。由于是在指令分片级别进行操作，除了注入大模型的流式分片，还必须严格拦截与屏蔽原生的文字流、TTS 指令流及播报链路，以防止原生回复与注入回复交错冲突。

## 响应目标的捕获与复用

响应目标在 `CaptureResponseTargetHook` 处，于原生响应目标创建链路被捕获。`dispatchQueryToLLM()` 会先启动 `LLMController.stream(query)` 并通过 `shareIn(..., SharingStarted.Eagerly, replay = Int.MAX_VALUE)` 预热共享流，随后等待响应目标就绪后再开始 collect。在后续的文本流渲染阶段（`RenderTextStreamCardHook`），会直接复用已捕获的响应目标对象，而不是重新去查找目标对象，确保了注入指令能够准确路由到目标。

## 原生链路阻断

原生链路的阻断分布在以下关键 Hook 中：
- **原生文字流**：由 `BlockNativeTextStreamHook` 拦截，屏蔽与注入轮次冲突的原生文本分片。
- **原生 TTS 指令流**：由 `BlockNativeTtsStreamHook` 拦截，阻断下发给播报引擎的 TTS 指令。
- **原生 TTS 播放链路**：由 `BlockNativeTtsPlaybackHook` 拦截，屏蔽漏网的底层原生播报。

## 增量文本分片注入机制

注入阶段的关键状态和约束：
- 渲染前必须校验当前轮次是否仍为活跃的 `InjectedLLM` 轮次。
- 文本注入由 `RenderTextStreamCardHook` 维护当前活动的 `XiaoaiRenderSession` 进行，不是全局单例对象。
- 渲染逻辑是根据大模型返回的累计文本计算出本次增量 `delta`，对宿主执行的是分片注入，而非整段覆盖。
- 每个增量块会被包装为宿主可消费的 `Instruction` 对象。其中涉及的 `namespace`、`name`、`idPrefix` 等元数据，均通过 `XiaoaiConfigProvider` 提供。
- 在最后一帧终帧处理时，还会额外注入由配置定义的 `renderTextStreamCardFinalChunkText`（终帧补片文本），随后清空当前的渲染 session 状态。

## 关键源码导航

调试 XiaoAi 注入逻辑与查阅实现细节时，优先查看以下相对路径：
- **主入口与总线**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
- **配置提取与转换**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiConfigProvider.kt`
- **渲染会话状态**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiRenderSession.kt`
- **底层拦截与注入子 Hook**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/`
