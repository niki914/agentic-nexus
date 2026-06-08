# Breeno Domain

## 整体模型

Breeno 侧当前走**回答卡片层注入**：先插入一张回答卡片，再用累计文本持续刷新同一张卡片，形成流式输出效果。

## 关键 Hook

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`

- `BreenoChatHook.kt`：主编排入口；安装 session / response / input hooks，维护当前 render session。
- `BreenoConfigProvider.kt`：读取云控类名、方法名与静态配置。
- `BreenoFeedbackAssembler.kt`：给注入卡片补反馈区数据。

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/`

- `CaptureInputHook.kt`：在 `DataCenter#insertMessage` 前捕获 query，并缓存 `DataCenter` 实例。
- `BlockNativeCardHook.kt`：在 `InjectedLLM` 模式下拦截原生回答卡片。
- `SuppressCleanupHook.kt`：把命中的清理操作改成 `DoNothingOperation`。
- `ResetConversationSignalHook.kt`：命中宿主 reset 信号时触发 `onSessionReset()`。

## 渲染与生命周期

- `BreenoChatHook.renderStreamCard()` 首帧创建 mock bean，并调用 `insertMessage()`。
- 后续分片与终帧通过 `updateMessage()` 刷新同一 bean。
- `renderSessionMutex + currentRenderSession` 用于保证单个活跃渲染会话。
- `installFloatScreenDetachHooks()` 结合浮窗 detach 和 resume 做退出判定，触发 session reset。
- takeover 轮次会立即清理当前 render session，不继续注入 LLM 文本。
- `dataCenterInstance` 依赖 `CaptureInputHook` 捕获；如果未捕获到，`insertMessage()` / `updateMessage()` 调用会因为空接收者而不产生渲染效果。

## 调试入口

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`

- `BreenoChatHook.kt`
- `BreenoConfigProvider.kt`
- `BreenoFeedbackAssembler.kt`
- `subhooks/`
