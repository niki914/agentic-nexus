# LLM Runtime

## 组件职责边界

### LLMController
- **源码已落地**
- **负责**：作为流式调用的全局门面，持有唯一的 `Session` 实例与 `RuntimeState`（snapshot）。编排 `refresh`（配置拉取）、`stream`（流式请求）和 `resetConversation`（会话重置）。负责将底层的 `SessionEvent` 映射为项目内的 `LlmStreamEvent`。
- **不负责**：不直接解析配置组装 Prompt（委托给 `PromptComposer`）；不直接解析 Tool/MCP 的原始配置（委托给 `ToolManager`）；不负责格式化工具事件的宿主展示文本（委托给 `ToolEventFormatter`）。

### PromptComposer
- **源码已落地**
- **职责**：动态组合 `systemPrompt`。通过 `compose` 方法，将 `baseSystemPrompt`、`memorySections`、`toolSections` 和 `runtimeSections` 按照特定顺序拼接，输出包含各区块结构的 `PromptComposeResult`。

### ToolManager
- **源码已落地**
- **职责**：从 `LocalSettings` 提取并解析 Builtin Tools、Custom Tools 以及 MCP Servers 的配置，产出 `ResolvedTools` 对象。同时负责生成提示 LLM 当前可用工具的 prompt lines。

### ToolEventFormatter
- **源码已落地**
- **职责**：将结构化的工具运行事件（如 `ToolRunning`、`ToolSucceeded` 等 `LlmStreamEvent`）转化为宿主（Breeno / XiaoAi）可以直接展示的最小文本，并根据宿主特性区分 `AppendOnly` 和 `ReplaceStatus` 两种渲染模式。

## 流式事件映射

`SessionEvent`（底层 SDK 事件）与项目内的 `LlmStreamEvent` 的映射关系在 `LLMController` 中严格落地：

- `SessionEvent.RoundStarted` -> `LlmStreamEvent.RoundStarted`
- `SessionEvent.TextDelta` -> `LlmStreamEvent.TextDelta`（计算累计文本和 `charsPerSecond`）
- `SessionEvent.ToolRunning` -> `LlmStreamEvent.ToolRunning`（包装为 `ToolCallStatus`）
- `SessionEvent.ToolSucceeded` -> `LlmStreamEvent.ToolSucceeded`
- `SessionEvent.ToolFailed` -> `LlmStreamEvent.ToolFailed`
- `SessionEvent.Error` -> `LlmStreamEvent.Error`
- `SessionEvent.RoundCompleted` -> `LlmStreamEvent.Completed`

## Tool/MCP 落地状态

- **MCP 能力（已落地）**：HTTP 类型的 MCP Server 已经完全接入。`ToolManager` 会解析配置，`LLMController` 会将其真实注册到 `Session` 的 MCP DSL 中。
- **Local Tool 能力（半落地 / 仅 Snapshot）**：内置工具（Builtin Tools）与自定义工具（Custom Tools）在 `ToolManager` 中已经完成配置解析，但**尚未接入执行器**。当前在 `LLMController` 的 Session 初始化闭包中，遇到 `ToolCallKind.Local` 会抛出异常（`has no executor yet`）。

## 关键源码入口

- `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolEventFormatter.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt`
