# LLM Runtime

## 目的

本文件后续用于描述当前 LLM runtime 的现状，包括已经在源码中的实现，以及仍处于设计中的部分。

## 后续应填充的信息

- 当前 `LLMController` 负责什么，不负责什么
- `PromptComposer`、`ToolManager`、`ToolEventFormatter` 的职责边界
- `SessionEvent` 与项目内流式事件模型如何映射
- Tool/MCP 能力哪些已经真正接入，哪些还只是半落地或设计中
- `docs/.asc_task/llm_controller_v2/` 与当前源码之间的关系

## 建议引用的源码位置

- `app/src/main/java/.../chat/LLMController.kt`
- `app/src/main/java/.../chat/agentic/PromptComposer.kt`
- `app/src/main/java/.../chat/agentic/ToolManager.kt`
- `app/src/main/java/.../chat/agentic/ToolEventFormatter.kt`
- `app/src/main/java/.../chat/LlmStreamEvent.kt`
- `docs/.asc_task/llm_controller_v2/tech_survey.md`
- `docs/.asc_task/llm_controller_v2/tech_design.md`

## 写作约束

- 必须明确区分“源码已落地”和“设计文档目标态”
- 不要把运行时代码复制到文档里
- 只允许为解释架构而摘取最小代码片段
- 默认使用相对路径作为真正入口
