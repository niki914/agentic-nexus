# ASC-02 Onboarding 表单化架构设计

## 1. 设计目标

ASC-02 的目标不是简单把 `ConfigurePage` 填几个输入框，而是把它升级为一个可持续扩展的 provider-aware onboarding 表单页。

本设计要同时解决四类问题：

- 把当前占位页升级为真实表单页
- 让 provider 的业务信息与视觉信息有稳定承载结构
- 让 endpoint 覆写开关有清晰且可验证的状态机
- 为后续 provider 视觉扩展预留空间，但不把本轮变成页面重绘任务

## 2. 设计边界

### 2.1 本阶段会做

- 为 `ConfigurePage` 引入页面级 ViewModel
- 把 `ConfigurePage` 改造成 provider-aware 表单页
- 新增 provider metadata 模型
- 新增 provider 持久化字段
- 设计并实现 provider 视觉 token 分层
- 落地 endpoint 覆写开关
- 落地 `model` 和 `api key` 输入
- 为 `api key` 增加隐藏/显示输入能力

### 2.2 本阶段不会做

- 不修改当前导航结构
- 不改 `DonePage` 的套壳问题
- 不修正 `ChatOnly` 流转
- 不在本轮真正切换 `ConfigurePage` 的 screen 背景
- 不做 provider schema 引擎化

## 3. 架构总览

ASC-02 采用以下结构：

1. `ProviderSpec` 负责承载 provider metadata
2. `ConfigureViewModel` 负责表单状态、状态机、校验和持久化
3. `ConfigurePageContent` 负责渲染状态和转发事件
4. provider metadata 和表单文案分离：
   - provider metadata 进 Kotlin
   - 用户可见页面文案进 strings

这意味着：

- provider 的官方 endpoint、品牌名、图标与视觉 token 不再散在 `NexusPages.kt`
- 页面状态不再堆在 Composable 中
- `NexusPages.kt` 只负责导航装配和页面依赖注入

## 4. Provider metadata 设计

### 4.1 设计原则

provider metadata 不能只是一组“想到什么加什么”的颜色字段，否则一旦未来扩展页面背景、icon badge、说明块高亮，就会失控。

因此本设计采用：

- `sealed interface ProviderSpec`
- provider identity 与 provider visual tokens 分层
- button token 与 page token 分层

### 4.2 推荐数据结构

```kotlin
sealed interface ProviderSpec {
    val id: String
    val brandName: String
    val officialEndpoint: String
    val iconRes: Int
    val tintIcon: Boolean
    val visualTokens: ProviderVisualTokens
}

data class ProviderVisualTokens(
    val button: ProviderButtonTokens,
    val iconBadge: ProviderIconBadgeTokens,
    val page: ProviderPageTokens,
)

data class ProviderButtonTokens(
    val darkContainerColorRes: Int,
    val lightContainerColorRes: Int,
    val darkContentColorRes: Int,
    val lightContentColorRes: Int,
)

data class ProviderIconBadgeTokens(
    val darkContainerColorRes: Int?,
    val lightContainerColorRes: Int?,
    val darkContentColorRes: Int?,
    val lightContentColorRes: Int?,
)

data class ProviderPageTokens(
    val preferredAccentColorRes: Int,
    val suggestedOnAccentMode: OnAccentMode,
    val reservedDarkBackgroundColorRes: Int? = null,
    val reservedLightBackgroundColorRes: Int? = null,
)

enum class OnAccentMode {
    Light,
    Dark,
    AutoContrast,
}
```

### 4.3 为什么这样分层

#### `button`

用于当前已经真实落地的按钮颜色。

这是现阶段最明确、最稳定的 token 层。

#### `iconBadge`

用于 provider 选择卡片或未来配置页上的图标底色/图标前景色。

即使现在暂未全部使用，也应该把语义槽位定出来，避免后续滥用 button token。

#### `page`

用于 provider 页面级视觉扩展。

本轮只做结构设计，不强制在 `ConfigurePage` 上启用真正的 provider screen background。

其职责是：

- 提供页面 accent 语义
- 提供后续页面背景色保留位
- 提供正文前景色的判定模式

### 4.4 provider 注册方式

建议采用单点注册，而不是在多个 `when(providerId)` 里散写：

```kotlin
object ProviderSpecs {
    val all: List<ProviderSpec> = listOf(
        DeepSeekSpec,
        OpenAiSpec,
        AnthropicSpec,
        GoogleSpec,
    )

    fun find(providerId: String?): ProviderSpec = ...
}
```

这样可以保证：

- `ProviderPickPage`
- `ConfigurePage`
- 后续 provider settings 页

都从同一份 metadata 读取信息。

## 5. `ConfigurePage` 状态设计

### 5.1 ViewModel 结构

建议新增：

```kotlin
class ConfigureViewModel(
    initialProviderId: String?,
    loadSettings: suspend () -> LocalSettings,
    saveSettings: suspend (LocalSettings) -> Unit,
) : ComposeMVIViewModel<ConfigureIntent, ConfigureUiState, ConfigureEffect>()
```

页面侧通过 `pageViewModel()` 获取该 VM。

### 5.2 `UiState` 设计

```kotlin
data class ConfigureUiState(
    val providerSpec: ProviderSpec,
    val endpointOverrideEnabled: Boolean,
    val endpointInput: String,
    val modelInput: String,
    val apiKeyInput: String,
    val apiKeyVisible: Boolean,
    val isSaving: Boolean,
    val saveEnabled: Boolean,
    val inlineError: ConfigureInlineError? = null,
)
```

### 5.3 为什么 `endpointInput` 仍然保留在 state 中

即使 toggle 关闭时文本框显示官方 endpoint，`UiState` 里仍应保留 `endpointInput`，原因是：

- toggle 打开后需要恢复可编辑内容
- ViewModel 需要明确区分“展示值”和“实际自定义值”

但 UI 渲染时必须遵守下面的展示规则：

```kotlin
val displayedEndpoint = if (endpointOverrideEnabled) {
    state.endpointInput
} else {
    state.providerSpec.officialEndpoint
}
```

## 6. endpoint 覆写开关状态机

### 6.1 状态规则

#### 开关关闭

- endpoint 输入框不可聚焦
- endpoint 输入框 `enabled = false`
- 展示值固定为 `providerSpec.officialEndpoint`
- 保存时写回的 `endpoint` 也应使用 `providerSpec.officialEndpoint`

#### 开关打开

- endpoint 输入框允许编辑
- 展示值来自 `endpointInput`
- 保存时写回 `endpointInput`

### 6.2 用户已明确要求的行为

用户要求：

- 即使之前编辑过 endpoint，只要重新关闭开关，也要覆写为官方 endpoint

因此本设计明确：

- toggle 从 `true -> false` 时，ViewModel 立即把 `endpointInput` 重置成 `providerSpec.officialEndpoint`

这样可以让：

- 展示状态
- ViewModel 状态
- 最终落盘结果

保持一致。

### 6.3 provider 切换时的行为

虽然本轮不改导航结构，但 `ConfigurePage` 是通过 `providerId` 进入的，因此仍需定义 provider 初始化规则：

- 页面初始加载时，使用页面参数 `providerId`
- 若页面参数为空，再回退到已保存的 `provider`
- 若两者都为空，再回退到一个稳定默认 provider

当前 ASC-02 不设计“页面内切换 provider”，因此 provider 变化主要发生在重新进入该页时。

## 7. Intent / Effect 设计

### 7.1 `Intent`

```kotlin
sealed interface ConfigureIntent {
    data class Initialize(val providerId: String?) : ConfigureIntent
    data class SetEndpointOverride(val enabled: Boolean) : ConfigureIntent
    data class UpdateEndpoint(val value: String) : ConfigureIntent
    data class UpdateModel(val value: String) : ConfigureIntent
    data class UpdateApiKey(val value: String) : ConfigureIntent
    data object ToggleApiKeyVisibility : ConfigureIntent
    data object Save : ConfigureIntent
}
```

### 7.2 `Effect`

```kotlin
sealed interface ConfigureEffect {
    data object SaveSucceeded : ConfigureEffect
    data class SaveFailed(val reason: ConfigureErrorReason) : ConfigureEffect
}
```

### 7.3 事件分工原则

- 可恢复、可展示的错误优先进入 `UiState.inlineError`
- 导航或一次性提示进入 `Effect`

这样可以避免：

- 把所有错误都做成 toast/snackbar
- 让表单状态和一次性事件混在一起

## 8. 持久化设计

### 8.1 新增字段

`LocalSettings` 需要新增：

- `provider`

推荐 key：

- `"provider"`

### 8.2 保存规则

点击保存时，写入：

- `provider`
- `model`
- `api_key`
- `endpoint`

其中：

- 若 `endpointOverrideEnabled == false`，写入 `providerSpec.officialEndpoint`
- 若 `endpointOverrideEnabled == true`，写入 `endpointInput`

### 8.3 是否持久化 override 开关

本设计建议：

- 本轮不新增 `endpoint_override_enabled` 持久化字段

原因：

- 最终运行时真正消费的是 `endpoint`
- 只要 `endpoint` 与 `providerSpec.officialEndpoint` 相同，就等价于“未覆写”
- 额外持久化一个布尔值会引入多一组潜在不一致状态

因此初始化时可按以下规则推导：

```kotlin
val endpointOverrideEnabled = savedEndpoint.isNotBlank() &&
    savedEndpoint != providerSpec.officialEndpoint
```

这是本设计的关键约束之一。

## 9. UI 组件设计

### 9.1 页面骨架

`ConfigurePageContent` 需要从“居中说明文案页”转成“上部说明 + 中部表单 + 底部主按钮”的结构。

建议保留：

- 当前页面的 `hazeSource`
- 当前 provider button token 驱动的主按钮

建议改造：

- 将中间内容区改为垂直表单容器
- 保留 headline/description，但位置移到表单上方

### 9.2 endpoint 开关行

建议使用：

- 文案说明块
- `LiquidToggle`

形成单独的设置行组件，例如：

```kotlin
@Composable
fun EndpointOverrideRow(...)
```

不要把开关逻辑直接堆在页面根布局里。

### 9.3 endpoint 与 model 输入

直接复用 `LiquidTextField`。

其中 endpoint 输入必须满足：

- 开关关闭时 `enabled = false`
- 关闭时显示 provider 官方 endpoint
- 关闭时不能聚焦，也不会触发交互高光

`LiquidTextField` 当前已满足：

- `enabled = false` 时禁用焦点与交互高光

因此无需为 endpoint 输入重造组件。

### 9.4 api key 输入

建议新增一个轻包装组件，而不是直接魔改基础 `LiquidTextField`：

```kotlin
@Composable
fun LiquidSecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    ...
)
```

原因：

- `api key` 的“隐藏/显示”是明确的业务扩展，而不是所有 `LiquidTextField` 都需要的基础能力
- 包一层更容易保持基础组件稳定
- 未来 settings 页如需要 secret input，也可直接复用

## 10. `NexusPages.kt` 改造设计

当前 `NexusPages.kt` 中：

- provider 颜色逻辑在本文件内
- `ConfigurePage` 直接渲染纯展示版 `ConfigurePageContent`

本轮应改为：

- `ProviderPickPage` 从 `ProviderSpecs` 读取 provider 选项
- `ConfigurePage` 从 `ProviderSpecs` 查当前 provider
- `ConfigurePage` 分支只负责：
  - 注入 providerId
  - 注入 load/save 闭包
  - 监听 `SaveSucceeded`
  - 保存成功后进入 `DonePage`

这样 `NexusPages.kt` 继续承担“页面装配层”，但不再承担 provider 业务规则本体。

## 11. strings 设计

### 11.1 应进入 strings 的内容

- 页面 headline / description
- endpoint 开关标题与说明
- endpoint / model / api key 字段标签与 placeholder
- 保存按钮文案
- 表单错误提示
- api key 显示/隐藏按钮的可见文案或 content description

### 11.2 不进入 strings 的内容

- provider 英文品牌名
- 官方 endpoint
- provider metadata

这些内容应保留在 Kotlin 的 `ProviderSpec` 中。

## 12. 风险控制

### 12.1 token 扩张失控

控制方式：

- token 只按语义层定义，不为未来每个想象中的场景预定义字段
- 本轮只允许 `button`、`iconBadge`、`page` 三层

### 12.2 持久化规则不一致

控制方式：

- 不持久化 endpoint override 布尔值
- 只持久化最终生效的 endpoint
- 初始化通过 endpoint 与官方 endpoint 比较反推 override 状态

### 12.3 视觉范围失控

控制方式：

- `page` token 只做结构定义
- `ConfigurePage` 本轮不真正切换 screen background

## 13. Phase 2 输入约束

下一阶段实施计划必须遵守：

- 至少拆出 provider metadata 文件
- 至少拆出 `ConfigureViewModel`
- 至少拆出 `LiquidSecretTextField` 或同等职责组件
- `ConfigurePageContent` 改造成真实表单页
- `NexusPages.kt` 只做装配，不保留 provider 业务硬编码分支
- 不把 `DonePage` 和 `ChatOnly` 一并拉进本批

## 14. 人工校验结果

本设计通过人工校验确认：

- 符合 `ASC_may_25.md` 对 `ConfigurePage` 上 ViewModel 的要求
- 没有把 provider metadata 错放到资源文件
- 没有把 provider 视觉扩展直接升级成 screen 重绘任务
- 没有扩大到 `DonePage` / `ChatOnly`
- 与当前源码导航结构兼容
