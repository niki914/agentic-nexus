# ASC-01 Strings Baseline Implementation Plan

> **For agentic workers:** Implement this plan in small, reviewable steps. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 `strings.xml` 的业务分组基线，删除已确认死 key，并移除误导性的旧英文资源文件。

**Architecture:** 本任务只整理资源层，不做页面结构或 Kotlin 引用的大范围迁移。核心策略是“结构先行、引用不动”：先用注释和顺序把 `strings.xml` 收敛成稳定结构，再删除无引用资源与旧英文文件，把后续重命名压力留给对应业务 ASC。

**Tech Stack:** Android XML resources, Kotlin resource references, manual grep verification

---

## 文件职责

- `app/src/main/res/values/strings.xml`
  - 作为当前唯一生效的主资源文件
  - 按业务域与模块域完成注释分块和顺序整理
  - 删除已确认死 key
- `app/src/main/res/values-en/strings.xml`
  - 在本任务中直接删除
- `docs/.asc_task/asc_01_strings_baseline/progress.md`
  - 记录 Phase 2 完成和后续实现入口

## 实施约束

- 不修改 Kotlin 源码，除非发现删除死 key 后仍有残留引用
- 不新增 `ui_*`、`mcp_*` 等新 key
- 不重建英文资源
- 不改变任何可见页面行为

### Task 1: 重组 `strings.xml`

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 按目标块顺序重排现有字符串**

目标顺序：

```text
App / Module
Onboarding
Home
Settings
Builtin Tool Module
Custom Tool Module
MCP Module
```

- [ ] **Step 2: 为每个块添加 XML 注释**

目标注释：

```xml
<!-- App / Module -->
<!-- Onboarding -->
<!-- Home -->
<!-- Settings -->
<!-- Builtin Tool Module -->
<!-- Custom Tool Module -->
<!-- MCP Module -->
```

- [ ] **Step 3: 保留旧 key，但放到正确块内**

说明：

- 本任务不修改 `nexus_*` key 名
- 仅调整可读性和结构

- [ ] **Step 4: 删除死 key**

删除：

```xml
<string name="nexus_home_headline">主页占位</string>
<string name="nexus_home_description">聊天界面暂未接入，本页仅用于承接正式 chrome 和设置入口。</string>
```

- [ ] **Step 5: 人工检查 XML 结构**

检查项：

- `<resources>` 包裹完整
- 注释块顺序正确
- 无重复 key
- 无空块或残留死 key

### Task 2: 删除旧英文资源文件

**Files:**
- Delete: `app/src/main/res/values-en/strings.xml`

- [ ] **Step 1: 确认文件仍存在**

目标文件：

```text
app/src/main/res/values-en/strings.xml
```

- [ ] **Step 2: 删除文件**

删除原因：

- 含中文残留
- 容易让人误判“英文资源已可用”
- ASC-09 才负责重建稳定英文资源

- [ ] **Step 3: 确认仓库中不再存在该文件**

检查目标：

```text
app/src/main/res/values-en/strings.xml
```

### Task 3: 做实现后安全检查

**Files:**
- Verify: `app/src/main/res/values/strings.xml`
- Verify: `app/src/main/res/values-en/strings.xml`

- [ ] **Step 1: 检查死 key 无残留引用**

检查 key：

```text
nexus_home_headline
nexus_home_description
```

预期：

- 仅 0 处匹配

- [ ] **Step 2: 检查旧英文文件无残留**

检查目标：

```text
values-en/strings.xml
```

预期：

- 文件已不存在

- [ ] **Step 3: 更新 `progress.md`**

更新内容：

- 标记 Phase 2 完成
- 下一步进入实现
- 记录本批不涉及 Kotlin 引用迁移

## 自检结果

- 设计文档中的边界已全部映射到任务
- 没有安排 Kotlin 重命名任务，符合 ASC-01 范围
- 所有任务都落在资源整理与安全检查范围内，没有扩散到页面实现
