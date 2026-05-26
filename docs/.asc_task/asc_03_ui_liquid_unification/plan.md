# ASC-03 UI Liquid 统一任务规划

## 1. 规划原则

- 单 ASC，多 Batch
- 每个 Batch 完成后暂停，等待人工回归
- 先统一组件与交互内核，最后统一 shape
- `liquid_example` 在最终 Batch 删除
- 单个 Task 对应一个目标文件或一个强绑定文件组

## 2. Feature 拆分

### F-01 按钮交互内核统一

- 目标：收敛 CTA 按钮与顶栏圆形按钮的交互实现
- 预估：中等
- 依赖：无

### F-02 输入区容器统一

- 目标：收敛 Liquid 输入区容器与配置页编辑区
- 预估：中等
- 依赖：F-01

### F-03 Toggle 正式化与 demo 断引用

- 目标：替换配置页对 demo toggle 的依赖，清理主线 UI 对 `liquid_example` 的残留引用
- 预估：中等
- 依赖：F-01、F-02

### F-04 Shape 原语统一与删除收尾

- 目标：收口 shape 原语、命名与最终删除 demo 包
- 预估：中等
- 依赖：F-01、F-02、F-03

## 3. Task 明细

### F-01 按钮交互内核统一

#### T-01

- 文件：`ui/infra/interaction/LiquidInteractiveStyle.kt`
- 目标：新增交互样式对象，集中表达 press / drag 相关参数
- AC：
  - 提供可复用的 style 数据结构
  - 不引入组件语义耦合

#### T-02

- 文件：`ui/infra/interaction/LiquidInteractiveLayer.kt`
- 目标：新增共享 layer 变换函数
- AC：
  - 支持根据 `pressProgress`、`offset`、`size` 计算平移与缩放
  - 被按钮与顶栏按钮复用

#### T-03

- 文件：`ui/infra/component/LiquidButton.kt`
- 目标：改为消费共享交互内核
- AC：
  - 不再手写变换公式
  - 保持现有公开 API 不变

#### T-04

- 文件：`ui/infra/component/TintLiquidButton.kt`
- 目标：对齐按钮包装层的交互与 disabled 策略
- AC：
  - 仅保留语义化包装职责

#### T-05

- 文件：`ui/infra/component/MaterialTintLiquidButton.kt`
- 目标：对齐 Material 风格按钮包装层
- AC：
  - 保持调用点不变

#### T-06

- 文件：`ui/infra/ActionBarButton.kt`
- 目标：去除重复拖拽检测与重复交互接线
- AC：
  - 复用 `DragGestureInspector`
  - 接入共享交互内核
  - 不改变顶栏按钮外观和对外行为

### F-02 输入区容器统一

#### T-07

- 文件：`ui/infra/component/LiquidTextFieldContainer.kt`
- 目标：新增共享输入容器
- AC：
  - 封装 backdrop、clip、padding、surface、focus 时交互禁用

#### T-08

- 文件：`ui/infra/component/LiquidTextField.kt`
- 目标：委托共享输入容器
- AC：
  - 保持公开 API 不变
  - IME / focus 行为不回退

#### T-09

- 文件：`ui/infra/component/LiquidSecretTextField.kt`
- 目标：委托共享输入容器
- AC：
  - 只保留 secret-specific 差异
  - IME / focus 行为不回退

#### T-10

- 文件：`ui/nexus/content/HomeChatComponents.kt`
- 目标：验证首页 composer 继续走正式 Liquid 输入实现
- AC：
  - 不引入页面层重复逻辑

#### T-11

- 文件：`ui/nexus/content/ConfigurePageContent.kt`
- 目标：配置页编辑区完全走正式输入组件
- AC：
  - endpoint / model / apiKey 使用统一容器能力

### F-03 Toggle 正式化与 demo 断引用

#### T-12

- 文件：`ui/infra/component/LiquidToggle.kt`
- 目标：提供正式 toggle 组件入口
- AC：
  - 基于现有 `StyledSwitch` 稳定实现
  - 对配置页和 settings 侧可消费

#### T-13

- 文件：`ui/infra/component/StyledSwitch.kt`
- 目标：迁移或收口现有 switch 实现
- AC：
  - 明确它与 `LiquidToggle` 的关系
  - 不产生命名歧义

#### T-14

- 文件：`ui/nexus/content/ConfigurePageContent.kt`
- 目标：替换 demo toggle
- AC：
  - 主线 UI 不再依赖 demo toggle
  - 配置页开关行为稳定

#### T-15

- 文件：`app/ui` 下所有仍引用 `liquid_example` 的主线文件
- 目标：断开主线引用
- AC：
  - 主线 UI 对 `liquid_example` 的引用清零

### F-04 Shape 原语统一与删除收尾

#### T-16

- 文件：`ui/infra/shape/LiquidShapes.kt`
- 目标：新增统一 shape 原语
- AC：
  - 提供字段、卡片、气泡、胶囊 shape 入口
  - 迁移 `G2RoundedCornerShape`

#### T-17

- 文件：`ui/infra/component/SettingsGroupCard.kt`
- 目标：改为使用共享 shape 原语
- AC：
  - 移除局部私有 shape 实现

#### T-18

- 文件：`ui/infra/component/LiquidTextField.kt`
- 目标：切换到共享 field shape
- AC：
  - 输入区 shape 统一

#### T-19

- 文件：`ui/infra/component/LiquidSecretTextField.kt`
- 目标：切换到共享 field shape
- AC：
  - 输入区 shape 统一

#### T-20

- 文件：`ui/infra/component/StyledTextField.kt`
- 目标：切换到共享 field shape
- AC：
  - settings 表单输入与主线字段 shape 对齐

#### T-21

- 文件：`ui/nexus/content/HomeChatComponents.kt`
- 目标：切换聊天气泡与相关局部 shape 使用点
- AC：
  - 只切换原语，不改页面语义

#### T-22

- 文件：`app/src/main/java/com/niki914/nexus/agentic/app/ui/liquid_example/`
- 目标：删除 demo 包
- AC：
  - 主线代码无残留引用后再删除

## 4. Batch 编排

### Batch 1

- 覆盖：F-01
- 包含 Task：
  - T-01
  - T-02
  - T-03
  - T-04
  - T-05
  - T-06
- 说明：
  - 先把按钮和顶栏圆形按钮的交互内核统一
  - 回归范围相对集中，便于人工检查

### Batch 2

- 覆盖：F-02
- 包含 Task：
  - T-07
  - T-08
  - T-09
  - T-10
  - T-11
- 说明：
  - 聚焦编辑区和配置页输入容器
  - 与 Batch 1 串行，避免交互内核边改边接

### Batch 3

- 覆盖：F-03
- 包含 Task：
  - T-12
  - T-13
  - T-14
  - T-15
- 说明：
  - 配置页 toggle 正式化
  - 先断主线引用，不急着删目录

### Batch 4

- 覆盖：F-04
- 包含 Task：
  - T-16
  - T-17
  - T-18
  - T-19
  - T-20
  - T-21
  - T-22
- 说明：
  - 做 shape 收尾和 demo 删除
  - 必须在前三批稳定后再执行

## 5. Batch 验收标准

### Batch 1 验收

- CTA 按钮按压、拖拽、高光效果仍正常
- 顶栏左右按钮的外观、缩放、阴影、点击行为不回退
- `LiquidScreen` 公开 API 不变

### Batch 2 验收

- 首页输入框与配置页输入框共享正式容器实现
- `api key` 显示/隐藏逻辑保持正常
- focus / IME 行为不回退

### Batch 3 验收

- 配置页不再引用 demo toggle
- 正式 toggle 命名与包路径清晰
- 主线 UI 对 `liquid_example` 的引用清零

### Batch 4 验收

- field / card / bubble / capsule 的 shape 原语清晰
- 输入类不会因为高度增加而变成巨大圆角
- `app/ui/liquid_example` 已删除

## 6. 实施顺序建议

1. 先做 Batch 1，确认顶栏和按钮稳定
2. 再做 Batch 2，确认输入区稳定
3. 然后做 Batch 3，完成配置页 toggle 和 demo 断引用
4. 最后做 Batch 4，统一 shape 并删除 demo 包

## 7. 审查修正记录

- 将顶栏 API 改造为 `ActionSpec` 的方案否决，原因是回归风险高且用户不希望本轮触碰
- 配置页 demo `LiquidToggle` 路线否决，原因是用户明确指出该实现有 bug
- shape 承载采用“原语优先”，否决“只做 Modifier 扩展”的方案

## 8. 校验说明

当前仓库未发现标准 `validate_plan.sh`，因此 Phase 2 继续采用轻量 ASC：

- 通过任务切分完整性与依赖关系人工校验
- 不伪造脚本执行结果
