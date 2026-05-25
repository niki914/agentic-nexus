# ASC-01 Strings Baseline 架构设计

## 1. 设计目标

ASC-01 的实现目标不是“一次性把全部 strings 改名”，而是建立一个可持续执行的资源体系基线：

- 让 `strings.xml` 的分组结构清晰可读
- 让新旧命名的迁移边界明确
- 立即删除已确认死 key
- 删除质量较差且已失真的旧英文资源文件
- 为后续 ASC 提供可直接执行的命名映射，不再让实现阶段临时发挥

## 2. 设计边界

### 2.1 本阶段会做

- 重组 `app/src/main/res/values/strings.xml`
- 以 XML 注释分块梳理现有字符串
- 删除死 key：
  - `nexus_home_headline`
  - `nexus_home_description`
- 删除 `app/src/main/res/values-en/strings.xml`
- 固定旧 key 到新命名域的映射规则

### 2.2 本阶段不会做

- 不全量修改 Kotlin 中的 `R.string.nexus_*` 引用
- 不创建新的英文资源文件
- 不顺手改页面结构或页面文案
- 不在 ASC-01 中把所有旧 key 改成新 key

## 3. 文件职责设计

### 3.1 `app/src/main/res/values/strings.xml`

职责：

- 作为当前唯一有效的 UI / 模块可见文案主文件
- 用注释按业务块与模块块组织
- 暂时容纳“旧 key + 新规则”的过渡形态

设计原则：

- 优先保证结构清晰
- 不为了命名洁癖强行拉大改动面
- 允许旧 key 在过渡期存在，但必须放到它所属的目标业务块中

### 3.2 `app/src/main/res/values-en/strings.xml`

职责变化：

- ASC-01 中直接删除

原因：

- 当前英文文件不是稳定镜像
- 存在中文残留，例如 `nexus_provider_pick_title`、`nexus_configure_title`、`nexus_done_headline`
- 如果继续保留，会制造“英文资源已可用”的错觉

### 3.3 Kotlin 引用文件

职责：

- 本阶段原则上不动
- 仅当资源删除会导致编译问题时才做最小修复

当前判断：

- 由于只删除未引用 key，本阶段默认不需要修改 Kotlin 文件

## 4. 命名映射设计

本节定义的是**目标命名域**，不是要求 ASC-01 一次性完成全部 rename。

### 4.1 UI 域映射

| 当前前缀 | 目标前缀 | 适用范围 |
| --- | --- | --- |
| `nexus_startup_*` | `ui_onboard_startup_*` | 启动海报与宿主状态 |
| `nexus_provider_pick_*` | `ui_onboard_provider_pick_*` | provider 选择 |
| `nexus_configure_*` | `ui_onboard_configure_*` | onboarding 配置页 |
| `nexus_done_*` | `ui_onboard_done_*` | onboarding 完成页 |
| `nexus_home_*` | `ui_home_*` | home 页面、输入框、发送按钮、tool 状态 |
| `nexus_settings_*` | `ui_settings_*` | settings 入口、分组标题、摘要、placeholder |

### 4.2 模块域映射

| 当前前缀 | 目标前缀 | 适用范围 |
| --- | --- | --- |
| `nexus_builtin_tools_*` | `builtin_tool_*` | 内置工具设置页与提示 |
| `nexus_custom_tools_*` | `custom_tool_*` | 自定义工具设置页与提示 |
| `nexus_mcp_*` | `mcp_*` | MCP 设置页与提示 |

### 4.3 特殊保留项

- `app_name` 保持不变
- `xposed_desc` 当前不是 `nexus_*`，且属于模块说明，ASC-01 不改名

## 5. XML 组织设计

`strings.xml` 调整为以下分块顺序：

1. App / Module
2. Onboarding
3. Home
4. Settings
5. Builtin Tool Module
6. Custom Tool Module
7. MCP Module

对应示意：

```xml
<resources>
    <!-- App / Module -->
    <string name="app_name">...</string>
    <string name="xposed_desc">...</string>

    <!-- Onboarding -->
    <string name="nexus_startup_headline">...</string>
    <string name="nexus_provider_pick_title">...</string>
    <string name="nexus_configure_title">...</string>
    <string name="nexus_done_headline">...</string>

    <!-- Home -->
    <string name="nexus_home_title">...</string>

    <!-- Settings -->
    <string name="nexus_settings_title">...</string>

    <!-- Builtin Tool Module -->
    <string name="nexus_builtin_tools_page_description">...</string>

    <!-- Custom Tool Module -->
    <string name="nexus_custom_tools_page_description">...</string>

    <!-- MCP Module -->
    <string name="nexus_mcp_page_description">...</string>
</resources>
```

说明：

- 这里先组织“块”，再处理“重命名”
- 旧 key 出现在正确业务块中，就已经完成了 ASC-01 最关键的结构收敛

## 6. 迁移策略设计

### 6.1 ASC-01 的迁移策略

ASC-01 采用“结构先行、引用不动”的最小策略：

- 调整 `strings.xml` 排序与注释
- 删除死 key
- 删除旧英文文件
- 不批量改 `R.string.*`

这样可以保证：

- 本批改动可控
- 后续页面重做时不会被资源 rename 反向干扰

### 6.2 后续 ASC 的承接策略

- ASC-02：在 onboarding 页面真实改造时，将相关 `nexus_startup_*` / `nexus_provider_pick_*` / `nexus_configure_*` / `nexus_done_*` 迁移到 `ui_onboard_*`
- ASC-03：若 `DonePage` 文案重写，直接落 `ui_onboard_done_*`
- ASC-05 / ASC-06：settings 新页面开发时，将 `nexus_settings_*` 迁移到 `ui_settings_*`
- ASC-08：MCP 文案资源化时，统一改用 `mcp_*`
- Custom Tools / Builtin Tools 后续改动时，分别迁移到 `custom_tool_*` 与 `builtin_tool_*`

## 7. 删除策略设计

### 7.1 立即删除

- `nexus_home_headline`
- `nexus_home_description`
- `app/src/main/res/values-en/strings.xml`

### 7.2 暂不删除

- 所有仍被 Kotlin 引用的 `nexus_*`

原因：

- ASC-01 不是全量引用迁移任务
- 保留被引用 key 可以避免把一个资源整理批次升级成大范围代码重命名

## 8. 风险控制

### 8.1 主要风险

- 只调整注释和顺序，团队后续可能继续往旧前缀新增 key
- 旧命名长期滞留，导致“规则存在但未执行”

### 8.2 控制手段

- 在 `ASC_may_25.md` 和本设计文档同时写明：新增 key 禁止继续用 `nexus_*`
- 后续每个 ASC 只迁移自己触达页面或模块的 key
- 不允许在未触达的业务域做顺手全量 rename

## 9. Phase 2 输入约束

下一阶段任务规划必须遵守以下约束：

- Task 1 只动 `app/src/main/res/values/strings.xml`
- Task 2 只删除 `app/src/main/res/values-en/strings.xml`
- Task 3 只做死 key 安全检查与收尾
- 默认不安排 Kotlin 文件修改任务

## 10. 人工校验结果

本设计通过人工校验确认以下事实：

- 删除的两个 home key 当前未被 Kotlin 或 XML 引用
- 删除 `values-en/strings.xml` 不会产生源码级路径依赖问题
- 当前仓库无 ASC 标准脚本，需继续使用轻量 ASC 方式推进
