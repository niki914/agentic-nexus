# Turn State

## 目的

本文件后续用于描述通用会话状态模型，以及 query 捕获、takeover、reset、turnId 的关键语义。

## 后续应填充的信息

- 当前使用的会话状态模型是什么
- `InjectedLLM` 与 `NativeTakeover` 的行为差异
- query 捕获后的分流条件是什么
- `turnId` 如何生成以及为什么要保证单调递增
- 会话重置在什么时机发生，谁负责触发

## 建议引用的源码位置

- `app/src/main/java/.../chat/ConversationTurnState.kt`
- `app/src/main/java/.../chat/ConversationJournal.kt`
- `app/src/main/java/.../mod/feat/AbstractAssistantHook.kt`

## 写作约束

- 不要抄数据类和方法体全文
- 不要在文档里重新实现状态机
- 用状态定义、触发条件、相对路径来组织内容
