# Render Pipeline

## 目的

本文件后续用于描述 Breeno 与 XiaoAi 两条响应注入链路，并说明两者的关键差异。

## 后续应填充的信息

- Breeno 的注入模型是什么，为什么是单卡片全量刷新
- XiaoAi 的注入模型是什么，为什么是增量文本分片注入
- 两条链路各自依赖哪些前置捕获点
- 各自如何阻断原生响应与原生 TTS
- 终帧处理、session 清理、并发控制分别在哪一层处理

## 建议引用的源码位置

- `app/src/main/java/.../mod/feat/oppo/BreenoChatHook.kt`
- `app/src/main/java/.../mod/feat/oppo/subhooks/`
- `app/src/main/java/.../mod/feat/hyper/XiaoaiChatHook.kt`
- `app/src/main/java/.../mod/feat/hyper/subhooks/`

## 写作约束

- 不要把 Hook 实现逐段复制进来
- 除非必须解释架构，不要贴代码片段
- 重点写链路、状态、差异、约束，并用相对路径指向源码
