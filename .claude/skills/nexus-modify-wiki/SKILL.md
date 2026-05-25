---
name: nexus-modify-wiki
description: Use when updating or adding `nexus-lookup` wiki pages, especially after code changes, doc review, or when fixing stale paths, status drift, and overgrown reference content.
---

# Nexus Modify Wiki

## Overview

这个 skill 用于修改 `nexus-lookup` 下的 wiki 文档，目标是让文档持续反映**当前源码事实**，而不是继续累积过时设计、失效引用和写作者视角的噪音。

它不是通用文档美化器。它只服务于 `nexus-lookup` 这套仓库内 wiki。

## When to Use

在以下场景使用：

- 新增或重写 `nexus-lookup` 下的任意 wiki 文档
- 代码已变化，需要同步 wiki 里的路径、状态或术语
- 审稿后需要统一清理失效引用、过期设计痕迹、重复膨胀内容
- 发现 wiki 把“半落地”写成“已落地”，或把 PRD 写成既成事实
- 发现路径写成 `../`、`file:///`、`.../`、孤立文件名，导致读者无法按项目路径继续查源码

不要用于：

- 回答单个源码问题但不需要修改 wiki
- 编写 PRD、任务计划、方案设计文档
- 维护 `docs/.asc_task/` 下的产物

## Required Prep

- **REQUIRED SKILL:** 先加载 `Prompt Engineering`
- **REQUIRED SKILL:** 再加载 `nexus-lookup`
- 必须先读 `nexus-lookup/index.md`
- 只读当前目标文档及其对应源码入口，不顺手扩写无关页面

## Core Rules

### 1. 源码优先

- 当前实现、调用顺序、能力状态、目录入口，一律以源码为准
- 禁止读取、引用、登记 `docs/.asc_task/`
- PRD 只能描述目标态或提案，不能作为“已实现”的证据

### 2. 路径统一

- 路径统一使用**项目根相对路径**
- 推荐写法：`app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- 禁止 `../`、`../../../../../`、`file:///`、绝对路径、`.../` 省略路径
- 除非同一小节已经先给出公共前缀，否则不要只写 `LLMController.kt`

### 3. 引用必须验真

- 每次写入一个文档、源码或目录路径前，先确认它真实存在
- 如果目标已不存在，直接删除引用，不要保留“历史上有过”的痕迹
- 不要凭目录命名习惯臆断路径，例如把 `agentic/app/ui/` 写成 `ui/feat/`

### 4. 状态必须拆细

- 禁止把半落地能力粗暴写成“已落地”
- 要拆成“哪部分已落地，哪部分未落地”
- 如果某能力只有配置解析、模型结构或 UI 骨架存在，而执行器、联动逻辑或真实数据源未落地，必须明确写出缺口

### 5. 删除作者视角噪音

以下内容默认不应该出现在 wiki 正文：

- `## 目的`
- `## 写作约束`
- `## 全局写作约束`
- `## 文档维护原则`
- `## 建议信息来源`
- “后续应填充的信息”“建议引用的源码位置”这类写给文档作者的话

wiki 面向读者，不面向下一位写文档的人。

### 6. 按分区写内容

- `overview/`：项目总览、当前状态、能力边界
- `architecture/`：跨模块机制、调用链、状态流转、配置流转
- `domains/`：宿主专项心智、关键 Hook、调试入口
- `reference/`：索引、注册表、路径地图；不承载行为分析

不要把一个分区写成另一个分区。

### 7. 控制重复膨胀

- 类似 `source-map.md` 的索引页，二级标题下先声明公共前缀，再列子路径
- 允许写法：
  - `## app/src/main/java/com/niki914/nexus/agentic/chat/`
  - `- LLMController.kt`
  - `- agentic/ToolManager.kt`
- 不要在同一节里重复十几次相同前缀

### 8. 术语统一

- 同一个概念只保留一个主名称
- 如果历史上有别名，首次出现时说明一次，后续统一使用主名称
- 相邻文档中的 Hook 名、模式名、状态名必须对齐

## Editing Workflow

1. 读 `nexus-lookup/index.md`，确认应该改哪一篇，不扩散
2. 读目标文档，圈出以下问题：
   - 路径不规范
   - 引用失效
   - 使用了 `docs/.asc_task/`
   - 作者视角噪音
   - 状态写粗
   - 同前缀重复膨胀
3. 读支撑这些断言的真实源码路径
4. 删除失效信息，不保留“历史包袱”
5. 用项目根相对路径、统一术语、细粒度状态重写正文
6. 如果改动会影响相邻索引页或状态页，同步检查：
   - `index.md`
   - `overview/current-status.md`
   - `reference/source-map.md`
   - `reference/task-docs-registry.md`
7. 交付前做一次全文自检

## Rewrite Checklist

- 已读 `index.md`
- 已确认目标文档所属分区和职责边界
- 每个“当前/已落地/实际/会”断言都能回指源码
- 不含 `docs/.asc_task/`
- 不含 `../`、`file:///`、绝对路径、`.../`
- 不含无意义的作者说明章节
- 不含不存在的文件或目录引用
- 半落地能力已拆成“已实现部分 / 未实现部分”
- 术语在相邻文档中保持一致
- 索引页已收敛重复前缀

## Good vs Bad

### 路径

Bad:

```md
- [ASC_may_25.md](../../../../../ASC_may_25.md)
- [BreenoChatHook.kt](file:///Users/xxx/.../BreenoChatHook.kt)
- app/src/main/java/.../Entrance.kt
```

Good:

```md
- `ASC_may_25.md`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/BreenoChatHook.kt`
- `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`
```

### 状态

Bad:

```md
- 已落地：LLM Controller v2
```

Good:

```md
- 已落地：`LLMController`、`PromptComposer`、`ToolManager`、HTTP MCP 注册
- 未落地：Local Tool executor
```

### 失效引用

Bad:

```md
- `docs/.asc_task/llm_controller_v2/tech_design.md`
- `app/src/main/java/com/niki914/nexus/agentic/chat/v2/LLMController.kt`
```

Good:

```md
- `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- 删除所有 `.asc_task` 相关登记
```

### 控制重复

Bad:

```md
- `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt`
- `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`
```

Good:

```md
## app/src/main/java/com/niki914/nexus/agentic/chat/

- `LLMController.kt`
- `LlmStreamEvent.kt`
- `agentic/ToolManager.kt`
```

## Common Mistakes

- 把项目根相对路径写成文档相对链接，导致路径不可用
- 把设计稿、任务产物、缓存目录当成实现依据
- 看到一个目录名就凭感觉补完整条路径，没有核实文件是否存在
- 用“已落地”掩盖半落地事实
- 在 wiki 里保留写作者说明，污染读者视角
- 在索引页无脑重复完整前缀，导致内容膨胀

## Output

完成修改后，给出简短报告：

```text
修改报告

已改文件:
- path/to/file.md — 做了什么

已删除的失效引用:
- path/to/missing-doc.md

已统一的术语或路径:
- old -> new

剩余待核实问题:
- 哪些地方还需要进一步看源码
```
