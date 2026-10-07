# Text Actions（文本选择菜单动作）— WIP 参考实现

> **状态：WIP / 不可合入。** 本 PR 只做代码暂存，供后续「多 Agent 架构」需求完成后参考重写。
> 代码届时会过时，请只当作参考实现看待。

---

## 1. 需求：想做什么

在其他 App 里长按选中文本时，系统选择菜单（和复制 / 粘贴 / 分享同一排）里出现一个 Zafiro 入口。点击后弹出动作选择单，用户选一条预设提示词（如 Explain / Translate / Research），Zafiro 用「选中的文本 + 该提示词」发起一轮新对话。

要点：

- 提示词由用户自定义，可增删改；内置 6 条默认动作（Explain / Translate / Research / Summarize / Rewrite / Reply），英文硬编码、可编辑。
- 每条动作的配置项（原始需求）：自定义提示词 / 附带选中的文本 / 是否附带截图 / 是否后台运行。
- 提示词对用户可见，不走 `<zfr-*>` 隐藏块。构造形如：

  ```
  > [NOTE]: The text below was copied from another app.
  >
  > <选中的文本，逐行加 "> " 前缀>

  <动作提示词>
  ```

- 支持后台静默执行（不打开主 App）；但没有保活载体（悬浮球 / 常驻通知）时回落到打开主 App。
- 永远直接发送，不做「待发送」中间态。
- 无图标（系统选择菜单不支持自定义图标）。

设置入口：设置 → 能力（Ability）段 → MCP 之后，新增一页 Text Actions。

## 2. 参考项目：SwiftSlate

<https://github.com/Musheer360/SwiftSlate>（MIT，Kotlin，Android accessibility + AI text transformation）

本实现参考了它的 PROCESS_TEXT 入口，抄了三点：

1. **入口是一次性、dialog 主题、`excludeFromRecents` 的 Activity**，不驻留、不进最近任务，选完 finish。
2. **manifest 包可见性 workaround**：同一个 activity 上再挂一条 `VIEW` + `BROWSABLE` + `https`、**故意不带 `DEFAULT`** 的 intent-filter。Android 11+ 下，某些编辑器（其注释举了 Gmail）自己 query PROCESS_TEXT handlers 却没声明匹配的 `<queries>`，本包对它不可见，就会静默缺席它的选择菜单；匹配它已声明的 https 查询即可恢复可见性，且不带 DEFAULT 保证不会变成 https 链接处理器。（可见性是包级粒度。）
3. **`ProcessTextInput` 的脏数据处理**：null / BOM / 零宽与 format 字符 / 换行归一 / 用 trim 判空但发送原始未 trim 文本。对应本实现的 `TextSelectionInput`。

未采用它的：

- 它的自研 `SlateBottomSheet`（不用 M3 `ModalBottomSheet`，因为后者会消费子列表的 overscroll 辉光，且渲染在独立窗口）。本实现用现有 uikit `OptionSheet`（M3 `ModalBottomSheet` 封装），因为动作数量少、不需要滚动。
- 它「替换选中文本」的结果回传（`setResult(RESULT_OK, EXTRA_PROCESS_TEXT)`）。本实现只读，不回写。

## 3. 现在的代码

### 3.1 数据与存储

- `app/.../repo/TextActionsModels.kt`：`TextAction(id, name, promptTemplate, enabled)` + 6 条 `defaultTextActions`。
- `app/.../repo/TextActionsCodec.kt`：JSON 编解码（复用 `SettingsJsonCodecUtils`）。
- `store/.../StoreDescriptorRegistry.kt`：新增 `TEXT_ACTIONS_ID = "actions.text"`，落 `settings/actions/text_actions.json`。
- `app/.../repo/XRepo.kt`：`TextActionsApi`（list / get / listEnabled / replace / delete / setEnabled）+ `seedTextActions()`（按 id 补齐缺失默认项，启动时 seed）；另有全局 `textActionSilent`（app.state）。
- `app/.../repo/AppStateSettingsCodec.kt`：新增 `text_action_silent`。

### 3.2 设置 UI

- `ui/nav/ZafiroSettingsGroup.kt`：新增 `TextActions`。
- `ui/model/SettingsState.kt`：接入 Ability 段（MCP 之后）。
- `ui/content/SettingsDetailPageContent.kt`：新增分支。
- `ui/content/TextActionsSettingsContent.kt`：列表页（`SettingsSpecPageContent`，CardList 行 + 一个全局「后台运行」开关行，右上角 + 号）。
- `ui/nav/ZafiroPages.kt` + `ui/ZafiroPageContent.kt` + `ui/route/TextActionDetailRoute.kt`：新增 `TextActionDetailPage`。
- `ui/content/TextActionDetailContent.kt`：详情编辑页（名称 / 提示词 + 删除 + 未保存守卫），复用 `EditableSettingsDetailChrome` / `EditableSettingsDetailFormScaffold`。
- `ui/model/TextActionsState.kt`：MVI ViewModel（列表 + 表单 + save / delete + `settingsChanges` 热刷新）。
- 5 语言 strings。

### 3.3 触发入口

- `app/src/main/AndroidManifest.xml`：新增 `TextActionActivity`（PROCESS_TEXT filter + 包可见性 filter）。
- `res/values/themes.xml`：`Theme.AppDefault.TextAction`（透明、无 dim、无窗口动画）。
- `app/.../trigger/TextActionActivity.kt`：解析选中文本 → 读启用动作 → 弹 `OptionSheet` → 选中后拼提示词、新建会话、`agent.load` + `stream` → 静默 finish 或拉起 `MainActivity`。
- `app/.../trigger/TextActionPromptComposer.kt`：提示词拼接（纯函数）。
- `app/.../trigger/TextSelectionInput.kt`：选中文本解析（纯函数）。
- `app/.../app/App.kt`：启动 seed。
- 单测：`app/src/test/.../trigger/TextActionPromptTest.kt`（拼接 / 解析 / codec）。

### 3.4 数据流

选中文本 → `TextActionActivity` → `XRepo` 读动作 → `OptionSheet` → `Composer` 拼提示词 → `ConversationRepo.createConversation` 建档 → `agent.load(id)` → `updateDraft` → `agent.stream()` → 静默 or `startActivity(MainActivity)`。

## 4. Review 发现的问题

### 4.1 半透明失效（现象根因）

`TextActionActivity` 用 `setContent { BaseTheme { ... } }`，而 `BaseTheme` 的 `SideEffect` 无条件执行 `window.decorView.setBackgroundColor(colorScheme.background.toArgb())`（`ui-kit/.../BaseTheme.kt:69`），在 decorView 上刷了一层不透明主题底色，直接抵消了 manifest / theme 的 `windowIsTranslucent`。结果是用户看到一个新的纯色 Activity，而不是「透出下层原 App 的半透明层」。（`OptionSheet` 自带的 M3 scrim 只是变暗，不是遮死。）

### 4.2 配置项只实现了一个（需求缺口）

原需求每条动作应带：`附带选中文本` / `自定义提示词` / `附带截图` / `后台运行`。现状：

| 配置项 | 状态 |
|---|---|
| 自定义提示词 | 已实现 |
| 附带选中文本 | 无条件附带，没有开关 |
| 附带截图 | 完全没有（模型、UI、capture 都没有） |
| 后台运行 | 实现成了**全局**一个开关，不是每条一个 |

### 4.3 会话命名

`ConversationRepo.createConversation(id, prompt)` 用首条输入当标题，于是标题变成 `> [NOTE]...` 那一坨。期望标题 = 动作名（如 `Translate`）。

另外顶栏标题由 `HomeChatViewModel.applyConversation` 在 id 变化时从 turn 文本现推，不读 Room，所以即便改了 Room title，顶栏首帧仍是 prompt 推导值。

### 4.4 冷启动 / 会话恢复竞态

- 场景 A（冷启动）：触发回合刚起来，`HomeChatViewModel.restoreLastConversationOnStartup()` 会 `agent.load(lastOpened)`，而 `AgentImpl.load()` 第一件事是 `releaseRound()`，把刚发起的回合停掉。
- 场景 B（进程热、已有回合在跑）：`agent.load(newId)` 会静默打断用户当前正在看的回答。单 Agent 架构下同一时刻只能有一个回合，硬来就是丢用户内容。

### 4.5 重复度（bad taste）

`ui/model/TextActionsState.kt`（431 行）几乎是 `ExecutionRulesSettingsState.kt`（467 行）的逐行改写；`TextActionsSettingsContent` / `TextActionDetailContent` 同样与 ExecutionRules 同构（表单快照、未保存判定、乐观更新 + 回滚、`settingsChanges` 热刷新全部一样）。

### 4.6 死代码 / 预留面

`TextAction.enabled` + `setEnabled` + `ItemEnabledChanged` + `toggleItemEnabled` + `listEnabled` 全套存在，但没有任何 UI 能禁用一个动作（`save` 还无条件 `enabled = true`），实际恒为 true。`XRepo.textActionSilentSetting` flow 无消费方。

### 4.7 文案方向写反

`text_actions_field_prompt_description` 写「选中文本会附在提示词**后**」，而 composer 是 quote 在前、template 在后；同页 `text_actions_page_description` 写的是「附在提示词**前**」。自相矛盾。

## 5. 将要修改的点（如果继续做）

1. `BaseTheme` 增加 `paintWindowBackground: Boolean = true` 之类的开关，`TextActionActivity` 传 `false`，恢复真透明。
2. 配置项改成 per-action：`TextAction` 增加 `includeText` / `includeScreenshot` / `silent`；开关从列表页挪进详情页；触发入口读 `action.silent` 并保留「无保活载体则回落前台」的兜底。
3. 截图：选完动作、`OptionSheet` 收起后再截（趁 activity 仍透明，截到的仍是源 App，不会带 sheet）；无 root / Shizuku 时静默跳过。复用 `ScreenshotBuiltin` 的 `screencap` + `SharedImageCodec.ingestFile`。
4. 会话命名：`ConversationRepo.createConversation` 增加可选 title；顶栏在 id 变化时改读 Room 的 `summary.title`。
5. 竞态：
   - 场景 A：`restoreLastConversationOnStartup` 在引擎已挂会话（`agent.conversation.value.id != null`）或回合运行中时跳过恢复。
   - 场景 B：`TextActionActivity.dispatch()` 在 `agent.load()` 前判 `agent.status.value.isRunning`，是则 Toast 报错 + `TODO: 让用户选择是否打断` + finish（必须早于 `load()`，因为 `load()` 自己会先 `releaseRound()`）。
6. 砍掉 `enabled` 空壳与 `textActionSilentSetting` 死代码。
7. 收敛重复：先按第 6 点瘦身，再看是否抽公共的「表单快照 / 乐观更新回滚」小工具（不引入泛型基类；第三个同款 CRUD 出现再抽）。

不写任何向后兼容：本功能未上线，涉及的 JSON store 可直接改结构，不做迁移。

## 6. 为什么这是一个 WIP PR

这不是要合入的代码。需求承接会落在一个**多 Agent 架构**的新需求上：允许多个 Agent 共存，从根上解决「单 Agent 全局串行，导致文本选择触发与主会话互相打断」的问题。

因此：

- 本 PR 只做暂存，**不可合入**。
- 代码届时一定过时（尤其是入口如何与主会话共存、以及设置页如何组织）。
- 价值在于可作为参考实现，尤其是：manifest 包可见性 workaround、PROCESS_TEXT 脏数据解析、以及「透明 Activity + BottomSheet 选动作」的骨架。

## 7. 待决

- 多 Agent 架构落定后，文本选择入口是新建会话还是投递到某个指定 Agent。
- 截图是否保留（唯一带额外失败模式：依赖 root / Shizuku，且弹窗前后有时序节奏问题）。
