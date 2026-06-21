# ColorOS / Breeno Domain

## 概览

Breeno 模块是 Nexus 的核心宿主实现之一。其核心心智是**复用同一张原生回答卡片，并通过反射不断全量覆盖文本内容以模拟流式输出**。

## 注入入口与路由

- `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt` 作为 Xposed 的加载入口，会过滤包名并调用 `HostApp.fromPackageName`。
- 如果命中 Breeno 宿主，则实例化 `BreenoChatHook`。
- `Entrance` 在启动时会通过 `HookLocalSettings.update` 和 `XRepo.web.await()` 拉取云端最新配置和白名单。
- `RuntimeBootstrap.installIfNeeded` 将 `BreenoChatHook` 挂载到运行期。

## 生命周期与接管

Breeno 的接管流程由 `AbstractAssistantHook` 模板编排，由 `BreenoChatHook` 具体实现：

### 1. 输入捕获 (`CaptureInputHook`)
- 监听 Breeno 的输入相关方法。
- 同时通过回调将 `dataCenterInstance`（Breeno 的数据中心单例）截获保存，用于后续手动插卡和更新卡片。
- 输入文本传递给 `AbstractAssistantHook.handleCapturedQuery` 进行接管判定。

### 2. 接管判定 (`TakeoverResolver`)
- 查询经过 `TakeoverResolver.resolve` 匹配规则。
- 如果判定为 `NativeTakeover`（原生接管），调用 `clearRenderSession` 结束本次注入，不阻拦 Breeno 自身逻辑。
- 如果判定为 `InjectedLLM`（LLM 接管），则记录 `TurnMode` 并进入大模型流式请求。

### 3. 原生卡片阻断 (`BlockNativeCardHook`)
- 对于被接管的 query，`BlockNativeCardHook` 会根据 `selfInjectedFlagKey` 标记，拦截 Breeno 原生下发的回答卡片，防止原生结果覆盖注入内容。
- `SuppressCleanupHook` 负责阻止 Breeno 的某些清理逻辑，防止卡片被提前回收。
- `ResetConversationSignalHook` 监听重置信号，在适当的时机清理会话上下文。

### 4. 渲染注入 (`BreenoChatHook.renderStreamCard`)
Breeno 的渲染模式是**全量刷新**，不是增量 append：
- **首包 (isFirst)**：
  - 反射实例化 `viewBeanClass`。
  - 设置 `chatType`、`roomId`、`recordId` 等上下文标识。
  - 通过 `BreenoFeedbackAssembler.attachIfNeeded` 组装点赞等反馈组件。
  - 通过 `dataCenterInsertMessageMethod` 将这张伪造的卡片插入到 Breeno 的消息流中。
- **后续包**：
  - 通过 `setContentMethod` 将**从头累加到当前的全量 chunk** 写入 Bean。
  - 标记 `isFinal` 等状态。
  - 通过 `dataCenterUpdateMessageMethod` 触发 Breeno 刷新卡片。
- **结束包 (isFinal)**：
  - 清理本地的 `BreenoRenderSession`，释放指针。

## 关键模型

- **`BreenoRenderSession`**：维护当前对话回合的 UI 句柄，包含 `turnId`、`recordId` 和实际的 `bean` 实例。
- **`dataCenterInstance`**：Breeno 内部用于管理消息列表的中心对象，通过拦截首个请求获取，后续伪造的卡片插入和更新全靠反射调用它。