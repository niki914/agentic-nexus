# LLM Runtime

## 组件职责边界

### LLMController
- **源码已落地**
- **负责**：作为流式调用的全局门面，持有唯一的 `Session` 实例与 `RuntimeState` snapshot。编排 `refresh`、`stream`、`resetConversation`，并通过 `LlmStreamEventMapper` 将底层 `SessionEvent` 映射为项目内的 `LlmStreamEvent`。
- **不负责**：不直接解析配置组装 Prompt；不直接解析 Tool/MCP 原始配置；不直接执行具体 `CustomTool` 命令。

### PromptComposer
- **源码已落地**
- **职责**：组合 `systemPrompt`，按 `baseSystemPrompt`、memory、tool、runtime 区块产出 `PromptComposeResult`。

### ToolManager
- **源码已落地**
- **职责**：从 `LocalSettings` 解析 builtin tool flags、`custom_tools`、MCP servers，产出 `ResolvedTools`，并生成提示 LLM 当前可用工具的 prompt lines。

### SessionToolBinder
- **源码已落地**
- **职责**：把 `ResolvedTools` 绑定到 `SessionConfig.Builder`，注册 local tools、HTTP MCP servers，并使用 discovered tools cache 改善 MCP 冷启体验。

### ToolCallDispatcher / CustomToolExecutor
- **源码已落地**
- **职责**：在 `ToolCallKind.Local` 回调中查找 `LocalTool.Custom`，通过 `/system/bin/sh -c` 执行 `custom_tools` 配置的命令，并将成功或失败结果序列化为 JSON 字符串。

### ToolEventFormatter
- **源码已落地**
- **职责**：将结构化工具运行事件转成宿主可展示文本，并根据宿主渲染特性区分 `AppendOnly` 和 `ReplaceStatus`。

## 流式事件映射

`SessionEvent` 与项目内 `LlmStreamEvent` 的映射由 `LlmStreamEventMapper` 负责：

- `SessionEvent.RoundStarted` -> `LlmStreamEvent.RoundStarted`
- `SessionEvent.TextDelta` -> `LlmStreamEvent.TextDelta`（计算累计文本和 `charsPerSecond`）
- `SessionEvent.ToolRunning` -> `LlmStreamEvent.ToolRunning`
- `SessionEvent.ToolSucceeded` -> `LlmStreamEvent.ToolSucceeded`
- `SessionEvent.ToolFailed` -> `LlmStreamEvent.ToolFailed`
- `SessionEvent.Error` -> `LlmStreamEvent.Error`
- `SessionEvent.RoundCompleted` -> `LlmStreamEvent.Completed`

## Tool/MCP 落地状态

- **HTTP MCP（已落地）**：`ToolManager` 解析 MCP server 配置，`SessionToolBinder` 注册到 `Session`，`McpInterceptorHttpEngine` 与 `McpDiscoveryCacheStore` 维护 discovered tools cache。
- **CustomTool（已落地）**：`LocalSettings.customTools` -> `ToolManager.buildCustomTools()` -> `ResolvedTools.customTools` -> `SessionToolBinder.localTools` -> `ToolCallDispatcher` -> `CustomToolExecutor`；当前模型只支持固定 `command` 执行方式。
- **Builtin Tool Flags（半落地）**：配置解析和 prompt lines 已存在，未看到独立 builtin 执行链路。

## 关键源码入口

- `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/CustomToolExecutor.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/McpDiscoveryCacheStore.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/McpInterceptorHttpEngine.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/LlmStreamEventMapper.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolEventFormatter.kt`
