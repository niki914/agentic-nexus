# UI Shell Domain

## 结构边界

UI Shell 当前明确拆成两层：

- 页面与业务状态在 `app/`
- 导航、壳层、交互与通用组件在 `composebase/`

当前实现使用自定义导航栈，不依赖 Jetpack Navigation。

## 当前页面流

### `app/src/main/java/com/niki914/nexus/agentic/app/`

- `MainActivity.kt`：解析 `StartupAssistantUi`，调用 `AppLaunchDecision.resolve()`，再进入 `NexusApp`。
- `EXT.kt`：根据 `OsUtils.getCurr()` 解析 `StartupAssistantUi`，并选择优先宿主包名。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/`

- `NexusApp.kt`：持有导航栈与 page chrome。
- `NexusPages.kt`：页面分发；`StartupPage` 点击继续后，`Breeno` / `XiaoAi` 进入 `ProviderPickPage`，`ChatOnly` 进入 `HomePage`。
- `PageChrome.kt`：页面 chrome 注入点。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/`

- `NexusPage.kt`：定义 `StartupPage`、`ProviderPickPage`、`ConfigurePage`、`DonePage`、`HomePage`、`SettingsHomePage`、`SettingsDetailPage`、`McpServerDetailPage`、`CustomToolDetailPage`。
- `NexusSettingsGroup.kt`：当前分组为 `ModelConfig`、`Memory`、`BuiltinTools`、`CustomShellTools`、`Mcp`、`ExecutionRules`、`About`。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/`

- `StartupAssistantUi.kt`：当前宿主枚举为 `Breeno`、`XiaoAi`、`ChatOnly`。
- `AppLaunchDecision.kt`：当前只根据 `onboardingCompleted` 决定 `StartupPage` 或 `HomePage`；不再检查 `endpointPresent`。
- `SettingsState.kt`：所有 settings group 当前默认都可见；不再隐藏大部分分组。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/`

- `StartupPageContent.kt`
- `ConfigurePageContent.kt`
- `DonePageContent.kt`：当前是独立完成页实现，不再复用配置页内容。
- `SettingsHomePageContent.kt`
- `SettingsDetailPageContent.kt`：`ModelConfig`、`BuiltinTools`、`CustomShellTools`、`Mcp` 走真实内容；`Memory`、`ExecutionRules`、`About` 仍走 placeholder。
- `BuiltinToolsSettingsContent.kt`：builtin tool 列表与启用开关。
- `CustomToolsSettingsContent.kt`：custom tool 列表与启用开关。
- `mcp/McpSettingsContent.kt`：MCP server 列表与启用开关。
- `CustomToolDetailContent.kt`：custom tool 详情编辑页。
- `mcp/McpServerDetailContent.kt`：MCP server 详情编辑页。

## 基建层

### `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`

- `LiquidScreen.kt`
- `LiquidScreenState.kt`
- `LiquidScreenSwipeContent.kt`
- `LiquidViewportAvoidance.kt`
- `nav/NavigationController.kt`
- `interaction/`
- `shape/G2Shapes.kt`

### `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/`

- `LiquidButton.kt`
- `LiquidTextField.kt`
- `LiquidSecretTextField.kt`
- `LiquidToggle.kt`
- `SettingsGroupCard.kt`
- `SettingNavigationItem.kt`
- `SettingExpandableTextItem.kt`
- `SettingExpandableTextCard.kt`
- `SettingToggleItem.kt`
- `SettingsToggleListItemCard.kt`
- `SettingsDetailFormScaffold.kt`
- `SettingsListPageContent.kt`

## 当前边界

- 不要把 UI Shell 理解成只有页面层；导航、page chrome、交互层和组件层已明确下沉到 `composebase/`。
- 不要把 settings 全部视为已完成；当前只有 `ModelConfig`、`BuiltinTools`、`CustomShellTools`、`Mcp` 有真实内容，其余分组仍是占位。
- 不要把宿主差异理解成初始页差异；当前差异发生在 `StartupPage` 的继续按钮逻辑，而不是 `AppLaunchDecision`。
