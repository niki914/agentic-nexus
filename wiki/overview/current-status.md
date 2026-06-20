# Current Status

> 本页只登记本轮已回到源码核对的现状。没有源码回指的能力，不在这里写成已实现。

## Stable

### Builtin tools

- `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt` 的默认注册表当前包含 9 个 builtin 实现：`CreateCustomToolBuiltin`、`LaunchAppBuiltin`、`MemorizeBuiltin`、`NotifyBuiltin`、`OpenUriBuiltin`、`ReadCustomToolBuiltin`、`TerminalBuiltin`、`SshTerminalBuiltin`、`SearchAppsBuiltin`。
- `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` 的 `BuiltinToolApi` 仍兼容旧 `run_command` 开关读取，但实际注册表并不是“2 个 builtin tools”，而是上面的 9 个默认实现。

### Store registry 与配置落盘模型

- `ipc/src/main/java/com/niki914/nexus/ipc/store/StoreDescriptorRegistry.kt` 当前注册了 11 个静态 store id：`web_settings`、`local_settings`、`agents.registry`、`agent.main.config`、`agent.main.memory`、`tools.builtin`、`tools.custom`、`tools.mcp.servers`、`rules.execution`、`rules.takeover`、`app.state`。
- 同一文件还支持两类动态 store：`agent.config.<agentId>` 会映射到 settings/agents/ 目录下按 agentId 命名的 config.json，`tools.mcp.cache.<serverId>` 会映射到 settings/tools/mcp/cache/ 目录下按 serverId 命名的 JSON 文件。
- `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` 的初始化逻辑会落盘 `agents.registry`、`agent.main.config`、`agent.main.memory`，说明 agent registry 已经不是纯文档概念。

### Agent registry 与会话历史页面

- `app/src/main/java/com/niki914/nexus/agentic/repo/AgentSettingsCodec.kt` 与 `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` 已实现 agent profile 的解析、默认 `main` agent 补全、`list/get/saveProfile/setEnabled`、以及按 `agentId` 读写 LLM 配置。
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/NexusPage.kt`、`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`、`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/route/HomePageRoute.kt` 证明 `ConversationHistoryPage` 已接入页面枚举、路由分发和 Home 入口。
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/route/ConversationHistoryPageRoute.kt` 会加载会话列表、处理选中、删除当前或历史会话；`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConversationHistoryPageContent.kt` 已实现 loading / empty / error / list / delete-confirmation；`app/src/main/java/com/niki914/nexus/agentic/app/conversation/ConversationRepo.kt` 已提供 `listConversations()`、`getConversation()`、`deleteConversation()` 等持久化能力。

## In Progress

### Multi-agent state

- 多 agent 的数据层已经部分落地：`agents.registry`、`agent.config.<agentId>`、`RuntimeAgentProfile`、`AgentMemoryMode`、以及 builtin / custom / MCP 的 `enabled_for_agents` 编解码，分别可在 `ipc/src/main/java/com/niki914/nexus/ipc/store/StoreDescriptorRegistry.kt`、`app/src/main/java/com/niki914/nexus/agentic/repo/AgentSettingsCodec.kt`、`app/src/main/java/com/niki914/nexus/agentic/repo/ToolSettingsCodec.kt`、`app/src/main/java/com/niki914/nexus/agentic/repo/McpSettingsCodec.kt` 中回指。
- 但 runtime 侧还不是完整的多 agent 执行链：`agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeSettingsGateway.kt` 只有 `readLlmConfig(agentId)` 带 `agentId`，`listMcpServers()`、`listCustomTools()`、`listBuiltinToolSettings()`、`listExecutionRules()` 仍是无参接口；`app/src/main/java/com/niki914/nexus/agentic/repo/XRepoRuntimeGateway.kt` 也按这个形状实现。
- `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` 当前直接调用 `gateway.readLlmConfig()` 的默认参数，并搭配上述无参列表接口刷新 runtime；因此“多 agent 已完整接通到运行时”目前没有源码证据，只能记为部分落地。

## Proposal

- 本轮核对范围内，没有新增到可以单独登记的 source-backed Proposal 条目。

## Unverified

- 宿主注入主链、Breeno / XiaoAi 渲染细节、设置页覆盖范围、MCP transport 细节，本轮没有为这次修文重新逐项回到源码核验，因此这里不再沿用旧页面中的笼统“已实现”表述。
