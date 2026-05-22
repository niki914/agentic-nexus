# Boot Sequence

## 目的

本文件后续用于描述 Xposed 入口到业务 Hook 安装完成之间的启动链路。

## 后续应填充的信息

- 受支持宿主是如何被识别的
- `Entrance` 在加载时做了哪些关键步骤
- `ContextHook`、`ActivityHook`、`ContextProvider` 在启动期各自承担什么角色
- 宿主类型如何路由到 Breeno / XiaoAi 的具体实现
- `RuntimeBootstrap` 或等价运行时安装点的顺序与约束

## 建议引用的源码位置

- `app/src/main/java/.../Entrance.kt`
- `h/src/main/java/.../ContextHook.kt`
- `h/src/main/java/.../ActivityHook.kt`
- `h/src/main/java/.../ContextProvider.kt`
- `app/src/main/java/.../mod/feat/`

## 写作约束

- 不要把方法实现整段贴进文档
- 如果必须解释时序，只能摘取最小必要片段，且目的必须是解释链路
- 优先用步骤、阶段、相对路径来描述
