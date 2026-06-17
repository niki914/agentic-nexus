# Current Status

## 已实现

- **双宿主注入主链**：`app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt` 会先初始化 `XRepo` 与 `RuntimeEnvironment`，再按 `HostApp.fromPackageName()` 路由到 `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt` 或 `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`，并通过 `RuntimeBootstrap.installIfNeeded()` 安装业务 Hook。
- **宿主侧 takeover 分流**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt` 在捕获 query 后，会用 `XRepo.takeoverRules.list()` 和 `app/src/main/java/com/niki914/nexus/agentic/takeover/TakeoverResolver.kt` 决定 `TurnMode.InjectedLLM` 或 `TurnMode.NativeTakeover`；命中原生接管时会停止当前 LLM 轮次，不再继续分发到 `LLMController`。
- **多 store 设置模型**：设置已不再只靠 `web_settings.json` / `local_settings.json` 两个文件。`ipc/src/main/java/com/niki914/nexus/ipc/store/StoreDescriptorRegistry.kt` 当前把配置拆成 `settings/hooks.json`、`settings/agents/main/config.json`、`settings/agents/main/memory.json`、`settings/tools/builtin_tools.json`、`settings/tools/custom_tools.json`、`settings/tools/mcp/servers.json`、`settings/rules/execution_rules.json`、`settings/rules/takeover_rules.json`、`settings/app_state.json`，以及按 server 动态生成的 `settings/tools/mcp/cache/<serverId>.json`。
- **配置读写链**：主 App 与宿主进程都会初始化 `XRepo`；`XRepo.web.await()` 仍负责 hook 配置读取，但底层持久化已由 `ipc/src/main/java/com/niki914/nexus/ipc/store/ConfigPersistence.kt`、`ipc/src/main/java/com/niki914/nexus/ipc/store/XIpcStoreRepository.kt`、`ipc/src/main/java/com/niki914/nexus/ipc/cp/SettingsContentProvider.kt` 提供多 store 原子读写与 IPC。
- **Breeno 注入链**：已落地 query 捕获、原生回答卡片拦截、清理抑制、单卡片全量刷新渲染、会话 reset。
- **XiaoAi 注入链**：已落地 query 捕获、响应目标捕获、Instruction 白名单拦截、TTS 播放拦截、增量文本分片注入、终帧补片与渲染 session 清理。
- **LLM runtime**：`agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` 已接通 session 复用、配置 refresh、Prompt 组装、local tool 调度、MCP tools refresh、流式事件映射。
- **Builtin / Custom / MCP**：三类工具的配置解析、启用状态、执行链与缓存写回均已接通。builtin 中当前终端工具名是 `terminal` 与 `ssh_terminal`，注册入口在 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt`，旧的 `run_command` 只作为 builtin 开关兼容别名保留在 `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`。
- **终端会话能力**：`agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/TerminalBuiltin.kt` 提供本地 Android 终端会话；`agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/SshTerminalBuiltin.kt` 提供交互式 SSH 会话；两者共用 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/TerminalSessionPool.kt` 管理 opaque session handle、异步执行与交互输出收集。
- **UI Shell 主链**：已落地 `StartupPage`、`ProviderPickPage`、`ConfigurePage`、`DonePage`、`HomePage`、`SettingsHomePage`、`SettingsDetailPage`，并接入 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt` 自定义导航壳层。
- **设置页真实内容**：`ModelConfig`、`Memory`、`BuiltinTools`、`CustomShellTools`、`Mcp`、`Takeover`、`ExecutionRules`、`About` 都有独立内容。`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/SettingsDetailPageContent.kt` 已把这些分组分发到真实内容页；`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/EditableSettingsDetailScaffold.kt`、`composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsDetailFormScaffold.kt` 与 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt` 已构成设置列表页和详情编辑页骨架。

## 部分实现

- **启动分流**：`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/AppLaunchDecision.kt` 目前只根据 `onboardingCompleted` 决定 `StartupPage` 或 `HomePage`；宿主差异分流发生在 `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt` 的继续按钮逻辑里，不在初始页决策阶段。
- **命令执行安全**：`terminal` 和 custom tool 都接入了 `ShellCommandSafetyPolicy`、超时控制与 `ExecutionRules`，但当前仍不是完整沙箱或审批体系。
- **runtime settings 门面**：`app/src/main/java/com/niki914/nexus/agentic/repo/XRepoRuntimeGateway.kt` 已向 runtime 暴露 LLM、memory、builtin、custom、MCP、execution rules，但 `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeSettingsGateway.kt` 里还没有 takeover rules 的独立访问接口，宿主 Hook 仍直接读 `XRepo.takeoverRules`。

## 未见实现

- **独立的宿主差异化启动页**：当前没有针对 `Breeno` / `XiaoAi` 的独立 startup 页面实现。
- **stdio / 本地进程型 MCP transport**：当前 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ToolManager.kt` 只构造 `McpServerDefinition.Http`。
- **SSH 私钥认证**：`agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/SshTerminalBuiltin.kt` 明确拒绝 `private_key_pem` / `passphrase`，当前只支持密码认证。

## 提案或技术债

- **多语言注入支持**：Breeno 多语言支持仍停留在提案层。
- **Breeno 云控建模**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoConfigProvider.kt` 仍有 dataclass 化与 `suspend` 化重构 TODO。
- **通用 Hook 抽象收敛**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt` 仍保留统一 subhook / config / 方法命名的 TODO。
- **XiaoAi 等待目标就绪**：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt` 仍存在 `targetReady.await()` 的死等风险注释。
