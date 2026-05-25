# Current Status

## 已落地能力

- **基础架构与路由**：Xposed 入口与多宿主分发支持 `ColorOS / Breeno` 与 `HyperOS / XiaoAi`，位于 `app/` 与 `h/` 模块。
- **配置同步机制**：本地 Python 配置服务器按包名/版本号下发 WebSettings；`WEB_SETTINGS` 与 `LOCAL_SETTINGS` 均由 `XIpcStoreRepository` / `ConfigPersistence` 文件化持久化，宿主读取通过 `SettingsContentProvider.openFile()` 暴露的文件流完成，写入与 mutate 仍通过 provider call 分发。
- **Breeno 注入实现**：基于卡片层拦截原生回答，使用单卡片全量刷新模式渲染 LLM 输出。
- **XiaoAi 注入实现**：基于底层指令流与文字流拦截做增量文本分片注入，并拦截 TTS 流与原生播放。
- **LLM Runtime**：`LLMController` 持有单例 `Session` 与 runtime snapshot，支持配置刷新、流式请求、会话重置和统一事件映射。
- **HTTP MCP**：MCP server 配置解析、Session 注册、discovered tools cache 与 HTTP interceptor 已落地。
- **CustomTool**：`custom_tools` 从 `LocalSettings` 解析到 `LocalTool.Custom`、local tool 注册、`ToolCallDispatcher` 与 `CustomToolExecutor` 执行链路已落地；builtin 入口使用 `create_custom_tool`。
- **UI Shell**：`NexusApp`、startup/home/configure/selection/settings 页面、Liquid 组件与导航控制器已落地，自定义工具设置页已接入 `custom_tools` 读写闭环。

## 半落地能力

- **Builtin Tool Flags**：已解析 builtin tool flags 并生成 prompt lines，未看到独立 builtin 执行链路。
- **UI 完整态**：当前导航结构已经固定为源码现状，剩余工作集中在 onboarding 表单化、DonePage 去套壳、隐藏 settings 分组落地和 strings 体系收口。

## 提案

- **多语言支持**：为 Breeno 分支提供多语言注入支持。

## 已知技术债、待重构点、重要 TODO

- `RenderTextStreamCardHook`：当前使用监视器锁且硬编码类名，需将类名上云并重构锁机制。
- `BaseConfigProvider` / `BreenoConfigProvider`：需引入 Dataclass 描述 hook spot，并将下游读参方式重构为 `suspend`。
- `AbstractAssistantHook`：需统一所有业务分支的 subhooks 命名、云 config 结构及生命周期方法名。
- `LLMController`：需补充前置配置检查与快速失败逻辑。
