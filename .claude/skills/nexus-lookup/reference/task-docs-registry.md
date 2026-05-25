# Task Docs Registry

## UI 与导航系列

### `UI-PRD.md`

- **主题**：定义 App Shell 的信息架构和页面流，涵盖 Startup、Home、Configure、Selection、Settings 树以及 Liquid Glass UI 方向。
- **状态**：`部分落地`
- **已落地**：`NexusApp`、主要页面 content、Liquid 基础组件、导航控制器、Settings 主页/详情页、自定义工具设置页。
- **仍需核对**：PRD 中的完整动效、极端状态、细粒度设置树覆盖情况。
- **源码核对入口**：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt`

### `apple-liquid-glass-philosophy.md`

- **主题**：Liquid Glass 视觉原则与组件设计参考。
- **状态**：`参考文档`
- **源码核对入口**：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt`

### `SESSION.md`

- **主题**：S3ss10n、LLM 和 MCP 网络请求相关记录。
- **状态**：`参考文档`
- **源码核对入口**：
  - `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/`
