# Session 库问题整理

本文记录 Nexus 接入 `s3ss10n` 后暴露出的 session 库侧问题。Nexus 当前只做必要 workaround，不继续扩展 MCP 发现、缓存和去重逻辑。

## 1. MCP discovery 状态不可观测（已解决）

Nexus 需要知道 MCP server 当前是失败、加载中还是可用，以便写入 prompt，让 Agent 理解工具状态。

历史问题：

- 早期 `Session` 能注册 MCP server，也能 `refreshMcpTools()`，但缺少稳定的 discovery 状态订阅或查询入口。
- 早期 `refreshMcpTools()` 返回本次刷新结果，但 Nexus 仍难以表达“当前缓存来自何时、哪些 server 不可达、哪些工具是 stale cache”。
- Nexus 曾为了捕获 `tools/list` 结果补充 `McpInterceptorHttpEngine`，这是临时 workaround，不再作为业务层长期 discovery 机制。

当前状态：

- `s3ss10n` `2.1.1` 已提供 `getMcpDiscoverySnapshot()`。
- Nexus 已消费 `Idle / Discovering / Available / Failed / UsingStaleCache` 状态并写入 prompt runtime section。
- `Failed` 状态只输出 `load failed`，不把连接错误、异常 message 等原因注入 prompt。

## 2. MCP cache update hook（已解决）

Nexus 当前仍需要在 MCP tools 发现后写入本地 cache；这是业务持久化边界，不由 session 内存 cache 替代。

历史问题：

- cache 更新曾依赖 Nexus 自定义 HTTP engine 拦截响应。
- 失败清缓存逻辑曾散落在 Nexus 的 `LLMController.refresh()` 中，库和业务边界不清晰。
- session 库最清楚 discovery 生命周期，但业务层曾承担 discovery 协议响应解析。

当前状态：

- `s3ss10n` `2.1.1` 已提供 `mcpHooks { onToolsDiscovered(serverName, tools) }`。
- Nexus 已将 discovered tools 持久化入口迁移到 session hook，继续写入本地 cache，支持冷启动 bootstrap。
- MCP refresh 失败时不再清理 Nexus 持久化 cache，stale/failed 状态交给 session snapshot 表达。

说明：`mcpHooks {}` 不只是 tool call hooks 的变体，它本质上是 MCP discovery/cache 生命周期 hook。

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

这通常意味着请求历史里出现了既没有 `content` 也没有 `tool_calls` 的 assistant message。可能存在内部错误导致这种消息的出现

## 5. 超时参数

除了网络超时以外，希望添加一个 llm 超时，口径是若干秒内没有出新的事件

## Nexus 当前边界

- Nexus 可以消费 discovery 状态、展示状态、持久化业务 cache。
- Nexus 不继续实现 MCP discovery 协议细节。
- Nexus 不在业务层实现最终 tool registry 去重。
- `McpInterceptorHttpEngine` 只是历史临时补洞；session API 迁移后不再保留在主运行时链路。
