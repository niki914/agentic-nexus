# ASC-01 Strings Baseline 技术调研

## 1. 任务目标

为 UI 与模块可见文案建立稳定的 strings 基线，包含以下内容：

- 建立新的业务域 / 模块域命名规则
- 调整 `values/strings.xml` 的分组结构
- 删除已确认死 key
- 删除旧 `values-en/strings.xml`
- 为后续 ASC 提供渐进迁移策略，避免一次性全量重命名

本任务不包含：

- 重做页面结构
- 重做 onboarding / settings UI
- 一次性迁移全部 `nexus_*` 引用
- 重建英文资源

## 2. 当前代码事实

### 2.1 资源文件

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

### 2.2 当前字符串现状

- `values/strings.xml` 当前共 107 条字符串
- 当前命名几乎全部集中在 `nexus_*`
- 文案已自然分成以下几类，但文件内尚未按规则注释分块：
  - onboarding
  - home
  - settings
  - builtin tools
  - custom tools
  - MCP
  - app / xposed 模块说明

### 2.3 当前引用范围

`nexus_*` 资源引用当前分布在 13 个 Kotlin 文件，主要位于：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusApp.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/NexusPage.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/nav/NexusSettingsGroup.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/`

结论：

- 如果 ASC-01 做全量 rename，会同时触达资源文件、导航定义、页面 content、settings 相关实现
- 这会把原本“资源基线整理”升级成“跨模块重命名任务”，不符合当前批次边界

### 2.4 已确认死 key

以下 key 当前仅存在于资源文件，不在 Kotlin 中被引用：

- `nexus_home_headline`
- `nexus_home_description`

说明：

- 这两个 key 可以在 ASC-01 中直接删除

## 3. 命名与分组规则

### 3.1 UI 业务域

- `ui_onboard_*`
- `ui_home_*`
- `ui_settings_*`
- `ui_common_*`

### 3.2 模块域

- `llm_*`
- `session_*`
- `mcp_*`
- `tool_*`
- `builtin_tool_*`
- `custom_tool_*`
- `hook_*`
- `ipc_*`
- `server_*`
- `breeno_*`
- `xiaoai_*`

### 3.3 XML 组织

- `strings.xml` 以注释分块组织
- 同一块内部按业务阅读顺序聚合
- 中文和英文文件后续必须保持同一块结构
- 不再新增 `nexus_*` key

## 4. 方案发散

### 方案 A：一次性全量 rename

做法：

- 在 ASC-01 中把全部 `nexus_*` 迁移到 `ui_*` / 模块域
- 同步修改所有 Kotlin 引用
- 直接重建英文资源

优点：

- 一步到位
- 命名体系立即变干净

缺点：

- 改动面过大
- 极易与 ASC-02 之后的页面重做产生冲突
- 很多 placeholder 页面会被迫一起改名，收益低

### 方案 B：基线先行，渐进迁移

做法：

- 在 ASC-01 中只做资源层基线建立
- 删除死 key
- 删除旧英文文件
- 为后续任务约束新命名与新分组
- Kotlin 引用只在“本批必须动”的位置最小迁移
- 其余旧 `nexus_*` 随后续 ASC 分批迁移

优点：

- 改动边界清晰
- 风险小
- 更符合“每次只聚焦一个 ASC 任务”

缺点：

- 会存在一段时间的新旧 key 并存
- 需要后续 ASC 自律，不继续新增旧命名

## 5. 推荐方案

推荐采用 **方案 B：基线先行，渐进迁移**。

原因：

- 当前导航和页面还在继续演进，先把命名体系与 XML 结构定住更重要
- 当前 `nexus_*` 覆盖 13 个 Kotlin 文件，不值得在 ASC-01 做全量 rename
- 你已经明确希望每个 ASC 任务边界清楚，因此 ASC-01 应严格限制在资源基线层

## 6. ASC-01 预期改动面

必改文件：

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

可能涉及的最小 Kotlin 改动：

- 若为防止后续新增旧命名，需要修改少量当前批次必触达的资源引用
- 但默认不做全量引用替换

## 7. 风险与边界

### 风险

- 如果在 ASC-01 中全量改名，后续 ASC-02 / ASC-03 会出现二次重命名
- 如果只写规则不动文件，后续很容易继续把新文案写进旧 `nexus_*`

### 边界

- ASC-01 的本质是“建立基线并清掉明显坏味道”
- ASC-02 开始才进入页面级表单与文案迁移

## 8. 基础设施缺口

标准 ASC 流水线所要求的以下基础设施当前仓库不存在：

- `validate_tech_survey.sh`
- `validate_tech_design.sh`
- `validate_plan.sh`
- 对应 guide / template 文件

因此本轮采用轻量 ASC：

- 仍然生成 `tech_survey.md`、`progress.md`
- 但校验改为人工自检，不伪造脚本执行结果
