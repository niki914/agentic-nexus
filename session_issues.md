# Session 库问题整理

本文记录 Nexus 接入 `s3ss10n` 后暴露出的 session 库侧问题。Nexus 当前只做必要 workaround，不继续扩展 MCP 发现、缓存和去重逻辑。

## 1. MCP discovery 状态不可观测

Nexus 需要知道 MCP server 当前是失败、加载中还是可用，以便写入 prompt，让 Agent 理解工具状态。

当前问题：

- `Session` 能注册 MCP server，也能 `refreshMcpTools()`，但缺少稳定的 discovery 状态订阅或查询入口。
- `refreshMcpTools()` 返回本次刷新结果，但 Nexus 仍难以表达“当前缓存来自何时、哪些 server 不可达、哪些工具是 stale cache”。
- Nexus 为了捕获 `tools/list` 结果补了 `McpInterceptorHttpEngine`，这本质是 workaround，不应该成为业务层长期维护的 discovery 机制。

期望 session 库提供：

- MCP discovery 状态模型，例如 `Idle / Discovering / Available / Failed / UsingStaleCache`。
- server 维度的状态、错误原因、最近成功刷新时间、discovered tool 数量。
- 能直接被宿主用于 prompt/runtime status 的只读快照。

## 2. MCP cache update hook

Nexus 当前需要在 MCP tools 发现后写入本地 cache，失败时清理对应 server cache。

当前问题：

- cache 更新依赖 Nexus 自定义 HTTP engine 拦截响应。
- 失败清缓存逻辑散落在 Nexus 的 `LLMController.refresh()` 中，库和业务边界不清晰。
- session 库最清楚 discovery 生命周期，但业务层反而承担了 discovery 结果落库和失败补偿。

期望 session 库提供：

- 类似 `mcpHooks {}` 的 discovery 生命周期回调。
- `onToolsDiscovered(server, tools)`，用于业务层持久化 cache。
- `onDiscoveryFailed(server, error, previousCachePolicy)`，用于明确是否保留 stale cache。
- `onDiscoveryStateChanged(server, state)`，用于 UI 或 prompt 状态更新。

说明：`mcpHooks {}` 不应只是 tool call hooks 的变体，它本质上是 MCP discovery/cache 生命周期 hook。

## 3. MCP tool 去重与冲突策略

Nexus 顶层不应该实现 MCP tool 去重。去重最好发生在 session 库内部，因为库最接近最终发给模型的 tools 列表。

当前问题：

- 同一个 server 重复返回同名 tool 时，业务层无法确认最终 request 是否重复。
- 多个 server 暴露同名 tool 时，需要明确是 namespace、覆盖、报错，还是保留并改名。
- Nexus 只能看到本地配置和 cache，无法可靠判断 session 库最终组装的 tool registry。

期望 session 库提供：

- 同 server 内同名 tool 的去重规则。
- 跨 server 同名 tool 的冲突策略，例如 `serverName/toolName` namespace 或显式失败。
- 调试用最终 tool registry 快照，便于 Nexus 写日志和 UI。

## 4. 空 assistant message 防御

Nexus 曾遇到 OpenAI 兼容服务返回：

```text
Invalid assistant message: content or toolcalls must be set
```

这通常意味着请求历史里出现了既没有 `content` 也没有 `tool_calls` 的 assistant message。

期望 session 库提供：

- 在追加历史或发送请求前拒绝/过滤非法 assistant message。
- 在错误事件中暴露问题消息的 round、role、index，便于宿主定位。
- 如果是 streaming tool_calls 合并过程产生的空 assistant，应由库侧修复历史建模。

## Nexus 当前边界

- Nexus 可以消费 discovery 状态、展示状态、持久化业务 cache。
- Nexus 不继续实现 MCP discovery 协议细节。
- Nexus 不在业务层实现最终 tool registry 去重。
- `McpInterceptorHttpEngine` 只是临时补洞，后续应在 session 库能力完善后移除。
