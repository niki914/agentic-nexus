# XiaoAi Domain

## 目的

本文件后续用于给 XiaoAi 相关任务提供专项导航，帮助读者快速进入响应目标捕获、文本流注入与 TTS 拦截链路。

## 后续应填充的信息

- XiaoAi 分支的整体心智模型
- 响应目标是如何被捕获并在后续渲染中复用的
- 原生文字流、TTS 指令流、TTS 播放链路分别在哪里被阻断
- 增量文本分片注入的关键状态和约束
- 调试 XiaoAi 相关问题时优先查看哪些相对路径

## 建议引用的源码位置

- `app/src/main/java/.../mod/feat/hyper/XiaoaiChatHook.kt`
- `app/src/main/java/.../mod/feat/hyper/XiaoaiConfigProvider.kt`
- `app/src/main/java/.../mod/feat/hyper/XiaoaiRenderSession.kt`
- `app/src/main/java/.../mod/feat/hyper/subhooks/`

## 写作约束

- 不要复制 Hook 与渲染实现全文
- 除非为了说明架构，否则不要贴代码
- 用术语解释、链路描述、相对路径导航替代源码复制
