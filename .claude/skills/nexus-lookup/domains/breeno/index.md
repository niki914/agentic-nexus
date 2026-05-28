# Breeno Domain

## 整体心智模型

Breeno 的宿主注入模型是**“单卡片全量刷新”**。Breeno 的响应注入与拦截都发生在**回答卡片层（DataCenter / UI 层）**，hook 点相对较高。模型输出通过单次插入后，使用全量文本不断刷新同一条回答卡片来呈现增量流式效果。

## 关键 Hook 列表与各自职责

- **`CaptureInputHook`**
  - **触发点**: `DataCenter#insertMessage` 执行前。
  - **职责**: 拦截用户输入并将其传递给 `AbstractAssistantHook` 捕获 Query，同时缓存当前的 `DataCenter` 实例供后续注入卡片时使用。
- **`BlockNativeCardHook`**
  - **触发点**: `DataCenter#insertMessage` 执行前。
  - **职责**: 在 `InjectedLLM` 模式下，拦截原生 Breeno 的回答卡片。
- **`SuppressCleanupHook`**
  - **触发点**: 由 `BreenoConfigProvider.SuppressCleanup.hookTarget` 指向的宿主清理链路。
  - **职责**: 将命中的清理操作替换为 `DoNothingOperation`，避免系统因为判定异常而移除被注入或被拦截的卡片。
- **`ResetConversationSignalHook`**
  - **触发点**: 宿主会话重置方法调用时。
  - **职责**: 监听宿主新会话创建或会话切换，触发重置当前 LLM session 状态。

## 数据流向与生命周期联动

- **用户 query 捕获**: 发生在卡片层的 `CaptureInputHook`。
- **响应注入**: 发生在 `BreenoChatHook#renderStreamCard()`。首帧通过 `dataCenter.insertMessage` 插入，后续分片和终帧均通过 `dataCenter.updateMessage` 对该卡片进行全量刷新。
- **原生卡片拦截**: 发生在卡片层的 `BlockNativeCardHook`。
- **session reset 与生命周期的联动点**: 在 `BreenoChatHook.installSessionHooks()` 中安装了浮窗 View 的 `detach` 监听并跟踪目标 Activity 的 `onResume()` 生命周期。若浮窗 detach 后 700ms 内未回到目标页面，判定用户已离开，则触发 `onSessionReset()` 清理当前回合。

## 调试 Breeno 问题的相对路径

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt` (流式渲染卡片与流程控制)
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoConfigProvider.kt` (云控类名、方法名与 Mock 静态配置)
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoFeedbackAssembler.kt` (卡片反馈按钮组装)
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/` (所有的 Breeno 注入与拦截 Hook 逻辑)
