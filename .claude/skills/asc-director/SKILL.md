---
name: asc-director
description: Use when receiving a new feature request, PRD, technical design task, bug fix plan, page development, or module refactoring that requires multi-phase research → design → planning → coding pipeline. Keywords：需求、功能、PRD、方案设计、技术调研、任务拆解、重构、新增页面、Bug 修复方案、模块开发、ASC。
---

# ASC 全链路研发编排

接收原始需求，按 4 阶段流水线依次产出技术调研（含需求）、架构设计、任务规划并按 Batch 编码落地。

<HARD-GATE>
1. NEVER 跳过 Phase 0 探索。即使需求看似简单，MUST 至少完成代码摸底 + 需求澄清 + 方案发散。
2. Phase 3 中 NEVER 在 controller 上下文中直接编写实现代码。MUST 派发 subagent。（Review 修复阶段除外）
3. Phase 3 中 NEVER 跳过 reviewer 校验。全部 Batch 完成后 MUST 派发 reviewer subagent 做全量审查。
4. 每个 Phase 结束时 MUST 运行对应的校验脚本（scripts/validate_*.sh），报错 MUST 修复后才能推进。
5. Phase 3 派发 subagent 时，MUST 在 prompt 中包含 `tech_design.md`、`tech_survey.md`、`plan.md` 的文件路径，并指示 subagent 在写代码/审查前 MUST 先读取这三份文档。`tech_design.md` 是实现主依据。
6. Phase 3 每个 Batch 验收通过后，MUST 立即更新 `progress.md`（标记 Batch + 所有 Task 完成状态）。未更新前 NEVER 继续下一个 Batch。
</HARD-GATE>

---

## §1 核心法则

1. **文件驱动状态机**：通过 `{PROJECT_DIR}/docs/.asc_task/<project_name>/` 下的文件存在性判断当前阶段。前序文件缺失时 MUST 拒绝推进。
2. **分步执行，强制确认**：每个阶段产出后立即停止，等待用户确认后方可推进。NEVER 一次性输出多个阶段的产物。
3. **主动检索优先**：立即执行 `分析 → 定位 → 读取`。仅在工具读取失败时降级询问用户。
4. **最小改动 + 上下文忠诚**：优先扩展/装饰器/组合模式，MUST 复用现有基础设施，NEVER 重新发明轮子。
5. **增量迭代**：用户反馈修改意见时，定点修改并递增版本号（v1.0 → v1.1）。NEVER 推倒重写未受影响的章节。
6. **决策留痕**：关键推断 MUST 记录依据。输出文件名包含版本号。NEVER 覆盖历史产物。
7. **阶段上下文裁剪**：每个 Phase 开始时，ONLY 读取当前 Phase 需要的 reference（见 §4）。避免无关 reference 占用 token。

---

## §2 动态项目路径

- `{PROJECT_DIR}` 为用户工程的根目录（即 controller 的工作目录），由运行时上下文决定。
- Phase 0 根据用户需求提取简短英文项目名（snake_case），记为 `<project_name>`。
- 所有中间文档存放在 `{PROJECT_DIR}/docs/.asc_task/<project_name>/`。
- 源代码路径由 `tech_design.md` 和 `plan.md` 决定。

---

## §3 核心文件

| 文件 | 用途 | 创建阶段 |
|:-----|:-----|:---------|
| `tech_survey.md` | 需求理解 + 技术调研与选型 | Phase 0 |
| `tech_design.md` | 架构与 API 设计 | Phase 1 |
| `plan.md` | Feature 拆分 + Task 明细 + Batch 编排 | Phase 2 |
| `progress.md` | 动态执行进度与错误记录 | Phase 0 创建，全程更新 |

---

## §4 流水线总览与上下文加载

| 阶段 | 核心产出 | MUST 读取的 Reference | 校验脚本 |
|:-----|:--------|:---------------------|:---------|
| Phase 0 | `tech_survey.md` + `progress.md` | `tech_survey_guide.md` | `validate_tech_survey.sh` |
| Phase 1 | `tech_design.md` | `api_design_guide.md` | `validate_tech_design.sh` |
| Phase 2 | `plan.md` | `plan_guide.md` | `validate_plan.sh` |
| Phase 3 | 源代码文件 | `implementer_prompt.md`, `reviewer_prompt.md` | - |

> 每个 Phase 产出前 SHOULD 参阅 `templates/` 目录下的对应样例文件。

---

## §5 状态检查与恢复

每次回复用户之前，MUST 执行以下自检：

1. `progress.md` 不存在 → 强制进入 Phase 0。
2. 存在 → 读取 `Current Phase` 状态。
3. 前序文件完整性校验：Phase 1 需 `tech_survey.md`；Phase 2 需 `tech_design.md`；Phase 3 需 `plan.md`。缺失则拒绝推进。
4. **中断恢复**：对话上下文丢失时，读取 `progress.md` Context 章节恢复关键上下文，从断点续传。

---

## §6 各阶段规则

### Phase 0: 需求探索与技术摸底

> 详细指南：[references/tech_survey_guide.md](references/tech_survey_guide.md)
> 填充样例：[templates/tech_survey_example.md](templates/tech_survey_example.md)

#### Step 1: 项目初始化
1. **提取项目名** → 创建 `{PROJECT_DIR}/docs/.asc_task/<project_name>/`。
2. **范围评估** → 过大则拆分，🛑 暂停确认。

#### Step 2: 代码摸底（静默）
1. 从需求提取业务实体关键词 → 读取 3-7 个关键入口文件。
2. 识别现有架构、能力边界、可复用模块。
3. 完整性自检：失败则列出需用户提供的文件清单。

#### Step 3: 带认知的需求澄清
1. 基于代码现状提精准问题，≤5 个，一次一问，多选题优先。
2. PRD 级别输入可跳过但 MUST 说明。每个问题单独 🛑 等待回答。

#### Step 4: 方案发散 + 选型
1. ≥2 种方案，附复用度/改动量/风险。YAGNI 审查 + Lead with recommendation。
2. 🛑 暂停等用户选择。

#### Step 5: 内部审查 + 决策讨论
1. **PM 审查** → 需求 Gap + 功能覆盖。**架构师审查** → 代码完整性 + 最小改动方案。
2. **🧠 决策讨论** → ≤5 个决策点，🛑 暂停。用户回复"跳过讨论"时按 AI 倾向决策并记录。

#### Step 6: 收敛 + 产出
1. 产出 `tech_survey.md` + 初始化 `progress.md`。
2. 运行 `scripts/validate_tech_survey.sh`，修复所有报错。
3. 🛑 停止，等待确认后进入 Phase 1。

### Phase 1: 架构设计

> 详细指南：[references/api_design_guide.md](references/api_design_guide.md)
> 填充样例：[templates/tech_design_example.md](templates/tech_design_example.md)

1. **架构特征分析** → 识别强制工具类、架构模式、命名规范。
2. **草案设计** → 需求映射到具体类/方法/属性变更。**设计隔离检查** → What/How/Depends 三问。
3. **🧠 设计讨论** → ≤3 个争议点，🛑 暂停。
4. **红队审查** → PM + 架构师双重审查。**零模糊原则** → 完整签名，NEVER 模糊描述。
5. **产出** `tech_design.md`，运行 `scripts/validate_tech_design.sh`。
6. 更新 `progress.md`，🛑 停止。

### Phase 2: 任务规划

> 详细指南：[references/plan_guide.md](references/plan_guide.md)
> 填充样例：[templates/plan_example.md](templates/plan_example.md)

#### Step 1：Feature 拆分
从 `tech_design.md` 按独立功能点拆分 Feature（F-01, F-02, ...），标注功能描述、预估 LOC、依赖关系。

#### Step 2：Task 切分
每个 Feature 内部按原子性原则拆 Task（1 Task = 1 目标文件），保留伪代码签名、AC。

#### Step 3：Batch 编排

| 规则 | 说明 |
|:-----|:-----|
| 单个 Feature ≥ 500 LOC | 独占一个 Batch |
| 单个 Feature < 500 LOC | 与同层级 Feature 聚合至 ≥ 500 或无更多可聚合 |
| 聚合边界 | NEVER 跨依赖层级聚合 |
| 并行 | 无依赖冲突的 Batch 可并行执行 |

#### Step 4：对抗审查
PM → 架构师 → 结对伙伴三轮审查，AI 内部自动执行，终稿附"审查修正记录"。

#### Step 5：产出与校验
1. 产出 `plan.md`，运行 `scripts/validate_plan.sh`。
2. 同步到 `progress.md`，🛑 停止。

### Phase 3: 按 Batch 落地（Subagent 驱动）

> Implementer 模板：[references/implementer_prompt.md](references/implementer_prompt.md)
> Reviewer 模板：[references/reviewer_prompt.md](references/reviewer_prompt.md)
> 填充样例：[templates/implementer_prompt_example.md](templates/implementer_prompt_example.md), [templates/reviewer_prompt_example.md](templates/reviewer_prompt_example.md)

#### 进入前一次性询问

> 即将按 Batch 编排开始实现。是否需要在 Batch 之间人工确认？（默认：否，全部连续执行）

记录到 `progress.md` 的 `Batch Pause Mode` 字段。

#### Per-Batch 循环

1. **读取计划** → 从 `progress.md` 找下一个 Pending Batch。
2. **上下文加载** → 读取 Code Context（依赖文件 + 目标文件）。确认 `tech_design.md`、`tech_survey.md`、`plan.md` 三份文档存在于项目目录中。
3. **派发 implementer subagent** → 按模板构造 prompt，prompt 中包含三份文档的文件路径（见 HARD-GATE#5），subagent 会自行读取。
4. **逐 Task 验收** → subagent 返回后，controller 对照 plan.md 逐个检查该 Batch 内每个 Task 是否已实现：
   - 全部 DONE → 进入第 5 步。
   - 有 Task 未完成/遗漏 → **补派 subagent**（仅传未完成的 Task），直到该 Batch 内所有 Task 均 DONE。
   - BLOCKED → 补充上下文/拆分/升级。
5. **更新 progress.md（MUST，NEVER 跳过）** → 每个 Batch 验收通过后，立即写入 progress.md：标记 Batch 为 `[x] Completed`，逐条标记其内所有 Task 为 `[x] Completed`，记录 Modified Files。未更新 progress.md 之前 NEVER 继续下一个 Batch。
6. **是否暂停** → 用户选择了 Batch 间确认则 🛑；否则继续。
7. 无依赖冲突的 Batch 可并行派发。

#### 全部 Batch 完成后

1. **全局完整性检查** → 遍历 `progress.md` 的 Task Progress，确认**每个 Feature 的每个 Task** 均为 `[x] Completed`。有未完成的 → 定位到对应 Batch 补执行，更新 progress.md 后再继续。
2. **派发 reviewer subagent** → 三份文档（tech_survey + tech_design + plan）完整原文 + implementer 报告全量喂入，做全量代码审查。
3. **Review 结果**：✅ 通过 → Phase 3 结束；❌ 有问题 → controller 直接修复（不派 subagent）。
4. 🛑 暂停，等待确认。

#### 纪律与并行

- **实现阶段**：MUST 通过 subagent，controller 不直接编辑源代码。**Review 修复阶段**：controller 直接修复。
- **并行红线**：多 Batch 改同一文件、存在顺序依赖、需共享运行时调试状态 → 退化为串行。
- implementer/reviewer 不进行 git commit（除非用户明确要求）。

---

## §7 交互路由

| 用户输入 | 当前阶段 | 行为 |
|:---------|:---------|:-----|
| 新需求描述 | 任意 | 识别 `<project_name>`，执行 Phase 0 |
| 回答澄清问题 | Phase 0 | 记录回答，继续或进入方案发散 |
| 选定方案 | Phase 0 | 收敛确认 → 产出 tech_survey.md |
| "跳过探索" | Phase 0 | AI 按判断直接收敛 |
| "确认" / "通过" | Phase 0–2 | 执行下一阶段 |
| "继续" | Phase 3 | 执行下一个 Pending Batch |
| "跳过 [Batch-ID]" | Phase 3 | 标记 Batch 内所有 Task `[-] Skipped` |
| "重做 [Batch-ID]" | Phase 3 | 重置 Batch 内所有 Task 为 `[ ] Pending` |
| 修改意见 | 任意 | 定点修改，递增版本号 |
| "跳过讨论" | Phase 0-1 | AI 按倾向决策 |

---

## §8 启动引导

首次交互时输出：

> 👋 **ASC 研发流水线已就位。**
>
> **工作流：**
> 1. 输入需求 → 自动识别项目名
> 2. 🧠 **需求探索 + 技术摸底**：代码检索 → 逐步澄清 → 方案发散 → 选型决策 → 收敛确认
> 3. 依次产出 3 份核心文档（每份确认后推进）+ 1 份进度文件（全程更新）
> 4. 架构设计阶段包含 🧠 设计讨论
> 5. 每份文档产出后自动运行校验脚本
> 6. 代码实现：按 Batch 派发 subagent 编码 + 全量审查
>
> **请输入您的原始需求：**

---

> 错误处理与重试策略：[references/error_handling.md](references/error_handling.md)
> 自查清单：[references/red_flags_checklist.md](references/red_flags_checklist.md)
> 阶段门禁：[references/phase_gate_checks.md](references/phase_gate_checks.md)
