# Turn State

本文件描述当前源码里的轮次状态模型、takeover 语义与 reset 触发点。

## 状态模型

当前通用状态对象是 `ConversationTurnState(turnId, lastQuery, mode)`，定义在：

- `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt`

当前模型里**没有** `roomId` 字段；`roomId` 只在宿主 Hook 分发和渲染时作为参数向下传递。

## Query 捕获与分流

统一分流入口在：

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`

实际顺序是：

1. `installInputHooks()` 捕获用户输入后，调用 `handleCapturedQuery(roomId, query)`。
2. `ConversationTurnState().nextTurn(query, mode)` 生成新的 `turnId`。
3. `shouldTakeOver(query)` 为 `true` 时进入 `TurnMode.NativeTakeover`；否则进入 `TurnMode.InjectedLLM`。
4. `ActiveTurnStore.setCurrent(nextTurnState)` 写入当前活跃轮次。
5. `onTurnStateChanged(nextTurnState)` 让宿主侧补充清理逻辑。
6. takeover 模式下调用 `LLMController.stopCurrentRound(keepCurrentTurn = false)` 后直接返回。
7. 注入模式下调用 `dispatchQueryToLLM(turnId, roomId, query)`。

## TurnId 生成

`turnId` 由 `ConversationTurnState.kt` 内部的 `TurnIdGenerator` 生成：

- 实现：`AtomicLong(System.currentTimeMillis())`
- 更新：`maxOf(previous + 1L, System.currentTimeMillis())`

该实现保证 `turnId` 单调递增，避免并发输入或极短时间连续输入导致的乱序。

## Reset 语义

通用 reset 行为在 `AbstractAssistantHook.onSessionReset()`：

- 清空 `ActiveTurnStore`

宿主侧会在 override 中补充额外清理：

- `BreenoChatHook.onSessionReset()`：额外调用 `LLMController.resetConversation()`，并清空当前 `BreenoRenderSession`。
- `XiaoaiChatHook.onSessionReset()`：额外调用 `LLMController.resetConversation()`，重置 `capturedResponseTarget`、`targetReady` 与 `RenderTextStreamCardHook` 的当前 session。

## 宿主触发点

- **Breeno**
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/ResetConversationSignalHook.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt` 的 `installFloatScreenDetachHooks()` 组合浮窗 detach 与页面 resume 做退出判定。

- **XiaoAi**
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt` 的 `installFloatScreenDetachHooks()` 组合浮窗 detach 与页面 resume 做退出判定。
  - 当前没有独立的 `ConversationJournal` 或额外 turn store 文件。

## 关键源码

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`

- `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt`
- `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ActiveTurnStore.kt`
- `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/TurnMode.kt`

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/`

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
