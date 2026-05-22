# UI Shell Domain

## 目的

本文件后续用于给主 App UI、onboarding、home chat、settings tree、导航状态机相关任务提供导航。

## 后续应填充的信息

- 当前 UI 的现实状态和目标状态
- 哪些 UI 能力已经在源码中存在，哪些只在 PRD 中
- `UI-PRD.md` 与 `prd-nav-status-machine.md` 各自解决什么问题
- 后续阅读 UI 源码时应该先看哪些基础设施、哪些页面入口
- 多语言、Liquid Glass、NavigationController 的约束和边界

## 建议引用的源码位置

- `UI-PRD.md`
- `prd-nav-status-machine.md`
- `app/src/main/java/.../app/MainActivity.kt`
- `composebase/src/main/java/.../`
- 实际 UI 目录后续出现后，再补充对应相对路径

## 写作约束

- 必须明确哪些内容是提案，哪些内容已落地
- 不要把 PRD 大段复制到这里
- 不要抄 Compose 页面代码
- 默认只写相对路径和阅读建议
