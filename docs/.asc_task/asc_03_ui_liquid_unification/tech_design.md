# ASC-03 UI Liquid 统一架构设计

## 1. 设计目标

ASC-03 的目标不是做一套新的 UI，而是在不放大回归面的前提下，把当前主线 UI 的 Liquid 基建收敛为一套可持续维护的结构。

本设计要同时解决五类问题：

- 统一按钮、输入区、顶栏按钮之间重复的 liquid 交互接线
- 把配置页从 `liquid_example` 依赖中解开
- 保持 `LiquidScreen` 顶栏实现稳定，避免再次踩到阴影 / chrome 回归
- 为后续 shape 统一预留稳定原语
- 在最终批次删除 `app/ui/liquid_example`

## 2. 设计边界

### 2.1 本次会做

- 新增共享的 liquid 交互内核与参数对象
- 收敛 `LiquidButton` / `ActionBarButton` / `LiquidTextField` / `LiquidSecretTextField`
- 将配置页的 toggle 迁入正式 infra 体系
- 统一 shape 原语的定义入口
- 最终删除 `app/ui/liquid_example`

### 2.2 本次不会做

- 不修改页面导航和页面结构
- 不把聊天页或启动页的专属组件抽成通用 infra
- 不改变 `LiquidScreen` 的公开调用方式
- 不引入“万能 Liquid 控件”
- 不为了封装而新增不必要的 `layerBackdrop` 级联

## 3. 架构总览

ASC-03 采用以下分层：

1. `shape` 原语层：定义字段、卡片、胶囊的统一 `Shape`
2. `interaction` 原语层：定义高光、拖拽、按压启停与形变映射
3. `component` 容器层：按钮、输入框、toggle、顶栏按钮等正式组件
4. `page` 使用层：配置页、首页、settings 页面只消费正式组件，不再拼装 demo 代码

这意味着：

- 统一的不是“控件本体语义”，而是“形状原语 + 交互原语 + 容器能力”
- 顶栏按钮、CTA 按钮、输入框依旧各自保留自己的语义层

## 4. 关键设计决策

### 4.1 `LiquidScreen` 顶栏 API 保持不变

当前 `LiquidScreen` 的实现状态是：

- 视觉形态和点击逻辑已经由 infra 内部固定
- 公开层面仍使用 `leftButton` / `rightButton` `Composable slot`
- slot 内容最终被包裹进内部 `ActionBarButton`

本轮决策：

- **不修改 `LiquidScreen` 的公开参数**
- **不引入 `ActionSpec` 改造**
- 只在内部实现上收敛顶栏按钮与共享交互基建的关系

原因：

- 用户明确担心这里再次引发阴影 / chrome 回归
- 现有 API 已经可用，收益不足以覆盖本轮风险

### 4.2 配置页 toggle 不再沿用 demo 版本

当前配置页引用的是 `liquid_example` 下的 `LiquidToggle`，用户已明确指出该实现有 bug，不能继续保留。

本轮决策：

- 以现有 `StyledSwitch` 为正式实现基础
- 对外收口为新的 `LiquidToggle`
- `StyledSwitch` 在迁移期可保留为内部兼容实现名，最终是否删除由 Batch 4 决定

原因：

- `StyledSwitch` 语义和稳定性更适合作为正式组件基础
- 配置页需要的是正式、可维护的 toggle，而不是 demo 视觉实现

### 4.3 Shape 统一采用“原语优先”

本轮决策：

- 先统一 `Shape` 原语，不以 `Modifier` 作为唯一入口
- 允许后续在少量场景补 `Modifier` 组合层，但 shape 定义以原语为准

原因：

- 当前同一个 shape 需要同时提供给 `drawBackdrop(shape = { ... })` 和 `.clip(shape)`
- 如果只做 `Modifier` 扩展，shape 仍会在 backdrop 侧重复传递

## 5. Shape 设计

### 5.1 新增统一 shape 原语文件

建议新增：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/LiquidShapes.kt`

建议提供：

```kotlin
fun liquidFieldShape(cornerRadius: Dp = 28.dp): Shape
fun liquidCardShape(cornerRadius: Dp = 28.dp): Shape
fun liquidBubbleShape(cornerRadius: Dp = 24.dp): Shape
fun liquidCapsuleShape(): Shape
```

并将当前 `SettingsGroupCard.kt` 中的 `G2RoundedCornerShape` 迁移到该文件，作为共享实现细节。

### 5.2 三类 shape 语义

#### 字段 / 卡片类

- 默认固定半径
- 优先使用 G2 连续曲率圆角
- 半径 clamp 到 `minDimension / 2`

适用对象：

- `LiquidTextField`
- `LiquidSecretTextField`
- `StyledTextField`
- `SettingsGroupCard`

#### 气泡 / 面板类

- 使用与字段同类的固定半径语义
- 允许 radius 更小，例如 `24.dp`

适用对象：

- 首页消息气泡

#### 胶囊类

- 使用 `ContinuousCapsule(G2Continuity())`
- 保持随尺寸自适应的胶囊形态

适用对象：

- `LiquidButton`
- 顶栏圆形按钮之外的 pill / 状态胶囊

## 6. 交互基建设计

### 6.1 新增交互样式对象

建议新增：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt`

建议结构：

```kotlin
data class LiquidInteractiveStyle(
    val basePressScalePx: Dp,
    val maxDragScalePx: Dp,
    val translationDamping: Float = 0.05f,
    val highlightEnabled: Boolean = true,
)
```

### 6.2 新增共享 layer 变换函数

建议新增：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt`

核心职责：

- 把 `InteractiveHighlight.pressProgress + offset` 映射到 `translationX/Y + scaleX/Y`
- 由按钮、输入框、顶栏按钮共享

建议接口：

```kotlin
fun GraphicsLayerScope.applyLiquidInteractiveTransform(
    style: LiquidInteractiveStyle,
    pressProgress: Float,
    offset: Offset,
    size: Size,
)
```

### 6.3 高光状态保持复用

保留现有：

- `InteractiveHighlight`
- `DragGestureInspector`

但接线方式统一到“容器层共享 helper”，避免组件各自手抄 `then(interactiveHighlight.modifier)...`

## 7. 组件设计

### 7.1 按钮体系

#### `LiquidButton`

保持职责：

- CTA / 胶囊按钮容器
- backdrop、tint、surfaceColor、clickable 由它负责

改造点：

- 不再内联写变换公式
- 改为消费共享 `LiquidInteractiveStyle` 与 `applyLiquidInteractiveTransform`

#### `TintLiquidButton` / `MaterialTintLiquidButton`

保持公开 API 尽量不变。

改造点：

- 仅做语义化包装
- disabled 颜色策略尽量统一

### 7.2 顶栏圆形按钮

#### `ActionBarButton`

当前作为 `LiquidScreen` 私有基建存在。

本轮设计：

- 保持 `LiquidScreen` 使用方式不变
- `ActionBarButton` 内部接入共享交互层
- 去掉重复的拖拽检测实现，复用 `DragGestureInspector`

可选命名收尾：

- Batch 4 评估是否正名为 `CircularLiquidIconButton`
- 但不要求本轮改变 `LiquidScreen` 公开 API

### 7.3 输入区体系

#### 新增内部容器层

建议新增：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt`

职责：

- backdrop
- shape
- focus 时关闭 liquid 拖拽 / 高光
- padding / clip / onDrawSurface
- enabled 颜色策略

#### `LiquidTextField`

保留公开 API，内部改为委托容器层。

#### `LiquidSecretTextField`

保留公开 API，内部同样委托容器层，只保留：

- `visualTransformation`
- trailing visibility action

#### `StyledTextField`

本轮不强行并入 Liquid 输入区交互体系。

当前定位：

- settings 表单输入
- 在 shape 收尾批次切换到共享 shape 原语

原因：

- 它当前没有 liquid 拖拽/高光
- 提前把它并进 Liquid 输入区会放大回归面

### 7.4 Toggle 体系

建议新增正式组件：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt`

设计策略：

- 以 `StyledSwitch` 的稳定实现为基础
- 重命名 / 重定位到正式 infra 组件
- 配置页和 settings 行后续都消费这条正式路径

本轮不再保留 demo `LiquidToggle` 为主线实现。

## 8. Backdrop 设计约束

### 8.1 顶栏区域

`LiquidScreen` 继续维护：

- `chromeBackdrop`
- 顶栏 blur 层
- 顶栏按钮使用同一个 chrome backdrop

不改动这条拓扑。

### 8.2 输入框与 CTA 按钮

保持现有相对安全的使用方式：

- `rememberLayerBackdrop()` 作为本地容器 backdrop
- `drawBackdrop(...)` 直接消费

### 8.3 Toggle 迁移约束

如果 `LiquidToggle` 基于 `StyledSwitch` 重命名迁移：

- 优先沿用 `StyledSwitch` 当前的 backdrop 路径
- 不引入 demo 版 `LiquidToggle` 那套额外 layer 组合

### 8.4 明确禁令

- 不新增“包装型 helper”偷偷在内部调用 `layerBackdrop`
- 任何新增 `layerBackdrop` 的地方，都必须有明确的 backdrop 拓扑说明

## 9. 批次设计

### 9.1 Batch 1：按钮体系

目标文件：

- `ui/infra/component/LiquidButton.kt`
- `ui/infra/component/TintLiquidButton.kt`
- `ui/infra/component/MaterialTintLiquidButton.kt`
- `ui/infra/ActionBarButton.kt`
- 新增交互共享文件

完成标准：

- 按钮与顶栏按钮共享交互内核
- `LiquidScreen` API 不变

### 9.2 Batch 2：输入区体系

目标文件：

- `ui/infra/component/LiquidTextField.kt`
- `ui/infra/component/LiquidSecretTextField.kt`
- 新增 `LiquidTextFieldContainer.kt`
- `ui/nexus/content/ConfigurePageContent.kt`
- `ui/nexus/content/HomeChatComponents.kt`

完成标准：

- 首页 composer 与配置页输入容器走同一条正式实现路径
- IME / focus 行为不回退

### 9.3 Batch 3：Toggle 与 demo 断引用

目标文件：

- `ui/infra/component/StyledSwitch.kt`
- 新增或重定位 `ui/infra/component/LiquidToggle.kt`
- `ui/nexus/content/ConfigurePageContent.kt`
- 其余仍依赖 demo 的主线 UI 文件

完成标准：

- 主线 UI 不再引用 `liquid_example` 中的 toggle
- 配置页 toggle 替换为正式 infra 组件

### 9.4 Batch 4：Shape 与删除收尾

目标文件：

- 新增 `ui/infra/shape/LiquidShapes.kt`
- `SettingsGroupCard.kt`
- `LiquidTextField.kt`
- `LiquidSecretTextField.kt`
- `StyledTextField.kt`
- 相关 bubble / capsule 使用点
- 删除 `app/ui/liquid_example`

完成标准：

- shape 原语统一
- demo 包删除
- 主线代码无残留引用

## 10. 文件变更建议

### 新增文件

- `ui/infra/interaction/LiquidInteractiveStyle.kt`
- `ui/infra/interaction/LiquidInteractiveLayer.kt`
- `ui/infra/component/LiquidTextFieldContainer.kt`
- `ui/infra/component/LiquidToggle.kt`
- `ui/infra/shape/LiquidShapes.kt`

### 重点修改文件

- `ui/infra/component/LiquidButton.kt`
- `ui/infra/component/TintLiquidButton.kt`
- `ui/infra/component/MaterialTintLiquidButton.kt`
- `ui/infra/component/LiquidTextField.kt`
- `ui/infra/component/LiquidSecretTextField.kt`
- `ui/infra/component/StyledTextField.kt`
- `ui/infra/component/StyledSwitch.kt`
- `ui/infra/ActionBarButton.kt`
- `ui/infra/LiquidScreen.kt`
- `ui/nexus/content/ConfigurePageContent.kt`
- `ui/nexus/content/HomeChatComponents.kt`
- `ui/infra/component/SettingsGroupCard.kt`

## 11. 风险控制

### 风险 1：顶栏重构导致 chrome 回归

控制方式：

- 保持 `LiquidScreen` API 不变
- 顶栏 backdrop 拓扑不变
- 只抽内部交互接线

### 风险 2：toggle 迁移把 settings 路线也带崩

控制方式：

- 用 `StyledSwitch` 为基础，而不是继续修 demo `LiquidToggle`
- 迁移时先保兼容命名，再做最终收口

### 风险 3：shape 原语提前介入导致多批返工

控制方式：

- shape 收尾放到 Batch 4

### 风险 4：删除 `liquid_example` 时仍有残留引用

控制方式：

- Batch 3 先完成断引用
- Batch 4 再删目录

## 12. 校验说明

当前仓库未发现标准 `validate_tech_design.sh`，因此 Phase 1 继续采用轻量 ASC：

- 通过源码事实和设计约束人工校验
- 不伪造脚本执行结果
