# Nexus Wiki Index

> 本文件是 `nexus-lookup` 的总入口。任何检索先从这里开始，再按场景路由到对应子文档。

## 使用目标

这套 wiki 只负责三件事：

- 给出稳定的知识地图
- 说明应该去哪里继续读源码
- 区分稳定事实、进行中设计、未来提案

它不负责存放源码副本，也不应该演变成另一个 `CLAUDE.md`。

## 知识地图

### overview/ — 项目总览与当前状态

| 文件 | 层级 | 预期内容 |
| --- | --- | --- |
| `overview/project-overview.md` | Stable | 项目定位、模块结构、宿主矩阵、关键入口、主要目录 |
| `overview/current-status.md` | In Progress | 当前已落地能力、半落地能力、纯提案能力、近期 TODO |

### architecture/ — 核心机制与链路

| 文件 | 层级 | 预期内容 |
| --- | --- | --- |
| `architecture/boot-sequence.md` | Stable | Xposed 入口、Context 捕获、宿主路由、Hook 安装顺序 |
| `architecture/turn-state.md` | Stable | 会话状态、turnId、takeover 语义、session reset |
| `architecture/render-pipeline.md` | Stable | Breeno 与 XiaoAi 的响应注入模型与差异 |
| `architecture/config-resolution.md` | Stable | 本地配置、远程配置、IPC、server 回退逻辑 |
| `architecture/llm-runtime.md` | In Progress | `LLMController`、Prompt、Tool、MCP、流式事件运行时 |

### domains/ — 业务专项导航

| 文件 | 层级 | 预期内容 |
| --- | --- | --- |
| `domains/breeno/index.md` | Stable | Breeno 侧业务心智、关键 Hook、源码入口、调试关注点 |
| `domains/xiaoai/index.md` | Stable | XiaoAi 侧业务心智、关键 Hook、源码入口、调试关注点 |
| `domains/ui-shell/index.md` | In Progress | UI Shell、onboarding、settings tree、导航状态机、PRD 来源 |

### reference/ — 高频索引与注册表

| 文件 | 层级 | 预期内容 |
| --- | --- | --- |
| `reference/source-map.md` | Stable | 关键类、目录、文件入口的相对路径地图 |
| `reference/task-docs-registry.md` | In Progress | PRD、设计稿与源码落地状态的注册表 |

## 检索建议

| 场景 | 推荐阅读顺序 |
| --- | --- |
| 工程结构、模块职责、关键入口 | `overview/project-overview.md` -> `reference/source-map.md` |
| 现在项目做到哪、哪些能力还没正式落地 | `overview/current-status.md` -> `reference/task-docs-registry.md` |
| Xposed 启动、宿主匹配、Hook 安装顺序 | `architecture/boot-sequence.md` -> `reference/source-map.md` |
| 会话状态、takeover、turnId、reset | `architecture/turn-state.md` |
| Breeno 响应注入、卡片刷新、相关子 Hook | `domains/breeno/index.md` -> `architecture/render-pipeline.md` |
| XiaoAi 响应目标捕获、文本流/TTS 拦截、分片注入 | `domains/xiaoai/index.md` -> `architecture/render-pipeline.md` |
| 配置来源、IPC、server、版本回退 | `architecture/config-resolution.md` -> `reference/source-map.md` |
| LLMController、Prompt、Tool、MCP、事件流 | `architecture/llm-runtime.md` -> `overview/current-status.md` |
| S3ss10n 库、LLM 及 MCP 网络请求 | `SESSION.md` |
| UI、onboarding、settings tree、Liquid Glass、导航状态机 | `domains/ui-shell/index.md` -> `reference/task-docs-registry.md` |
| 某个设计文档是否已经实现 | `reference/task-docs-registry.md` -> `overview/current-status.md` -> 相关源码路径 |
