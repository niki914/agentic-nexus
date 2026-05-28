# UI Shell Domain

## 现状

- UI Shell 对应 `app/src/main/java/com/niki914/nexus/agentic/app/ui/`，范围不止页面，还包括导航、顶栏 chrome、交互反馈、形状和表单组件。
- 当前实现使用 Compose 自定义栈，不引入 Jetpack Navigation；`NexusPage` 负责声明标题、按钮和 blur layer，栈行为由 `NavigationController` 统一管理。
- 当前已落地 onboarding 主链路、Home/Settings 壳层、Builtin/MCP/CustomTools 列表页；部分 settings 分组和详情编辑仍是占位或空壳。

## `app/src/main/java/com/niki914/nexus/agentic/app/`

- `MainActivity.kt`：解析 `StartupAssistantUi`，调用 `AppLaunchDecision.resolve()`，再进入 `NexusApp`。

## `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/`

- `StartupAssistantUi.kt`：宿主分为 `Breeno`、`XiaoAi`、`ChatOnly`。
- `AppLaunchDecision.kt`：`onboardingCompleted && endpointPresent` 时直接进入 `HomePage()`；否则进入 `StartupPage`。
- `AppLaunchDecision.kt`：三个 `StartupAssistantUi` 分支当前都会把 `startupPage` 解析为 `StartupPage`；宿主分流不发生在初始页判定阶段。
- `SettingsState.kt`：settings 声明 3 个 section、8 个分组：`ProviderModel`、`Network`、`Memory`、`BuiltinTools`、`ShellRules`、`Mcp`、`CustomTools`、`About`；默认可见分组只有 `BuiltinTools`、`Mcp`、`CustomTools`。

## `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/`

- `NexusPage.kt`：当前页面模型包括 `StartupPage`、`ProviderPickPage`、`ConfigurePage`、`DonePage`、`HomePage`、`SettingsHomePage`、`SettingsDetailPage`、`McpServerDetailPage`、`CustomToolDetailPage`。
- `NexusSettingsGroup.kt`：分组 title、summary 和 route suffix 在这里集中定义。

## `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/`

- `NexusApp.kt`：`NexusApp` 持有导航栈，页面内容通过 `LiquidScreen` + `LiquidScreenSwipeContent` 装配；顶栏是稳定 chrome，不跟随页面局部布局重建。
- `NexusPages.kt`：`NexusPageContent` 负责页面分发。
- `NexusPages.kt`：`StartupPage` 点击继续后，`Breeno` / `XiaoAi` 进入 `ProviderPickPage`，`ChatOnly` 直接进入 `HomePage()`。
- `NexusPages.kt`：`DonePage` 仍复用 `ConfigurePageContent`，不是独立页面实现。

## `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/`

- `SettingsHomePageContent.kt`：按 `SettingsSectionUiState` 渲染 section。
- `SettingsDetailPageContent.kt`：只有 `BuiltinTools`、`Mcp`、`CustomTools` 走真实内容分支；其他分组当前只会落到占位内容。
- `BuiltinToolsSettingsContent.kt`：已支持 builtin tool 列表加载与开关写回。
- `McpSettingsContent.kt`：已支持 MCP server 列表展示、启用开关和跳转详情页。
- `CustomToolsSettingsContent.kt`：已支持 `custom_tools` 列表读取、启用开关写回和跳转详情页。
- `McpServerDetailContent.kt`、`CustomToolDetailContent.kt`：当前仍是空内容壳层。

## `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`

- `LiquidScreen.kt`、`LiquidScreenState.kt`、`LiquidScreenSwipeContent.kt`：负责 action bar、标题方向切换、blur layer、viewport avoidance 和页面转场。
- `nav/NavigationController.kt`：提供 `push` / `pop` / `resetTo` 的自定义栈管理，不使用路由图。
- `component/`：已集中 `LiquidButton`、`LiquidTextField`、`LiquidSecretTextField`、`LiquidToggle`、`SettingsGroupCard`、`SettingNavigationItem`、`SettingExpandableTextItem`、`SettingExpandableTextCard`、`SettingsToggleListItemCard`。
- `interaction/`、`shape/G2Shapes.kt`：承载液态按压、拖拽变形、高亮、手势检测和形状定义。

## 边界

- 不要把 UI Shell 理解成“只有页面”；当前源码已拆成页面层、导航层、壳层、交互层和组件层。
- 不要把 `McpServerDetailPage`、`CustomToolDetailPage` 误判为已完成 CRUD；当前只落地了列表页入口、部分动作位和详情页路由，详情内容本身仍未实现。
- 不要把宿主分流理解成初始页差异；当前三种宿主都会先进入 `StartupPage`，分流发生在继续按钮逻辑里。
