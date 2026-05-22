# Nexus Agent 启动文档

## 作用

本文件用于在每次会话开始前为 Agent 注入最小且稳定的仓库上下文，使其不必从零猜测项目结构、信息来源与首选工作路径。

## 项目定位

Nexus 是一个 Android Xposed 模块，当前面向两条宿主分支：

- `ColorOS / Breeno`
- `HyperOS / XiaoAi`

## 工作原则

- 默认先理解上下文，再动手修改
- 当前实现以源码为准，wiki 为检索地图，设计文档仅代表意图
- 不把 `docs/.asc_task/` 当成已实现证据
- 只在任务明确需要时扩散阅读范围，避免盲目全仓搜索

## 首选模式

- 需要项目上下文、架构事实、源码入口、能力现状时，优先进入 `nexus-lookup`
- 在 `nexus-lookup` 模式下，先读 `index.md`，按检索建议路由，再看对应源码
- 若 wiki 与源码冲突，以源码为准

## Skill 路由

- `nexus-lookup`
  - 用于理解项目结构、模块边界、架构链路、宿主差异、配置来源、源码入口
  - 任何需要“先搞清楚这个仓库是什么、代码在哪、现状如何”的任务，先走它
- `nexus-modify-wiki`
  - 用于修改 `nexus-lookup` 体系下的 wiki 文档
  - 仅在需要修正路径、状态、术语、失效引用、文档膨胀时使用
- `asc-director`
  - 用于新增功能、方案设计、技术调研、任务拆解、页面开发、模块重构、Bug 修复方案
  - 适合多阶段任务，不适合回答单个局部源码问题
- `Prompt Engineering`
  - 用于编写或修改提示词、Agent 文档、Skill 文档、任务说明
  - 凡是目标读者主要是 Agent 而不是人类用户，优先加载它

## 默认执行顺序

1. 判断任务是否需要仓库上下文
2. 若需要，先进入 `nexus-lookup`
3. 若任务是 wiki 维护，切到 `nexus-modify-wiki`
4. 若任务是方案或复杂执行编排，切到 `asc-director`
5. 若任务是提示词或 Agent 文档编写，切到 `Prompt Engineering`
6. 完成路由后再读源码、给结论或实施修改
