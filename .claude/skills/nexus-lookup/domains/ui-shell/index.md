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
- `NexusPages.kt`：按 `NexusPage` 分发到 route 层；`StartupPage` 点击继续后，`Breeno` / `XiaoAi` 进入 `ProviderPickPage`，`ChatOnly` 进入 `HomePage`。
- `PageChrome.kt`：页面 chrome 注入点。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/route/`

- `StartupPageRoute.kt`、`ProviderPickPageRoute.kt`、`ConfigurePageRoute.kt`、`DonePageRoute.kt`、`HomePageRoute.kt`：主页面路由装配。
- `SettingsHomePageRoute.kt`、`SettingsDetailPageRoute.kt`：设置页路由装配。
- `McpServerDetailRoute.kt`、`CustomToolDetailRoute.kt`、`TakeoverRuleDetailRoute.kt`、`ExecutionRuleDetailRoute.kt`：详情页路由装配。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/`

- `NexusPage.kt`：定义主流程页面、设置页面，以及 `McpServerDetailPage`、`CustomToolDetailPage`、`TakeoverRuleDetailPage`、`ExecutionRuleDetailPage`。
- `NexusSettingsGroup.kt`：当前分组为 `ModelConfig`、`Memory`、`BuiltinTools`、`CustomShellTools`、`Mcp`、`Takeover`、`ExecutionRules`、`About`。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/`

- `StartupAssistantUi.kt`：当前宿主枚举为 `Breeno`、`XiaoAi`、`ChatOnly`。
- `AppLaunchDecision.kt`：当前只根据 `onboardingCompleted` 决定 `StartupPage` 或 `HomePage`；不再检查 `endpointPresent`。
- `SettingsState.kt`：所有 settings group 当前默认都可见；不再隐藏大部分分组。
- `MemorySettingsState.kt`：读取、新增、编辑、删除 `XRepo.memory` 中的记忆条目。
- `TakeoverSettingsState.kt`：维护 takeover 规则列表、启停、校验、编辑与删除。
- `ExecutionRulesSettingsState.kt`：维护 ExecutionRules 列表、启用模式、编辑与删除。
- `AboutSettingsState.kt`：提供 About 页展示状态。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/`

- `StartupPageContent.kt`
- `ConfigurePageContent.kt`
- `DonePageContent.kt`：当前是独立完成页实现，不再复用配置页内容。
- `SettingsHomePageContent.kt`
- `SettingsDetailPageContent.kt`：`ModelConfig`、`BuiltinTools`、`CustomShellTools`、`Mcp`、`About`、`Memory`、`Takeover`、`ExecutionRules` 均有独立分发；未知分组才落到 `TODOPageContent`。
- `ModelConfigSettingsContent.kt`：复用配置页表单作为 settings 内的 ModelConfig 编辑入口，并处理未保存退出确认。
- `MemorySettingsContent.kt`：Memory 列表与编辑弹窗。
- `TakeoverSettingsContent.kt`：takeover 规则列表、启用开关与详情入口。
- `TakeoverRuleDetailContent.kt`：takeover 规则详情编辑页，支持名称、目标、启用状态和 patterns。
- `ExecutionRulesSettingsContent.kt`：ExecutionRules 列表、启用开关与详情入口。
- `ExecutionRuleDetailContent.kt`：ExecutionRules 详情编辑页，支持名称、启用模式和 patterns。
- `AboutSettingsContent.kt`：About 页内容与外链展示。
- `BuiltinToolsSettingsContent.kt`：builtin tool 列表与启用开关。
- `CustomToolsSettingsContent.kt`：custom tool 列表与启用开关。
- `mcp/McpSettingsContent.kt`：MCP server 列表与启用开关。
- `CustomToolDetailContent.kt`：custom tool 详情编辑页。
- `mcp/McpServerDetailContent.kt`：MCP server 详情编辑页。

## 基建层

### `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`

- `ConfirmationLiquidDialog.kt`
- `LiquidDialog.kt`
- `LiquidScreen.kt`
- `LiquidScreenContentContext.kt`
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
- `SettingsItemSurface.kt`
- `SettingsListItem.kt`
- `SwipeDismissSettingsItemCard.kt`
- `SettingsToggleListItemCard.kt`
- `SettingsDetailFormScaffold.kt`
- `SettingsSegmentedSelector.kt`
- `SettingsListPageContent.kt`

## 当前边界

- 不要把 UI Shell 理解成只有页面层；导航、page chrome、交互层和组件层已明确下沉到 `composebase/`。
- 不要把 settings 全部视为已完成；`Memory`、`Takeover`、`ExecutionRules`、`About` 已有真实内容，但命令执行隔离仍不是完整沙箱或审批体系。
- 不要把宿主差异理解成初始页差异；当前差异发生在 `StartupPage` 的继续按钮逻辑，而不是 `AppLaunchDecision`。
