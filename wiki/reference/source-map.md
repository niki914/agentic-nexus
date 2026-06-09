# Source Map

按主题分组的项目根相对路径地图，用于快速定位仍然存在的高频源码入口。

## app/src/main/java/a0/a0/a0/a0/a0/a0/

- `Entrance.kt`: Xposed 入口

## app/src/main/java/com/niki914/nexus/agentic/mod/

- `HookLocalSettings.kt`: Hook 侧本地配置读取
- `SettingModels.kt`: `LocalSettings`、`WebSettings` 等配置模型
- `XService.kt`: 本地/远程配置门面

## app/src/main/java/com/niki914/nexus/agentic/mod/feat/

- `AbstractAssistantHook.kt`: 通用 Hook 模板
- `BaseConfigProvider.kt`: 宿主配置提供者基类
- `FloatScreenResetDetector.kt`: 浮窗重置检测
- `HookTarget.kt`: 宿主目标枚举
- `SubHook.kt`: 子 Hook 协议

## app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/

- `BreenoChatHook.kt`: Breeno 业务 Hook 入口
- `BreenoConfigProvider.kt`: Breeno 配置提供者
- `BreenoFeedbackAssembler.kt`: Breeno UI 反馈组装

## app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/subhooks/

- `BlockNativeCardHook.kt`: 拦截原生卡片
- `CaptureInputHook.kt`: 捕获输入
- `ResetConversationSignalHook.kt`: 重置会话信号
- `SuppressCleanupHook.kt`: 阻止清理操作

## app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/

- `XiaoaiChatHook.kt`: XiaoAi 业务 Hook 入口
- `XiaoaiConfigProvider.kt`: XiaoAi 配置提供者
- `XiaoaiRenderSession.kt`: XiaoAi 渲染会话

## app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/

- `BlockNativeInstructionByWhitelistHook.kt`: 按白名单拦截原生指令
- `BlockNativeTtsPlaybackHook.kt`: 拦截原生 TTS 播放
- `CaptureInputHook.kt`: 捕获输入
- `CaptureResponseTargetHook.kt`: 捕获响应目标
- `EXT.kt`: Hyper 子 Hook 扩展函数
- `RenderTextStreamCardHook.kt`: 渲染文本流卡片

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/

- `ActiveTurnStore.kt`: 活跃 turn 状态缓存
- `ConversationTurnState.kt`: 会话状态流转
- `LLMController.kt`: LLM 控制器入口
- `LlmModels.kt`: 运行时模型定义
- `LlmStreamEvent.kt`: 统一流事件定义
- `TurnMode.kt`: turn 模式枚举

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/

- `PromptComposer.kt`: 提示词组装
- `SessionToolBinder.kt`: local tools 与 MCP servers 绑定
- `ToolCallDispatcher.kt`: local tool 调度
- `ToolManager.kt`: 工具配置解析与 prompt lines 生成

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/

- `BuiltinTool.kt`: builtin tool 协议
- `BuiltinToolExecutor.kt`: builtin tool 执行器
- `BuiltinToolRegistry.kt`: builtin tool 注册表
- `BuiltinToolSettingsManager.kt`: builtin tool 设置读写门面

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/

- `CreateCustomToolBuiltin.kt`: 内建创建 custom tool
- `LaunchAppBuiltin.kt`: 启动已安装应用
- `MemorizeBuiltin.kt`: 内建记忆写入工具
- `NotifyBuiltin.kt`: 内建通知工具
- `OpenUriBuiltin.kt`: 打开 URI
- `ReadCustomToolBuiltin.kt`: 内建读取 custom tool 定义工具
- `RunCommandBuildin_WIP_SAFE.kt`: 内建命令执行工具
- `SearchAppsBuiltin.kt`: 搜索已安装应用

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/device/

- `AppInfoProvider.kt`: 已安装应用信息提供者
- `AppInfoCache.kt`: 应用信息缓存

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/

- `CustomToolExecutor.kt`: custom tool 执行器
- `CustomToolManager.kt`: custom tool 配置与校验

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/

- `McpDiscoveryCacheStore.kt`: MCP discovered tools 缓存

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/

- `ShellCommandRunner.kt`: shell 命令执行器
- `ShellCommandSafetyPolicy.kt`: shell 命令安全策略

## agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/

- `LlmStreamEventMapper.kt`: `SessionEvent` 到 `LlmStreamEvent` 的映射
- `LocalToolResultClassifier.kt`: local tool 结果失败分类
- `ToolEventFormatter.kt`: 工具事件格式化

## app/src/main/java/com/niki914/nexus/agentic/app/

- `App.kt`: Application 入口
- `MainActivity.kt`: 主 Activity

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/

- `NexusApp.kt`: 主 App Shell 入口
- `NexusPages.kt`: 页面装配与路由分发
- `PageChrome.kt`: 页面 chrome 装配

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/

- `AppLaunchDecision.kt`: 初始页面判定
- `AboutSettingsState.kt`: About 设置页状态
- `BuiltinToolSettingsState.kt`: builtin tools 设置页状态
- `ConfigureState.kt`: 配置页状态
- `CustomToolSettingsState.kt`: 自定义工具设置页状态
- `ExecutionRulesSettingsState.kt`: 执行规则设置页状态
- `HomeChatState.kt`: 首页对话状态
- `MemorySettingsState.kt`: Memory 设置页状态
- `McpSettingsState.kt`: MCP 设置页状态
- `ProviderSpec.kt`: provider 规格定义
- `SettingsState.kt`: 设置页状态
- `StartupAssistantUi.kt`: 启动宿主 UI 类型
- `TakeoverSettingsState.kt`: takeover 规则设置页状态

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/

- `NexusPage.kt`: 页面定义
- `NexusSettingsGroup.kt`: 设置组模型

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/route/

- `ConfigurePageRoute.kt`: 配置页路由装配
- `CustomToolDetailRoute.kt`: custom tool 详情页路由装配
- `DonePageRoute.kt`: 完成页路由装配
- `ExecutionRuleDetailRoute.kt`: 执行规则详情页路由装配
- `HomePageRoute.kt`: 首页路由装配
- `McpServerDetailRoute.kt`: MCP server 详情页路由装配
- `ProviderPickPageRoute.kt`: provider 选择页路由装配
- `ProviderRouteColors.kt`: provider 路由配色
- `SettingsDetailPageRoute.kt`: 设置详情页路由装配
- `SettingsHomePageRoute.kt`: 设置首页路由装配
- `StartupPageRoute.kt`: 启动页路由装配
- `TakeoverRuleDetailRoute.kt`: takeover 规则详情页路由装配

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/

- `AboutSettingsContent.kt`: About 设置页
- `BuiltinToolsSettingsContent.kt`: Builtin Tools 设置页
- `ConfigureEditableField.kt`: 配置字段编辑块
- `ConfigurePageContent.kt`: 配置页内容
- `ConfigurePagePolicy.kt`: 配置页策略
- `CustomToolDetailContent.kt`: 自定义工具详情页
- `CustomToolsSettingsContent.kt`: 自定义工具设置页
- `DonePageContent.kt`: 完成页内容
- `ExecutionRuleDetailContent.kt`: 执行规则详情编辑页
- `ExecutionRulesSettingsContent.kt`: 执行规则设置页列表
- `HomeChatComponents.kt`: 首页对话组件
- `HomePageContent.kt`: 首页内容
- `MemorySettingsContent.kt`: Memory 设置页
- `ModelConfigSettingsContent.kt`: Model Config 设置页
- `ProviderAccessSettingsBlock.kt`: provider 接入设置块
- `ProviderAdvancedSettingsBlock.kt`: provider 高级设置块
- `SelectionPageContent.kt`: Provider 选择页内容
- `SettingsDetailPageContent.kt`: 设置详情分发入口
- `SettingsHomePageContent.kt`: 设置首页内容
- `StartupPageContent.kt`: 启动页内容
- `StartupPosterBackground.kt`: 启动页背景
- `TakeoverRuleDetailContent.kt`: takeover 规则详情编辑页
- `TakeoverSettingsContent.kt`: takeover 设置页列表
- `TODOPageContent.kt`: 未知设置分组兜底页

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/mcp/

- `McpServerDetailContent.kt`: MCP server 详情页
- `McpSettingsContent.kt`: MCP 设置页

## app/src/main/java/com/niki914/nexus/agentic/chat/

- `LlmStreamCollectors.kt`: App 侧流事件收集与展示适配

## app/src/main/java/com/niki914/nexus/agentic/repo/

- `LocalSettingsCodec.kt`: `LocalSettings` 编解码与字段投影
- `LocalSettingsDefaults.kt`: 本地设置默认值
- `LocalSettingsStore.kt`: `LocalSettings` 读写存储
- `XRepo.kt`: App 侧设置读写门面
- `XRepoRuntimeGateway.kt`: runtime 访问 repository 的桥接

## app/src/main/java/com/niki914/nexus/agentic/runtime/

- `AppRuntimeBridge.kt`: App 侧 runtime 桥接
- `IpcRuntimeHostGateway.kt`: 通过 IPC 访问宿主 runtime

## composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/

- `ActionBarButton.kt`: 顶栏按钮基建
- `ConfirmationLiquidDialog.kt`: 确认弹窗封装
- `LiquidDialog.kt`: Liquid 弹窗容器
- `LiquidScreen.kt`: 顶层 Liquid Shell
- `LiquidScreenContentContext.kt`: Liquid Shell 内容上下文
- `LiquidScreenState.kt`: 标题、按钮、blur layer 状态
- `LiquidScreenSwipeContent.kt`: 页面转场容器
- `LiquidViewportAvoidance.kt`: 视口避让与窗口 inset 处理

## composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/

- `NavigationController.kt`: 导航控制器
- `Navigator.kt`: 导航能力接口
- `Page.kt`: 页面接口
- `PageViewModel.kt`: 页面 ViewModel 基类

## composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/

- `LiquidButton.kt`、`LiquidTextField.kt`、`LiquidSecretTextField.kt`、`LiquidToggle.kt`: 基础输入与开关组件
- `SettingsGroupCard.kt`、`SettingsListItem.kt`、`SettingNavigationItem.kt`、`SettingToggleItem.kt`: 设置页列表与分组组件
- `SettingExpandableTextItem.kt`、`SettingsDetailFormScaffold.kt`、`SettingsSegmentedSelector.kt`: 设置详情编辑组件
- `SwipeDismissSettingsItemCard.kt`、`SettingsToggleListItemCard.kt`: 设置项滑动删除与开关卡片
- `SettingsListPageContent.kt`、`SettingsDetailPageDefaults.kt`: 设置页容器与默认配置

## composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/

- `DragGestureInspector.kt`: 拖拽手势检测
- `InteractiveHighlight.kt`: 交互高亮
- `LiquidInteractiveLayer.kt`: 液态交互层
- `LiquidInteractiveStyle.kt`: 交互参数模型

## composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/

- `G2Shapes.kt`: G2 系列形状定义

## composebase/src/main/java/com/niki914/nexus/cb/

- `BaseTheme.kt`: Compose 主题入口
- `ComposeMVIViewModel.kt`: Compose MVI ViewModel 基类

## composebase/src/main/java/com/niki914/nexus/cb/theme/

- `LightColorScheme.kt`: 浅色调色板
- `Typography.kt`: 字体样式
- `primaryLight.kt`: 主色定义

## h/src/main/java/com/niki914/nexus/h/

- `IXposed.kt`: Xposed Hook 抽象入口

## h/src/main/java/com/niki914/nexus/h/util/

- `ContextHook.kt`、`ContextProvider.kt`、`HookSideLoader.kt`: 当前入口启动链使用的 Context 捕获与 Hook 装载辅助
- `ActivityHook.kt`: Activity Hook 辅助；当前 `Entrance` 未启用
- `HookExtensions.kt`、`ReflectionExtensions.kt`、`XTry.kt`: Hook 与反射工具
- `Inspector.kt`、`InspectExtensions.kt`、`Xlogging.kt`: 调试检查与日志工具
- `OsUtils.kt`、`RootUtils.kt`、`XProvider.kt`: 系统环境、Root 与 Xposed Provider 辅助

## h/src/main/java/com/niki914/nexus/h/xevent/

- `XEvent.kt`: 事件定义
- `XEventEnvelope.kt`: 事件包装
- `XEventType.kt`: 事件类型
- `XEventUtils.kt`: 事件工具

## ipc/src/main/java/com/niki914/nexus/ipc/

- `XIpcBridge.kt`: IPC 桥接
- `XNotificationBridge.kt`: 通知桥接
- `XRes.kt`: 宿主枚举、IPC method/field、Store 文件 URI 契约

## ipc/src/main/java/com/niki914/nexus/ipc/cp/

- `SettingsContentProvider.kt`: 跨进程配置 Provider
- `XProviderDispatcher.kt`: Provider `call()` 分发器

## ipc/src/main/java/com/niki914/nexus/ipc/store/

- `ConfigPersistence.kt`: `WEB_SETTINGS` 与 `LOCAL_SETTINGS` 的 JSON 文件持久化
- `IpcJsonMutator.kt`: JSON mutate 工具
- `XIpcStoreRepository.kt`: Store 级读写与并发保护

## server/

- `server.py`: 本地静态配置服务
- `com.heytap.speechassist/120803/config.json`: Breeno 配置
- `com.miui.voiceassist/507013003/config.json`: XiaoAi 配置

## 根目录文档

- `README.md`: 主 README
- `SESSION.md`: S3ss10n / LLM / MCP 相关记录
- `apple-liquid-glass-philosophy.md`: Liquid Glass 设计参考
