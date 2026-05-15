# Reviewer Subagent Prompt Template

全部 Batch 完成后，派发 reviewer subagent 对全量代码做一轮审查。

---

## Prompt 模板

```
Task tool:
  description: "Review all implemented code for project {PROJECT_NAME}"
  prompt: |
    你正在审查项目 {PROJECT_NAME} 的全量实现代码。

    ────────────────────────────────
    ## 第一步：读取设计文档（MUST，审查前强制执行）
    ────────────────────────────────

    在审查任何代码之前，你 MUST 先依次读取以下三份文档：

    1. `{PROJECT_DIR}/tech_design.md` — 技术方案（审查主依据）
       审查时 MUST 逐项对比实现代码是否严格遵循此文档中的设计。

    2. `{PROJECT_DIR}/tech_survey.md` — 需求与技术调研
       审查时 MUST 逐条对比验收标准（AC）是否满足。

    3. `{PROJECT_DIR}/plan.md` — 任务计划
       审查时 MUST 逐 Feature、逐 Task 对比 AC。

    读取完成后再继续下面的审查。NEVER 跳过此步骤直接审查。

    ────────────────────────────────
    ## Implementer 报告汇总
    ────────────────────────────────
    {ALL_IMPLEMENTER_REPORTS}

    ────────────────────────────────
    ## 审查规则
    ────────────────────────────────

    ### 重要：不要信任报告
    MUST 独立验证一切。读取实际代码、逐条对比 AC、独立运行校验。

    ### 审查纪律（强制）
    - 你是 reviewer，不是 implementer：不得修改任何源码文件
    - 若发现问题，只输出"问题清单 + 修复建议"，由 controller 修复

    ### A. 设计合规（对照 tech_design.md — 最重要）
    - 对照设计决策记录，确认实现与设计一致
    - 检查类/方法签名是否完全匹配
    - 检查是否有偏离设计的实现

    ### B. 需求合规（对照 tech_survey.md）
    - 逐条对比验收标准（AC-xx），确认实现是否满足
    - 检查约束条件是否被遵守
    - 检查需求澄清记录中的决策是否被正确落地

    ### C. 计划合规（对照 plan.md）
    - 逐 Feature、逐 Task 对比 AC
    - 检查遗漏或超范围实现
    - 检查跨 Feature 接口一致性

    ### D. 代码质量
    - 命名清晰、职责单一、与代码库风格一致
    - 基础设施复用，NEVER 重新发明轮子
    - 跨 Task 接口对齐、类型匹配、调用链完整

    ## 报告格式
    **Status:** ✅ 全部通过 | ❌ 有问题

    ### 设计合规（tech_design.md）
    - [设计决策 vs 实际实现对照]

    ### 需求合规（tech_survey.md）
    - [逐条 AC-xx 检查结果]

    ### 计划合规（plan.md）
    - [逐 Feature/Task AC 检查结果]

    ### 代码质量
    - **优点**: [具体优点]
    - **问题**: [Critical/Important/Minor 分级，附 file:line]

    ### 跨 Feature 集成检查
    - [接口一致性、调用链完整性]

    ### 结论
    [全部通过 / 需要修复（列出修复项，附 file:line）]
```

---

## 占位符说明

| 占位符 | 来源 | 说明 |
|:-------|:-----|:-----|
| `{PROJECT_NAME}` | progress.md | 项目名 |
| `{PROJECT_DIR}` | `{PROJECT_DIR}/docs/.asc_task/<project_name>` | 项目文档目录的相对路径 |
| `{ALL_IMPLEMENTER_REPORTS}` | 各 Batch 返回 | 所有 Batch 的完整报告（非摘要） |

---

## Review 结果处理

- ✅ 通过 → Phase 3 结束
- ❌ 有问题 → controller 直接修复（不派 subagent），修复完成后 Phase 3 结束

> 填充样例参阅 [templates/reviewer_prompt_example.md](../templates/reviewer_prompt_example.md)
