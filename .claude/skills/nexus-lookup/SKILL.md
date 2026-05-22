---
name: nexus-lookup
description: Use when working in the Nexus repository and the task needs project context before code search, editing, debugging, design, or architecture discussion.
---

# Nexus Lookup

## Overview

This skill is the lookup entry for the Nexus repository.

Use it to route to the right wiki document before reading code, changing code, debugging behavior, or discussing architecture. The goal is to reduce blind source search and keep project context stable.

## When to Use

Activate this skill when the task involves any of the following:

- understanding project structure or module boundaries
- locating architecture facts before editing code
- debugging behavior across Xposed entry, hook pipeline, IPC, config, or LLM runtime
- comparing Breeno and XiaoAi behavior
- checking whether a capability is already implemented or still only exists in PRD or task docs
- discussing UI shell, onboarding, settings tree, tool calling, or MCP support

Do not use this skill for trivial questions that can be answered from one already-known file.

## Retrieval Protocol

1. Read `index.md` first.
2. Match the current task against the `检索建议` table.
3. Read only the recommended documents in order.
4. Prefer wiki facts first, then inspect source files through the relative paths recorded in the docs.
5. If wiki and source diverge, trust source code for current implementation and treat design docs as intent only.
6. Keep the final lookup report concise and continue the actual task after routing is complete.

## Output Format

Use a short lookup report in this shape:

```text
检索报告

已读文档:
- path/to/doc.md — 一句话摘要

相关源码入口:
- relative/path/to/file.kt — 作用

现状判断:
- 已落地 / 进行中 / 提案

下一步建议:
- 建议先读哪个源码或继续做什么
```

## Hard Rules

- Always read `index.md` before reading any subdocument.
- DO NOT read `docs/.asc_task/` or treat task docs as proof of implementation.
- ALWAYS trust source code for current implementation over design documents.
- Do not paste large code blocks into lookup reports.
- Prefer relative source paths over copied code.
- If an architecture explanation truly requires code excerpts, keep them minimal and explanatory.

## Document Tiers

Use these tiers while reading:

- `Stable`: stable facts that should match current source structure
- `In Progress`: active design or partially landed work
- `Proposal`: PRD or future-facing intent, not proof of implementation

## Common Mistakes

- jumping directly into grep without reading `index.md`
- attempting to read or parse `docs/.asc_task/` files instead of relying on actual source code
- treating design documents as the final source of truth instead of trusting the source code
- copying code into wiki instead of pointing to source paths
- reading every document instead of routing by scenario
