# Task Docs Registry

记录仓库根目录仍可作为检索入口的任务文档与参考文档。实现与现状判断一律以源码为准。

## UI 与导航系列

### `apple-liquid-glass-philosophy.md`

- **主题**：Liquid Glass 视觉原则与组件设计参考。
- **状态**：`参考文档`
- **源码核对入口**：
  - `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/`
    - `LiquidScreen.kt`
    - `LiquidScreenState.kt`
    - `LiquidViewportAvoidance.kt`
  - `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/`
    - `LiquidButton.kt`
    - `LiquidTextField.kt`
    - `LiquidToggle.kt`
    - `SettingsGroupCard.kt`

## Runtime 与网络系列

### `SESSION.md`

- **主题**：S3ss10n、LLM 和 MCP 网络请求相关记录。
- **状态**：`参考文档`
- **源码核对入口**：
  - `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/`
    - `LLMController.kt`
    - `LlmModels.kt`
    - `LlmStreamEvent.kt`
  - `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/`
    - `PromptComposer.kt`
    - `SessionToolBinder.kt`
    - `ToolManager.kt`
    - `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`
