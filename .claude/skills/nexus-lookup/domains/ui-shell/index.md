# UI Shell Domain

## 当前实现

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/` 已形成一套独立于 Jetpack Navigation 的 Compose UI Shell：`NexusApp` 持有导航栈，`NexusPages` 负责页面装配，页面模型由 `NexusPage` 与 `NexusSettingsGroup` 定义。
- 已落地页面包括 Startup、ProviderPick、Configure、Done、Home、SettingsHome、SettingsDetail。设置详情页当前会按 group 分发到 Builtin Tools、MCP、Custom Tools 三类真实页面。
- `MainActivity` 会先解析 `StartupAssistantUi` 与 `AppLaunchDecision`，再进入 `NexusApp`。当前源码中的 `AppLaunchDecision` 仍被临时返回固定到 `StartupPage`，因此启动流现状与目标态不同。

## 已落地基建

- 顶层壳层由 `LiquidScreen`、`LiquidScreenState`、`LiquidScreenSwipeContent` 组成，负责稳定的 action bar、标题切换方向、blur layer 与页面转场。
- 导航基建位于 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/`，核心是 `NavigationController`、`Navigator`、`Page`、`PageViewModel`，使用自定义栈管理而不是路由图。
- 统一风格组件已集中在 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/`，包括按钮、文本框、密码框、Toggle、设置项卡片、导航行，以及 `ExpandableLiquidTextFieldCard` 这类可复用表单组件。
- 交互风格基建位于 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/`，包含液态按压/拖拽变形、高亮与手势检测；形状定义集中在 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt`。

## 已落地业务页

- Onboarding 流程已具备 Startup、ProviderPick、Configure、Done 的页面骨架与表单交互。
- Settings Home 已支持按 section 渲染可见分组；Settings Detail 已接入 `BuiltinToolsSettingsContent`、`McpSettingsContent`、`CustomToolsSettingsContent`。
- `CustomToolsSettingsContent` 已形成 `custom_tools` 配置读写闭环；`McpSettingsContent` 已支持列表、编辑、新增、删除；`BuiltinToolsSettingsContent` 已支持 builtin tool 开关读写。

## 半落地与边界

- `NexusSettingsGroup` 已定义 ProviderModel、Network、Memory、BuiltinTools、ShellRules、Mcp、CustomTools、About 八类分组，但默认可见分组目前只有 BuiltinTools、Mcp、CustomTools，其余分组仍未在设置首页暴露。
- `SettingsDetailPageContent` 对未单独实现的分组仍返回占位内容，因此 settings tree 目前不是“全量完成”，而是“部分页面已落地 + 部分分组隐藏或占位”。
- `DonePage` 当前仍通过复用 `ConfigurePageContent` 渲染，不是独立页面实现。
- 当前 UI 开发约束仍以 `ASC_may_25.md` 的导航收敛结论和字符串资源约束为准。

## 阅读建议

- 启动入口与初始页面判定：
  - `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/AppLaunchDecision.kt`
- 页面壳层与导航装配：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/`
- Liquid 基建与统一交互风格：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/`

## 约束和边界

- 禁止把 UI Shell 理解成“只有页面”。当前源码已经把导航、顶栏状态、交互反馈、形状与表单组件拆成独立基建层。
- 不引入 Jetpack Navigation。页面只实现 `Page` 契约，自描述标题、按钮显隐和 blur layer，栈行为由 `NavigationController` 统一负责。
- 顶栏属于稳定 chrome，不跟随页面局部布局重建；标题方向与转场方向由 `LiquidScreenState` 和 `LiquidScreenSwipeContent` 协同维护。

## 核心源码引用

- `ASC_may_25.md`
- `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/AppLaunchDecision.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/SettingsDetailPageContent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/BuiltinToolsSettingsContent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/McpSettingsContent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/ExpandableLiquidTextFieldCard.kt`
