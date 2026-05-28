# LLM Runtime

## 包结构

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`

- `LLMController.kt`: 运行时入口；持有唯一 `Session`，负责 refresh、stream、resetConversation
- `LlmModels.kt`: `LlmRuntimeSnapshot`、`ResolvedTools`、`LocalTool`、`McpServerDefinition`
- `LlmStreamEvent.kt`: 项目内统一流式事件模型

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/`

- `PromptComposer.kt`: 组装 `system`、`memory_*`、`tool_*`、`runtime_*` 区块
- `ToolManager.kt`: 解析 builtin/custom/MCP 配置，产出 `ResolvedTools` 与 prompt lines
- `SessionToolBinder.kt`: 把 local tools 和 MCP servers 绑定到 `SessionConfig.Builder`
- `ToolCallDispatcher.kt`: 分发 local tool 调用
- `buildin/`: builtin tool 注册、设置读写、执行器与具体实现；目录名当前就是 `buildin`
- `custom/`: custom tool 设置写入与运行时执行
- `mcp/`: HTTP MCP discovery 拦截与缓存持久化
- `shell/`: shell 安全策略与命令执行器
- `stream/`: `SessionEvent` -> `LlmStreamEvent` 映射与工具事件文案格式化

## 主链

### Refresh

- `LLMController.refresh()`: 从 `XRepo` 读取 `llm`、`mcp`、`customTools`、`builtinTools`
- `ToolManager.resolve()`: 配置 -> `ResolvedTools`，并生成 prompt lines
- `PromptComposer.compose()`: base prompt、memory、tool、runtime 区块 -> 最终 `systemPrompt`
- `SessionToolBinder.bindTools()`: builtin/custom local tools 与 MCP servers -> `SessionConfig.Builder`
- `Session.refreshMcpTools()`: 首次建会话或 MCP 指纹变化时重新发现工具
- `mcp/McpInterceptorHttpEngine.kt` + `mcp/McpDiscoveryCacheStore.kt`: 持久化 discovered tools cache
- 失败服务：`LLMController.refresh()` 会清理对应 MCP cache，并重新 resolve/bind

### Stream

- `LLMController.stream()`: 每次优先尝试 `refreshIfPossibleFromHookContext()`；refresh 失败时退回最近一次 `runtimeState`，再把底层回调交给 `stream/LlmStreamEventMapper.kt`
- `LlmStreamEventMapper.kt`: 已覆盖 `RoundStarted`、`TextDelta`、`ToolRunning`、`ToolSucceeded`、`ToolFailed`、`Error`、`RoundCompleted`
- `TextDelta`: 计算 `charsPerSecond`
- `stream/ToolEventFormatter.kt`: 负责工具事件宿主文案；支持 `AppendOnly`、`ReplaceStatus`

## 组件边界

### `LLMController.kt`

- 已落地：运行时总入口、唯一 `Session`、最近一次 `LlmRuntimeSnapshot`
- 负责：refresh、stream、resetConversation、MCP refresh 时机控制、local tool hook 分发
- 不负责：不直接解析 repo 原始配置；不直接执行 builtin/custom 具体逻辑

### `PromptComposer.kt`

- 已落地：输出 `PromptComposeResult`
- 区块：`system`、`memory_*`、`tool_*`、`runtime_*`
- 边界：`runtimeSections` 入口已留出，但 `LLMController.buildRuntimeSections()` 当前返回空列表

### `ToolManager.kt`

- 已落地：统一解析三类工具配置
- 产物：`ResolvedTools.builtinTools`、`ResolvedTools.customTools`、`ResolvedTools.mcpServers`、`ResolvedTools.promptLines`

### `SessionToolBinder.kt`

- 已落地：builtin/custom 注册为 local tools；MCP server 注册到 `mcp {}`
- 边界：builtin 会注入完整 schema；custom 只注册 `name + description`，执行时仍走固定命令

## 能力状态

### Builtin

- 执行链：`XRepo.builtinTools.list()` -> `ToolManager.buildBuiltinTools()` -> `ResolvedTools.builtinTools` -> `SessionToolBinder.localTools` -> `ToolCallDispatcher` -> `buildin/BuiltinToolExecutor.kt` -> 具体 builtin
- 已注册：`create_custom_tool`、`notify`、`run_command`
- 现状：不是“只有 flags 没执行链路”；执行器、注册表、设置管理器都已落地
- 边界：`run_command` 已可执行，但实现类仍是 `RunCommandBuildin_WIP_SAFE`；当前不是完整沙箱或审批体系

### Custom

- 写入链：`custom/CustomToolManager.kt` 做名称校验、保留名冲突校验、基础 shell 安全校验，再写入 `XRepo.customTools`
- 执行链：`ToolManager.buildCustomTools()` -> `ResolvedTools.customTools` -> `SessionToolBinder.localTools` -> `ToolCallDispatcher` -> `custom/CustomToolExecutor.kt`
- 边界：只执行配置里的固定 `command`；tool call 的 `argumentsJson` 不参与命令模板展开

### MCP

- 已落地：HTTP MCP server 解析、注册、`tools/list` 拦截、discovered tools cache 持久化、失败服务缓存清理
- 模型边界：`McpServerDefinition` 当前只有 `Http`
- 未见能力：stdio / 本地进程型 MCP transport 不在当前源码里

### Stream

- 已落地：`SessionEvent` -> `LlmStreamEvent` 映射、文本流吞吐统计、tool 状态事件、宿主展示格式化
- 边界：`LlmStreamEvent.kt` 的 `chunk` / `fullText` 对 tool 事件仍是占位文本；真实宿主展示细节依赖 `stream/ToolEventFormatter.kt`

## Shell 安全链

- 结构：分成“写入前拦截”和“执行前拦截”，不是只在运行时检查一次

### Custom 写入前

- `custom/CustomToolManager.validate()`: 校验名称格式、保留 builtin 名冲突、重复名称
- `shell/ShellCommandSafetyPolicy.evaluate()`: 同阶段拦截高风险命令
- `XRepo.customTools`: 仅在通过校验后写入

### Custom 执行前

- `ToolCallDispatcher.executeLocalTool()`: 命中 `LocalTool.Custom` 后进入 `custom/CustomToolExecutor.kt`
- `CustomToolExecutor.kt`: 再次调用 `ShellCommandSafetyPolicy.evaluate()`
- `shell/ShellCommandRunner.kt`: 通过后用 `ProcessBuilder("/system/bin/sh", "-c", command)` 执行
- 默认：超时 10 秒，`stderr` 合并进 `stdout`，返回 JSON 字符串

### `run_command` 执行前

- `buildin/impl/RunCommandBuildin_WIP_SAFE.kt`: 解析 `command`、`workdir`、`timeout_ms`、`merge_stderr`
- `shell/ShellCommandSafetyPolicy.evaluate()`: 执行前同样拦截
- `shell/ShellCommandRunner.kt`: 通过后执行；默认工作目录是 Android 设备上的 `/`

### Safety Policy

- `shell/ShellCommandSafetyPolicy.kt`: 做 shell-like token 拆分、大小写归一化、引号与转义处理
- 递归 payload：覆盖 `sh -c`、`bash -c`、`mksh -c`、`eval`
- 明确拦截：`reboot`、`su`、`setprop`、`dd`、`rm -rf`、`pm uninstall`、`cmd package uninstall`
- 边界：当前只是基础黑名单策略，不是完整隔离环境；不提供权限审批、路径白名单或参数级细粒度约束

## 关键源码入口

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`

- `LLMController.kt`
- `LlmModels.kt`
- `LlmStreamEvent.kt`
- `agentic/PromptComposer.kt`
- `agentic/ToolManager.kt`
- `agentic/SessionToolBinder.kt`
- `agentic/ToolCallDispatcher.kt`

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/`

- `BuiltinToolExecutor.kt`
- `BuiltinToolRegistry.kt`
- `BuiltinToolSettingsManager.kt`
- `impl/CreateCustomToolBuiltin.kt`
- `impl/NotifyBuiltin.kt`
- `impl/RunCommandBuildin_WIP_SAFE.kt`

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/`

- `CustomToolManager.kt`
- `CustomToolExecutor.kt`

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/`

- `McpDiscoveryCacheStore.kt`
- `McpInterceptorHttpEngine.kt`

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/`

- `ShellCommandSafetyPolicy.kt`
- `ShellCommandRunner.kt`

## `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/`

- `LlmStreamEventMapper.kt`
- `ToolEventFormatter.kt`
