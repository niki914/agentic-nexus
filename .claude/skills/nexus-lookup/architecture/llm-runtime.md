# LLM Runtime

本文档只记录当前源码已经存在的 LLM runtime 事实，不把设计意图写成实现。

## 运行时主链

### Refresh

`LLMController.refresh()` 的当前顺序是：

1. 从 `RuntimeEnvironment.awaitSettingsGateway()` 读取 LLM、builtin、custom、MCP 配置。
2. `ToolManager.resolve()` 解析出 `ResolvedTools` 与 prompt lines。
3. 先把不含 runtime prompt 的配置写入 `SessionConfig.Builder`。
4. 当 session 首次创建、provider 变化，或 MCP 指纹变化时，调用 `Session.refreshMcpTools()`。
5. `McpDiscoveryCacheStore.onToolsDiscovered()` 把发现到的 tools 写回本地 settings cache。
6. `PromptComposer.compose()` 把 `system`、`memory_*`、`tool_*`、`runtime_*` 区块拼成最终 `systemPrompt`。
7. 再次 `applyRuntimeConfig()`，写入最终 prompt 与最新 tool 绑定。

### Stream

`LLMController.stream(query)` 的当前顺序是：

1. 先尝试 `refreshIfPossibleFromHookContext()`。
2. refresh 失败时回退到最近一次 `runtimeState`；若仍为空，则发出 `LlmStreamEvent.Error`。
3. `session.send(query)` 的底层事件交给 `LlmStreamEventMapper.map()`。
4. 文本流累计全文并计算 `charsPerSecond`。
5. tool 相关事件交给宿主层按需用 `ToolEventFormatter` 转成展示文案。

## 组件边界

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`

- `LLMController.kt`：总入口；维护单例 `Session`、最近一次 `LlmRuntimeSnapshot`、MCP 刷新时机与 stream 主链。
- `LlmModels.kt`：`ResolvedLlmConfig`、`ResolvedTools`、`LocalTool`、`McpServerDefinition` 等运行时模型。
- `LlmStreamEvent.kt`：对宿主暴露的统一流式事件模型。
- `ConversationTurnState.kt`：turn 状态与 `turnId` 生成。
- `ActiveTurnStore.kt`：当前注入轮次状态存储。

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/`

- `PromptComposer.kt`：把 `system`、`memory_*`、`tool_*`、`runtime_*` 区块拼成最终 prompt；当前 `runtimeSections` 已用于写入 MCP discovery 状态行，不是空实现。
- `ToolManager.kt`：解析 builtin、custom、MCP 配置，产出 `ResolvedTools`。
- `SessionToolBinder.kt`：把 local tools 与 MCP servers 绑定到 `SessionConfig.Builder`。
- `ToolCallDispatcher.kt`：在运行时分发 builtin 和 custom local tool 调用。

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/`

- `BuiltinToolRegistry.kt`：当前默认注册 `create_custom_tool`、`notify`、`run_command`。
- `BuiltinToolExecutor.kt`：builtin 执行入口。
- `BuiltinToolSettingsManager.kt`：builtin 设置读写辅助。
- `impl/CreateCustomToolBuiltin.kt`
- `impl/NotifyBuiltin.kt`
- `impl/RunCommandBuildin_WIP_SAFE.kt`

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/`

- `CustomToolManager.kt`：custom tool 的校验、保存、删除、启停。
- `CustomToolExecutor.kt`：执行固定 shell command；不把 `argumentsJson` 展开进命令模板。

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/`

- `McpDiscoveryCacheStore.kt`：把 discovered tools 写回本地缓存。

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/`

- `ShellCommandSafetyPolicy.kt`：基础危险命令拦截。
- `ShellCommandRunner.kt`：通过 `/system/bin/sh` 执行命令，带超时和 stdout/stderr 处理。

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/`

- `LlmStreamEventMapper.kt`：`SessionEvent` -> `LlmStreamEvent`。
- `ToolEventFormatter.kt`：tool 事件文案格式化，支持 `AppendOnly` 与 `ReplaceStatus`。

## 能力状态

### Builtin

- 已实现：注册表、启用状态读取、执行分发。
- 当前默认 builtin：`create_custom_tool`、`notify`、`run_command`。
- 边界：`run_command` 运行在 Android 设备 `/system/bin/sh` 环境，不是桌面 shell；安全策略仍是基础黑名单拦截。

### Custom

- 已实现：名称校验、保留名校验、基础命令安全校验、保存、删除、启停、执行。
- 边界：执行的始终是配置里的固定 `command`；tool call 入参不会参与命令模板展开。

### MCP

- 已实现：HTTP MCP server 绑定、tools discovery、discovered tools 本地缓存写回、按 server 指纹控制 refresh。
- 边界：当前 `McpServerDefinition` 只有 `Http`；源码里未见 stdio / 本地进程 transport。

### Stream

- 已实现：`RoundStarted`、`TextDelta`、`ToolRunning`、`ToolSucceeded`、`ToolFailed`、`Error`、`Completed` 映射。
- 边界：tool 展示文本由 `ToolEventFormatter` 生成；`LlmStreamEvent` 自身只承载结构化状态。

## 关键源码

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`

- `LLMController.kt`
- `LlmModels.kt`
- `LlmStreamEvent.kt`
- `ConversationTurnState.kt`
- `ActiveTurnStore.kt`

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/`

- `PromptComposer.kt`
- `ToolManager.kt`
- `SessionToolBinder.kt`
- `ToolCallDispatcher.kt`
- `buildin/BuiltinToolRegistry.kt`
- `buildin/BuiltinToolExecutor.kt`
- `buildin/BuiltinToolSettingsManager.kt`
- `buildin/impl/CreateCustomToolBuiltin.kt`
- `buildin/impl/NotifyBuiltin.kt`
- `buildin/impl/RunCommandBuildin_WIP_SAFE.kt`
- `custom/CustomToolManager.kt`
- `custom/CustomToolExecutor.kt`
- `mcp/McpDiscoveryCacheStore.kt`
- `shell/ShellCommandSafetyPolicy.kt`
- `shell/ShellCommandRunner.kt`
- `stream/LlmStreamEventMapper.kt`
- `stream/ToolEventFormatter.kt`
