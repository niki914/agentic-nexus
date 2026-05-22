# Current Status

## 已落地能力

- **基础架构与路由**：Xposed 入口与多宿主分发（当前支持 `ColorOS / Breeno` 与 `HyperOS / XiaoAi`），位于 `app/` 与 `h/` 模块。
- **配置同步机制**：本地 Python 配置服务器（按包名/版本号下发）与主 App/宿主进程的 IPC 配置桥接（`XIpcBridge`，`SettingsContentProvider`）。
- **Breeno 注入实现**：基于卡片层拦截的原生回答屏蔽与流式渲染（单卡片全量刷新模式）。
- **XiaoAi 注入实现**：基于底层指令流与文字流拦截的增量文本分片注入，包含 TTS 与原生播放拦截。
- **基础 LLM 对接**：基于 `s3ss10n` 的旧版 `LLMController` 单例门面，支持基础的流式输出拦截与注入。
- **LLM Controller v2 重构**：实现了单例 session 状态对齐，统一了自定义 tool、MCP 及 memory prompt 从 `LocalSettings` 的扩展入口。
- **MCP Tool Cache MVP**：为 MCP discovered tools 增加 `LocalSettings` 缓存兜底，以优化冷启体验。

## 提案

- **UI Shell 升级**：主应用 `MainActivity` 界面升级为 Apple Liquid Glass 风格。
- **多语言支持**：为 Breeno 分支提供多语言注入支持。
- **生成测速**：在 LLMController 中实现字数测速功能。

## 已知技术债、待重构点、重要 TODO

- `RenderTextStreamCardHook`：当前使用了监视器锁且硬编码类名，需将类名上云并重构锁机制。
- `BaseConfigProvider` / `BreenoConfigProvider`：需引入 Dataclass 描述 hook spot，并将下游读参方式重构为 `suspend`。
- `AbstractAssistantHook`：需统一所有业务分支的 subhooks 命名、云 config 结构及生命周期方法名。
- `LLMController`：需补充前置配置检查与快速失败逻辑。