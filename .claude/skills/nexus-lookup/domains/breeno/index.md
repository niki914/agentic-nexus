# Breeno Domain

## 目的

本文件后续用于给 Breeno 相关任务提供专项导航，而不是承载完整源码说明。

## 后续应填充的信息

- Breeno 分支的整体心智模型
- 关键 Hook 列表与各自职责
- 用户 query 从哪里捕获，响应从哪里注入，原生卡片从哪里拦截
- session reset 与浮窗 / Activity 生命周期的联动点
- 调试 Breeno 问题时最该先看的相对路径

## 建议引用的源码位置

- `app/src/main/java/.../mod/feat/oppo/BreenoChatHook.kt`
- `app/src/main/java/.../mod/feat/oppo/BreenoConfigProvider.kt`
- `app/src/main/java/.../mod/feat/oppo/BreenoFeedbackAssembler.kt`
- `app/src/main/java/.../mod/feat/oppo/subhooks/`

## 写作约束

- 不要抄 Hook 实现和反射细节代码
- 除非为了解释架构，否则不要贴代码
- 只写业务链路、职责分工、调试入口、相对路径
