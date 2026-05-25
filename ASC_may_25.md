# ASC May 25

## 当前结论

- UI 导航结构以当前源码为准，不再参考已过时的 PRD 页面流。
- 当前正确入口与导航事实以以下源码为准：
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/NexusPage.kt`
- 本轮目标是补齐正式 UI、移除套壳感、补真实设置页，不是重做导航基建。
- 页面级 ViewModel 只给有真实状态的页面，避免给纯展示页强行套模板。

## Strings 规则

### 总原则

- 所有可见文案都必须进入资源文件，禁止在 Kotlin 中新增硬编码 UI 字符串。
- 命名按业务域或模块域，不按零散页面实现细节命名。
- `values/strings.xml` 与 `values-en/strings.xml` 保持同一分组结构。
- 一个 key 命名后尽量稳定，避免同义重复 key。

### UI 业务域命名

- onboarding：`ui_onboard_*`
- home：`ui_home_*`
- settings：`ui_settings_*`
- 通用导航或全局操作：`ui_common_*`

示例：

- `ui_onboard_startup_headline`
- `ui_onboard_configure_api_key_label`
- `ui_onboard_done_enter_home`
- `ui_home_title`
- `ui_settings_mcp_title`

### 模块域命名

- LLM / Session：`llm_*`、`session_*`
- MCP：`mcp_*`
- Tool / Builtin Tool / Custom Tool：`tool_*`、`builtin_tool_*`、`custom_tool_*`
- Hook / 宿主能力：`hook_*`
- IPC：`ipc_*`
- Server / 配置服务：`server_*`
- 如需更贴近现有业务对象，可继续细化为 `breeno_*`、`xiaoai_*`

说明：

- 模块域字符串用于非纯 UI 文案，但仍需要中英适配的用户可见提示、错误信息、状态说明。
- UI 页面里引用模块域字符串是允许的，前提是该文案本质属于模块能力本身，而不是页面专属文案。

### XML 组织

```xml
<resources>
    <!-- Onboarding -->
    <string name="ui_onboard_startup_headline">...</string>
    <string name="ui_onboard_done_enter_home">...</string>

    <!-- Home -->
    <string name="ui_home_title">...</string>

    <!-- Settings -->
    <string name="ui_settings_mcp_title">...</string>

    <!-- MCP Module -->
    <string name="mcp_load_failed">...</string>
</resources>
```

规则：

- 用 XML 注释划分业务块或模块块。
- 同一块内部按阅读顺序聚合，不追求字典序。
- 新增 key 时优先放入正确分组，不要继续在旧 `nexus_*` 区域堆积。

## 页面与 ViewModel 原则

- 应该上 ViewModel：
  - `ConfigurePage`
  - 已开始承载真实状态的 settings detail 页面
  - 后续需要表单状态、异步加载、保存反馈的页面
- 不强制上 ViewModel：
  - `StartupPage`
  - `DonePage`
  - 纯参数驱动的轻页面

## ASC 任务队列

### ASC-01 文案基线整理

- 目标：建立 strings 分组与命名规则，删除死 key，移除旧英文资源文件。
- 范围：只动资源文件和必要引用，不改页面结构。
- 备注：旧 `nexus_*` 不再新增；迁移按任务逐步做，不全量梭哈。

### ASC-02 Onboarding 表单化

- 目标：把 `ConfigurePage` 从展示页改成真实表单页。
- 范围：允许引入页面级 ViewModel。
- 备注：导航结构不改，不回到过时 PRD 的旧信息架构。

### ASC-03 DonePage 去套壳

- 目标：`DonePage` 独立内容与视觉，不再复用 `ConfigurePageContent`。
- 范围：页面内容重做。
- 备注：纯展示页，不为了统一模板强上 ViewModel。

### ASC-04 ChatOnly 路径修正

- 目标：修正 ChatOnly 进入 Home 的路径语义和相关文案。
- 范围：仅修正现有流转与说明。
- 备注：不新增导航节点。

### ASC-05 Settings 第一批

- 目标：优先实现 `ProviderModel`。
- 范围：真实设置内容，不做 placeholder。
- 备注：作为 settings detail 的模板页。

### ASC-06 Settings 第二批

- 目标：实现 `Network`。
- 范围：沿用 ASC-05 的状态组织方式。
- 备注：不在本任务顺手扩散到其他分组。

### ASC-07 Settings 后续批

- 目标：实现 `Memory`、`ShellRules`、`About`。
- 范围：按优先级拆分，不要求一次做完。
- 备注：低于 ASC-05 / ASC-06。

### ASC-08 MCP 文案扫尾

- 目标：`McpSettingsState` 中用户可见错误提示全部资源化。
- 范围：只改文案来源与 key，不改业务逻辑。
- 备注：模块类提示应进入 `mcp_*` 域。

### ASC-09 英文资源回填

- 目标：在中文 key 稳定后重建 `values-en/strings.xml`。
- 范围：仅补英文资源。
- 备注：必须与中文资源分组保持一致。

## 当前执行顺序

1. ASC-01
2. ASC-02
3. ASC-03
4. ASC-04
5. Review 卡点
6. ASC-05
7. ASC-06
8. ASC-08
9. ASC-09
