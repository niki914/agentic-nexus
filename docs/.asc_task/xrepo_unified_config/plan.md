# 任务规划清单 v1.0

## 1. Feature 列表

| Feature ID | 功能描述 | 预估 LOC | 依赖 Feature |
|:-----------|:---------|:---------|:-------------|
| F-01 | XRepo 核心、强类型模型、codec、store、单元测试底座 | ~850 | - |
| F-02 | MCP server、headers、discovered tools cache 接入 XRepo | ~360 | F-01 |
| F-03 | CustomTool 与 BuiltinTool 配置读写接入 XRepo | ~420 | F-01 |
| F-04 | LLM runtime 与 UI ViewModel 迁移到 XRepo | ~520 | F-02, F-03 |

## 2. Batch 编排表

| Batch ID | 包含 Feature | 预估总 LOC | 前置 Batch | 可并行 |
|:---------|:------------|:-----------|:-----------|:-------|
| B-01 | F-01 | ~850 | - | - |
| B-02 | F-02 | ~360 | B-01 | 与 B-03 并行 |
| B-03 | F-03 | ~420 | B-01 | 与 B-02 并行 |
| B-04 | F-04 | ~520 | B-02, B-03 | - |

> 编排说明：F-01 是底座且超过 500 LOC，独占首批。F-02 与 F-03 都只依赖 F-01，目标文件不重叠，可并行。F-04 同时消费 MCP 与 Tool 的强类型 API，必须在 B-02、B-03 后执行。

## 3. 任务清单 (Task List)

### Feature F-01: XRepo 核心、强类型模型、codec、store、单元测试底座

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-01 | Contracts | Contract | **新增 repo 强类型模型**：定义 `data class LlmConfig(...)`、`data class McpServer(...)`、`data class McpTool(...)`、`data class CustomTool(...)`、`data class BuiltinToolSetting(...)`、`data class CustomToolValidation(...)`。 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt` | `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt` | - | L | ~90 LOC | 外部调用方能导入 repo models；文件中不暴露 `JsonObject`、`JsonArray`、`LocalSettings` |
| T-02 | Infra | Infra | **新增 LocalSettingsStore 抽象**：定义 `internal interface LocalSettingsStore { suspend fun read(context: Context): LocalSettings; suspend fun write(context: Context, settings: LocalSettings) }` 与 `internal object XServiceLocalSettingsStore`。 | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsStore.kt` | `app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt`, `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt` | - | L | ~45 LOC | 生产 store 调用 `XService.getLocalSettings/putLocalSettings`；接口为 internal |
| T-03 | Logic | Logic | **新增 LocalSettingsCodec 纯函数**：实现 `parseLlm`、`withLlm`、`withLlmAccess`、`parseMcpServers`、`withMcpServers`、`parseMcpCache`、`withMcpCache`、`withoutMcpCache`、`parseCustomTools`、`withCustomTools`、`parseBuiltinFlags`、`withBuiltinFlag`、`withBoolean`。 | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt` | `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt` | - | M | ~360 LOC | codec 写回只替换目标 top-level key；MCP 读取兼容 `url` 与 `transport.url`；headers/cache/custom/builtin round-trip 保持稳定 |
| T-04 | Logic | Logic | **新增 XRepo 主入口**：实现 `object XRepo`，签名包含 `fun init(context: Context)`、`suspend fun llm(): LlmConfig`、`suspend fun saveLlmAccess(...)`、`suspend fun saveLlm(config: LlmConfig)`、`suspend fun onboardingCompleted()`、`suspend fun setOnboardingCompleted(value: Boolean)`、`val mcp`、`val customTools`、`val builtinTools`。 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | `app/src/main/java/com/niki914/nexus/agentic/mod/ContextProvider.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsStore.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt` | - | M | ~230 LOC | `updateLocal` 使用同一个 `Mutex` 包住 read-transform-write；未 init 时可等待 `ContextProvider.await()` |
| T-05 | Tests | Test | **新增 codec 单元测试**：测试函数包含 `parseMcpServers_readsUrlHeadersAndEnabled()`、`withMcpServers_writesNameUrlEnabledHeaders()`、`mcpCache_roundTripByUrlAndHeaders()`、`parseCustomTools_ignoresBlankEntries()`、`withBuiltinFlag_preservesOtherFlags()`、`withLlmAccess_preservesPromptProxyAndTools()`。 | `app/src/test/java/com/niki914/nexus/agentic/repo/LocalSettingsCodecTest.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt` | - | M | ~170 LOC | JVM 测试可运行；每个测试断言具体 JSON key 与强类型字段 |
| T-06 | Tests | Test | **新增 XRepo 行为单元测试**：用 fake `LocalSettingsStore` 覆盖 `saveLlmAccess_updatesOnlyAccessFields()`、`mcpSave_replacesByNameAndPreservesOtherServers()`、`mcpClearCache_removesOnlyTargetCacheKey()`、`customToolSave_rejectsUnsafeCommand()`、`builtinSetEnabled_rejectsUnknownTool()`。 | `app/src/test/java/com/niki914/nexus/agentic/repo/XRepoTest.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsStore.kt` | - | M | ~190 LOC | fake store 能记录写入次数；失败校验不写入；成功写入保留其他配置域 |

### Feature F-02: MCP server、headers、discovered tools cache 接入 XRepo

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-07 | Logic | Logic | **迁移 McpDiscoveryCacheStore 持久化**：修改 `suspend fun persistDiscoveredTools(...)` 路径，使解析出的 `DiscoveredTool` 映射为 `McpTool` 后调用 `XRepo.mcp.saveDiscoveredTools(url, headers, tools)`。 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt` | - | M | ~90 LOC | 该文件不再调用 `XService.putLocalSettings`；cache key 仍按 url + headers 匹配 |
| T-08 | Logic | Logic | **新增 ToolManager 强类型 resolve overload**：实现 `fun resolve(customTools: List<CustomTool>, mcpServers: List<McpServer>, builtinSettings: List<BuiltinToolSetting>): ResolvedTools`；旧 `resolve(settings: LocalSettings)` 通过 `LocalSettingsCodec` 转发到新 overload。 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt`, `app/src/test/java/com/niki914/nexus/agentic/chat/v2/ToolManagerTest.kt` | - | M | ~170 LOC | 旧测试继续通过；强类型 overload 能保留 MCP headers 与 cached tools |
| T-09 | Tests | Test | **扩展 ToolManager MCP 测试**：新增 `resolveFromTypedConfig_preservesMcpHeadersAndCache()`，构造 `McpServer(headers=...)` 与 `McpTool(inputSchemaJson=...)` 验证 runtime `McpServerDefinition.Http`。 | `app/src/test/java/com/niki914/nexus/agentic/chat/v2/ToolManagerTest.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt` | - | L | ~100 LOC | typed resolve 生成的 MCP server 含 headers；cached tool inputSchema 与原 JSON 等价 |
| T-10 | Tests | Test | **新增 MCP cache store 测试**：用 fake `XRepo` store 验证 `McpDiscoveryCacheStore` 解析 response 后写入 `mcp_discovered_tools_cache`。 | `app/src/test/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStoreTest.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | - | M | ~120 LOC | 输入 tools/list response 后，repo cache 中存在工具名、description、inputSchemaJson |

### Feature F-03: CustomTool 与 BuiltinTool 配置读写接入 XRepo

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-11 | Logic | Logic | **迁移 CustomToolManager 持久化**：将 `load(context)`、`persist(context, tools)`、`createOrUpdate(...)`、`delete(...)`、`setEnabled(...)` 的读写路径改为 `XRepo.customTools`，保留 public result 类型与校验语义。 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/ShellCommandSafetyPolicy.kt` | - | M | ~180 LOC | manager 内不再构造 `custom_tools` JSON；unsafe command 仍返回原错误语义 |
| T-12 | Logic | Logic | **迁移 BuiltinToolSettingsManager 持久化**：将 flags 解析与写回改为 `XRepo.builtinTools`，保留 `load(context)` 与 `setEnabled(context, name, enabled)` 对外行为。 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt` | - | M | ~130 LOC | manager 内不再构造 `builtin_tool_flags` JSON；未知 builtin tool 返回失败 |
| T-13 | Logic | Logic | **迁移 CreateCustomToolBuiltin 写入路径**：将 custom tool 创建逻辑改为构造 `CustomTool` 后调用 `XRepo.customTools.save(tool, overwrite)`。 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | - | L | ~60 LOC | builtin 创建成功后落入 `XRepo` 写入路径；校验失败 message 保持可读 |
| T-14 | Tests | Test | **扩展 Tool manager 相关测试**：覆盖 `CustomToolManager` 与 `BuiltinToolSettingsManager` 的 XRepo 写入行为，测试 unsafe command、duplicate name、unknown builtin 三条错误路径。 | `app/src/test/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManagerTest.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | - | M | ~150 LOC | 测试通过；失败路径不写入 fake store；成功路径写入强类型配置 |

### Feature F-04: LLM runtime 与 UI ViewModel 迁移到 XRepo

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-15 | Logic | Logic | **迁移 LLMController 配置读取**：在 `suspend fun refresh(context: Context)` 中调用 `XRepo.init(context)`，读取 `XRepo.llm()`、`XRepo.customTools.list()`、`XRepo.mcp.list()`、`XRepo.builtinTools.list()`；用 `XRepo.mcp.fingerprint()` 替换 raw JSON 指纹；MCP refresh failed names 调用 `XRepo.mcp.clearCacheByServerNames(names)`。 | `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` | - | H | ~190 LOC | `LLMController` 不再用 `settings.mcpServers?.toString()`；MCP 失败后会清理对应 cache |
| T-16 | Logic | Logic | **迁移 ConfigureViewModel 工厂依赖**：将配置页保存/读取路径替换为 `XRepo.llm()` 与 `XRepo.saveLlmAccess(...)`，保留 prompt/proxy 等未展示字段。 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt` | - | M | ~100 LOC | 配置页保存 provider/endpoint/model/apiKey 时不覆盖 prompt/proxy/memory |
| T-17 | Logic | Logic | **迁移 McpSettingsViewModel 到 XRepo.mcp**：将 `loadSettings/saveSettings` 依赖替换为 `XRepo.mcp.list/save/delete/setEnabled`，UI state 继续使用现有 `McpServerItem`。 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsState.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/test/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsViewModelTest.kt` | - | M | ~120 LOC | ViewModel 外部不再接收 `LocalSettings`；保存 MCP 时 headers 不被 codec 误删 |
| T-18 | Logic | Logic | **迁移 CustomToolsSettingsContent 读写入口**：将页面中的 `CustomToolManager` 直接调用替换为 ViewModel 或轻量 state holder 调用 `XRepo.customTools.list/setEnabled/delete`。 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` | - | M | ~90 LOC | 列表展示、启用开关、删除操作均走 `XRepo.customTools` |
| T-19 | Logic | Logic | **迁移 Context 初始化入口**：在 UI 与 Hook 已有 context 链路中调用 `XRepo.init(applicationContext)`，覆盖 `App.onCreate`、`MainActivity.onCreate` 或 `Entrance` 中已获得 Context 的路径。 | `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/App.kt`, `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`, `app/src/main/java/com/niki914/nexus/agentic/mod/ContextProvider.kt` | - | M | ~40 LOC | UI 进程启动后 `XRepo` 已 init；Hook 路径若未 init 仍可通过 `ContextProvider.await()` 兜底 |
| T-20 | Tests | Test | **更新 UI ViewModel 测试**：把 `McpSettingsViewModelTest` 从 fake `LocalSettings` 注入改为 fake `XRepo` store，覆盖 load、save trim、duplicate reject 三个用例。 | `app/src/test/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsViewModelTest.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsState.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | - | M | ~120 LOC | 测试不再断言 `savedSettings.props`；改为断言 `XRepo.mcp.list()` 的强类型结果 |

## 4. 实施步骤 (Steps per Task)

### T-01: 新增 repo 强类型模型
- [ ] 创建 `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt`
- [ ] 定义 `LlmConfig`，字段与 `tech_design.md` 保持一致
- [ ] 定义 `McpServer`、`McpTool`、`CustomTool`、`BuiltinToolSetting`
- [ ] 定义 `CustomToolValidation(field: String, message: String)`
- [ ] 检查模型文件不 import `kotlinx.serialization.json` 与 `LocalSettings`

### T-02: 新增 LocalSettingsStore 抽象
- [ ] 创建 `LocalSettingsStore.kt`
- [ ] 定义 internal `LocalSettingsStore`
- [ ] 实现 internal `XServiceLocalSettingsStore`
- [ ] 确认 read/write 仅委托 `XService`

### T-03: 新增 LocalSettingsCodec 纯函数
- [ ] 创建 `LocalSettingsCodec.kt`
- [ ] 实现 LLM parse/write 函数
- [ ] 实现 MCP servers parse/write，读取兼容 `transport.url`
- [ ] 实现 MCP cache parse/write/remove，复用 url + headers 稳定 key
- [ ] 实现 custom tools parse/write
- [ ] 实现 builtin flags parse/write
- [ ] 确认所有 with 函数保留非目标 top-level key

### T-04: 新增 XRepo 主入口
- [ ] 创建 `XRepo.kt`
- [ ] 增加 `init(context)` 与 `context()` await 逻辑
- [ ] 增加 `writeMutex` 与 `updateLocal(transform)`
- [ ] 挂载 `mcp`、`customTools`、`builtinTools`
- [ ] 实现 onboarding 与 LLM API
- [ ] 增加 internal test hook：`installStoreForTest`、`resetForTest`

### T-05: 新增 codec 单元测试
- [ ] 创建 `LocalSettingsCodecTest.kt`
- [ ] 覆盖 MCP servers headers parse/write
- [ ] 覆盖 MCP cache round-trip
- [ ] 覆盖 custom tools 空项过滤
- [ ] 覆盖 builtin flags 保留行为
- [ ] 覆盖 LLM access 保存不覆盖 prompt/proxy

### T-06: 新增 XRepo 行为单元测试
- [ ] 创建 `XRepoTest.kt`
- [ ] 实现 fake `LocalSettingsStore`
- [ ] 测试 `saveLlmAccess` 只更新 access 字段
- [ ] 测试 `XRepo.mcp.save` 以 name 替换
- [ ] 测试 `XRepo.mcp.clearCache` 只删除目标 cache
- [ ] 测试 CustomTool unsafe command 返回 validation
- [ ] 测试 unknown builtin 返回 validation

### T-07: 迁移 McpDiscoveryCacheStore 持久化
- [ ] 定位现有 `persistDiscoveredTools`
- [ ] 将 response 解析结果映射为 `McpTool`
- [ ] 写入改为 `XRepo.mcp.saveDiscoveredTools(url, headers, tools)`
- [ ] 删除该文件中对 `XService.putLocalSettings` 的直接依赖

### T-08: 新增 ToolManager 强类型 resolve overload
- [ ] 在 `ToolManager` 新增 typed overload
- [ ] 将 builtin settings 映射为 `LocalTool.Builtin`
- [ ] 将 custom tools 映射为 `LocalTool.Custom`
- [ ] 将 MCP servers 映射为 `McpServerDefinition.Http`
- [ ] 旧 `resolve(settings)` 使用 `LocalSettingsCodec` 后转发

### T-09: 扩展 ToolManager MCP 测试
- [ ] 在 `ToolManagerTest` 新增 typed resolve 测试
- [ ] 构造带 headers 的 `McpServer`
- [ ] 构造带 `inputSchemaJson` 的 `McpTool`
- [ ] 断言 runtime MCP headers 与 schema 等价

### T-10: 新增 MCP cache store 测试
- [ ] 创建 `McpDiscoveryCacheStoreTest.kt`
- [ ] 用 fake store 初始化 `XRepo`
- [ ] 输入 tools/list response JSON
- [ ] 调用 discovery store 持久化入口
- [ ] 断言 `XRepo.mcp.cachedTools(server)` 返回目标工具

### T-11: 迁移 CustomToolManager 持久化
- [ ] 保留 `CustomToolManager` public API 与结果类型
- [ ] 将读取替换为 `XRepo.customTools.list()`
- [ ] 将创建/更新替换为 `XRepo.customTools.save(...)`
- [ ] 将删除替换为 `XRepo.customTools.delete(name)`
- [ ] 将启用开关替换为 `XRepo.customTools.setEnabled(name, enabled)`

### T-12: 迁移 BuiltinToolSettingsManager 持久化
- [ ] 保留 `BuiltinToolSettingsManager` public API
- [ ] 将列表读取替换为 `XRepo.builtinTools.list()`
- [ ] 将启用开关替换为 `XRepo.builtinTools.setEnabled(name, enabled)`
- [ ] 保持 unknown builtin 的失败语义

### T-13: 迁移 CreateCustomToolBuiltin 写入路径
- [ ] 构造 repo `CustomTool`
- [ ] 调用 `XRepo.customTools.save(tool, overwrite)`
- [ ] 将 validation 转为 builtin result failure
- [ ] 保持成功 message 包含 tool name

### T-14: 扩展 Tool manager 相关测试
- [ ] 为 CustomTool manager 准备 fake store
- [ ] 覆盖 unsafe command 失败
- [ ] 覆盖 duplicate name 失败
- [ ] 覆盖 unknown builtin 失败
- [ ] 覆盖成功写入后 repo list 返回目标配置

### T-15: 迁移 LLMController 配置读取
- [ ] 在 `refresh(context)` 开始处调用 `XRepo.init(context)`
- [ ] 读取 LLM、custom tools、MCP servers、builtin settings
- [ ] 调用 `ToolManager.resolve(customTools, mcpServers, builtinSettings)`
- [ ] 用 `XRepo.mcp.fingerprint()` 替代 raw JSON 指纹
- [ ] MCP refresh 失败后调用 `clearCacheByServerNames`

### T-16: 迁移 ConfigureViewModel 工厂依赖
- [ ] 定位 `ConfigureViewModel` 的 load/save 注入点
- [ ] 读取改为 `XRepo.llm()`
- [ ] 保存 access 字段改为 `XRepo.saveLlmAccess(...)`
- [ ] 保留 prompt/proxy/memory 不被覆盖

### T-17: 迁移 McpSettingsViewModel 到 XRepo.mcp
- [ ] 移除 ViewModel 构造参数中的 raw settings load/save
- [ ] load 改为 `XRepo.mcp.list()`
- [ ] save 改为 `XRepo.mcp.save(McpServer(...))`
- [ ] delete 改为 `XRepo.mcp.delete(name)`
- [ ] toggle 改为 `XRepo.mcp.setEnabled(name, enabled)`

### T-18: 迁移 CustomToolsSettingsContent 读写入口
- [ ] 定位页面内 `CustomToolManager` 调用
- [ ] 列表读取改为 `XRepo.customTools.list()`
- [ ] 启用开关改为 `XRepo.customTools.setEnabled(name, enabled)`
- [ ] 删除改为 `XRepo.customTools.delete(name)`
- [ ] 错误提示使用 validation message

### T-19: 迁移 Context 初始化入口
- [ ] 在 `MainActivity.onCreate` 调用 `XRepo.init(applicationContext)`
- [ ] 检查 `App.onCreate` 是否需要只加 init 而不动 seed 逻辑
- [ ] 检查 `Entrance` 已拿到 Context 后是否需要加 init
- [ ] 确认未 init 的 Hook 路径仍能 fallback 到 `ContextProvider.await()`

### T-20: 更新 UI ViewModel 测试
- [ ] 修改 `McpSettingsViewModelTest` 使用 fake store 初始化 `XRepo`
- [ ] load 测试断言 UI state
- [ ] save 测试断言 `XRepo.mcp.list()` 强类型结果
- [ ] duplicate 测试断言列表未新增

## 5. 审查修正记录

### Round 1: PM（完整性与价值）
- Feature 覆盖了需求中的所有关键名词：`XRepo`、单元测试、MCP headers/cache、CustomTool、BuiltinTool、LLM runtime、UI ViewModel。
- `App.kt` 调试 seed 被明确排除在第一批正式迁移外，避免计划误伤调试白名单。
- 每个 Feature 都映射到 Batch，且每个 Batch 有独立回归目标。

### Round 2: 架构师（结构与解耦）
- 任务遵守一个 Task 对应一个目标文件的原则，测试文件也独立拆分。
- `XRepo` 本体扩展受控，新增配置域通过子域 API 与 codec 扩展。
- Batch 没有跨依赖层级聚合：F-04 依赖 F-02/F-03，所以单独放在 B-04。

### Round 3: 结对伙伴（可执行性与细节）
- 每个 Task 均包含目标文件、依赖视野、伪代码签名或具体方法名。
- 验收标准都能通过代码检查或单元测试验证，不依赖主观判断。
- B-02 与 B-03 文件集合基本不重叠，可以在 Phase 3 中并行派发；B-04 需要等待两者完成。
