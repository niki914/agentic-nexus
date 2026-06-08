# Project Overview

## 项目定位

Nexus 是一个 Android Xposed 模块，目标是在受支持的宿主进程中接管或替换原生语音助手回答链路，并把大模型运行时、配置同步、设置 UI 与宿主 Hook 编排到同一套工程里。

## 支持的宿主

- `ColorOS / Breeno`
  - 宿主包名：`com.heytap.speechassist`
  - 主 Hook：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`
- `HyperOS / XiaoAi`
  - 宿主包名：`com.miui.voiceassist`
  - 主 Hook：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`

## 模块边界

- `app/`：Xposed 入口、宿主分发、主 App UI、Breeno/XiaoAi 业务 Hook。
- `agent-runtime/`：LLM runtime、Prompt 组装、Tool 解析与执行、MCP 发现缓存、流式事件映射。
- `h/`：Xposed 运行时封装、Hook 基类、Context/Activity 观测与通用工具。
- `composebase/`：Compose UI 基建、导航栈、`LiquidScreen` 壳层、交互层与通用组件。
- `ipc/`：配置读写桥接、`ContentProvider` IPC、Store 持久化与通知桥。
- `server/`：本地 Python 静态配置服务，按包名和版本号提供 `config.json`，缺失版本时回退到最近版本。

## 关键入口

### `app/src/main/java/`

- `a0/a0/a0/a0/a0/a0/Entrance.kt`：Xposed 入口；加载 `ContextHook`，初始化 repo/runtime，再按宿主路由业务 Hook。
- `com/niki914/nexus/agentic/app/MainActivity.kt`：主 App 入口；初始化 `XRepo`，计算启动页并进入 `NexusApp`。
- `com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`：Breeno/XiaoAi 通用生命周期与 query 分流基类。
- `com/niki914/nexus/agentic/mod/XService.kt`：配置读取、远端刷新、通知发送门面。

### `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`

- `LLMController.kt`：运行时总入口；负责 refresh、stream、session reset、MCP 刷新时机。
- `ConversationTurnState.kt`：轮次状态与 `turnId` 生成。
- `agentic/`：Prompt、Tool、MCP、shell 安全与流式事件映射。

### `ipc/src/main/java/com/niki914/nexus/ipc/`

- `XIpcBridge.kt`：统一屏蔽主进程与宿主进程的配置读写差异。
- `XRes.kt`：宿主枚举、IPC contract 与 Store URI 定义。
- `cp/SettingsContentProvider.kt`：宿主到主 App 的 IPC 边界。
- `store/`：`web_settings.json`、`local_settings.json` 的持久化实现。

### `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/`

- `NexusApp.kt`：主 App 导航栈与页面壳层装配。
- `NexusPages.kt`：页面分发与 onboarding / settings 导航流。
- `nav/NexusPage.kt`：页面模型定义。

### `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`

- `LiquidScreen.kt`
- `LiquidScreenState.kt`
- `LiquidScreenSwipeContent.kt`
- `nav/NavigationController.kt`
