# UI PRD: Nexus App Shell v1

## 1. 背景

当前工程已经具备以下基础：

- 有可复用的 Liquid Glass 风格 UI 基建，核心为 `LiquidScreen`、`LiquidScreenState`、`LiquidScreenSwipeContent`
- 有一套自研轻量导航基建，核心为 `NavigationController`
- 有 `HomeDemo` 作为导航与转场实验场
- 有 `SettingsScreen` 作为设置类页面的视觉参考
- 有 `liquid_example` 作为玻璃质感控件、按钮、弹窗的样式参考

当前缺失的是一套完整、统一、可进入开发的正式 UI 方案。项目已经明确会包含 MCP、自定义 Tool、Provider 配置、AI 记忆、内置 Tool 开关、命令黑白名单等配置能力，因此需要先定义稳定的信息架构与页面流，再开始页面开发。

本 PRD 仅覆盖 UI 与交互结构，不覆盖持久化、真实业务调用、网络联调与 Hook 侧逻辑接入。

## 2. 目标

本期目标：

- 建立一套正式的 App Shell UI，而不是继续扩展 demo 页面
- 完成从冷启动到 Home 的完整新手配置流
- 完成 Home 对话测试页骨架
- 完成从 Home 进入设置树的导航结构
- 保证中英双语可扩展，禁止在 Kotlin 中硬编码 UI 文案
- 全部使用 mock 数据驱动，允许使用 MVI ViewModel 管理页面状态

非目标：

- 不实现真实持久化
- 不实现真实 LLM 对话
- 不实现真实 MCP、自定义 Tool 执行
- 不在本期引入 Jetpack Navigation
- 不在本期接入复杂表单校验、导入导出、权限链路

## 3. 产品原则

### 3.1 视觉原则

- 采用现有 `ui.infra` 为正式 UI 外壳
- 视觉风格以轻量、悬浮、玻璃态为主，避免厚重卡片和满屏实体背景
- 顶栏是稳定 chrome，不跟着页面局部布局反复重建
- 重要操作使用单一高识别度主按钮，不堆砌 CTA
- 内容优先，导航层尽量轻

### 3.2 工程原则

- 页面结构优先复用现有基建，拒绝再起一套平行体系
- 文案只允许放在 `strings.xml` / `values-en/strings.xml`
- 表单与设置项优先抽成无状态可复用 composable
- mock 数据由 ViewModel 提供，UI 不直接拼假数据
- 后续接持久化时，尽量不推翻页面层结构

## 4. 用户场景

### 4.1 首次进入

用户首次打开应用，尚未配置 `api_key`，进入 onboarding 流：

`Startup -> BrandCheck -> ProviderPick -> ConfigureBasic -> ConfigureModel -> Done -> Home`

### 4.2 已配置用户

若本地配置中 `api_key` 非空，则跳过 onboarding，直接进入 Home。

### 4.3 仅聊天模式

若设备品牌不受支持，用户仍可进入 Home 的聊天测试骨架，但不展示与宿主强绑定的引导文案。

### 4.4 后续修改配置

用户从 Home 右上角更多菜单进入 Settings，修改 provider、模型、内置 tools、MCP、自定义 tools 等配置。

## 5. 信息架构

### 5.1 顶层结构

```text
App Shell
├── Onboarding Flow
│   ├── Startup
│   ├── BrandCheck
│   ├── ProviderPick
│   ├── ConfigureBasic
│   ├── ConfigureModel
│   └── Done
├── Home
│   └── Chat Test
└── Settings Flow
    ├── SettingsHome
    ├── ProviderModel
    ├── Network
    ├── Memory
    ├── BuiltinTools
    ├── ShellRules
    ├── MCP
    ├── CustomTools
    └── About
```

### 5.2 配置分组

#### Provider & Model

- Provider
- Official BaseURL 开关
- Custom Endpoint / BaseURL
- API Key
- Model
- System Prompt

#### Network

- Proxy

#### Memory

- Memory Enable
- Memory Root Path
- Memory Size Limit

#### Built-in Tools

- Show Tool Calling
- Open URI
- Open App
- Get Device Info
- Run Shell Command
- Fallback To Native

#### Shell Rules

- Run With Root
- Blacklist / Whitelist Mode
- Command List
- Ask Before Execute

#### MCP

- MCP 服务列表
- MCP 启用状态
- MCP 基础描述信息

#### Custom Tools

- Tool 列表
- Tool 名称
- Tool 描述
- 本地脚本路径
- 参数 Schema 摘要
- 启用状态

#### About

- 当前 App 说明
- 开源信息入口

## 6. 核心页面定义

### 6.1 Startup

定位：

- 海报页
- 无配置时的冷启动入口
- 主要承担品牌感与引导，不承担配置逻辑

布局：

- 全屏内容
- 无 title
- 无左右按钮
- 中央为大字标题、副标题、设备 OS 相关说明
- 底部一个大主按钮进入下一步

行为：

- 点击主按钮进入 `BrandCheck`

状态：

- 根据当前 OS 展示不同 subtitle
- 使用 mock 文案，不接真实检测逻辑时可先伪造 OS

### 6.2 BrandCheck

定位：

- 决定当前宿主环境与接下来的配置引导语境

布局：

- 可带 title
- 页面主体是 3 张大卡片
- 候选项：`Breeno`、`XiaoAi`、`Not Supported`

行为：

- 点击 `Breeno` 或 `XiaoAi` 进入 `ProviderPick`
- 点击 `Not Supported` 后展示明显按钮 `Just use chat mode`
- 点击该按钮直接进入 Home

说明：

- 这里不需要真实品牌判断结果绑定，只要 UI 上允许用户手动选择或确认

### 6.3 ProviderPick

定位：

- 只做 Provider 选择，不承载详细配置

布局：

- 有 title
- 有左返回按钮
- 无右按钮
- 主要内容为 Provider Tile 列表或 2x2 栅格

候选项：

- DeepSeek
- OpenAI
- Anthropic
- Google

行为：

- 选择任一 Provider 后进入 `ConfigureBasic`
- 其中 OpenAI / Anthropic 在下一页允许关闭 Official BaseURL 并输入自定义地址
- 其他 Provider 默认走官方地址，也允许后续在 Settings 里再调

### 6.4 ConfigureBasic

定位：

- 第一页基础连接配置

布局：

- 有 title
- 有左返回按钮
- 页面中部是表单卡片
- 页面底部固定双按钮：`Prev`、`Next`

字段：

- Official BaseURL 开关
- Endpoint / BaseURL
- API Key

字段规则：

- 当 Provider 为 OpenAI / Anthropic 时，显示 Official BaseURL 开关
- 当 Official BaseURL 为关闭时，显示 Endpoint / BaseURL 输入项
- 当 Provider 为 DeepSeek / Google 时，可先按 mock 规则直接显示只读文案或隐藏该开关

说明：

- 此页只要求交互可用，不要求真实校验
- 输入框可直接受控于 ViewModel state

### 6.5 ConfigureModel

定位：

- 第二页模型配置

布局：

- 有 title
- 有左返回按钮
- 页面中部是表单卡片
- 页面底部固定双按钮：`Prev`、`Complete`

字段：

- Model
- System Prompt

说明：

- `System Prompt` 使用多行输入
- 允许内置 mock 默认值

### 6.6 Done

定位：

- onboarding 完成确认页

布局：

- 无 title
- 有左返回按钮
- 主体沿用 Startup 的海报式布局
- 底部一个大主按钮
- 主按钮上方或下方提供次级入口

动作：

- 主按钮：`Enter Home`
- 次级入口：`Advanced Settings`
- 左返回：回到 `ConfigureModel`

行为：

- 点击 `Enter Home` 切换到 Home Flow
- 点击 `Advanced Settings` 直接进入 SettingsHome

### 6.7 Home / Chat Test

定位：

- 本期的主页
- 本质上是聊天测试页骨架

布局：

- 无左返回按钮
- 右上角有更多按钮
- 中部为 mock 消息列表
- 底部为悬浮输入区

内容：

- 若无消息，展示空状态与引导文案
- 若有消息，展示 mock user / assistant 气泡

更多菜单：

- Settings
- About
- 可预留 Switch Brand

行为：

- 发送按钮只向本地 mock 列表插入一条用户消息并追加一条假回复
- 不接 `LLMController`

### 6.8 SettingsHome

定位：

- 所有设置的一级目录

布局：

- 有 title
- 有左返回按钮
- 无右按钮
- 主体为设置分组列表

分组入口：

- Provider & Model
- Network
- Memory
- Built-in Tools
- Shell Rules
- MCP
- Custom Tools
- About

### 6.9 ProviderModel

定位：

- 对 onboarding 配置页的持久入口

布局与行为：

- 复用 `ConfigureBasic` 与 `ConfigureModel` 的表单内容
- 但在 Settings 中改为设置页风格，可拆成两个分组卡片，也可拆成二级页面

约束：

- 简单项可以直接内联在一页
- 如果一页信息密度过高，应拆为二级页面，不要硬塞

### 6.10 Network

字段：

- Proxy

### 6.11 Memory

字段：

- Enable
- Root Path
- Size Limit

说明：

- 当前为纯 mock
- 仅体现本地文件记忆的存在感与未来可配置性

### 6.12 Built-in Tools

字段：

- Show Tool Calling
- Open URI
- Open App
- Get Device Info
- Run Shell Command
- Fallback To Native

说明：

- `Run Shell Command` 作为入口项跳到 `ShellRules`

### 6.13 ShellRules

字段：

- Run With Root
- Blacklist / Whitelist Mode
- Command List
- Ask Before Execute

说明：

- `Command List` 可先用多行文本 mock 输入

### 6.14 MCP

定位：

- 展示 MCP Server 列表与启停状态

布局建议：

- 列表卡片，每项含名称、描述、启用开关、状态摘要

本期范围：

- 使用 mock server 列表，不做真实连接状态探测

### 6.15 CustomTools

定位：

- 展示用户预设的本地脚本工具

核心心智：

- 用户通过 GUI 管理工具定义
- AI 实际调用的是工具名
- App 内部再路由到用户预设脚本路径执行

布局建议：

- 列表页展示 Tool 卡片
- 点击进入详情编辑页或弹出编辑表单

字段：

- Name
- Description
- Script Path
- Args Schema Summary
- Enabled

本期范围：

- 全部 mock，不做文件选择器与真实执行

## 7. 交互与导航规则

### 7.1 总体导航

- 使用现有 `NavigationController`
- 不引入 Jetpack Navigation
- onboarding flow 与 settings flow 均运行在统一的 app shell 中

### 7.2 冷启动入口规则

- 若 `api_key` 为空，进入 onboarding
- 若 `api_key` 非空，直接进入 Home

### 7.3 Flow 切换规则

- 从 `Done -> Home` 时不保留 onboarding 返回路径
- 即进入 Home 后，系统返回不应再回到 onboarding
- Settings 退出后回到 Home

### 7.4 顶栏规则

- 顶栏由 `LiquidScreen` 统一承载
- title、左右按钮是否展示、按钮图标与点击事件随导航状态变化
- `Startup` 与 `Done` 允许隐藏 title
- 必要时允许整块 chrome 隐身，以保持海报式页面纯净

### 7.5 返回规则

- onboarding 中部页面允许返回上一步
- Home 本身无左返回
- Settings 树使用标准 push / pop

## 8. 视觉与组件规范

### 8.1 需要复用的现有能力

- `LiquidScreen`
- `LiquidScreenState`
- `LiquidScreenSwipeContent`
- `NavigationController`
- `ActionBarButton`
- `liquid_example` 中的玻璃按钮与弹窗实现思路

### 8.2 建议新增的通用组件

- `LiquidPrimaryAction`
- `LiquidSecondaryAction`
- `SettingsCard`
- `SettingsRowToggle`
- `SettingsRowEntry`
- `SettingsRowInput`
- `ProviderTile`
- `ChatBubble`
- `ChatComposer`

### 8.3 组件职责

#### LiquidPrimaryAction

- 大尺寸主按钮
- 用于 Startup、Done、onboarding 底部主 CTA

#### SettingsCard

- 承载一组设置项
- 提供统一圆角、背景、分隔与内边距

#### SettingsRowToggle

- 映射 switch 类配置项

#### SettingsRowEntry

- 映射导航类配置项
- 支持右箭头、说明文字与当前值摘要

#### SettingsRowInput

- 映射输入型配置项
- 可用于单行或多行文本

## 9. 多语言策略

### 9.1 语言范围

- 本期支持中文与英文
- 默认 `values/` 为中文
- 新增 `values-en/strings.xml` 为英文

### 9.2 文案约束

- 禁止在 Kotlin 中硬编码 UI 字符串
- `Page` 级标题使用 `@StringRes`
- 页面描述、按钮文案、表单 label、空状态文案全部使用资源字符串

### 9.3 命名约定

- `onboarding_*`
- `home_*`
- `settings_*`
- `tools_*`
- `mcp_*`
- `customtool_*`

## 10. 数据与状态策略

### 10.1 本期数据源

- 全部页面使用 mock 数据
- mock 数据放在对应 ViewModel 中
- UI 通过 state 渲染，不直接 new 假数据

### 10.2 推荐状态模型

- `OnboardingUiState`
- `HomeChatUiState`
- `SettingsHomeUiState`
- `ProviderModelUiState`
- `MemoryUiState`
- `BuiltinToolsUiState`
- `ShellRulesUiState`
- `McpUiState`
- `CustomToolsUiState`

### 10.3 ViewModel 范式

- 使用现有 `ComposeMVIViewModel`
- intent 只处理页面交互
- effect 仅在必要时用于 toast、对话框等一次性事件

## 11. 代码组织建议

```text
app/ui/
├── infra/
│   ├── ...
│   └── component/
├── feat/
│   ├── onboarding/
│   ├── home/
│   └── settings/
└── ...
```

说明：

- `infra` 放跨页面复用组件与导航/壳层能力
- `feat/onboarding` 放 onboarding 页面与 VM
- `feat/home` 放聊天测试页骨架
- `feat/settings` 放设置树页面与 VM

## 12. 开发边界

本 PRD 允许如下实现策略：

- 首页聊天只做 mock 列表与输入框动效
- Settings 各页只做 UI，不连真实配置仓库
- `api_key` 非空判断可先由 mock local state 模拟，再替换成真实读取
- MCP 与 CustomTools 只展示静态/假动态内容

本 PRD 不允许如下实现策略：

- 在页面中直接写死中文字符串
- 为了快而绕开 `ui.infra` 再写一套 Scaffold
- 在没有抽象的情况下复制粘贴多份相同表单
- 把所有配置项硬塞进单页长列表，导致信息密度失控

## 13. 验收标准

### 13.1 Onboarding

- 首次进入时能完整走通 `Startup -> Done -> Home`
- `BrandCheck`、`ProviderPick`、`ConfigureBasic`、`ConfigureModel`、`Done` 都有明确可点击反馈
- 返回逻辑正确

### 13.2 Home

- Home 可展示 mock 消息
- 可发送一条 mock 消息并收到一条 mock 回复
- 右上角更多菜单可进入 Settings

### 13.3 Settings

- SettingsHome 至少能进入全部一级分组
- Shell Rules、MCP、Custom Tools 至少有可见骨架页
- Provider & Model 页能覆盖 onboarding 涉及字段

### 13.4 多语言

- 所有新页面无 Kotlin 硬编码 UI 字符串
- 中文与英文资源齐全

### 13.5 视觉一致性

- 顶栏动画与页面切换统一
- 主按钮、设置卡片、列表项视觉风格统一
- 启动页与完成页明显区别于普通设置页

## 14. 推荐开发顺序

1. 调整 `LiquidScreen` 与 page title 模型，补足无 chrome / `@StringRes` 支持
2. 新增通用组件：按钮、设置卡片、设置行
3. 落地 onboarding flow
4. 落地 Home 聊天骨架
5. 落地 SettingsHome 与一级分组
6. 落地 Shell Rules / MCP / CustomTools 等二级页
7. 补齐中英字符串

## 15. 风险

- 若继续沿用 `String title`，多语言一定变成后补债
- 若不提前抽表单与设置项组件，Settings 页面很快会复制失控
- `SettingsScreen` 现有实现未接入正式基建，只能参考，不能直接拼接复用
- 如果 Startup / Done 不支持无 chrome，海报页气质会被顶栏破坏

## 16. 结论

本期 UI 应先完成一套正式的 app shell，而不是继续堆 demo。最佳落地方式是：

- 以 `ui.infra` 为唯一壳层基础
- 用 onboarding flow 解决首次配置
- 用 Home 聊天骨架作为主页
- 用 settings tree 承接全部高级配置
- 用 MVI + mock 数据驱动页面
- 用资源字符串保证中英双语可扩展

该方案能在不接业务逻辑的前提下，把页面骨架、交互路径和后续配置承载能力一次性搭好，后面接真实数据时只需要替换 state 来源，而不用推翻 UI 结构。
