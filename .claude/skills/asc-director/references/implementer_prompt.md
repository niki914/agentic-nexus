# Implementer Subagent Prompt Template

为每个 Batch 派发 implementer subagent 时使用此模板。一个 subagent 一次性实现一个 Batch 内的全部 Task。

---

## Prompt 模板

```
Task tool:
  description: "Implement Batch {BATCH_ID}: {BATCH_SUMMARY}"
  prompt: |
    你正在实现 Batch {BATCH_ID}。

    ────────────────────────────────
    ## 第一步：读取设计文档（MUST，写代码前强制执行）
    ────────────────────────────────

    在写任何代码之前，你 MUST 先依次读取以下三份文档：

    1. `{PROJECT_DIR}/tech_design.md` — 技术方案（实现主依据）
       你的代码 MUST 严格遵循此文档中的类/方法/属性签名、架构模式、接口定义。
       如果 plan.md 与 tech_design.md 有冲突，以 tech_design.md 为准。

    2. `{PROJECT_DIR}/tech_survey.md` — 需求与技术调研
       包含需求背景、约束条件、验收标准。你的实现 MUST 满足其中的约束和 AC。

    3. `{PROJECT_DIR}/plan.md` — 任务计划
       包含 Feature 拆分、Task 明细、Batch 编排。从中定位本 Batch 的 Task。

    读取完成后再继续下面的步骤。NEVER 跳过此步骤直接写代码。

    ────────────────────────────────
    ## 本 Batch 范围
    ────────────────────────────────
    {BATCH_SCOPE}
    （列出本 Batch 包含的 Feature ID + Task ID 清单，从 plan.md Batch 编排表提取。）

    ────────────────────────────────
    ## 现有代码上下文
    ────────────────────────────────
    {CODE_CONTEXT}
    （读取本 Batch 所有 Task 的「视野」列文件 + 目标文件当前内容。新建文件注明"新建"。）

    ────────────────────────────────
    ## 你的工作
    ────────────────────────────────
    1. 从 plan.md 中定位本 Batch 的 Task，按依赖顺序逐个实现
    2. 严格遵循 tech_design.md 中的类/方法/属性签名，NEVER 偏离设计
    3. 满足 tech_survey.md 中的约束条件和验收标准
    4. MUST 复用现有基础设施，NEVER 重新发明轮子
    5. 所有 Task 完成后执行自审（见下方）
    6. 报告结果

    禁止事项：
    - 除非用户明确要求，否则不得进行 git commit

    工作目录: {WORKING_DIR}

    ## 校验策略（强制）
    - 不跑完整编译/link/联调/慢测试
    - 仅做 AI Code Review 自审：逻辑自洽、接口对齐、无语法/类型错误
    - 只有 controller 明确要求时才执行编译验证

    ## 自审清单
    - **设计对齐（最重要）**：签名/类型/调用方式与 tech_design.md 一致？
    - **需求对齐**：满足 tech_survey.md 的约束和 AC？
    - **完整性**：每个 Task 的 AC 每条都实现了？
    - **质量**：命名清晰？风格一致？复用工具类？
    - **纪律**：YAGNI？只做要求的？每个 Task 只改了其目标文件？
    - **跨 Task 一致性**：接口对齐？类型匹配？

    ## 报告格式
    - **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
    - 逐 Task 报告：Task ID + 实现内容 + 修改文件
    - 自审发现
    - 疑虑（如有）
```

---

## 占位符说明

| 占位符 | 来源 | 说明 |
|:-------|:-----|:-----|
| `{BATCH_ID}` | plan.md | 如 `B-01` |
| `{BATCH_SUMMARY}` | plan.md | 一句话概述 |
| `{PROJECT_DIR}` | `{PROJECT_DIR}/docs/.asc_task/<project_name>` | 项目文档目录的相对路径 |
| `{BATCH_SCOPE}` | plan.md Batch 编排表 | 本 Batch 的 Feature + Task ID 清单 |
| `{CODE_CONTEXT}` | 代码库实际文件 | 视野列文件 + 目标文件当前内容 |
| `{WORKING_DIR}` | 实际路径 | 如 `./src/` |

> 填充样例参阅 [templates/implementer_prompt_example.md](../templates/implementer_prompt_example.md)
