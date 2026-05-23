# UI Shell Domain

## 现实状态与目标状态

- **现实状态**：已基于 `app/src/main/java/com/niki914/nexus/agentic/app/ui/` 实现 `NexusApp` 壳层，包含基于 `NavigationController` 的自定义栈式状态机，落地了 Startup、Home、Configure、Selection、Settings 树状导航和命令工具设置页。
- **目标状态**：继续核对 `UI-PRD.md` 中的完整动效、极端状态、细粒度设置树覆盖情况，并把配置持久化与运行时能力接入更多设置页面。

## 基础设施与 PRD 对应关系

- **已落地的 UI 能力**：`LiquidScreen` 及状态管理、`LiquidScreenSwipeContent` 转场、`NavigationController` 导航机、`SettingsGroupCard`、`SettingsNavigationRow`、`CustomToolsSettingsContent` 等组件与页面。
- **已落地的业务能力**：命令工具设置页已读写 `command_tools` 配置，运行时侧已有 `CommandToolExecutor` 执行链路。
- **半落地的业务能力**：非 command 类型 custom tools 与 builtin tool flags 仍以配置解析和 prompt 暴露为主。
- **`UI-PRD.md` 解决的问题**：定义 App Shell 的视觉原则、用户场景、页面信息架构以及组件规范，指导页面流的设计。

## 阅读建议

- **基础设施**：阅读源码时，首先了解导航与壳层机制：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt`
- **页面入口**：业务开发入口从 `MainActivity` 延伸：
  - `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/`

## 约束和边界

- **多语言约束**：禁止在 Kotlin 中硬编码 UI 字符串，必须使用 `@StringRes`。
- **视觉与 Liquid Glass 约束**：视觉风格以轻量、悬浮、玻璃态为主，避免厚重卡片和满屏实体背景。顶栏是稳定 chrome，不跟随页面局部布局重建。
- **NavigationController 约束**：不引入 Jetpack Navigation。各页面实现 `Page` 接口进行自描述，状态机仅负责栈操作，并由 `interceptBack` 控制系统返回键行为。

## 核心源码引用

- `UI-PRD.md`
- `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`
