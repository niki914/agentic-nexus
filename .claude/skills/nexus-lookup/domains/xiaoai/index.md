# XiaoAi Domain

## 整体模型

XiaoAi 侧当前走**响应目标 + Instruction 分片注入**。LLM 累计文本会被切成 delta，再包装成宿主 `Instruction` 注入已捕获的响应目标。

## 关键 Hook

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`

- `XiaoaiChatHook.kt`：主编排入口；安装输入、响应、生命周期 subhook，并协调目标捕获与流式消费。
- `XiaoaiConfigProvider.kt`：提供响应目标、白名单拦截、文字流注入、TTS 播放拦截所需的运行时类名和方法名。
- `XiaoaiRenderSession.kt`：记录当前渲染轮次的 session 状态。

### `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/`

- `CaptureInputHook.kt`：捕获 query。
- `CaptureResponseTargetHook.kt`：捕获后续注入用的响应目标对象。
- `BlockNativeInstructionByWhitelistHook.kt`：在 `InjectedLLM` 模式下按白名单放行原生 `Instruction`，其余默认拦截。
- `BlockNativeTtsPlaybackHook.kt`：拦截原生 TTS 播放。
- `RenderTextStreamCardHook.kt`：计算 delta、构造宿主 `Instruction`、注入分片并管理当前 `XiaoaiRenderSession`。

## 当前数据流

1. `dispatchQueryToLLM()` 先启动 `LLMController.stream(query)`，并用 `shareIn(..., SharingStarted.Eagerly, replay = Int.MAX_VALUE)` 预热共享流。
2. 等待 `CaptureResponseTargetHook` 完成响应目标捕获。
3. `collectAsChunk` 消费共享流，把累计文本交给 `RenderTextStreamCardHook.render()`。
4. `RenderTextStreamCardHook` 根据上次已渲染全文计算增量 `delta`。
5. 终帧额外注入 `finalChunkText`，随后清空当前 session。

## 边界与注意点

- 当前源码里没有单独的 `BlockNativeTextStreamHook` 或 `BlockNativeTtsStreamHook`；原生阻断已经收敛为 Instruction 白名单拦截加 TTS 播放拦截。
- `targetReady.await()` 仍带有死等风险 TODO；目标捕获失败时不会自动降级到其他注入路径。
- `RenderTextStreamCardHook` 只维护一个活跃 `XiaoaiRenderSession`，session reset 时会被显式清空。

## 调试入口

- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiChatHook.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiConfigProvider.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/XiaoaiRenderSession.kt`
- `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/subhooks/`
