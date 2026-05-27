# Source Map

按主题分组的相对路径地图，用于快速定位高频源码入口。

## app/src/main/java/com/niki914/nexus/agentic/mod/feat/

- `AbstractAssistantHook.kt`: 通用 Hook 模板
- `oppo/BreenoChatHook.kt`: Breeno 业务 Hook 入口
- `oppo/BreenoConfigProvider.kt`: Breeno 配置提供者
- `oppo/BreenoFeedbackAssembler.kt`: Breeno UI 反馈组装
- `oppo/subhooks/BlockNativeCardHook.kt`: 子 Hook (拦截原生卡片)
- `oppo/subhooks/CaptureInputHook.kt`: 子 Hook (捕获输入)
- `oppo/subhooks/ResetConversationSignalHook.kt`: 子 Hook (重置会话信号)
- `oppo/subhooks/SuppressCleanupHook.kt`: 子 Hook (阻止清理操作)
- `hyper/XiaoaiChatHook.kt`: XiaoAi 业务 Hook 入口
- `hyper/XiaoaiRenderSession.kt`: XiaoAi 渲染会话
- `hyper/XiaoaiConfigProvider.kt`: XiaoAi 配置提供者
- `hyper/subhooks/RenderTextStreamCardHook.kt`: 子 Hook (渲染文本流卡片)
- `hyper/subhooks/CaptureResponseTargetHook.kt`: 子 Hook (捕获响应目标)
- `hyper/subhooks/CaptureInputHook.kt`: 子 Hook (捕获输入)
- `hyper/subhooks/BlockNativeTextStreamHook.kt`: 子 Hook (拦截原生文本流)
- `hyper/subhooks/BlockNativeTtsStreamHook.kt`: 子 Hook (拦截原生 TTS 流)
- `hyper/subhooks/BlockNativeTtsPlaybackHook.kt`: 子 Hook (拦截原生 TTS 播放)

## app/src/main/java/com/niki914/nexus/agentic/chat/

- `ConversationTurnState.kt`: LLM 会话状态流转
- `ConversationJournal.kt`: LLM 交互日志
- `LLMController.kt`: LLM 控制器入口
- `LlmStreamEvent.kt`: 流事件定义
- `LlmModels.kt`: 模型定义
- `agentic/PromptComposer.kt`: 提示词组装
- `agentic/ToolManager.kt`: 工具配置解析与 prompt lines 生成
- `agentic/SessionToolBinder.kt`: local tools 与 MCP servers 绑定
- `agentic/ToolCallDispatcher.kt`: local tool 调度
- `agentic/CustomToolExecutor.kt`: custom tools 执行器
- `agentic/McpDiscoveryCacheStore.kt`: MCP discovered tools 缓存
- `agentic/McpInterceptorHttpEngine.kt`: MCP discovery HTTP interceptor
- `agentic/LlmStreamEventMapper.kt`: `SessionEvent` 到 `LlmStreamEvent` 的映射
- `agentic/ToolEventFormatter.kt`: 工具事件格式化

## app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/

- `NexusApp.kt`: 主 App Shell 入口
- `NexusPages.kt`: 页面装配与路由分发
- `model/AppLaunchDecision.kt`: 初始页面判定
- `nav/NexusPage.kt`: 页面定义
- `nav/NexusSettingsGroup.kt`: 设置组模型
- `content/StartupPageContent.kt`: 启动页内容
- `content/SelectionPageContent.kt`: Provider 选择页内容
- `content/ConfigurePageContent.kt`: 配置页与 DonePage 复用布局
- `content/HomePageContent.kt`: 首页内容
- `content/HomeChatComponents.kt`: 首页对话组件
- `content/SettingsHomePageContent.kt`: 设置首页内容
- `content/SettingsDetailPageContent.kt`: 设置详情分发入口
- `content/BuiltinToolsSettingsContent.kt`: Builtin Tools 设置页
- `content/McpSettingsContent.kt`: MCP 设置页
- `content/CustomToolsSettingsContent.kt`: 自定义工具设置页
- `content/StartupPosterBackground.kt`: 启动页背景

## app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/

- `LiquidScreen.kt`: 顶层 Liquid Shell
- `LiquidScreenState.kt`: 标题、按钮、blur layer 状态
- `LiquidScreenSwipeContent.kt`: 页面转场容器
- `ActionBarButton.kt`: 顶栏按钮基建

## app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/

- `NavigationController.kt`: 导航控制器
- `Navigator.kt`: 导航能力接口
- `Page.kt`: 页面接口
- `PageViewModel.kt`: 页面 ViewModel 基类

## app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/

- `LiquidButton.kt`: 基础 Liquid 按钮
- `TintLiquidButton.kt`: 可着色 Liquid 按钮
- `MaterialTintLiquidButton.kt`: Material 风格着色按钮
- `LiquidTextField.kt`: 基础文本输入
- `LiquidSecretTextField.kt`: 密码输入
- `ExpandableLiquidTextFieldCard.kt`: 可展开多行表单卡片
- `LiquidToggle.kt`: 自定义 Toggle
- `LiquidToggleStateMachine.kt`: Toggle 状态机
- `SettingsGroupCard.kt`: 设置分组卡片
- `SettingsNavigationRow.kt`: 设置导航行
- `SettingsToggleRow.kt`: 设置开关行

## app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/

- `LiquidInteractiveLayer.kt`: 液态交互层
- `LiquidInteractiveStyle.kt`: 交互参数模型
- `DragGestureInspector.kt`: 拖拽手势检测
- `InteractiveHighlight.kt`: 交互高亮

## app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/

- `G2Shapes.kt`: G2 系列形状定义

## app/src/main/java/a0/a0/a0/a0/a0/a0/

- `Entrance.kt`: Xposed 入口

## app/src/main/java/com/niki914/nexus/agentic/mod/

- `XService.kt`: 本地/远程配置门面
- `SettingModels.kt`: `LocalSettings`、`WebSettings` 等配置模型
- `HookLocalSettings.kt`: Hook 侧本地配置读取

## ipc/src/main/java/com/niki914/nexus/ipc/

- `XIpcBridge.kt`: IPC 桥接；主 App 直连 repository，宿主通过 provider call / file stream 访问
- `XRes.kt`: 宿主枚举、IPC method/field、Store 文件 URI 契约
- `cp/SettingsContentProvider.kt`: 跨进程配置 Provider；`call()` 处理命令型操作，`openFile()` 暴露只读 Store 文件流
- `cp/XProviderDispatcher.kt`: Provider call 分发器，GET 返回 Store handle，PUT / MUTATE 返回 success-only
- `store/ConfigPersistence.kt`: `WEB_SETTINGS` 与 `LOCAL_SETTINGS` 的 JSON 文件持久化
- `store/XIpcStoreRepository.kt`: Store 级 `Mutex`、读写与 JSON mutate

## server/

- `server.py`: 本地静态配置服务
- `com.heytap.speechassist/{version_code}/config.json`: Breeno 配置
- `com.miui.voiceassist/{version_code}/config.json`: XiaoAi 配置

## 根目录文档

- `README.md`: 主 README
- `SESSION.md`: S3ss10n / LLM / MCP 相关记录
- `ASC_may_25.md`: 当前 UI 收敛结论、ASC 任务与 strings 约束
- `apple-liquid-glass-philosophy.md`: 苹果 Liquid Glass 设计理念
