# ASC-03 UI Liquid 统一技术调研

## 1. 任务目标

本任务要把 `app/ui` 下已经落地的 Liquid UI 基建做一次受控统一，重点不是新增页面，而是收敛重复实现、收紧 infra 边界，并最终删除 `liquid_example`。

本任务当前已明确的目标：

- 统一 `ui/infra` 与 `ui/nexus/content` 中重复的 liquid 交互实现
- 将配置页正式纳入本次统一范围
- 采用单 ASC、多 Batch 的方式推进，并在每个 Batch 后人工回归
- 最终删除 `app/ui/liquid_example`
- 收敛圆角策略，但把 shape 统一放在靠后的收尾 Batch，减少前期返工
- 明确 `infra` 与业务组件边界，避免页面层继续发散自定义实现

本任务当前不包含：

- 新增业务页面或导航流
- 修改 `ui-shell` 的页面结构
- 运行编译或启动程序验证 UI
- 为了统一而引入大而全的万能组件体系

## 2. 当前源码事实

### 2.1 当前 liquid 交互实现存在多处分叉

当前液态拖拽和高光的核心能力已经部分抽到：

- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt`
- `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt`

但相同的位移/缩放映射逻辑仍分别散落在：

- `ui/infra/component/LiquidButton.kt`
- `ui/infra/component/LiquidTextField.kt`
- `ui/infra/component/LiquidSecretTextField.kt`
- `ui/infra/ActionBarButton.kt`

其中：

- `LiquidTextField` 与 `LiquidSecretTextField` 的容器层几乎镜像复制
- `ActionBarButton` 没有复用现有 interaction 基建，重复维护了手势、高光和形变实现

结论：

- 当前不是“完全没有抽象”，而是“交互内核抽了一半，接线层仍然重复”

### 2.2 页面层存在稳定复用组件，也存在场景专用组件

跨页稳定复用且适合继续放在 `infra/component` 的组件包括：

- `SettingsGroupCard`
- `SettingsNavigationRow`
- `SettingsToggleRow`
- `StyledTextField`
- `TintLiquidButton`
- `MaterialTintLiquidButton`

单页语义明显、当前不适合上升为通用 infra 的组件包括：

- `HomeChatComponents.kt` 中的聊天气泡、工具状态、composer 外层语义组件
- `StartupPosterBackground.kt`

结论：

- 本任务重点应落在“交互基建”和“设置/表单类组件统一”
- 不应顺手把聊天页或启动页视觉件也抽成通用 infra

### 2.3 配置页已部分进入统一体系，但仍残留 demo 依赖

`ConfigurePageContent.kt` 当前已经使用：

- `LiquidTextField`
- `LiquidSecretTextField`
- `SettingsGroupCard`
- `TintLiquidButton`

但仍直接依赖：

- `app/src/main/java/com/niki914/nexus/agentic/app/liquid_example/components/LiquidToggle.kt`

结论：

- 配置页已经是主线 UI，不再是临时例外
- 本轮必须把配置页从 demo 包依赖中解开

### 2.4 圆角策略当前已形成三套语义，但没有被显式统一

当前仓库里能看到三类 shape 用法：

- 胶囊类：
  - `LiquidButton` 使用 `ContinuousCapsule(G2Continuity())`
- 固定半径输入类：
  - `LiquidTextField` / `LiquidSecretTextField` / `StyledTextField` 使用 `RoundedCornerShape(28.dp)`
- 固定半径连续曲率类：
  - `SettingsGroupCard` 定义并使用 `G2RoundedCornerShape(28.dp)`
  - 首页消息气泡使用 `G2RoundedCornerShape(24.dp)`

用户额外确认的偏好是：

- 输入区应优先采用“类似首页编辑框”的固定半径思路
- 不希望因为控件高度增加而让圆角无限趋向胶囊

结论：

- shape 不应只靠页面随手写
- 但 shape 统一应放在后期收尾 Batch，避免前面组件收口时反复返工

### 2.5 `layerBackdrop` 存在明确误用风险

用户已明确提醒：

- `Modifier.layerBackdrop(backdrop)` 不能乱用
- 一旦 backdrop 关系接错，容易形成递归记录或错误层级

当前判断：

- 后续重构中应优先复用现有安全路径
- 非必要不要新引入额外 `layerBackdrop` 组合链
- 如果要迁移 `LiquidToggle` 或圆形液态按钮，必须把 backdrop 层次作为显式设计约束

## 3. 已澄清的执行约束

### 3.1 任务组织方式

用户已选择：

- 单 ASC
- 多 Batch
- 同一套调研 / 设计 / 计划文档贯穿实现

这意味着：

- 设计约束和边界只维护一份
- 组件批次间的统一性更容易守住

### 3.2 回归节奏

用户已选择：

- 每个 Batch 完成后人工回归

这意味着：

- 计划设计必须天然支持停顿点
- 每个 Batch 都要具备“局部可验收”的边界

### 3.3 `liquid_example` 删除边界

用户已选择：

- 最终 Batch 删净 `liquid_example`

这意味着：

- 早期 Batch 先完成迁移、断引用和主线替换
- 删除目录动作放在最终收尾，避免中途因为大面积清理影响定位

### 3.4 Shape 统一时机

用户当前倾向：

- 先做组件类型统一
- 最后再统一 shape

当前判断：

- 这是合理的
- shape 是横切面，如果太早统一，会迫使按钮、输入框、卡片同时变动，增加回归面

## 4. 方案发散

### 方案 A：单 ASC，多 Batch，按组件类型推进，shape 收尾统一

做法：

- 在同一个 ASC 中完成调研、设计和计划
- 实现阶段按组件类型拆批次，例如：
  - Batch 1：按钮与顶栏圆形液态按钮
  - Batch 2：输入区与配置页编辑区
  - Batch 3：设置 / 开关 / 剩余液态组件与 `liquid_example` 断引用
  - Batch 4：shape 统一、命名收尾、删除 `liquid_example`
- 每个 Batch 后暂停，等待人工回归

优点：

- 统一约束只维护一份
- 回归节奏清晰
- 能把“shape 最后统一”的策略自然纳入计划
- 最符合用户当前风险偏好

缺点：

- 需要在计划里提前设计好批次边界
- 后续 Batch 不能随意跨批改动

### 方案 B：多个独立 ASC，按钮 / 输入框 / shape 各开一个任务

做法：

- 为按钮、编辑区、shape、demo 清理分别开独立 ASC

优点：

- 单任务范围更窄
- 文档更短

缺点：

- 交互内核、shape 策略、infra 边界会重复设计
- `liquid_example` 删除边界很容易跨任务互相卡住
- 不利于最终形成一套统一的 UI 基建认知

### 方案 C：一次性全量统一

做法：

- 单次实现按钮、输入区、toggle、shape、demo 清理全部内容

优点：

- 理论上总工期最短

缺点：

- 回归面过大
- 一旦出现视觉或交互问题，难以定位是哪个重构层导致
- 明显不符合用户对 UI 风险的担忧

## 5. 推荐方案

推荐采用 **方案 A：单 ASC，多 Batch，按组件类型推进，shape 收尾统一**。

原因：

- 用户已明确选择单 ASC、多 Batch、每 Batch 回归、最终 Batch 删净 `liquid_example`
- 当前问题的本质是“同一套交互基建跨组件散落”，不适合拆成多个彼此独立的 ASC
- shape 确实应该放后面做，这样能减少按钮、输入框、卡片同时回归造成的返工

## 6. 初步批次建议

### Batch 1：按钮体系统一

目标：

- 收敛 `LiquidButton`、`TintLiquidButton`、`MaterialTintLiquidButton`
- 抽出或正名顶栏圆形液态按钮
- 清理 `ActionBarButton` 与 interaction 基建的分叉

验收重点：

- CTA 按钮交互保持稳定
- `LiquidScreen` 顶栏按钮形态与行为不回退

### Batch 2：编辑区统一

目标：

- 收敛 `LiquidTextField` 与 `LiquidSecretTextField`
- 正式纳入配置页编辑区
- 评估并迁移 settings 侧表单输入与液态输入边界

验收重点：

- 首页 composer 与配置页输入框行为一致
- 聚焦、失焦、IME 联动不回退

### Batch 3：开关 / 其余组件 / demo 断引用

目标：

- 处理配置页 toggle
- 将仍然依赖 `liquid_example` 的主线 UI 替换为 infra 正式组件
- 清理剩余重复交互实现

验收重点：

- 配置页切换逻辑不回退
- 主线 UI 不再依赖 demo 包

### Batch 4：shape 收尾与 demo 删除

目标：

- 统一 shape 原语
- 收口命名、辅助函数和 modifier
- 删除 `app/ui/liquid_example`

验收重点：

- 输入类、卡片类、胶囊类 shape 策略清晰且一致
- demo 包删除后主线 UI 无残留引用

## 7. 设计约束

### 7.1 infra / 业务边界约束

- `infra` 负责稳定壳层、通用交互和跨页复用组件
- 业务层负责页面语义、文案组织和状态装配
- 不把聊天页和启动页场景组件强行上升为通用 infra

### 7.2 交互基建约束

- 优先抽象“交互内核”和“容器层”，不要先做万能控件
- `enabled` / 高光 / 拖拽启停策略必须收敛，不允许每个组件各写一套

### 7.3 backdrop 约束

- `layerBackdrop` 只在必要场景使用
- 重构中不得为了“封装漂亮”而新增不清楚的 layer 叠加
- 任何使用 `layerBackdrop` 的组件都必须在 Phase 1 明确 backdrop 拓扑，避免递归

### 7.4 shape 约束

- 输入类优先固定半径，不采用随高度放大的胶囊圆角
- 胶囊类与输入类 shape 不强行合并
- shape 统一在后期批次执行

## 8. 风险与注意事项

### 风险 1：交互统一过度，反而把控件语义搅乱

如果试图把按钮、输入框、toggle 都塞进同一个万能组件，代码会迅速变复杂。

控制方式：

- 只统一交互内核和容器能力
- 控件语义层继续保持分离

### 风险 2：`layerBackdrop` 使用错误导致递归或渲染层级问题

控制方式：

- 在 Phase 1 显式梳理 backdrop 拓扑
- 复用已有安全路径，避免即兴组合

### 风险 3：shape 过早统一，造成重复返工

控制方式：

- 先统一组件与交互内核
- 最后收 shape

### 风险 4：demo 删除时机过早，影响中间批次定位

控制方式：

- 前几批只断主线引用
- 最终批次统一删除 `liquid_example`

## 9. 推荐的 Phase 1 输入

下一阶段架构设计应重点回答：

- 交互内核要抽到什么层级，如何给按钮 / 输入框 / 顶栏按钮复用
- 顶栏圆形液态按钮是否公开为 `infra/component` 原语
- `LiquidTextField` / `LiquidSecretTextField` / `StyledTextField` 的边界如何收敛
- 配置页 toggle 应迁入 `infra` 还是替换为现有 settings 开关体系
- shape 原语如何定义，哪些组件在最终 Batch 切换
- `liquid_example` 删除前，哪些中间兼容层需要保留

## 10. 校验说明

当前仓库仍未发现标准 ASC validator scripts，因此本轮 Phase 0 继续采用轻量 ASC：

- 通过源码阅读和人工校验完成调研
- 不伪造脚本执行结果
