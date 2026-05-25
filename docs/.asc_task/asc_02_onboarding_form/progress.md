# ASC-02 Progress

## Project

- Name: `asc_02_onboarding_form`
- Scope: `ConfigurePage` 表单化

## Current Phase

- Phase 3 completed
- Waiting for user review
- Configure form UI / ViewModel wiring 已落地

## Context

- `ASC-01` 已完成并验收通过
- 当前导航结构以源码为准，不回到已删除的 `UI-PRD.md`
- `ASC-02` 只处理 `ConfigurePage` 表单化，不并入 `DonePage` 去套壳或 `ChatOnly` 路径修正
- 页面级 ViewModel 对 `ConfigurePage` 是合理且被允许的

## Findings

- `ConfigurePage` 已替换为真实表单，接入 endpoint override、model、api key 输入
- `NexusPages.kt` 已接入页面级 `ConfigureViewModel`，保存成功后才进入 `DonePage`
- `LiquidSecretTextField` 已落地，api key 支持隐藏/显示
- Configure strings 已从占位文案切换为正式表单文案
- `DonePage` 去套壳与 `ChatOnly` 流转修正仍保持在 ASC-02 范围外

## Decisions

- `ConfigurePage` 首版表单包含：
  - endpoint 覆写开关
  - endpoint 输入框
  - model 输入框
  - key 输入框
- 官方 endpoint 按 provider 切换
- 本轮新增 `provider` 持久化字段
- provider metadata 放 Kotlin，不放 strings
- 采用 `sealed interface` / 分层 token 路线
- 本轮只做 token 设计，不真正落地 screen 背景换肤
- endpoint override 不单独持久化布尔值，初始化时通过已保存 endpoint 与 provider 官方 endpoint 的比较反推
- `api key` 采用专用 secret input 组件而不是直接污染基础 `LiquidTextField`
- `NexusPages.kt` 在实现后只负责页面装配，不再保留 provider 业务硬编码分支

## Validation

- Standard ASC validator scripts not found in repository
- Manual review used instead of scripted validation
- Batch 2 手工核对目标已覆盖：真实表单、secret input、保存后跳转

## Next Step

- 预留后续人工检查：不同 provider 默认 endpoint、override 开关回退、保存后进入 `DonePage`
- `DonePage` 和 `ChatOnly` 继续留给后续 ASC
