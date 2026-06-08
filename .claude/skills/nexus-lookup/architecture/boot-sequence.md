# Boot Sequence

本文档描述从 Xposed 入口到业务 Hook 安装完成之间的当前启动链路。

## 启动时序

1. **入口匹配**
   - `a0/a0/a0/a0/a0/a0/Entrance.kt` 通过 `getTarget()` 返回 `Target.filter(*XValues.appList.toTypedArray())`，只监听受支持宿主包名。

2. **基础 Hook 装载**
   - `Entrance.onLoad()` 先用 `HookSideLoader.load()` 装载：
   - `h/src/main/java/com/niki914/nexus/h/util/ContextHook.kt`
   - `ActivityHook` 与 `FloatWindowHook` 当前在入口中仍是注释状态，不属于已启用启动链。

3. **上下文与运行时初始化**
   - `ContextProvider.await()` 等待宿主 `Context` 就绪。
   - `XRepo.init(ctx)` 初始化本地 repo。
   - `RuntimeEnvironment.install(createAppRuntimeBridge())` 安装 runtime settings gateway。
   - `HookLocalSettings.update(ctx)` 刷新宿主进程本地设置缓存。
   - `XRepo.web.await()` 读取当前宿主可见的 `WEB_SETTINGS`。

4. **宿主路由**
   - `Entrance.onSettingsFetched()` 用 `HostApp.fromPackageName(params.packageName)` 判断宿主。
   - `HostApp.Breeno` 路由到 `BreenoChatHook(scope)`。
   - `HostApp.XiaoAi` 路由到 `XiaoaiChatHook(scope)`。
   - 未命中时直接返回，不安装业务 Hook。

5. **Runtime 安装**
   - `RuntimeBootstrap.installIfNeeded()` 为当前宿主进程安装单例 `Runtime`。
   - `Runtime` 当前只挂载命中的主业务 Hook，由该 Hook 再继续安装 session / response / input subhook。

## 关键源码

### `app/src/main/java/`

- `a0/a0/a0/a0/a0/a0/Entrance.kt`
- `com/niki914/nexus/agentic/mod/HookLocalSettings.kt`
- `com/niki914/nexus/agentic/repo/XRepo.kt`
- `com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
- `com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`

### `h/src/main/java/com/niki914/nexus/h/`

- `util/ContextHook.kt`
- `util/ContextProvider.kt`
- `core/runtime/RuntimeBootstrap.kt`
