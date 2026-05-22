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
- `agentic/ToolManager.kt`: 工具管理器
- `agentic/ToolEventFormatter.kt`: 工具事件格式化

## app/src/main/java/a0/a0/a0/a0/a0/a0/

- `Entrance.kt`: Xposed 入口

## app/src/main/java/com/niki914/nexus/agentic/mod/

- `XService.kt`: 本地/远程配置门面

## ipc/src/main/java/com/niki914/nexus/ipc/

- `XIpcBridge.kt`: IPC 桥接
- `XRes.kt`: 宿主枚举与 IPC 常量

## server/

- `server.py`: 本地静态配置服务
- `com.heytap.speechassist/{version_code}/config.json`: Breeno 配置
- `com.miui.voiceassist/{version_code}/config.json`: XiaoAi 配置

## 根目录文档

- `README.md`: 主 README
- `UI-PRD.md`: UI 与 PRD
- `prd-nav-status-machine.md`: 导航状态机 PRD
- `apple-liquid-glass-philosophy.md`: 苹果 Liquid Glass 设计理念
