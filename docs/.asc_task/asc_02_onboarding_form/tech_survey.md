# ASC-02 Onboarding 表单化技术调研

## 1. 任务目标

将 `ConfigurePage` 从占位展示页升级为真实 onboarding 表单页。

本任务当前已明确的目标：

- 为 `ConfigurePage` 引入页面级 ViewModel
- 将页面做成 provider-aware 表单
- 支持 `endpoint` 覆写开关
- 支持真实输入 `model` 与 `api key`
- 为 `api key` 提供隐藏/显示能力
- 将所选 provider 持久化到本地设置
- 为后续 provider 视觉扩展建立可持续的 token 结构

本任务当前不包含：

- 修改导航结构
- 处理 `DonePage` 去套壳
- 修正 `ChatOnly` 路径
- 在 ASC-02 真正落地 provider screen 背景换肤

## 2. 当前源码事实

### 2.1 当前 onboarding 流转

当前导航流为：

- `StartupPage`
- `ProviderPickPage`
- `ConfigurePage(providerId)`
- `DonePage`
- `HomePage`

其中：

- `ProviderPickPage` 已经能把 `providerId` 传给 `ConfigurePage`
- `ConfigurePage` 目前只根据 `providerId` 切换按钮颜色
- 点击完成后直接进入 `DonePage`
- 没有真实输入、校验或保存逻辑

相关入口文件：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/NexusPage.kt`

### 2.2 当前状态管理样板

仓库已经有稳定的页面级 ViewModel / MVI 路线：

- `pageViewModel()` 绑定页面级生命周期
- `ComposeMVIViewModel` 作为状态、意图和副作用基座
- `McpSettingsViewModel` 是最接近“有真实表单和保存流程”的样板

结论：

- `ConfigurePage` 上 ViewModel 符合项目约定
- 不需要发明新模式，直接复用现有 page-scope VM + MVI 即可

### 2.3 当前配置数据模型

当前 `LocalSettings` 已有：

- `endpoint`
- `api_key`
- `model`
- `prompt`

但尚未有：

- `provider`
- endpoint 覆写开关字段

当前 App 默认设置会种入一套默认值，其中 `endpoint` 当前只有一个全局默认值。

结论：

- 如果要做 provider-aware onboarding，就需要新增 provider 持久化字段
- endpoint 的“官方默认值”不应继续只靠一个全局默认值表达

## 3. 已澄清的产品约束

### 3.1 表单结构

用户已明确首版表单结构为：

- 一个 `LiquidToggle`，控制是否覆写 endpoint
- 一个 endpoint `LiquidTextField`
- 一个 model `LiquidTextField`
- 一个 key 输入组件

endpoint 的行为规则：

- toggle 关闭时：
  - 文本框不可聚焦
  - 文本框显示当前 provider 的官方 endpoint
  - 若用户此前编辑过自定义 endpoint，也要被官方 endpoint 覆盖显示
- toggle 打开时：
  - 文本框允许编辑
  - 用户可输入自定义 endpoint

### 3.2 provider 默认 endpoint

用户已明确：

- 官方 endpoint 需要按 provider 切换
- 不采用全局同一个 endpoint

### 3.3 provider 持久化

用户已明确：

- ASC-02 中要新增 `provider` 字段并落盘

### 3.4 provider metadata 的承载位置

用户补充的真实倾向是：

- provider 名称、颜色、官方 endpoint 更像 provider metadata
- 它们不需要多语言，放在 Kotlin 中比放到 `strings.xml` 更合理

当前判断：

- 这个判断是对的
- `strings.xml` 应只保存页面文案、字段标签、说明、错误提示
- provider metadata 应集中收口到 Kotlin 模型

### 3.5 视觉扩展边界

用户希望后续 provider 可以进一步扩展视觉，例如：

- 按钮色
- 文本色
- 图标背景色
- 未来可能扩展到 screen 背景色

但用户也明确选择：

- ASC-02 只做 token 设计，不在本轮真正落地 screen 背景切换

这意味着：

- 本轮要设计可扩展的视觉 token 分层
- 但不要把页面背景换肤也塞进 ASC-02 的实现范围

## 4. 方案发散

### 方案 A：集中式 `ProviderSpec` + 分层 token

做法：

- 引入 `sealed interface` / `sealed class` 表达 provider metadata
- 将 `id`、品牌名、官方 endpoint、图标信息、按钮 token、页面 token 集中管理
- `ConfigurePage` 的 ViewModel 根据 `providerId` 或已保存的 `provider` 读取 `ProviderSpec`
- 本轮只真正使用：
  - 官方 endpoint
  - 按钮相关 token
  - 表单可用的基础页面 token
- 为 screen 背景预留 token 字段，但不在本轮启用

优点：

- provider 逻辑单点收口
- 后续 ASC-03 / ASC-04 / settings provider 页都能复用
- 最符合用户希望的“sealed interface，方便后续扩展”
- 能避免颜色字段越加越乱

缺点：

- 需要先设计 token 语义边界
- 比“就地补字段”多一点前期设计工作

### 方案 B：最小改动直写

做法：

- 保留 `NexusPages.kt` 里的 provider 颜色分支
- 在 `ConfigurePage` 本地补 endpoint 开关和表单状态
- provider 默认 endpoint 也写在页面或 ViewModel 内部

优点：

- 改动路径最短
- 适合只追求功能可用

缺点：

- provider 信息继续分散
- 后续接 provider 背景、provider settings、provider 文案时会快速失控
- 不符合用户对后续扩展性的担忧

### 方案 C：动态字段 schema

做法：

- 不只抽 provider metadata，还把页面字段也 schema 化
- 页面按 schema 动态渲染 form

优点：

- 扩展性最强

缺点：

- 对当前 4 个固定 provider 明显过重
- 会把 ASC-02 放大成“表单引擎设计”

## 5. 推荐方案

推荐采用 **方案 A：集中式 `ProviderSpec` + 分层 token**。

原因：

- 用户已明确偏向 `sealed interface`
- provider 视觉后续一定会继续扩展
- 当前仓库的 provider 颜色逻辑已经散在 `NexusPages.kt` 中，继续直写只会加剧分散
- 只要控制好 token 分层，并明确“本轮只设计 token、不落地 screen 背景”，就能兼顾扩展性与边界

## 6. 设计约束

### 6.1 本轮必须坚持的约束

- 不修改当前导航结构
- `ConfigurePage` 是本轮核心，`DonePage` 不并入
- `ChatOnly` 路径不在本轮处理
- provider metadata 放 Kotlin，不放 strings
- 页面文案仍然进入资源文件
- `api key` 输入组件允许为 ASC-02 新增专用 UI 组件

### 6.2 token 设计约束

provider token 不能只是一坨颜色字段，至少需要按语义分层：

- provider identity
- button tokens
- icon / badge tokens
- page tokens

并明确：

- 本轮 page tokens 只做结构设计
- 不把 provider screen background 切换作为 ASC-02 必须落地项

## 7. 风险与注意事项

### 风险 1：token 语义混乱

如果直接用“按钮内容色”兼作“页面正文色”或“页面背景色”，后续会很难维护。

控制方式：

- 在 Phase 1 明确 token 语义边界
- 将“按钮 token”和“页面 token”分开

### 风险 2：endpoint 覆写状态与真实持久化脱节

如果只在 UI 层覆盖显示，而不设计清楚保存规则，后续实际保存值会混乱。

控制方式：

- 在 Phase 1 明确 toggle 开关关闭时，最终落盘值是否写回官方 endpoint
- 明确 ViewModel 内部源状态与展示状态的关系

### 风险 3：把 ASC-02 扩成视觉重构

一旦顺手落地 screen 背景换肤、DonePage 改造、ChatOnly 修正，范围会失控。

控制方式：

- 明确把这些点留给后续 ASC
- 本轮只做表单与 provider metadata 基建

## 8. 推荐的 Phase 1 输入

下一阶段架构设计应重点回答：

- `ProviderSpec` 的 sealed interface 结构如何定义
- `ProviderVisualTokens` 如何分层
- `ConfigureUiState` / `ConfigureIntent` / `ConfigureEffect` 如何建模
- endpoint 覆写开关的状态机如何定义
- provider 与 endpoint/model/apiKey 的持久化写回策略如何定义
- `api key` 隐藏输入组件是扩展 `LiquidTextField` 还是包一层新组件

## 9. 校验说明

当前仓库仍未发现标准 ASC validator scripts，因此本轮 Phase 0 继续采用轻量 ASC：

- 通过源码阅读和人工校验完成调研
- 不伪造脚本执行结果
