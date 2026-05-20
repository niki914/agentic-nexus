# PRD: Navigation State Machine

## Background

当前 app 没有统一的导航模型。`HomeDemo` 用裸 `Int` 做页面切换，`SettingsScreen` 自己引了 `NavController` 但路由未挂载。整个 app 的页面全集（startup, provider chooser, configure, done, homepage, settings tree）都还没有实现，需要一个能同时覆盖**线性条件流**和**树状钻取流**的导航状态机，避免上线前推倒重来。

## Navigation Patterns

### Pattern A: Startup Flow（线性、条件性、一次性）

```
[Startup] → [ProviderChooser] → [Configure] → [Done] → [HomePage]
```

- 页面顺序固定，只有前向移动
- 每一步都可能被跳过（provider 已选、已配置等）
- 一旦到达 `HomePage`，这些页面永久不可返回
- 返回键在整个 startup 阶段被禁用或由状态机消费

### Pattern B: Settings Tree（树状、栈式、可重入）

```
HomePage → SettingsRoot → Category("通用") → SubPage("关于")
                        → Category("外观") → SubPage("主题")
                        → Category("隐私")
```

- 不限深度，用户自由钻取
- 每层都可通过左按钮或系统返回键回退
- 不同分支之间互不干扰
- 可由 HomePage 的菜单随时进入，退出后回到 HomePage

## Design

### 核心抽象: `Page` 接口

每个页面自描述其导航能力，状态机只做栈操作，不关心页面内容。

```kotlin
interface Page {
    val title: String            // 标题栏文案
    val showLeftButton: Boolean  // 是否展示左按钮（通常对应 canGoBack）
    val showRightButton: Boolean // 是否展示右按钮（线性流中为"下一步"）
    val nextPage: Page?          // 线性流的下一页；树状页返回 null
    val interceptBack: Boolean   // true = 系统返回键由状态机 pop() 消费
}
```

### 核心组件: `NavigationController`

```kotlin
@Stable
class NavigationController(initialPage: Page) {
    val stack: List<Page>           // 只读，供观察
    val current: Page               // stack.last()
    val canGoBack: Boolean          // stack.size > 1
    val canGoForward: Boolean       // current.nextPage != null

    fun push(page: Page)            // 入栈（树状钻取）
    fun pop(): Boolean              // 出栈，返回 false 表示已到底
    fun replace(page: Page)         // 替换栈顶（原地更新）
    fun pushAll(pages: List<Page>)  // 批量入栈（startup 预计算）
}
```

### State Machine Semantics

| 操作 | 方向动画 | 使用场景 |
|------|---------|---------|
| `push()` | `Forward` | 进入子页面、startup 下一步 |
| `pop()` | `Back` | 返回上一页、系统返回键 |
| `replace()` | `None` | 同层内容替换（不常用） |

### Startup Flow: 预计算 + 批量压栈

```kotlin
fun computeStartupPages(context: AppContext): List<Page> {
    return buildList {
        add(StartupPage)
        if (!hasProvider()) add(ProviderChooserPage)
        if (!isConfigured()) add(ConfigurePage)
        add(DonePage)
        add(HomePage)
    }
}
```

冷启时一次性 `pushAll(computeStartupPages())`，用户每步 `pop()` 当前页即自然进入下一张。栈底始终是 `HomePage`，startup 页面走完后不可回退。

### Settings Tree: 动态 push/pop

```kotlin
// 用户点击 "通用"
controller.push(SettingsCategoryPage(path = listOf("通用"), children = [...]))

// 用户点击 "关于"
controller.push(SettingsCategoryPage(path = listOf("通用", "关于"), children = [...]))
```

`SettingsCategoryPage` 自己持有子节点数据，渲染子项列表。用户点子项时调用 `controller.push(child.toPage())`。返回键调用 `controller.pop()`。

### System Back Key

`LiquidScreen` 层挂载 `BackHandler`：

```kotlin
BackHandler(enabled = controller.current.interceptBack) {
    controller.pop()
}
```

- Startup 页: `interceptBack = true`，消费返回键，阻止退出 setup
- HomePage: `interceptBack = false`，系统返回键退出 app
- 设置页: `interceptBack = true`，返回键退回上一级设置

## Integration with LiquidScreen

```
NavigationController (栈状态 + 页面决策)
        │
        │  snapshotFlow / LaunchedEffect 监听 controller.current
        ▼
LiquidScreenState.navigateForward / navigateBack / update (标题动画)
        │
        ▼
LiquidScreen + LiquidScreenSwipeContent<Page> (渲染 chrome + 内容转场)
```

`LiquidScreenState` 不感知 `NavigationController`，两者的桥接在调用方（如 `MainActivity` 或顶层 `AppShell` composable）完成。

`LiquidScreenSwipeContent` 的泛型参数从 `Int` 改为 `Page`，`when (currentPage)` 分支用 sealed class 获得 exhaustiveness 检查。

## Page Catalog（未穷举）

```
StartupPage           — 启动画面
ProviderChooserPage   — 选择 LLM provider（条件出现）
ConfigurePage         — 基础配置（条件出现）
DonePage              — 完成页
HomePage              — 主页，导航的"根"
SettingsRootPage      — 设置入口页
SettingsCategoryPage  — 设置子分类，path 描述树中的位置
```

## Files to Touch

| File | Change |
|------|--------|
| **new** `app/.../ui/infra/Page.kt` | `Page` 接口 |
| **new** `app/.../ui/infra/NavigationController.kt` | 栈状态机实现 |
| **modify** `HomeDemo.kt` | 替换 `Int` page，接入 `NavigationController` |
| **modify** `LiquidScreen.kt` | 挂载 `BackHandler` |
| **modify** `LiquidScreenSwipeContent.kt` | 泛型约束改为 `Page`（可选） |
| **no change** `LiquidScreenState.kt` | 已足够通用，不改 |

## Non-Goals

- 不引入 Jetpack Navigation / NavHost
- 不做 URL 路由
- 不做 deep link
- 不做转场动画自定义（沿用 `TitleDirection` 的 Forward/Back 语义）
