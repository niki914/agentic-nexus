# Turn State

本文件描述 Nexus 的通用会话状态模型，以及 query 捕获、takeover、reset、turnId 的关键语义。

## 状态模型定义

核心会话状态模型为 `ConversationTurnState(roomId, turnId, lastQuery, mode)`。

其中 `mode` 决定了当前轮次的行为模式，仅包含两种状态：
- **`NativeTakeover`**：完全接管当前回答轮次，不注入大模型结果。
- **`InjectedLLM`**：拦截原生响应，注入本地配置的 LLM 流式回答。

## Query 捕获与分流

用户输入的 Query 捕获后，统一收口于 `AbstractAssistantHook.handleCapturedQuery(roomId, query)`，该方法负责进行路由分流：
- 内部调用 `shouldTakeOver(query)` 判定是否匹配接管关键词。
- 若返回 `true`，当前轮次状态标记为 `NativeTakeover`，随后调用 `onTakeoverTriggered()` 并直接返回。
- 否则，当前轮次进入 `InjectedLLM` 模式，随后进入 `dispatchQueryToLLM()`，交由 `LLMController` 发起流式请求。

## TurnId 生成机制

`turnId` 由 `TurnIdGenerator` 负责生成。为了确保流式响应片段拼接与渲染顺序的严格正确性，生成机制采用 `AtomicLong + maxOf(previous + 1, System.currentTimeMillis())`。该算法保证分配的 `turnId` 严格单调递增，杜绝由于并发或极短时间内的输入导致的乱序问题。

## 会话重置 (Reset)

会话重置的核心逻辑位于 `onSessionReset(roomId)`，执行时会清理上一房间的 `ConversationJournal` 并重置 `turnState`。触发时机由不同宿主的业务 Hook 决定，主要场景包括：
- **宿主原生重置点**：如 Breeno 和 XiaoAi 的 `ResetSessionHook` 探测到新会话创建或历史会话切换。
- **生命周期联动 (Breeno)**：监听浮窗 owner 的 detach Hook 与目标 Activity 的 `onResume()`；若浮窗 detach 后 700ms 内未回到目标页面，则判定为退出，触发重置。

## 源码引用

- 会话状态模型定义：[ConversationTurnState.kt](app/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt)
- 会话记录清理：[ConversationJournal.kt](app/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt)
- 捕获分流与重置入口：[AbstractAssistantHook.kt](app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt)
