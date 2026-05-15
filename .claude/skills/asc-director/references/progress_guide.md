# 进度追踪指南 (Progress Guide)

`progress.md` 的格式规范与更新规则。

---

## 文件用途

- 记录当前所处阶段
- 追踪每个 Batch / Feature / Task 的执行状态
- 存储调研结论等关键上下文，防止长对话信息丢失
- 支持中断后断点续传

---

## 初始化模板（Phase 0 创建）

````markdown
# Progress: <project_name>

## Project Goal
[简述项目目标]

## Current Phase
Phase 0 ✅

## Context (关键上下文)

### Phase 0 调研结论
[Phase 0 完成后在此追加核心选型结论和调研发现]

### Phase 1 设计决策
[Phase 1 完成后在此追加关键设计决策和架构选择]

### Phase 2 任务规划摘要
[Phase 2 完成后在此追加 Feature 总数、Batch 数、关键依赖链]

## Decisions
[随流程推进追加关键决策（如 feature_name、选型结论等）]

## Batch Pause Mode
[Phase 3 进入前记录：`auto`（默认，不暂停）或 `manual`（Batch 间人工确认）]

## Batch Progress
[Phase 2 完成后同步 Batch 列表]

## Task Progress
[Phase 2 完成后同步任务列表，按 Feature 分组]
````

---

## 更新规则

| 时机 | 更新内容 |
|:-----|:---------|
| Phase 0 完成 | 创建文件，写入 Project Goal，将调研结论追加到 Context 的 `### Phase 0 调研结论` 子章节，标记 Phase 0 Done |
| Phase 1 完成 | 标记 Phase 1 Done，将设计决策追加到 Context 的 `### Phase 1 设计决策` 子章节 |
| Phase 2 完成 | 标记 Phase 2 Done，同步 Batch 列表和 Task 列表，初始化 `[ ] Pending`，将规划摘要追加到 Context 的 `### Phase 2 任务规划摘要` 子章节 |
| Phase 3 进入前 | 记录 Batch Pause Mode（auto / manual） |
| Phase 3 每个 Batch | 标记 Batch `[x] Completed`，标记其内所有 Task `[x] Completed`，记录 Modified Files |
| Batch 失败 | 标记 `[!] Failed`，记录错误信息 |
| 全量 Review 完成 | 记录 Review 结果，如有修复则记录修复内容 |

> **状态标记格式兼容**：以下标记均可识别：
> - `[Done]` / `✅` / `Done` / `完成`
> - `⏳` / `In Progress` / `进行中`
> - `[ ] Pending` / `[x] Completed` / `[!] Failed`

---

## Batch Progress 格式

````markdown
## Batch Progress

- [x] B-01: F-01（评论数据模型） | ~150 LOC | Modified: models/comment.ts, interfaces/filter.ts
- [ ] B-02: F-02 + F-03（列表展示 + 评论发送） | ~380 LOC
- [!] B-03: F-04（评论编辑） | ~600 LOC | Error: 编译失败
````

---

## Task Progress 格式

````markdown
## Task Progress

### Feature F-01: 评论数据模型
- [x] T-01: 定义 CommentModel → `models/comment.ts` | Modified: models/comment.ts
- [x] T-02: 定义 IFilter 接口 → `interfaces/filter.ts` | Modified: interfaces/filter.ts

### Feature F-02: 评论列表展示
- [ ] T-03: 实现 CommentListView → `views/comment_list.ts`
- [ ] T-04: 实现 CommentListViewModel → `viewmodels/comment_list_vm.ts`

### Errors & Warnings
- B-03/T-08: CompileError - missing import (auto-fixed in retry 1)
````

---

## 中断恢复协议

### Step 1: 检测中断
- `progress.md` 存在 → 中断恢复场景

### Step 2: 恢复上下文
1. 读取 `Current Phase` 确定当前阶段
2. 读取 `Context` 章节恢复关键决策
3. 读取 `Batch Progress` 确定已完成/待执行 Batch
4. 读取 `Task Progress` 确定各 Task 状态

### Step 3: 验证前序文件
- 根据当前阶段检查所有前序文件

### Step 4: 断点续传
> 🔄 **检测到未完成的项目：`<project_name>`**
>
> - **当前阶段**：Phase X
> - **已完成 Batch**：[列表]
> - **待执行 Batch**：[下一步]
>
> 是否从断点继续？

### Step 5: 继续执行
- 用户确认后从 Current Phase 继续
- Phase 3 从第一个 `[ ] Pending` Batch 继续
