# Project Overview

## 项目定位与目标

Nexus 是一个 Android Xposed 模块。它的核心目标是在受支持的宿主进程中注入自定义的大语言模型（LLM）响应，并按宿主类型路由到对应的 Hook 实现，进而覆盖或拦截系统原生的语音助手回答链路。

## 支持的宿主

当前主要支持两条宿主分支：

- **ColorOS / Breeno**
  - 宿主包名：`com.heytap.speechassist`
  - 业务实现：`BreenoChatHook` 
  - 源码位置：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`
- **HyperOS / XiaoAi**
  - 宿主包名：`com.miui.voiceassist`
  - 业务实现：`XiaoaiChatHook` 
  - 源码位置：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`

## 主要模块与职责边界

- **`app/`** (application)：Xposed 模块的启动入口、宿主路由分发、主 App 页面层，以及各语音助手具体的业务 Hook 实现。
- **`agent-runtime/`** (library)：LLM runtime 模块。负责 `LLMController`、Prompt 组装、Tool 解析与调度、MCP 注册与流式事件映射。
- **`h/`** (library)：Xposed 框架层封装。负责 Runtime 编排、反射调用机制、Context 上下文捕获、日志记录与容错处理。
- **`composebase/`** (library)：UI 基建模块。负责 Compose 主题、`LiquidScreen` 壳层、导航基类、交互层与通用组件。
- **`ipc/`** (library)：跨进程通信模块。负责宿主进程与主 App 之间的配置读取、通知桥接，包含 ContentProvider 等 IPC 实现。
- **`server/`** (Python)：本地静态配置服务器。按宿主包名与版本号结构提供 `config.json`，并支持在未匹配当前版本时回退到最近已知版本。

## 目录结构与关键入口

读者在阅读源码时，建议优先从以下关键入口切入，以快速建立全局认识：

- **Xposed 与业务入口**
  - Xposed 初始化入口：`app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`
  - 通用业务 Hook 模板：`app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`
  - LLM 请求调度控制器：`agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
  - 本地与远程配置加载门面：`app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt`

- **底层框架与跨进程通信**
  - IPC 跨进程数据桥接：`ipc/src/main/java/com/niki914/nexus/ipc/XIpcBridge.kt`
  - 宿主枚举与 IPC 静态常量：`ipc/src/main/java/com/niki914/nexus/ipc/XRes.kt`
  - Hook 框架层 Runtime 基础：`h/src/main/java/com/niki914/nexus/h/` 

- **UI 页面层与基建层**
  - 主 App 页面入口：`app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt`
  - 页面与路由壳层：`app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/`
  - UI 基建与导航基础：`composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`

- **配置与服务端样例**
  - 本地 Python 服务端：`server/server.py`
  - Breeno 配置样例：`server/com.heytap.speechassist/120803/config.json`
  - XiaoAi 配置样例：`server/com.miui.voiceassist/507013003/config.json`
