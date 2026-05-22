# UI Shell Domain

## 现实状态与目标状态

- **现实状态**：已基于 `ui.infra` 实现了 `NexusApp` 壳层，包含基于 `NavigationController` 的自定义栈式状态机，落地了 Onboarding 流程（Startup、ProviderPick、Configure、Done）、Home 聊天页骨架以及 Settings 树状导航骨架。
- **目标状态**：当前大多数页面由 mock 数据驱动，后续目标是接入真实配置持久化、真实 LLM 对话调度（`LLMController`）及 MCP 执行，并持续丰富 Settings 详情页。

## 基础设施与 PRD 对应关系

- **已落地的 UI 能力**：`LiquidScreen` 及状态管理、`LiquidScreenSwipeContent` 转场、`NavigationController` 导航机、`SettingsGroupCard`、`SettingsNavigationRow` 等通用组件。
- **待落地的业务能力**：真实的持久化逻辑、MCP 真实服务探测、本地自定义工具执行、复杂表单校验。
- **`UI-PRD.md` 解决的问题**：定义 App Shell 的视觉原则、用户场景、页面信息架构以及组件规范，指导页面流的设计。
- **`prd-nav-status-machine.md` 解决的问题**：定义统一导航模型，支持线性条件流（如 Startup Flow）和树状栈式流（如 Settings Tree），统一接管不同页面的跳转动画和系统返回键拦截逻辑。

## 阅读建议

- **基础设施**：阅读源码时，首先了解导航与壳层机制：
  - `NavigationController.kt`：理解自定义栈式状态机的工作原理。
  - `LiquidScreen.kt` 与 `LiquidScreenSwipeContent.kt`：理解玻璃态外壳渲染与页面内容转场逻辑。
- **页面入口**：业务开发入口从 `MainActivity.kt` 延伸：
  - `NexusApp.kt`：整个应用的 UI 组装与外层导航事件响应。
  - `NexusPages.kt`：各业务页面的内容分发与路由实现。

## 约束和边界

- **多语言约束**：禁止在 Kotlin 中硬编码 UI 字符串，必须使用 `@StringRes`。
- **视觉与 Liquid Glass 约束**：视觉风格以轻量、悬浮、玻璃态为主，避免厚重卡片和满屏实体背景。顶栏是稳定 chrome，不跟随页面局部布局重建。
- **NavigationController 约束**：不引入 Jetpack Navigation。各页面实现 `Page` 接口进行自描述，状态机仅负责栈操作，并由 `interceptBack` 控制系统返回键行为。

## 核心源码引用

- [UI-PRD.md](UI-PRD.md)
- [prd-nav-status-machine.md](prd-nav-status-machine.md)
- [MainActivity.kt](app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt)
- [NavigationController.kt](app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt)
- [NexusApp.kt](app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt)
- [NexusPages.kt](app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt)
- [LiquidScreen.kt](app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt)
