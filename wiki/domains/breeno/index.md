# Breeno Domain

## 整体模型

Breeno 侧不是所有 query 都会进入 Nexus 注入。当前实际链路是：

1. `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/CaptureInputHook.kt` 捕获 query、roomId，并缓存后续渲染要用的 `DataCenter` 实例。
2. `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt` 用 takeover 规则先判定当前轮次属于 `InjectedLLM` 还是 `NativeTakeover`。
3. 只有 `InjectedLLM` 才会进入 Breeno 的回答卡片注入；`NativeTakeover` 会保留宿主原生回答路径。

## 当前两条路径

- `InjectedLLM`
  - `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt` 消费 `LLMController.stream(query)`。
  - 首帧创建 mock bean 后调用 `insertMessage()` 插入回答卡片。
  - 后续分片与终帧通过 `updateMessage()` 全量刷新同一张卡片。

- `NativeTakeover`
  - takeover 判定后，`AbstractAssistantHook.handleCapturedQuery(...)` 会调用 `LLMController.stopCurrentRound(keepCurrentTurn = false)` 并直接返回。
  - `BreenoChatHook.onTurnStateChanged(...)` 会清空当前 render session，避免把上一轮注入状态带进原生回答。
  - 原生回答卡片与宿主清理逻辑会继续走宿主自己的链路，不再由 Nexus 注入替换。

## 关键 Hook

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/`

- `AbstractAssistantHook.kt`：统一处理 query 捕获后的 takeover 分流、`ActiveTurnStore` 写入与 LLM 分发前置。

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`

- `BreenoChatHook.kt`：Breeno 主编排入口；安装 session / response / input hooks，维护当前 render session。
- `BreenoConfigProvider.kt`：读取云控类名、方法名与静态配置。
- `BreenoFeedbackAssembler.kt`：给注入卡片补反馈区数据。

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/`

- `CaptureInputHook.kt`：捕获 query 与 roomId，并缓存 `DataCenter` 实例。
- `BlockNativeCardHook.kt`：只在 `InjectedLLM` 模式下拦截原生回答卡片；`NativeTakeover` 与无活跃 turn 时放行。
- `SuppressCleanupHook.kt`：只在 `InjectedLLM` 模式下把命中的清理操作改成 `DoNothingOperation`。
- `ResetConversationSignalHook.kt`：命中宿主 reset 信号时触发 `onSessionReset()`。

## 渲染与生命周期

- `BreenoChatHook.renderStreamCard()` 首帧创建 mock bean，并调用 `insertMessage()`。
- 后续分片与终帧通过 `updateMessage()` 刷新同一 bean。
- `renderSessionMutex + currentRenderSession` 用于保证单个活跃渲染会话。
- `installFloatScreenDetachHooks()` 结合浮窗 detach 和 resume 做退出判定，触发 session reset。
- `onSessionReset()` 会重置 `LLMController` 并清空当前 render session。
- `dataCenterInstance` 依赖 `CaptureInputHook` 捕获；如果未捕获到，`insertMessage()` / `updateMessage()` 调用会因为空接收者而不产生渲染效果。

## 调试入口

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`

- `BreenoChatHook.kt`
- `BreenoConfigProvider.kt`
- `BreenoFeedbackAssembler.kt`
- `subhooks/`
