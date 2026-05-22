# Task Docs Registry

## 目的

本文件用于登记任务文档、PRD、设计稿，并说明它们与当前源码实现之间的关系。

## 注册列表

### UI 与导航系列

**[UI PRD: Nexus App Shell v1](UI-PRD.md)**
- **主题**：定义 App Shell 的信息架构和页面流，涵盖从冷启（Startup/Onboarding）到 Home，再到 Settings 树的 UI 规范与状态模型。
- **状态**：`部分落地`
- **源码落地**：部分
- **源码核对入口**：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/feat/`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`

**[PRD: Navigation State Machine](prd-nav-status-machine.md)**
- **主题**：设计了一套轻量级导航状态机（`NavigationController` 与 `Page` 接口），用于统一处理线性条件流（如冷启引导）和树状钻取流（如设置项）。
- **状态**：`部分落地`
- **源码落地**：部分
- **源码核对入口**：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/NavigationController.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/Page.kt`
