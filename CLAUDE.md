# AX Contract

<project>
Nexus 是一个 Android Xposed 模块。它在受支持宿主进程中注入自定义 LLM 响应，并按宿主类型路由到对应 Hook 实现，覆盖或拦截原生回答链路。当前支持 `ColorOS / Breeno` 与 `HyperOS / XiaoAi` 两条宿主分支。
</project>

<modules>

| 目录 | 类型 | 职责 | 关键入口 |
|------|------|------|----------|
| `app/` | application | Xposed 入口、宿主路由、Compose 壳层 UI、业务 Hook 实现 | `Entrance.kt`, `MainActivity.kt`, `AbstractAssistantHook.kt` |
| `h/` | library | Xposed 框架层、Runtime 编排、反射调用、上下文捕获、日志与容错 | `IXposed.kt`, `Runtime.kt`, `Hook.kt`, `ContextHook.kt` |
| `composebase/` | library | Compose 主题与 ViewModel 基础设施 | `BaseTheme.kt`, `ComposeMVIViewModel.kt` |
| `ipc/` | library | 宿主进程与主 App 的跨进程配置/通知桥接 | `XIpcBridge.kt`, `SettingsContentProvider.kt`, `XRes.kt` |
| `server/` | Python | 本地静态配置服务，按包名与版本号提供 `config.json`，并支持最近版本回退 | `server.py` |

所有模块的构建入口是根目录 `build.gradle.kts` 与各子模块 `build.gradle.kts`。

</modules>

<host_matrix>

| HostApp | 包名 | 业务实现 | 配置目录 |
|---------|------|----------|----------|
| `Breeno` | `com.heytap.speechassist` | `BreenoChatHook` | `server/com.heytap.speechassist/<versionCode>/config.json` |
| `XiaoAi` | `com.miui.voiceassist` | `XiaoaiChatHook` | `server/com.miui.voiceassist/<versionCode>/config.json` |

</host_matrix>

<boot_sequence>

1. `Entrance.getTarget()` 通过 `XValues.appList` 过滤受支持宿主包名，而不是写死单个宿主。
2. `Entrance.onLoad()` 先调用 `HookSideLoader.load(scope, ContextHook(), params)`，尽早拦截 `Application.onCreate()` 捕获宿主 `Context`。
3. 后台协程等待 `ContextProvider.await()` 返回宿主 `Context`，然后执行 `HookLocalSettings.update(ctx)` 与 `XService.getWebSettings(ctx)`。
4. `HostApp.fromPackageName(params.packageName)` 决定当前宿主类型；只有 `targetPkg == params.packageName` 时才继续安装业务 Hook。
5. `Breeno` 路由到 `BreenoChatHook`，`XiaoAi` 路由到 `XiaoaiChatHook`。
6. `RuntimeBootstrap.installIfNeeded()` 为当前进程创建单例 `Runtime`；当前工程的业务 Hook 都走 `hook.onHook(params)` 同步安装。

</boot_sequence>

<turn_state>

`ConversationTurnState(roomId, turnId, lastQuery, mode)` 是当前通用会话状态模型。

- `mode` 只有两种：`InjectedLLM` 与 `NativeTakeover`
- 入口在 `AbstractAssistantHook.handleCapturedQuery(roomId, query)`
- `shouldTakeOver(query)` 为 `true` 时，当前轮次标记为 `NativeTakeover`，调用 `onTakeoverTriggered()` 后直接返回
- 否则进入 `dispatchQueryToLLM()`，由 `LLMController` 发起流式请求
- `onSessionReset(roomId)` 会清理上一房间的 `ConversationJournal` 并重置 `turnState`
- `turnId` 由 `TurnIdGenerator` 生成，使用 `AtomicLong + maxOf(previous + 1, System.currentTimeMillis())` 保证单调递增

</turn_state>

<hook_topology>

`AbstractAssistantHook.onHook()` 固定执行顺序：

1. `onBeforeInstallHooks()`
2. `installSessionHooks()`
3. `installResponseHooks()`
4. `installInputHooks()`

`BreenoChatHook` 子 Hook：

| Hook | 触发点 | 作用 |
|------|--------|------|
| `RoomIdManagerHook` | `RoomIdManager#createRoom` after | 新会话创建时重置当前 session |
| `NativeCardPolicyHook` | `DataCenter#insertMessage` before | 在 `InjectedLLM` 模式下拦截原生回答卡片 |
| `OperationFactoryHook` | `OperationFactory#create` after | 将清理操作替换为 `DoNothingOperation`，避免系统移除注入卡片 |
| `InputHook` | `DataCenter#insertMessage` before | 捕获用户 query，并缓存 `DataCenter` 实例 |

`XiaoaiChatHook` 子 Hook：

| Hook | 触发点 | 作用 |
|------|--------|------|
| `ResetSessionHook` | 宿主会话重置点 | 清空当前 dialog 状态与渲染 session |
| `CaptureResponseTargetHook` | 响应目标创建链路 | 捕获后续文字流注入目标 |
| `BlockNativeTextStreamHook` | 原生文字流链路 | 屏蔽与注入轮次冲突的原生文本 |
| `BlockNativeTtsStreamHook` | 原生 TTS 指令链路 | 屏蔽与注入轮次冲突的原生 TTS 流 |
| `BlockNativeTtsPlaybackHook` | 原生 TTS 播放链路 | 屏蔽与注入轮次冲突的原生播报 |
| `RenderTextStreamCardHook` | 注入文字流链路 | 将 LLM 增量转换为宿主可消费的 `Instruction` |
| `CaptureInputHook` | 用户输入链路 | 捕获用户 query |

</hook_topology>

<render_pipeline>

`BreenoChatHook.renderStreamCard()`：

1. 校验 `turnId` 与 `mode` 是否仍匹配当前活跃轮次。
2. 通过 `BreenoConfigProvider` 读取 `viewBeanClass`、`DataCenter` 方法名、mock 方法集与 localData。
3. 首帧创建 `viewBean` 并注入 `chatType`、`roomId`、`recordId`、mock 数据和反馈按钮 localData。
4. 每片更新 `content`、`isFinal`、`isFirstSlice`。
5. 首帧调用 `dataCenter.insertMessage(bean)`，后续调用 `dataCenter.updateMessage(bean, false)`。
6. 终帧反转 `hideFeedbackView` 并再次 `updateMessage()`，最后清空 `currentRenderSession`。

`XiaoaiChatHook.renderStreamCard()`：

1. 先校验当前轮次是否仍为活跃 `InjectedLLM` 轮次。
2. `RenderTextStreamCardHook` 维护单个 `XiaoaiRenderSession`，根据累计文本计算本次增量 `delta`。
3. 从 `ResponseTargetStore` 取出已捕获的响应目标。
4. 将增量包装为宿主 `Instruction`，其 `namespace`、`name`、`idPrefix`、终帧补片文本都来自 `XiaoaiConfigProvider`。
5. 终帧额外注入 `renderTextStreamCardFinalChunkText`，然后清空当前渲染 session。

</render_pipeline>

<llm_pipeline>

- `LLMController` 使用 `com.github.niki914:S3ss10n:2.0.0`
- 每次发送请求前，通过 `HookLocalSettings.refreshFromHookContext()` 获取最新本地配置
- `endpoint`、`apiKey`、`model`、`prompt` 均来自 `LocalSettings`
- `SessionEvent.RoundStarted` 映射为首帧
- `SessionEvent.TextDelta` 追加到 `StringBuilder` 后回调中间帧
- `SessionEvent.RoundCompleted` 回调终帧
- `SessionEvent.Error` 会把错误文本追加进当前输出
- `SessionEvent.ToolRunning / ToolSucceeded / ToolFailed` 当前被忽略

</llm_pipeline>

<config_resolution>

配置读取优先级是：远程配置刷新后的持久化结果 > 已缓存的持久化结果 > 空 JSON `{}`。

主 App 刷新链路：

1. `MainActivity.onResume()` 根据 `RootUtils.getOsFamily()` 与已安装包，选择优先宿主包名。
2. `XService.refreshWebSettings(context, packageName, versionCode)` 发起 HTTP GET：`http://127.0.0.1:8788/<packageName>/<versionCode>/config.json`
3. 远程 JSON 写入 `XIpcBridge.writeWebSettingsJson()`
4. `XIpcBridge` 根据当前 `Context` 决定是直接本地落盘，还是走 `SettingsContentProvider`
5. `XIpcStoreRepository` 最终调用 `ConfigPersistence` 将 JSON 落到本地文件

宿主进程读取链路：

1. `XService.getWebSettings(context)` / `getLocalSettings(context)` 通过 `XIpcBridge.read*Json()` 读取 JSON
2. `parseJsonObject()` 将 JSON 解析为 `JsonObject`
3. `WebSettings` 暴露 `config`
4. `LocalSettings` 暴露 `endpoint`、`apiKey`、`model`、`prompt`、`proxy`、`takeoverKeywords`
5. `BaseConfigProvider.getElement(path)` 用点号路径读取云配置

服务端行为：

- `server/server.py` 以包名目录作为一层路径，而不是 alias
- 请求路径匹配 `/<packageName>/<versionCode>/config.json`
- 若对应版本目录不存在，则回退到同包名下“距离最近”的版本目录

</config_resolution>

<runtime_constraints>

- `RuntimeBootstrap` 通过双重检查锁保证每个进程只安装一次 `Runtime`
- `BreenoChatHook` 使用 `Mutex + currentRenderSession` 管理单个流式渲染会话
- `ConversationJournal` 使用 `Mutex` 串行化读写
- `HookLocalSettings` 在内存中缓存最近一次读取到的本地配置
- `XValues.getAppTypeOf(context)` 只接受三类来源：主 App、受支持宿主、未知包

</runtime_constraints>

<navigation>

- **Xposed 入口**: `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`
- **通用 Hook 模板**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/AbstractAssistantHook.kt`
- **Breeno 实现**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/oppo/`
- **XiaoAi 实现**: `app/src/main/java/com/niki914/nexus/agentic/mod/feat/hyper/`
- **LLM 调度**: `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`
- **本地/远程配置门面**: `app/src/main/java/com/niki914/nexus/agentic/mod/XService.kt`
- **IPC 桥接**: `ipc/src/main/java/com/niki914/nexus/ipc/XIpcBridge.kt`
- **宿主枚举与 IPC 常量**: `ipc/src/main/java/com/niki914/nexus/ipc/XRes.kt`
- **本地配置服务器**: `server/server.py`
- **Breeno 配置样例**: `server/com.heytap.speechassist/120704/config.json`
- **XiaoAi 配置样例**: `server/com.miui.voiceassist/507013003/config.json`
- **XiaoAi 专项说明**: `XIAOAI.md`
- **Gradle 自动任务**: `app/build.gradle.kts` 中的 `adbReverse`；`startServer` 已定义但默认未挂到 install/assemble

</navigation>

<todo_backlog>

- [ ] `RenderTextStreamCardHook`: 非常不符合预期的一个实现，使用了监视器锁，而且硬编码类名而没有上云，需要整改
- [ ] `MainActivity`: Use liquid glass~
- [ ] `BaseConfigProvider`: Dataclass to describe a hook spot
- [ ] `BaseConfigProvider`: 修改下游的读参方式，最好 suspend
- [ ] `AbstractAssistantHook`: 统一所有业务下的 subhooks、云 config、AbstractAssistantHook 方法命名
- [ ] `BreenoConfigProvider`: 修改读参方式，最好 suspend
- [ ] `BreenoChatHook`: Muti-Languages
- [ ] `LLMController`: 配置检查与快速失败
- [ ] `LLMController`: 字数测速

</todo_backlog>

## Hydra Orchestration Toolkit

Hydra is a Lead-driven orchestration toolkit. You (the Lead) make strategic
decisions at decision points; Hydra handles operational management.
`result.json` is the only completion evidence.

Why this design (vs. other coding-agent products):
- **SWF decider pattern, specialized for LLM deciders.** Hydra is the AWS SWF / Cadence / Temporal decider pattern. `hydra watch` is `PollForDecisionTask`; the Lead is the decider; `lead_terminal_id` enforces single-decider semantics.
- **Parallel-first, not bolted on.** `dispatch` + worktree + `merge` are first-class. Lead sequences nodes manually and passes context explicitly via `--context-ref`. Other products treat parallelism as open research; Hydra makes it the default.
- **Typed result contract.** Workers publish a schema-validated `result.json` (`outcome: completed | stuck | error`, optional `stuck_reason: needs_clarification | needs_credentials | needs_context | blocked_technical`). Other products return free-text final messages and require downstream parsing.
- **Lead intervention points.** `hydra reset --feedback` lets the Lead actually intervene at decision points instead of being block-and-join. A stale or wrong run is one `reset` away.

Core rules:
- Root cause first. Fix the implementation problem before changing tests.
- Do not hack tests, fixtures, or mocks to force a green result.
- Do not add silent fallbacks or swallowed errors.
- An assignment run is only complete when `result.json` exists and passes schema validation.

Workflow patterns:
1. Do the task directly when it is simple, local, or clearly faster without workflow overhead.
2. Use Hydra for ambiguous, risky, parallel, or multi-step work:
   ```
   hydra init --intent "<task>" --repo .
   hydra dispatch --workbench W --dispatch <id> --role <role> --intent "<desc>" --repo .
   hydra watch --workbench W --repo .
   # → DecisionPoint returned, decide next step
   hydra complete --workbench W --repo .
   ```
3. Use a direct isolated worker when only a separate worker is needed:
   `hydra spawn --task "<specific task>" --repo . [--worktree .]`

Agent launch rule:
- When dispatching Claude/Codex through TermCanvas CLI, start a fresh agent terminal with `termcanvas terminal create --prompt "..."`
- Do not use `termcanvas terminal input` for task dispatch; it is not a supported automation path

TermCanvas Computer Use:
- TermCanvas may dynamically inject a Computer Use MCP server into Claude/Codex terminals; it does not have to appear in static MCP settings files.
- For local macOS desktop apps or system UI, check for TermCanvas Computer Use before assuming only shell, browser, or Playwright tools are available.
- If available, call `status` first, then `setup` if permissions or helper health are missing, then `get_instructions` for the current operating protocol.
- Do not manually start `computer-use-helper`, write its state file, launch the MCP server, or hand-write JSON-RPC unless explicitly debugging Computer Use itself.

Workflow control:
- After dispatching, always call `hydra watch`. It returns at decision points.
1. Watch until decision point: `hydra watch --workbench <workbenchId> --repo .`
2. Inspect structured state: `hydra status --workbench <workbenchId> --repo .`
3. Reset a dispatch for rework: `hydra reset --workbench W --dispatch N --feedback "..." --repo .`
4. Approve a dispatch's output: `hydra approve --workbench W --dispatch N --repo .`
5. Merge parallel branches: `hydra merge --workbench W --dispatches A,B --repo .`
6. View event log: `hydra ledger --workbench <workbenchId> --repo .`
7. Clean up: `hydra cleanup --workbench <workbenchId> --repo .`

Telemetry polling:
1. Treat `hydra watch` as the main polling loop; do not infer progress from terminal prose alone.
2. Before deciding wait / retry / takeover, query:
   - `termcanvas telemetry get --workbench <workbenchId> --repo .`
   - `termcanvas telemetry get --terminal <terminalId>`
   - `termcanvas telemetry events --terminal <terminalId> --limit 20`
3. Trust `derived_status` and `task_status` as the primary decision signals.

`result.json` must contain (slim, schema_version `hydra/result/v0.1`):
- `schema_version`, `workbench_id`, `assignment_id`, `run_id` (passthrough IDs)
- `outcome` (completed/stuck/error — Hydra routes on this)
- `report_file` (path to a `report.md` written alongside `result.json`)

All human-readable content (summary, outputs, evidence, reflection) lives in
`report.md`. Hydra rejects any extra fields in `result.json`. Write `report.md`
first, then publish `result.json` atomically as the final artifact of the run.

When NOT to use: simple fixes, high-certainty tasks, or work that is faster to do directly in the current agent.

## TermCanvas Pin System

TermCanvas has a first-class pin store. Pins are persistent records of work
the user wants done — captured when the user expresses intent, not when the
work happens. Use the `termcanvas pin` CLI to read and write them. Any agent
terminal can record, read, and update pins.

When to record a pin:
- User says "记一下", "回头处理", "帮我留意", "later", "todo this", or any phrasing that defers the work.
- User describes a problem or idea but isn't asking you to fix it right now.
- User pastes a GitHub issue URL and asks you to track it (record the URL via `--link`).

Do NOT silently nod — capture the pin with `termcanvas pin add` so it survives the session.

When existing pin content is pasted or dropped into the conversation:
- Treat it as context from an existing TermCanvas pin, not as a request to create another pin.
- Use the user's surrounding instruction to decide whether to execute, investigate, or discuss it.
- If the intent is unclear, ask what to do next instead of assuming the pin should be solved immediately.

Recording a pin:
```
termcanvas pin add --title "<short imperative>" --body "<detail>" [--link <url>]
```
- `--title`: short, scannable. Rephrase the user's words into imperative mood. Use the same language the user used (e.g. Chinese input → Chinese title, English input → English title).
- `--body`: preserve enough context for a future agent or the user to resume without re-asking basic questions. Use the same language the user used.
- Keep the body neutral and task-centered. Do not write "the user said/用户说..." as the main framing; describe the request, symptom, evidence, and next step directly. Quote the user's exact words only under Evidence / References when the wording itself matters.
- For bugs, feature requests, research threads, design feedback, or follow-up engineering work, use a compact template. Include only sections that have real content; do not fill sections with guesses:
  `Background`: what prompted this and where it came from.
  `Observed / Request`: the concrete symptom, ask, or idea.
  `Expected / Goal`: what should be true when this is handled.
  `Evidence / References`: user quote, screenshot, link, file path, command output, or code location if available.
  `Next action`: the first useful concrete step when someone picks it up.
  `Unknowns`: missing decisions or facts that still need confirmation.
- If the information is thin, choose deliberately:
  If local context can answer it cheaply, inspect the relevant code, state, logs, or files before recording and include what you found.
  If the missing information changes scope, product behavior, security, or architecture, ask the user one concise question before recording.
  If the user is clearly deferring and cannot answer now, record the pin anyway but mark assumptions and unknowns explicitly.
- If it is only a personal memo or reminder, a short body is acceptable, but still include why it matters or when to revisit it if that is known.
- For multi-line bodies, pass real newlines. In shell commands, use ANSI-C quoting such as
  `--body $'line 1\nline 2'`; do not put literal `\n` sequences inside ordinary quotes.
- `--link <url>`: attach an external reference (GitHub issue, doc, etc.). Use `--link-type github_issue` for issue URLs.
- Repo defaults to cwd. Pass `--repo <path>` only if you need a different one.

Reading and updating pins:
- `termcanvas pin list` — list pins for the current repo (filter `--status done` etc.)
- `termcanvas pin show <id>` — read a single pin before acting on it
- `termcanvas pin render <id> --json` — render a pin's Markdown/HTML body to a PNG and print the output path
- `termcanvas pin update <id> --status done` — mark complete after finishing the work
- `termcanvas pin update <id> --body "..."` — refine the description as you learn more

Visual / HTML pins:
- If a pin contains a full HTML document, SVG diagrams, charts, mockups, visual layouts, or other content where visual understanding matters, run `termcanvas pin render <id> --json` after `pin show`.
- Inspect the returned `image_path` with whatever local image-viewing capability is available in the current agent environment. Use both the rendered screenshot and the pin source when reasoning about the artifact.
- By default, render output is written to `<repo>/.termcanvas/pin-renders/<pin-id>/latest.png` and overwritten on the next render, so it should not accumulate. Do not commit rendered pin images unless the user explicitly asks.
- Pass `--out <png>` only when the user asks for a specific export path.

Rules:
- Pins belong to the user. Don't invent pins the user didn't ask for.
- One pin per intent. Three deferred items = three `pin add` calls.
- After completing work that originated from a pin, call `pin update <id> --status done`.
- The pin store is local to TermCanvas. It does NOT auto-sync to GitHub. If the user wants something on GitHub, they will say so explicitly.
- Status values: `open` (default), `done`, `dropped`. Pick `dropped` (not delete) when a pin is abandoned, so the history is preserved.
