# Current Status

## 已实现

- **双宿主注入主链**：`Entrance` 已按 `HostApp.fromPackageName()` 路由到 `BreenoChatHook` 或 `XiaoaiChatHook`，并通过 `RuntimeBootstrap.installIfNeeded()` 安装业务 Hook。
- **配置读取与落盘**：主 App 与宿主进程都会初始化 `XRepo`；宿主入口通过 `XRepo.web.await()` 读取 `web_settings.json`，本地设置经 `XRepo` / `XIpcBridge` / `SettingsContentProvider` / `XIpcStoreRepository` 落盘到 `local_settings.json`。
- **Breeno 注入链**：已落地 query 捕获、原生回答卡片拦截、清理抑制、单卡片全量刷新渲染、会话 reset。
- **XiaoAi 注入链**：已落地 query 捕获、响应目标捕获、Instruction 白名单拦截、TTS 播放拦截、增量文本分片注入、终帧补片与渲染 session 清理。
- **LLM runtime**：`LLMController` 已落地 session 复用、配置 refresh、Prompt 组装、local tool 调度、MCP tools refresh、流式事件映射。
- **Builtin / Custom / MCP**：三类工具的配置解析、启用状态、执行链与缓存写回均已接通。
- **UI Shell 主链**：已落地 `StartupPage`、`ProviderPickPage`、`ConfigurePage`、`DonePage`、`HomePage`、`SettingsHomePage`、`SettingsDetailPage`，并接入 `LiquidScreen` 自定义导航壳层。
- **设置页真实内容**：`ModelConfig`、`Memory`、`BuiltinTools`、`CustomShellTools`、`Mcp`、`Takeover`、`ExecutionRules`、`About` 已有独立内容；`Memory`、`Takeover`、`ExecutionRules` 接入 `XRepo` 读写，`McpServerDetailContent`、`CustomToolDetailContent`、`TakeoverRuleDetailContent` 与 `ExecutionRuleDetailContent` 已是可编辑详情页。

## 部分实现

- **启动分流**：`AppLaunchDecision.resolve()` 目前只根据 `onboardingCompleted` 决定 `StartupPage` 或 `HomePage`；宿主差异分流发生在 `StartupPage` 的继续按钮逻辑里，不在初始页决策阶段。
- **命令执行安全**：`run_command` 与 custom tool 都有基础安全策略、超时控制和可配置 `ExecutionRules`，但当前仍不是完整沙箱或审批体系。

## 未见实现

- **独立的宿主差异化启动页**：当前没有针对 `Breeno` / `XiaoAi` 的独立 startup 页面实现。
- **stdio / 本地进程型 MCP transport**：当前 `McpServerDefinition` 只有 `Http`。

## 提案或技术债

- **多语言注入支持**：Breeno 多语言支持仍停留在提案层。
- **Breeno 云控建模**：`BreenoConfigProvider` 仍有 dataclass 化与 `suspend` 化重构 TODO。
- **通用 Hook 抽象收敛**：`AbstractAssistantHook` 仍保留统一 subhook / config / 方法命名的 TODO。
- **XiaoAi 等待目标就绪**：`XiaoaiChatHook.dispatchQueryToLLM()` 仍存在 `targetReady.await()` 的死等风险注释。
