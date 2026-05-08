# New Session DX README

> 这是面向开发者的下一代 `Session` DX 提案。
> 只描述开发者会接触到的 API、数据类型、事件与 DSL。
> 不涉及任何底层实现、协议细节、线程模型或内部模块拆分。

## 设计目标

- 面向 Android 开发者，优先保证调用简单、可读、可维护
- 保留“自动驱动单轮对话 tooling”的设计哲学
- 保留 `onToolCall` 必须返回 `Message.Tool` 的强约束
- 支持 MCP，但不让 MCP 污染会话主 API
- `Session.open {}` 与 `session.update {}` 共用同一套 DSL
- 已经在运行中的轮次不受配置更新影响
- 最新配置在下一次 `send(...)` 发起时生效

## MCP

### 已确认的 MCP Server 样本

- `as-locate-plugin-personal`
    - 形态: 远程 HTTP MCP endpoint
    - 示例地址: `http://127.0.0.1:51338/mcp`
    - 当前验证结果: `POST initialize`、`POST tools/list`、`POST tools/call` 都可正常返回
    - 对 Breeno 的意义: 适合作为 V1 的直接接入目标
- `miot-mcp`
    - 仓库: `https://github.com/javen-yan/miot-mcp`
    - 形态: Python FastMCP `stdio` server
    - 启动方式: `python mcp_server/mcp_server.py`
    - 对 Breeno 的意义: 适合作为协议兼容样本，不适合作为 Android 侧直接接入形态

### 当前确认

- 当前可用示例 MCP: `http://127.0.0.1:51338/mcp`
- 这不是 `stdio` 型 MCP，也不是 quickstart 里那种本地拉起脚本进程的模式
- 当前端点表现为单一 `/mcp` HTTP endpoint，使用 `POST` 承载 JSON-RPC
- 对该端点执行 `GET /mcp` 会返回 `405 method not allowed`
- `initialize` 可正常返回，当前协商到的 `protocolVersion` 为 `2025-06-18`
- `initialize` 返回的 `capabilities` 当前包含 `tools`
- `tools/list` 可正常返回，结果中每个 tool 都带有 `name`、`description`、`inputSchema`
- `tools/call` 可正常返回，当前示例返回形态为 `result.content: [{ "type": "text", "text": "..." }]`

### 正确接入目标

- MCP client 只负责三件事:
    - 发送 `initialize`
    - 拉取 `tools/list`
    - 执行 `tools/call`
    - 模型本身不需要理解 MCP 的 transport 或 server 拓扑
    - 对模型来说，它只看到一个被拍平后的 tool set
    - 本地工具、MCP 工具、未来其他工具源的来源差异，应该由 `Session` 在 routing 层消化
    - `hooks { ... }` 中暴露 `ToolCallKind`，只是为了让开发者在必要时做定制分流，而不是把 MCP 细节暴露给模型

### 当前示例端点的特殊点

- 这个 `as-locate-plugin-personal` 端点返回的是标准 JSON-RPC 外壳
- 但 `tools/call` 的业务结果放在 `result.content[]` 里
- `content[0].type` 当前为 `text`
- `content[0].text` 内部又是一个 JSON 字符串，而不是直接结构化对象
- 因此 Breeno 的第一版适配层需要支持“从 text content 中再解析一次 JSON”这一兼容逻辑
- 这属于 server-specific 兼容，不应污染上层会话 API

### 第二个样本带来的兼容要求

- `miot-mcp` 的测试脚本在 `initialize` 成功后会继续发送 `notifications/initialized`
- 这说明 Breeno 不能只实现 `initialize` 请求，还要补上初始化完成通知
- `miot-mcp` 的 tool result 同时提供:
    - `structuredContent`
    - `content`
    - `isError`
- 因此 Breeno 的结果解码优先级应该是:
    - 优先读取 `structuredContent`
    - 其次读取 `content[]`
    - 最后再做 `text` 内 JSON 的兼容解析
- `as-locate-plugin-personal` 属于 `content[0].text` 内嵌 JSON 的特例
- `miot-mcp` 更接近我们应当优先兼容的标准 MCP result 形态

### 后续实现约束

- 先做 HTTP MCP client，让 Breeno 主仓跑通当前 `/mcp` 示例
- 暂不为了 MCP 重写 `OkHttp`、SSE parser、chat transport
- 后续如果要支持:
    - `stdio`
    - Streamable HTTP
    - 旧版 HTTP + SSE
    - 其他 MCP server 的不同 content 结果形态
- 再把 transport 与结果解码层做成可插拔

### 本次探测结论

- 当前 `/mcp` 端点已经确认可以被 Breeno 作为远程 MCP server 使用
- `miot-mcp` 已确认是标准 `stdio` MCP server，可作为第二个协议兼容样本
- 现在阻塞项不在协议判断，而在业务侧补 MCP client 适配层与 `adb reverse` 端口映射
- 文档先以“HTTP MCP endpoint + JSON-RPC tools lifecycle + initialized 通知 + structuredContent 优先解析”为基线推进

## 总览

开发者只需要接触下面这些公开类型：

```kotlin
import com.niki914.s3ss10n.Session
import com.niki914.s3ss10n.SessionConfig
import com.niki914.s3ss10n.SessionEvent
import com.niki914.s3ss10n.SessionHooks
import com.niki914.s3ss10n.ToolCallRequest
import com.niki914.s3ss10n.ToolCallKind
import com.niki914.s3ss10n.McpServerConfig
import com.niki914.s3ss10n.LocalToolConfig
import com.niki914.s3ss10n.LocalToolProperty
import com.niki914.s3ss10n.ToolValueType
import com.niki914.s3ss10n.Message
```

## 最小使用方式

```kotlin
val session = Session.open {
    endpoint = "https://api.openai.com/v1/chat/completions"
    apiKey = "YOUR_API_KEY"
    model = "gpt-4.1-mini"
    systemPrompt = "You are a helpful assistant."
}

session.send("你好") { event ->
    when (event) {
        is SessionEvent.TextDelta -> renderDelta(event.delta)
        is SessionEvent.RoundCompleted -> renderFinal(event.fullText)
        is SessionEvent.Error -> showError(event.message)
        else -> Unit
    }
}
```

## 完整使用方式

```kotlin
val session = Session.open {
    endpoint = "https://api.openai.com/v1/chat/completions"
    apiKey = "YOUR_API_KEY"
    model = "gpt-4.1-mini"
    systemPrompt = "You are a helpful assistant."

    hooks { call ->
        when (call.kind) {
            ToolCallKind.Local -> delegate()
            ToolCallKind.Mcp -> delegate()
        }
    }

    localTools {
        add("toast") {
            description = "显示一个提示"
            string("message") {
                description = "要显示给用户的消息"
                required = true
            }
        }
    }

    mcp {
        add("aslocate") {
            http {
                url = "http://127.0.0.1:51338/mcp"
            }
        }
    }
}

session.send("帮我查一下这个符号定义") { event ->
    when (event) {
        is SessionEvent.TextDelta -> renderDelta(event.delta)
        is SessionEvent.ToolRunning -> showToolRunning(event.toolName)
        is SessionEvent.ToolSucceeded -> showToolSuccess(event.toolName)
        is SessionEvent.ToolFailed -> showToolFailure(event.toolName, event.message)
        is SessionEvent.RoundCompleted -> renderFinal(event.fullText)
        is SessionEvent.Error -> showError(event.message)
    }
}
```

## Session

```kotlin
interface Session {
    suspend fun send(
        text: String,
        onEvent: (SessionEvent) -> Unit = {}
    )

    fun update(
        block: SessionConfig.() -> Unit
    )

    suspend fun resetConversation()

    suspend fun close()

    companion object {
        fun open(
            block: SessionConfig.() -> Unit
        ): Session
    }
}
```

### 语义

- `open {}`: 创建一个可长期复用的会话实例
- `send(...)`: 发起一轮新的用户输入
- `update {}`: 更新会话配置，但不打断当前已在运行中的轮次
- `resetConversation()`: 清空当前对话历史，通常用于“新建对话”或“用户切换 MCP 后重新开始”
- `close()`: 释放会话资源

## SessionConfig

```kotlin
data class SessionConfig(
    var endpoint: String = "",
    var apiKey: String = "",
    var model: String = "",
    var systemPrompt: String? = null,
    var temperature: Float = 0.7f,
    var connectTimeoutSeconds: Long = 30,
    var readTimeoutSeconds: Long = 60,
    var writeTimeoutSeconds: Long = 30,
)
```

### 扩展 DSL

`SessionConfig` 同时承载下面三组 DSL：

- `hooks { ... }`
- `localTools { ... }`
- `mcp { ... }`

示例：

```kotlin
val session = Session.open {
    endpoint = "https://api.openai.com/v1/chat/completions"
    apiKey = "YOUR_API_KEY"
    model = "gpt-4.1-mini"
    systemPrompt = "You are a helpful assistant."

    hooks { delegate() }

    localTools {
        add("toast") {
            description = "显示一个提示"
            string("message") {
                required = true
            }
        }
    }

    mcp {
        add("aslocate") {
            http {
                url = "http://127.0.0.1:51338/mcp"
            }
        }
    }
}
```

## SessionHooks

`hooks` 是开发者接管 tool routing 的入口。

它不是事件监听器，而是**会影响对话推进结果的控制点**。

```kotlin
fun SessionConfig.hooks(
    block: suspend ToolCallRequest.() -> Message.Tool
)
```

### 语义

- `hooks { ... }` 必须返回 `Message.Tool`
- 如果开发者不处理某个 tool call，应该显式调用 `delegate()`
- `Handler` 这类业务封装属于开发者代码，框架不提供
- `hooks` 永远基于“当前 session 的 tooling 视图”运行，而不是绑定某个固定 provider

示例：

```kotlin
val session = Session.open {
    hooks { call ->
        when (call.name) {
            "toast" -> ok("""{"shown":true}""")
            else -> delegate()
        }
    }
}
```

或者：

```kotlin
val session = Session.open {
    hooks { call ->
        MyToolHandler.handle(call)
            .fallback { delegate() }
    }
}
```

`MyToolHandler` 是开发者自己的封装，不属于框架 API。

## ToolCallRequest

`ToolCallRequest` 是开发者在 `hooks { ... }` 中唯一需要理解的工具调用对象。

```kotlin
sealed interface ToolCallRequest {
    val id: String
    val name: String
    val argumentsJson: String
    val kind: ToolCallKind

    suspend fun delegate(): Message.Tool

    fun ok(contentJson: String): Message.Tool

    fun error(
        message: String,
        contentJson: String = """{"success":false}"""
    ): Message.Tool
}
```

### 为什么做成 `sealed interface`

- 这样可以在不破坏外层 DX 的前提下扩展新的 tooling 来源
- 开发者可以根据来源做分支
- 未来新增来源时，类型边界仍然稳定

## ToolCallKind

```kotlin
sealed interface ToolCallKind {
    data object Local : ToolCallKind
    data class Mcp(val serverName: String) : ToolCallKind
}
```

### 语义

- `Local`: 来自本地注册工具
- `Mcp`: 来自某个 MCP server

示例：

```kotlin
hooks { call ->
    when (val kind = call.kind) {
        ToolCallKind.Local -> delegate()
        is ToolCallKind.Mcp -> {
            if (kind.serverName == "aslocate") delegate()
            else error("unsupported mcp server")
        }
    }
}
```

## SessionEvent

`SessionEvent` 是 `send(...)` 期间的细粒度事件流。

```kotlin
sealed interface SessionEvent {
    data class RoundStarted(
        val input: String
    ) : SessionEvent

    data class TextDelta(
        val delta: String,
        val fullText: String
    ) : SessionEvent

    data class ToolRunning(
        val callId: String,
        val toolName: String,
        val kind: ToolCallKind
    ) : SessionEvent

    data class ToolSucceeded(
        val callId: String,
        val toolName: String,
        val kind: ToolCallKind,
        val resultJson: String
    ) : SessionEvent

    data class ToolFailed(
        val callId: String,
        val toolName: String,
        val kind: ToolCallKind,
        val message: String,
        val resultJson: String? = null
    ) : SessionEvent

    data class RoundCompleted(
        val fullText: String
    ) : SessionEvent

    data class Error(
        val stage: Stage,
        val message: String,
        val cause: Throwable? = null
    ) : SessionEvent

    enum class Stage {
        Transport,
        Parse,
        Tool,
        Session
    }
}
```

### 语义

- `RoundStarted`: 一次 `send(...)` 的首个“用户可感知事件”开始，通常是首个文本增量，也可能是首个 tool 开始执行
- `TextDelta`: 本轮 assistant 文本增量
- `ToolRunning`: 某个 tool 已开始执行
- `ToolSucceeded`: 某个 tool 已成功执行并回填
- `ToolFailed`: 某个 tool 执行失败
- `RoundCompleted`: 本轮结束，返回最终文本
- `Error`: 失败事件

### 设计原则

- `send(...)` 的事件回调只负责观察和 UI 渲染
- 真正控制 round 推进的是 `hooks { ... }`
- 事件可以很细，但事件本身不承担“决定是否继续下一轮”的责任

## localTools DSL

本地工具与 MCP 工具都属于同一个 tooling surface。

```kotlin
fun SessionConfig.localTools(
    block: LocalToolRegistry.() -> Unit
)

interface LocalToolRegistry {
    fun add(
        name: String,
        block: LocalToolConfig.() -> Unit
    )

    fun replace(
        name: String,
        block: LocalToolConfig.() -> Unit
    )

    fun remove(name: String)
}

data class LocalToolConfig(
    var description: String = "",
    var rawInputSchemaJson: String? = null
)

data class LocalToolProperty(
    val name: String,
    var type: ToolValueType = ToolValueType.String,
    var description: String = "",
    var required: Boolean = false,
    var enumValues: List<String> = emptyList()
)

enum class ToolValueType {
    String,
    Integer,
    Number,
    Boolean,
    Object,
    Array
}

fun LocalToolConfig.string(
    name: String,
    block: LocalToolProperty.() -> Unit = {}
)

fun LocalToolConfig.integer(
    name: String,
    block: LocalToolProperty.() -> Unit = {}
)

fun LocalToolConfig.number(
    name: String,
    block: LocalToolProperty.() -> Unit = {}
)

fun LocalToolConfig.boolean(
    name: String,
    block: LocalToolProperty.() -> Unit = {}
)

fun LocalToolConfig.object_(
    name: String,
    block: LocalToolProperty.() -> Unit = {}
)

fun LocalToolConfig.array(
    name: String,
    block: LocalToolProperty.() -> Unit = {}
)

fun LocalToolConfig.rawJsonSchema(
    json: String
)
```

示例：

```kotlin
session.update {
    localTools {
        replace("toast") {
            description = "显示一个新的提示"

            string("message") {
                description = "要显示给用户的消息"
                required = true
            }
        }

        remove("legacyTool")
    }
}
```

上面这段 DSL 对应的 JSON schema 形态大致如下：

```json
{
  "type": "object",
  "properties": {
    "message": {
      "type": "string",
      "description": "要显示给用户的消息"
    }
  },
  "required": ["message"]
}
```

再看一个稍复杂一点的例子：

```kotlin
localTools {
    add("setVolume") {
        description = "设置音量"

        integer("level") {
            description = "目标音量，范围 0 到 100"
            required = true
        }

        boolean("speakBack") {
            description = "设置完成后是否语音播报"
        }
    }
}
```

对应 JSON schema：

```json
{
  "type": "object",
  "properties": {
    "level": {
      "type": "integer",
      "description": "目标音量，范围 0 到 100"
    },
    "speakBack": {
      "type": "boolean",
      "description": "设置完成后是否语音播报"
    }
  },
  "required": ["level"]
}
```

### 设计原则

- 默认 DSL 只表达开发者最常见的参数定义需求
- 不在 DSL 层绑定 `kotlinx.serialization`、`moshi`、`gson` 或其他框架
- 不要求开发者先定义输入数据类才能声明 tool
- 参数 DSL 只负责声明“给模型看的 schema”
- 真正如何解析 `argumentsJson`，由开发者在 `hooks { ... }` 或自己的 `Handler` 中决定
- 复杂 schema 场景允许退回 `rawJsonSchema(...)` 作为逃生口
- `rawJsonSchema(...)` 是高级能力，不应该成为默认写法

## MCP DSL

V1 先只面向 Android 提供 HTTP MCP。

`stdio` 不作为当前稳定 DX 的一部分公开。

```kotlin
fun SessionConfig.mcp(
    block: McpRegistry.() -> Unit
)

interface McpRegistry {
    fun add(
        name: String,
        block: McpServerConfig.() -> Unit
    )

    fun replace(
        name: String,
        block: McpServerConfig.() -> Unit
    )

    fun remove(name: String)
}

data class McpServerConfig(
    var enabled: Boolean = true,
    var transport: McpTransport = McpTransport.Http(),
    var headers: Map<String, String> = emptyMap()
)

sealed interface McpTransport {
    data class Http(
        var url: String = ""
    ) : McpTransport
}
```

### 推荐写法

```kotlin
val session = Session.open {
    mcp {
        add("aslocate") {
            http {
                url = "http://127.0.0.1:51338/mcp"
            }
        }
    }
}
```

为了让 DSL 更自然，额外提供：

```kotlin
fun McpServerConfig.http(
    block: McpTransport.Http.() -> Unit
)
```

## update 语义

`open {}` 与 `update {}` 共享同一套配置 DSL。

```kotlin
session.update {
    endpoint = "https://api.openai.com/v1/chat/completions"
    model = "gpt-4.1-mini"

    mcp {
        replace("aslocate") {
            http {
                url = "http://127.0.0.1:51338/mcp"
            }
        }

        remove("legacy")
    }
}
```

### update 规则

- `update {}` 会立即更新 session 的 active config
- 已经在执行中的 round 不受影响
- 下一次 `send(...)` 发起时使用最新配置
- `update {}` 不会自动清空历史
- 如果开发者希望“像新建对话一样”生效，应显式调用 `resetConversation()`

## resetConversation

```kotlin
suspend fun Session.resetConversation()
```

### 什么时候应该调用

- 用户点击“新对话”
- 用户修改 MCP 列表后，希望新 schema 从干净历史开始生效
- 用户切换 endpoint / model / system prompt 后，希望重置上下文

示例：

```kotlin
session.update {
    mcp {
        replace("aslocate") {
            http {
                url = "http://127.0.0.1:60000/mcp"
            }
        }
    }
}

session.resetConversation()
```

## 推荐的开发者心智模型

- `Session` 是长期存在的会话对象
- `send(...)` 是一次用户轮次
- `hooks { ... }` 是控制 tool routing 的入口
- `SessionEvent` 是 UI 的观察入口
- `localTools {}` 与 `mcp {}` 共同组成当前可用的 tooling surface
- `update {}` 更新的是未来轮次使用的配置
- `resetConversation()` 用于明确切换上下文

## 一个完整例子

```kotlin
val session = Session.open {
    endpoint = "https://api.openai.com/v1/chat/completions"
    apiKey = "YOUR_API_KEY"
    model = "gpt-4.1-mini"
    systemPrompt = "You are a helpful assistant."

    hooks { call ->
        when (call.name) {
            "toast" -> ok("""{"shown":true}""")
            else -> delegate()
        }
    }

    localTools {
        add("toast") {
            description = "显示一个提示"
            string("message") {
                description = "要显示给用户的消息"
                required = true
            }
        }
    }

    mcp {
        add("aslocate") {
            http {
                url = "http://127.0.0.1:51338/mcp"
            }
        }
    }
}

session.send("帮我找一下 ChatSession 的定义") { event ->
    when (event) {
        is SessionEvent.RoundStarted -> showLoading()
        is SessionEvent.TextDelta -> render(event.fullText)
        is SessionEvent.ToolRunning -> showToolRunning(event.toolName)
        is SessionEvent.ToolSucceeded -> showToolSuccess(event.toolName)
        is SessionEvent.ToolFailed -> showToolFailure(event.toolName, event.message)
        is SessionEvent.RoundCompleted -> hideLoading()
        is SessionEvent.Error -> showError(event.message)
    }
}

session.update {
    mcp {
        replace("aslocate") {
            http {
                url = "http://127.0.0.1:60000/mcp"
            }
        }
    }
}

session.resetConversation()
```
