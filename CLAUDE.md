# AX Contract

<project>
Nexus 是一个 Android Xposed 模块。它在 [ColorOS 小布助手|...]进程中注入自定义 LLM 响应，替换原生 AI 回答。
</project>

<modules>

| 目录 | 类型 | 职责 | 关键入口 |
|------|------|------|----------|
| `app/` | application | Xposed 入口 + Compose UI + 核心 Hook 逻辑 | `Entrance.kt`, `MainActivity.kt` |
| `h/` | library | Xposed 框架层: Runtime 编排、反射工具、异步值传递 | `IXposed.kt`, `Runtime.kt`, `Hook.kt` |
| `composebase/` | library | 共享 Compose 主题 + MVI ViewModel 基类 | `BaseTheme.kt`, `ComposeMVIViewModel.kt` |
| `ipc/` | library | ContentProvider 跨进程持久化配置 | `XConfig.kt`, `SettingsContentProvider.kt` |
| `server/` | Python | 静态 HTTP 文件服务器，托管远程 config.json | `server.py` |

所有模块的 build 配置: `build.gradle.kts` (root), 各子目录 `build.gradle.kts`

</modules>

<boot_sequence>

1. `Entrance.kt` — Xposed 入口，`IXposed.getTarget()` 过滤目标包 `com.heytap.speechassist`
2. `HookSideLoader.load()` 立即安装 `ContextHook`，拦截 `Application.onCreate()` 捕获 Context
3. `ContextProvider.await()` 挂起等待 Context 就绪后，调用 `Context.getLocalSettings()` 读取本地 IPC 配置
4. 解析 `XSettings.props` 中的 `package_name` 和 `config`，将 config 注入 `KVProvider`
5. 根据 `targetPkg` 路由到具体 Hook 实现（当前仅 `BreenoChatHook`）
6. `RuntimeBootstrap.installIfNeeded()` 创建单例 `Runtime`，按双轨执行 hooks:
   - 同步轨: `hook.onHook(params)` 直接在主线程执行
   - DexKit 轨: `hook.useDexkit=true` 的 hook 在后台协程中执行，需先 `System.loadLibrary("dexkit")`

</boot_sequence>

<state_machine>

## ConversationTurnState — 每次用户输入触发一次状态迁移

```
ConversationTurnState(roomId, turnId, lastQuery, mode)
```

- `mode: TurnMode` = `InjectedLLM` | `NativeTakeover`
- 迁移入口: `AbstractAssistantHook.handleCapturedQuery(roomId, query)`
- 分支逻辑:
  - `shouldTakeOver(query)` 返回 true → `TurnMode.NativeTakeover` → 调用 `onTakeoverTriggered()` → 本次 LLM 不介入
  - 否则 → `TurnMode.InjectedLLM` → 调用 `dispatchQueryToLLM()`
- `shouldTakeOver()` 的逻辑: 检查 query 是否包含 `BreenoConfigProvider.takeoverKeywords` 中的任一关键词
- roomId 变化时（`RoomIdManagerHook` 检测到新建对话）→ 调用 `onSessionReset(roomId)` → 清空 `ConversationJournal` 中该 room 的记录 + 重置 turnState
- 并发保护: `BreenoChatHook` 内用 `synchronized(stateLock)` 保护 `roomTurnStates` 和 `renderSessions`

定义位置: `ConversationTurnState.kt`, 流转逻辑在 `AbstractAssistantHook.kt` 和 `BreenoChatHook.kt`

</state_machine>

<hook_topology>

`BreenoChatHook` 安装 4 个子 Hook（位于 `app/.../mod/oppo/subhooks/`）:

| Hook | 拦截目标 | 作用 |
|------|----------|------|
| `RoomIdManagerHook` | `RoomIdManager#createRoom` (after) | 检测新对话创建，触发 session reset |
| `InputHook` | `DataCenter#insertMessage` (before) | 捕获用户 query（chatType==query 时），回调 `onInput(roomId, query)` |
| `NativeCardPolicyHook` | `DataCenter#insertMessage` (before) | 拦截原生回答卡片: InjectedLLM 模式下将 `param.result = null`，Takeover 模式放行 |
| `OperationFactoryHook` | `OperationFactory#create` (after) | 替换 `CleanOperation` → `DoNothingOperation`，防止系统清理注入的卡片 |

Hook 执行的先后顺序由 `AbstractAssistantHook.onHook()` 模板方法控制:
1. `installSessionHooks()` — RoomIdManagerHook
2. `installResponseHooks()` — NativeCardPolicyHook + OperationFactoryHook
3. `installInputHooks()` — InputHook

</hook_topology>

<render_pipeline>

`BreenoChatHook.renderStreamCard()` — 将 LLM 流式输出渲染为 Breeno 原生卡片:

1. 校验当前 turn 是否仍为 active（turnId 匹配 + mode==InjectedLLM），否则丢弃
2. 首次渲染 (`isFirst`): 通过反射创建 `viewBeanClass` 实例，注入 mock 方法值和 localData
3. 更新卡片: `bean.setContent(chunk)`, `bean.setFinal(isFinal)`, `bean.setFirstSlice(isFirst)`
4. 首次: `dataCenter.insertMessage(bean)`, 后续: `dataCenter.updateMessage(bean, false)`
5. 最终帧: 清除 `hideFeedbackView` 标记，允许反馈按钮显示

卡片 Bean 的类名、方法名、mock 数据全部来自 `BreenoConfigProvider` → `server/breeno/{versionCode}/config.json`

</render_pipeline>

<config_resolution>

配置来源优先级: 远程拉取 > 本地缓存 > 空 JSON `{}`

解析链:
1. `MainActivity.onResume()` → `refreshLocalSettings()` → OkHttp GET `http://127.0.0.1:8788/{alias}/{versionCode}/config.json`
2. `XConfig.updateFromServerJson()` → `ConfigPersistence.writeConfig()` 写入文件
3. `XConfig.get()` → 先查内存缓存 → 再通过 ContentProvider IPC 读取 `SettingsContentProvider`
4. `XSettings.toXSettings()` 将 JSON 字符串解析为 `XSettings(props: JsonObject)`
5. `KVProvider.provide(configObj)` 注入全局 KV 存储
6. `BaseConfigProvider` 提供点号分隔路径的层级取值（如 `classes.room_id_manager`）
7. `BreenoConfigProvider` 对每个 key 做类型化封装

IPC 模块 (`ipc/`) 保证宿主 App 进程与 Xposed 注入的目标进程可共享同一份配置。

</config_resolution>

<invariants>

- 所有 Xposed hook 方法调用必须经过 `xTry` 包装，失败时自动 log 堆栈
- `BaseConfigProvider.getElement()` 对不存在的 path 输出 warning 日志，但返回 null 而非抛异常
- `Runtime` 每个进程只安装一次（`RuntimeBootstrap` 双重检查锁 + `@Volatile`）
- `ConversationTurnState.turnId` 由 `TurnIdGenerator` 单调递增生成，`maxOf(previous+1, System.currentTimeMillis())` 保证全局唯一
- `BreenoChatHook` 的 `renderSessions` 和 `roomTurnStates` 共享同一把 `stateLock`

</invariants>

<navigation>

- **LLM 调用**: `LLMController.kt` → 第三方库 `com.github.niki914:s3ss10n:1.0`
- **新 Session API 设计**: `NewSession.md` — s3ss10n 下一版 DSL 的完整 PRD
- **MCP 集成规划**: `README.md` 后半部分 — MCP 协议探测记录与接入方案
- **Breeno 逆向配置**: `server/breeno/120704/config.json` — 所有类名/方法名/参数类型的映射表
- **反射工具集**: `h/.../util/ReflectionExtensions.kt`, `HookExtensions.kt`
- **日志**: `h/.../util/Xlogging.kt` — `xlog()` 和 `xtlog()`
- **错误处理**: `h/.../util/XTry.kt` — `xTry()` 安全执行包装
- **项目模板化脚本**: `apply_template.py` + `template.json` — 用于从模板生成新项目实例
- **Gradle 自动任务**: `app/build.gradle.kts` 末尾 — `adbReverse` (端口 8788, 1234) + `startServer` (Python server)，挂载到 install/assemble 任务

</navigation>