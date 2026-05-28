# Progress: module_refactor

## Project Goal
通过“两刀方案”降低 `app` 模块职责密度：将 `app/ui/infra` 下沉到 `composebase`，新建 `agent-runtime` 承接 `chat`，并明确用户在 AS 的机械迁移与 ASC 的边界/依赖改造分工。

## Current Phase
Phase 3 ▶ B-01

## Context (关键上下文)

### Phase 0 调研结论
- 本轮只做“两刀方案”，不继续拆 `repo`，不拆宿主模块。
- 用户在 Android Studio 中执行包/目录移动；ASC 负责新模块、Gradle、导入修正与边界收敛。
- `ui/infra` 适合整体迁入 `composebase`，`chat` 适合形成新模块 `agent-runtime`。
- 迁移难点不在机械移动，而在 `XRepo` 对 runtime 细节的反向依赖。

### Phase 1 设计决策
- 跨模块迁移优先保持原 package 不变，降低 AS 机械迁移成本。
- 在 `agent-runtime` 中新增 `RuntimeSettingsGateway`、`RuntimeEnvironment` 与 shared settings models，切断 `LLMController -> XRepo` 依赖。
- `app/repo` 新增 `XRepoRuntimeGateway` 适配器，把现有 `XRepo` 能力暴露给 runtime。
- `composebase` 接收 `ui/infra` 代码，但不接收 `ui/nexus` 页面与业务状态。
- `agent-runtime` 接收 `chat/**`，`app` 继续保留 `mod`、`repo`、`app/ui/nexus`、`Entrance`。

### Phase 2 任务规划摘要
- 共拆分 6 个 Feature、6 个 Batch、77 个 Task。
- `F-04` 与 `F-05` 为大体量机械迁移，分别独占 `B-03`、`B-05`。
- `F-03` 已补入模型下沉波及的 `LocalSettingsCodec.kt` 与 3 个 UI 文件 import 修正，避免 Phase 3 漏改。
- `F-06` 被放在 `chat` 文件迁移之后，确保 runtime 去耦改造发生在新模块上下文内。

## Decisions
- D-01: 重构范围采用“两刀方案”
- D-02: `ui/infra` 迁入 `composebase`
- D-03: `chat` 迁入新模块 `agent-runtime`
- D-04: 机械迁移由用户在 AS 完成，ASC 负责边界与构建改造
- D-05: 迁移后的源码优先保持原 package，不把包重命名与模块迁移绑定
- D-06: 共享配置模型下沉到 `agent-runtime`，不再继续留在 `app/repo`
- D-07: 通过 `RuntimeSettingsGateway` + `XRepoRuntimeGateway` 消除 runtime 对 `app` 的反向依赖
- D-08: `ui/infra` 与 `chat` 机械迁移按“1 文件 = 1 Task”展开，避免目录级黑箱迁移
- D-09: 共享模型下沉影响 `LocalSettingsCodec.kt`、`ConfigureState.kt`、`McpSettingsState.kt`、`CustomToolsSettingsContent.kt`，这些文件必须纳入执行计划

## Batch Pause Mode
每个 Batch 完成后人工确认（半自动执行）

## Batch Progress
- [ ] B-01 `F-01` 新模块与 Gradle 骨架
- [ ] B-02 `F-02` runtime shared settings 契约层
- [ ] B-03 `F-04` UI Infra 物理迁移到 composebase
- [ ] B-04 `F-03` app/repo 接入与模型下沉收口
- [ ] B-05 `F-05` Chat Runtime 物理迁移到 agent-runtime
- [ ] B-06 `F-06` 迁移后 runtime 源码去 app 依赖

## Task Progress
- [ ] T-01 ~ T-05: `F-01` 新模块与 Gradle 骨架
- [ ] T-06 ~ T-08: `F-02` runtime shared settings 契约层
- [ ] T-09 ~ T-17: `F-03` app/repo 接入与模型下沉收口
- [ ] T-18 ~ T-47: `F-04` UI Infra 物理迁移到 composebase
- [ ] T-48 ~ T-71: `F-05` Chat Runtime 物理迁移到 agent-runtime
- [ ] T-72 ~ T-77: `F-06` 迁移后 runtime 源码去 app 依赖
