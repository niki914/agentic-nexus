# ASC-01 Progress

## Project

- Name: `asc_01_strings_baseline`
- Scope: strings 基线整理

## Current Phase

- Phase 3 completed
- Implementation finished and awaiting user review

## Context

- 当前导航结构以源码为准，不再依据已删除的 `UI-PRD.md`
- 项目级约束记录在 `ASC_may_25.md`
- ASC-01 仅聚焦 strings 基线，不扩散到页面结构改造
- 用户在进入 Phase 3 时追加要求：现有 `nexus_*` 前缀也必须清理，所有字段按规则域落位
- 因此实际实现边界从“结构先行、渐进迁移”收敛为“本轮全量资源 key 迁移 + Kotlin 引用同步，不改页面逻辑”

## Findings

- `app/src/main/res/values/strings.xml` 当前共 107 条字符串
- `nexus_*` 当前分布于 13 个 Kotlin 文件
- `nexus_home_headline` 与 `nexus_home_description` 为已确认死 key
- `app/src/main/res/values-en/strings.xml` 已按本轮决定删除
- `app/src/main` 下的 `R.string.nexus_*` 引用已清零
- `app/src/main/res` 下的 `name="nexus_*"` 资源定义已清零

## Decisions

- 新增 key 禁止继续使用 `nexus_*`
- UI 页面使用 `ui_*` 业务域
- 模块提示使用模块域 key，例如 `mcp_*`、`custom_tool_*`、`builtin_tool_*`
- 用户已明确批准本轮执行全量 rename，并删除旧英文资源文件

## Validation

- Standard ASC validator scripts not found in repository
- Manual review used instead of scripted validation
- `grep` 确认无 `R.string.nexus_*` 残留
- `grep` 确认无 `name="nexus_*"` 资源定义残留
- `GetDiagnostics` 抽查核心改动文件无诊断错误

## Next Step

- Wait for user review and acceptance of ASC-01 implementation
- If accepted, move to the next ASC in `ASC_may_25.md`
