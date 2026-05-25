# ASC-01 Progress

## Project

- Name: `asc_01_strings_baseline`
- Scope: strings 基线整理

## Current Phase

- Phase 2 completed
- Waiting for user confirmation to enter implementation

## Context

- 当前导航结构以源码为准，不再依据已删除的 `UI-PRD.md`
- 项目级约束记录在 `ASC_may_25.md`
- ASC-01 仅聚焦 strings 基线，不扩散到页面结构改造
- 推荐方案为“基线先行，渐进迁移”

## Findings

- `app/src/main/res/values/strings.xml` 当前共 107 条字符串
- `nexus_*` 当前分布于 13 个 Kotlin 文件
- `nexus_home_headline` 与 `nexus_home_description` 为已确认死 key
- `app/src/main/res/values-en/strings.xml` 当前仍存在，计划在 ASC-01 中删除

## Decisions

- 新增 key 禁止继续使用 `nexus_*`
- UI 页面使用 `ui_*` 业务域
- 模块提示使用模块域 key，例如 `mcp_*`、`custom_tool_*`
- ASC-01 不做全量 rename

## Validation

- Standard ASC validator scripts not found in repository
- Manual review used instead of scripted validation

## Next Step

- Start implementation for ASC-01
- Modify `app/src/main/res/values/strings.xml`
- Delete `app/src/main/res/values-en/strings.xml`
- Run final grep-based safety checks and update progress after implementation
