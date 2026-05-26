# ASC-03 Progress

## Project

- Name: `asc_03_ui_liquid_unification`
- Scope: `app/ui` Liquid 基建统一与 `liquid_example` 清理

## Current Phase

- Phase 3 in progress
- Batch 1 completed
- Waiting for Batch 1 acceptance
- `tech_survey.md` / `tech_design.md` / `plan.md` 已产出
- Batch Pause Mode: 每个 Batch 完成后暂停，等待用户验收

## Context

- `ASC-01` 和 `ASC-02` 已存在历史产物，本任务在其基础上继续推进 UI 基建统一
- 当前任务不是新增业务功能，而是 UI 基础设施与主线页面的受控重构
- 配置页已正式纳入本次统一范围
- 用户明确要求每个 Batch 后人工回归，不做一次性全量重构
- 用户明确要求最终删除 `app/ui/liquid_example`
- 用户明确提醒 `layerBackdrop` 不能乱用，必须避免递归或错误层级

## Findings

- `InteractiveHighlight` 与 `DragGestureInspector` 已存在，但按钮、输入框、顶栏按钮仍有重复接线
- `LiquidTextField` 与 `LiquidSecretTextField` 存在明显镜像复制
- 配置页已经接入主线 Liquid 输入组件，但 toggle 仍依赖 `liquid_example`
- shape 目前至少有胶囊、固定半径输入、固定半径连续曲率三条语义线
- `ActionBarButton` 是当前最明显的分叉点之一

## Decisions

- 采用单 ASC、多 Batch 路线
- 实现阶段按 Batch 人工回归
- `liquid_example` 在最终 Batch 删净
- shape 统一放在后期收尾 Batch
- 优先统一交互内核与容器层，不做万能控件
- 配置页进入本次统一主线
- `layerBackdrop` 作为显式风险约束写入设计
- `LiquidScreen` 顶栏 API 本轮保持不变，不引入 `ActionSpec`
- 配置页 toggle 以 `StyledSwitch` 为正式实现基础，收口为正式 `LiquidToggle`
- shape 统一采用“原语优先”路线

## Validation

- Standard ASC validator scripts not found in repository
- Manual review used instead of scripted validation
- Phase 0 通过源码阅读与用户澄清完成
- Phase 1 / Phase 2 通过设计完整性和批次依赖人工校验完成
- Batch 1 通过源码 diff、限定文件范围检查与 IDE diagnostics 人工校验完成

## Batch Progress

- [x] Batch 1: F-01 按钮交互内核统一
- [x] T-01 `ui/infra/interaction/LiquidInteractiveStyle.kt`
- [x] T-02 `ui/infra/interaction/LiquidInteractiveLayer.kt`
- [x] T-03 `ui/infra/component/LiquidButton.kt`
- [x] T-04 `ui/infra/component/TintLiquidButton.kt`
- [x] T-05 `ui/infra/component/MaterialTintLiquidButton.kt`
- [x] T-06 `ui/infra/ActionBarButton.kt`
- [ ] Batch 2: F-02 输入区容器统一
- [ ] Batch 3: F-03 Toggle 正式化与 demo 断引用
- [ ] Batch 4: F-04 Shape 原语统一与删除收尾

## Modified Files

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt`

## Next Step

- 等待用户验收 Batch 1
- 用户确认后再进入 Batch 2：输入区容器统一
