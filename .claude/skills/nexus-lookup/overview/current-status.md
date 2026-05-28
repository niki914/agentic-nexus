# Current Status

## 已落地能力

- **基础架构与路由**：Xposed 入口与多宿主分发支持 `ColorOS / Breeno` 与 `HyperOS / XiaoAi`，位于 `app/` 与 `h/` 模块。
- **配置同步机制**：本地 Python 配置服务器按包名/版本号下发 WebSettings；`WEB_SETTINGS` 与 `LOCAL_SETTINGS` 均由 `XIpcStoreRepository` / `ConfigPersistence` 文件化持久化，宿主读取通过 `SettingsContentProvider.openFile()` 暴露的文件流完成，写入与 mutate 仍通过 provider call 分发。
- **Breeno 注入实现**：基于卡片层拦截原生回答，使用单卡片全量刷新模式渲染 LLM 输出。
- **XiaoAi 注入实现**：基于底层指令流与文字流拦截做增量文本分片注入，并拦截 TTS 流与原生播放。
- **LLM Runtime**：`agent-runtime` 模块中的 `LLMController` 持有单例 `Session` 与 runtime snapshot，支持配置刷新、流式请求、会话重置和统一事件映射。
- **HTTP MCP**：MCP server 配置解析、Session 注册、discovered tools cache 与 HTTP interceptor 已落地。
- **Builtin Tool**：`builtin_tool_flags` 由 `XRepo.builtinTools` 基于 `BuiltinToolRegistry` 解析并持久化，`ToolManager` 会将启用项解析为 `LocalTool.Builtin`、写入 prompt lines，并通过 `SessionToolBinder`、`ToolCallDispatcher`、`BuiltinToolExecutor` 打通执行链路；默认注册 `create_custom_tool`、`notify`、`run_command`。
- **CustomTool**：`custom_tools` 从 `LocalSettings` 解析到 `LocalTool.Custom`、local tool 注册、`ToolCallDispatcher` 与 `CustomToolExecutor` 执行链路已落地；可通过 builtin `create_custom_tool` 写回配置。
- **UI Shell 与导航**：`app` 模块中的 `NexusApp`、`NexusPages`、`NexusPage` 与 `composebase` 模块中的 `NavigationController`、`LiquidScreen` 壳层已落地，页面覆盖 Startup、ProviderPick、Configure、Done、Home、SettingsHome、SettingsDetail。
- **UI 基建组件**：`composebase` 模块中的 `LiquidScreenState`、`LiquidScreenSwipeContent`、`SettingsGroupCard`、`SettingNavigationItem`、`SettingToggleItem`、`SettingExpandableTextCard`、`SettingsToggleListItemCard`、`LiquidTextField`、`LiquidSecretTextField` 以及 liquid 交互层已落地，形成统一风格的 Compose 基建层。
- **UI 设置页能力**：`BuiltinToolsSettingsContent`、`McpSettingsContent`、`CustomToolsSettingsContent` 均已接入真实配置读写，不再只有自定义工具设置页落地。

## 半落地能力

- **UI 完整态**：Settings 分组模型已定义 ProviderModel、Network、Memory、BuiltinTools、ShellRules、Mcp、CustomTools、About，但默认可见分组目前只有 BuiltinTools、Mcp、CustomTools，未接入独立内容的分组仍是隐藏或占位状态。
- **DonePage**：`DonePage` 仍复用 `ConfigurePageContent`，未形成独立页面实现。

## 提案

- **多语言支持**：为 Breeno 分支提供多语言注入支持。

## 已知技术债、待重构点、重要 TODO

- `RenderTextStreamCardHook`：当前使用监视器锁且硬编码类名，需将类名上云并重构锁机制。
- `BaseConfigProvider` / `BreenoConfigProvider`：需引入 Dataclass 描述 hook spot，并将下游读参方式重构为 `suspend`。
- `AbstractAssistantHook`：需统一所有业务分支的 subhooks 命名、云 config 结构及生命周期方法名。
- `LLMController`：需补充前置配置检查与快速失败逻辑。
- `AppLaunchDecision`：当前已按 `onboardingCompleted` 与 `endpointPresent` 计算 `initialPage`，启动流与该决策保持一致；但 `StartupAssistantUi` 三个分支仍共用 `StartupPage`，尚未体现宿主差异化启动页。
