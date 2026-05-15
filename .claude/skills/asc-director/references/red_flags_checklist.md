# Red Flags 自查清单

按阶段分组的可执行自查清单。每个 Phase 结束时 MUST 过一遍当前阶段的 checklist。

---

## 全局 Red Flags

- [ ] "需求很简单，跳过探索" → 探索可以短但 NEVER 跳过
- [ ] "用户确认了，剩余阶段一口气做完" → 每阶段 MUST 独立暂停
- [ ] "这个文件太小不值得读" → 忽略小文件是上下文遗漏的头号原因
- [ ] "校验失败了，换个写法试" → MUST 先读错误日志分析

---

## Phase 0 自查

- [ ] 是否先做了代码摸底再提问？（NEVER 凭空澄清）
- [ ] 是否经过范围评估？
- [ ] 是否至少做了方案发散？（PRD 也 MUST 发散 How）
- [ ] tech_survey.md 含"需求概要"、"需求澄清记录"、"现状映射表"、"决策记录"？
- [ ] 校验脚本通过？

## Phase 1 自查

- [ ] 搜索了模糊词（"适当"/"合理"/"类似"等）？
- [ ] 每个模块通过 What/How/Depends？
- [ ] API 签名完整（参数+返回+异常）？
- [ ] "催我快点，跳过审查" → 审查是质量底线
- [ ] 校验脚本通过？

## Phase 2 自查

- [ ] Feature 拆分是否按独立功能点划分？
- [ ] 每个 Feature 标注了预估 LOC 和依赖关系？
- [ ] Feature 内部的 Task 保持 1 Task = 1 目标文件？
- [ ] Batch 编排是否遵循聚合规则（< 500 LOC 聚合，≥ 500 独占）？
- [ ] Batch 是否跨依赖层级聚合了？→ NEVER 允许
- [ ] 有 placeholder（TBD/TODO/参考现有/与T-xx类似）？
- [ ] 描述含"然后"/"同时"/"并且"？→ 拆分
- [ ] 每个 Task 有伪代码签名和 AC？
- [ ] 校验脚本通过？

## Phase 3 自查

- [ ] "直接在 controller 写代码" → NEVER，MUST 派发 subagent（Review 修复除外）
- [ ] "DONE 了不用 review" → NEVER 跳过全量审查
- [ ] "让 subagent 自己读 plan" → MUST 完整粘贴 Batch 内所有 Task 文本
- [ ] 全部 Batch 完成后是否检查了所有 Task/Feature 的完成状态？
- [ ] prompt 中搜索 `{` 确认无残留占位符？
