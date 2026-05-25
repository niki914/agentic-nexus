# ASC-02 Onboarding Form Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `ConfigurePage` 从占位说明页改造成真实的 provider-aware onboarding 表单页，并完成 provider 持久化、endpoint 覆写开关、model/api key 输入与保存跳转。

**Architecture:** 本轮以 `ProviderSpec` 作为 provider metadata 单一事实源，以页面级 `ConfigureViewModel` 承载表单状态、校验和持久化。UI 层只负责渲染状态与转发 intent，`NexusPages.kt` 只保留页面装配与导航流转，不再承担 provider 业务规则。

**Tech Stack:** Android Compose, page-scope ViewModel, `ComposeMVIViewModel`, local settings persistence, XML string resources

---

## 文件职责

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ProviderSpec.kt`
  - 新建 provider metadata 单点注册
  - 承载 provider identity、官方 endpoint、图标信息、视觉 token
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt`
  - 新建 `ConfigureUiState` / `ConfigureIntent` / `ConfigureEffect`
  - 新建 `ConfigureViewModel`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt`
  - 新建隐藏/显示密钥输入组件
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt`
  - 从占位说明页升级为真实表单页
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
  - 改为从 `ProviderSpec` 装配 provider 选项与 `ConfigurePage`
  - 注入页面级 ViewModel、load/save 闭包与保存成功跳转
- `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt`
  - 为 `LocalSettings` 新增 `provider`
- `app/src/main/res/values/strings.xml`
  - 增加 `ConfigurePage` 表单相关 UI 文案

## 实施约束

- 不修改导航结构
- 不把 `DonePage` 去套壳并入本轮
- 不把 `ChatOnly` 路径修正并入本轮
- provider metadata 放 Kotlin，不进入 strings
- 不新增 `values-en/strings.xml`
- 不单独持久化 endpoint override 布尔值
- `NexusPages.kt` 实现后只做装配，不再保留 provider 业务硬编码分支

## Feature 拆分

### F-01 Provider Metadata 收口

- 目标：以 `ProviderSpec` 收口 provider 的品牌名、官方 endpoint、图标信息和视觉 token
- 预估改动：中
- 依赖：无

### F-02 Configure 状态与持久化

- 目标：建立 `ConfigureViewModel`、表单状态机与保存逻辑
- 预估改动：中
- 依赖：F-01

### F-03 Configure 表单 UI

- 目标：实现 endpoint toggle、endpoint/model/api key 输入与密钥隐藏/显示
- 预估改动：中
- 依赖：F-02

### F-04 页面装配与导航收尾

- 目标：让 `ProviderPickPage` 和 `ConfigurePage` 使用新 metadata / 新 ViewModel，并在保存成功后进入 `DonePage`
- 预估改动：小
- 依赖：F-01、F-02、F-03

## Batch 编排

### Batch 1

- F-01 Provider Metadata 收口
- F-02 Configure 状态与持久化

### Batch 2

- F-03 Configure 表单 UI
- F-04 页面装配与导航收尾

---

### Task 1: 新建 `ProviderSpec`

**Files:**
- Create: `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ProviderSpec.kt`
- Modify: `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`

- [ ] **Step 1: 定义 provider metadata 模型**

实现目标：

- `sealed interface ProviderSpec`
- `ProviderVisualTokens`
- `ProviderButtonTokens`
- `ProviderIconBadgeTokens`
- `ProviderPageTokens`
- `OnAccentMode`

关键约束：

- `brandName`、`officialEndpoint` 进 Kotlin
- token 至少拆成 `button` / `iconBadge` / `page`
- `page` token 本轮只设计结构，不强制落地 screen 背景

- [ ] **Step 2: 注册 4 个 provider**

必须覆盖：

- `deepseek`
- `openai`
- `anthropic`
- `google`

每个 provider 至少包含：

- `id`
- `brandName`
- `officialEndpoint`
- `iconRes`
- `tintIcon`
- button token
- icon badge token
- page token

- [ ] **Step 3: 提供统一查询入口**

实现要求：

- 增加 `ProviderSpecs.all`
- 增加 `ProviderSpecs.find(providerId: String?): ProviderSpec`
- 必须有稳定 fallback，避免空 `providerId` 崩溃

- [ ] **Step 4: 替换 `NexusPages.kt` 里的 provider 硬编码选项来源**

替换目标：

- `ProviderPickPage` 里的 4 组 `SelectionOption`
- `providerButtonColors()` 的职责

预期结果：

- `SelectionOption` 由 `ProviderSpec` 派生
- `NexusPages.kt` 不再持有 provider 颜色硬编码分支

- [ ] **Step 5: 自检**

检查项：

- provider metadata 只存在一个单一事实源
- `NexusPages.kt` 不再使用 `when(providerId)` 硬编码颜色
- `ProviderSpec` 能覆盖当前 provider picker 和 configure 按钮的视觉需求

### Task 2: 扩展 `LocalSettings`，新增 `provider`

**Files:**
- Modify: `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt`

- [ ] **Step 1: 为 `LocalSettings` 新增 `provider` getter**

目标：

- 读取 key：`provider`
- 默认值为空字符串

- [ ] **Step 2: 保持现有字段兼容**

要求：

- 不影响现有 `endpoint` / `api_key` / `model` / `prompt`
- 不新增额外布尔字段如 `endpoint_override_enabled`

- [ ] **Step 3: 自检**

检查项：

- `LocalSettings` 仍是只读投影层
- 新增字段不会影响旧设置解析

### Task 3: 新建 `ConfigureViewModel` 与状态机

**Files:**
- Create: `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt`
- Modify: `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`

- [ ] **Step 1: 定义状态模型**

至少包含：

- `ConfigureUiState`
- `ConfigureInlineError`
- `ConfigureErrorReason`

`ConfigureUiState` 至少包含：

- `providerSpec`
- `endpointOverrideEnabled`
- `endpointInput`
- `modelInput`
- `apiKeyInput`
- `apiKeyVisible`
- `isSaving`
- `saveEnabled`
- `inlineError`

- [ ] **Step 2: 定义 intent / effect**

必须覆盖：

- 初始化 provider
- endpoint override 开关切换
- endpoint/model/api key 输入
- api key 显示隐藏切换
- 保存动作
- 保存成功 effect
- 保存失败 effect

- [ ] **Step 3: 实现初始化逻辑**

初始化规则：

- 优先使用页面参数 `providerId`
- 页面参数为空时回退到已保存 `provider`
- 两者都为空时使用稳定默认 provider

override 推导规则：

- 若已保存 endpoint 非空且不等于官方 endpoint，则 `endpointOverrideEnabled = true`
- 否则为 `false`

- [ ] **Step 4: 实现 endpoint override 状态机**

规则必须完全符合设计：

- toggle 关闭时：
  - `endpointInput` 立即重置为 `providerSpec.officialEndpoint`
  - endpoint 输入框后续显示官方 endpoint
- toggle 打开时：
  - endpoint 可编辑

- [ ] **Step 5: 实现保存逻辑**

写回字段：

- `provider`
- `endpoint`
- `model`
- `api_key`

写回规则：

- 未 override 时写官方 endpoint
- override 时写用户输入 endpoint

保存成功后：

- 发出 `SaveSucceeded`

保存失败后：

- 写入 `inlineError` 或发送失败 effect

- [ ] **Step 6: 自检**

检查项：

- 不单独持久化 endpoint override 布尔值
- 保存规则和初始化规则互相闭合
- ViewModel 不直接依赖 Compose API

### Task 4: 新建 `LiquidSecretTextField`

**Files:**
- Create: `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 基于 `LiquidTextField` 包一层 secret input**

组件职责：

- 接收 `value`
- 接收 `visible`
- 接收 `onToggleVisibility`
- 支持 trailing icon 或 trailing action

约束：

- 不直接魔改 `LiquidTextField.kt`
- 保持基础输入组件稳定

- [ ] **Step 2: 加入隐藏/显示能力**

要求：

- 隐藏时不直接明文展示 key
- 显示时可查看原值
- 按钮有可读的 content description 或可见文案

- [ ] **Step 3: 增加所需 strings**

至少新增：

- api key 字段标签
- api key placeholder
- 显示密钥
- 隐藏密钥

- [ ] **Step 4: 自检**

检查项：

- 新组件职责单一
- 不污染 `LiquidTextField`
- 文案全部走资源文件

### Task 5: 改造 `ConfigurePageContent` 为真实表单页

**Files:**
- Modify: `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 改造页面布局**

新布局结构：

- 顶部说明块
- 中部表单区
- 底部主按钮

要求：

- 保留现有 `hazeSource`
- 保留 provider button token 驱动的主按钮

- [ ] **Step 2: 增加 endpoint override 行**

组件要求：

- 有标题和说明
- 有 `LiquidToggle`
- 切换后把事件发送给 ViewModel

- [ ] **Step 3: 接入 endpoint / model / api key 三个输入**

规则：

- endpoint 使用 `LiquidTextField`
- model 使用 `LiquidTextField`
- api key 使用 `LiquidSecretTextField`
- endpoint 在 override 关闭时 `enabled = false`

- [ ] **Step 4: 接入错误展示与保存可用状态**

要求：

- `saveEnabled` 驱动主按钮 enable 状态
- `inlineError` 有对应展示位置
- 不新增硬编码错误文案

- [ ] **Step 5: 增加本页所需 strings**

至少覆盖：

- endpoint override 标题
- endpoint override 说明
- endpoint label / placeholder
- model label / placeholder
- 保存按钮文案
- 校验错误提示

- [ ] **Step 6: 自检**

检查项：

- `ConfigurePageContent` 只消费状态与回调，不自行管理业务状态
- endpoint 关闭 override 时不可聚焦
- 页面仍保持当前 onboarding 的整体视觉语言

### Task 6: 完成页面装配与保存跳转

**Files:**
- Modify: `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`

- [ ] **Step 1: 在 `ConfigurePage` 分支接入页面级 ViewModel**

要求：

- 使用 `pageViewModel()`
- 注入 `loadSettings` / `saveSettings` 闭包
- 将 `providerId` 传入 ViewModel 初始化

- [ ] **Step 2: 监听 `SaveSucceeded`**

要求：

- 保存成功后才进入 `DonePage`
- 不再使用“点按钮直接进入 `DonePage`”的占位逻辑

- [ ] **Step 3: 保持 `DonePage` 和其他导航节点不变**

要求：

- `DonePage` 仍然走当前逻辑
- 不在本任务顺手处理 `DonePage` 去套壳
- 不在本任务顺手处理 `ChatOnly`

- [ ] **Step 4: 实现后安全检查**

检查项：

- `NexusPages.kt` 只做装配
- provider metadata 不再分散在本文件
- Configure 页面成功保存前不能进入 `DonePage`

### Task 7: 实施后验证与文档同步

**Files:**
- Modify: `docs/.asc_task/asc_02_onboarding_form/progress.md`

- [ ] **Step 1: 检查 strings 与 Kotlin 引用一致**

检查目标：

- 新增 strings 均有引用来源
- 无不存在的 `R.string.*`

- [ ] **Step 2: 检查 provider metadata 收口**

检查目标：

- 不再存在第二份 provider 颜色/endpoint 硬编码表

- [ ] **Step 3: 更新 `progress.md`**

更新内容：

- 标记 Phase 2 completed
- 记录计划覆盖的实现批次
- 记录 `DonePage` / `ChatOnly` 仍在范围外

- [ ] **Step 4: 人工验收点**

确认以下行为纳入实现验收：

- 进入不同 provider 的 `ConfigurePage` 时，默认 endpoint 随 provider 切换
- 关闭 endpoint override 后，文本框禁用且显示官方 endpoint
- 重新关闭 override 时，之前自定义 endpoint 被官方 endpoint 覆盖
- `api key` 支持隐藏/显示
- 保存后才进入 `DonePage`

## 自检结果

- 计划已覆盖 `ProviderSpec`、视觉 token、表单状态机、provider 持久化、secret input、页面装配
- 未把 `DonePage` 去套壳和 `ChatOnly` 修正并入本轮
- 未要求新增英文资源
- 计划粒度已按 provider metadata、状态、UI、装配拆开，适合后续 Batch 执行
