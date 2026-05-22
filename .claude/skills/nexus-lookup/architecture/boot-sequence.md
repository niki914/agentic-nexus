# Boot Sequence

本文档描述从 Xposed 入口到业务 Hook 安装完成之间的启动时序。

## 1. 宿主识别与入口加载

- **目标识别**: `Entrance.getTarget()` 遍历 `XValues.appList` 动态匹配受支持的宿主包名，避免硬编码单一目标。
- **环境隔离**: `Entrance.onLoad()` 中验证目标宿主包名 `targetPkg` 与当前应用进程包名 `params.packageName` 相符时，才允许后续业务安装。

## 2. 基础环境捕获

在 `Entrance.onLoad()` 执行时，第一时间通过 `HookSideLoader` 挂载生命周期观测组件：

- **Context 捕获**: 加载 `ContextHook`，在 `Application.onCreate()` 阶段截获宿主全局 `Context`。
- **UI 观测**: 加载 `ActivityHook` 补充 Activity 生命周期追踪。该数据复用于后续业务层处理诸如浮窗 Detach 或页面恢复时的联动判断。

## 3. 上下文就绪与配置更新

启动独立协程，串联环境上下文和远端配置：

- 挂起调用 `ContextProvider.await()` 直至成功返回宿主 `Context`。
- 获取 `Context` 后，立即执行 `HookLocalSettings.update(ctx)` 刷新进程本地配置，并触发 `XService.getWebSettings(ctx)` 桥接获取最新配置。

## 4. 路由与业务 Hook 安装

- **类型路由**: 使用 `HostApp.fromPackageName(params.packageName)` 推断当前宿主。
  - `Breeno` 环境路由至 `BreenoChatHook`
  - `XiaoAi` 环境路由至 `XiaoaiChatHook`
- **运行时初始化**: `RuntimeBootstrap.installIfNeeded()` 采用双重检查锁为宿主进程初始化单例 `Runtime` 对象，建立运行基础设施。
- **业务挂载**: 最终调用具体业务 Hook 的 `hook.onHook(params)` 同步完成所有子 Hook 的注册。

## 源码参考路径

- **Entrance**: `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`
- **Hooks**: `h/src/main/java/com/niki914/nexus/h/util/ContextHook.kt`, `h/src/main/java/com/niki914/nexus/h/util/ActivityHook.kt`
- **Providers**: `h/src/main/java/com/niki914/nexus/h/util/ContextProvider.kt`
- **Feature Modules**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/`
