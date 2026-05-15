# 任务规划清单 v1.0

> **说明**：这是一个完整填充的 plan.md 样例（基于 AI Effect Camera 架构迁移场景），供 AI 生成时参照格式和质量标准。

## 1. Feature 列表

| Feature ID | 功能描述 | 预估 LOC | 依赖 Feature |
|:-----------|:---------|:---------|:-------------|
| F-01 | ViewEffect 契约层扩展 + UI 视图树补齐 + VC 响应逻辑 | ~90-190 | - |
| F-02 | VM Camera Owner：接收、生命周期、Delegate | ~130-280 | F-01 |
| F-03 | Generator 下线 camera + 可选清理 | ~60-180 | F-02 |

## 2. Batch 编排表

| Batch ID | 包含 Feature | 预估总 LOC | 前置 Batch | 可并行 |
|:---------|:------------|:-----------|:-----------|:-------|
| B-01 | F-01 | ~90-190 | - | - |
| B-02 | F-02 | ~130-280 | B-01 | - |
| B-03 | F-03 | ~60-180 | B-02 | - |

> **编排说明**：本场景三个 Feature 存在严格依赖链（F-01 → F-02 → F-03），无法并行。单个 Feature LOC 均 < 500，但由于跨依赖层级 NEVER 聚合，各自独立成 Batch。

## 3. 任务清单 (Task List)

### Feature F-01: ViewEffect 契约层扩展 + UI 补齐

| ID | 阶段 | 类型 | 任务详情（含签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:--------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-01 | Contracts | Contract | **新增 ViewEffect case**：在 `AIEffectProcessEffect.ViewEffect` 增加 `case requestCreateCamera(taskID: String, publisherInfo: TTVPPublisherInfo, effectModel: TSVEffectStickerModel?, cameraConfig: TTRecordVideoCameraConfig)`；并在现有 `handleViewEffect` 分发中覆盖该 case。 | `.../AIEffectProcessEffect.swift` | `AIEffectProcessViewModel.swift` | - | L | ~20-40 LOC | 编译通过；VM 能发出该 effect；VC 能收到并进入对应处理分支 |
| T-02 | UI | UI | **VC 视图树补齐 previewContainerView + previewMaskView**：1) 在 `addSubviews()` 中插入 previewContainerView 置于 backgroundImageView 底部，previewMaskView 叠在 preview 上方 2) 在 `setupConstraints()` 中对两者做 `edges == backgroundImageView` 3) 遮罩黑色可配置 alpha | `AIEffectProcessViewController.swift` | `AIEffectProcessViewController.swift` | - | L | ~40-80 LOC | previewContainerView/previewMaskView frame 与 backgroundImageView 一致；层级正确 |
| T-03 | UI | UI | **VC 响应 ViewEffect 创建 camera 并回传**：在 `bindViewModel()` 的 `onViewEffect` 分发中处理 `requestCreateCamera`：1) 使用 previewContainerView 创建 TTRecordVideoCamera 2) setPreviewModeType(.preserveAspectRatioAndFill) 3) applyAIEffect 4) 调用 viewModel.provideCamera(camera, taskID:) 回传 | `AIEffectProcessViewController.swift` | `TTRecordVideoCamera.h` | - | M | ~30-70 LOC | idle 新建任务时 VC 能创建 camera 并回传；VM 不持有 UIView |

### Feature F-02: VM Camera Owner

| ID | 阶段 | 类型 | 任务详情（含签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:--------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-04 | Core | Logic | **VM 接收 camera 并成为 owner**：`func provideCamera(_ camera: TTRecordVideoCamera, taskID: String)`：1) 校验 taskID 一致 2) self.camera = camera 3) camera.delegate = self 4) 绑定 task.msgBridge 5) 触发 startGenerationByRenderingCoverImage() | `AIEffectProcessViewModel.swift` | `AIEffectProcessViewModel.swift`, `TTRecordVideoCamera.h` | - | M | ~60-120 LOC | idle 启动生成不再调用 generator camera API；VM 持有 camera 且可渲染 cover 图 |
| T-05 | Core | Logic | **VM camera 生命周期与销毁收敛**：将 deinit/moveToBackground/retry/cleanupTask 中对 generator.destroyCamera 的调用迁移为 VM 内部统一 `cleanupCamera(reason:)`（stopCapture + delegate=nil + msgBridge=nil + camera=nil） | `AIEffectProcessViewModel.swift` | `AIEffectProcessViewModel.swift` | - | M | ~40-90 LOC | 退后台/重试/退出/deinit 均走 cleanupCamera；不再出现 generator.destroyCamera 调用 |
| T-06 | Core | Logic | **VM 实现 TTRecordVideoCameraDelegate**：`extension AIEffectProcessViewModel: TTRecordVideoCameraDelegate`，在 didFinishRecording/didReceiveAIGCFailed 中转发给 currentTask 回调 | `AIEffectProcessViewModel.swift` | `TTRecordVideoCamera.h` | - | M | ~30-70 LOC | camera 回调能驱动 task 的 onRecordingFinished/onRecordingFailed |

### Feature F-03: Generator 下线 camera + 可选清理

| ID | 阶段 | 类型 | 任务详情（含签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:--------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-07 | Infra | Infra | **Generator 下线 camera 能力**：移除 cameraService 属性 + setupCamera/startCapture/stopCapture/renderImage/destroyCamera 方法；cleanupTask 不再触发 camera 清理 | `AIEffectGenerator.swift` | `AIEffectGenerator.swift` | - | M | ~60-140 LOC | 编译通过；全工程不再引用 generator camera API |
| T-08 | Infra | Infra | **可选清理：删除不再使用的 camera service/builder**：确认无引用后删除 AIEffectCameraService.swift / AICameraBuilder.swift | `AIEffectCameraService.swift`, `AICameraBuilder.swift` | `AIEffectCameraService.swift`, `AICameraBuilder.swift` | - | L | ~0-40 LOC | 无引用、构建通过 |

## 4. 实施步骤 (Steps per Task)

### T-01: 新增 ViewEffect case

- [ ] 定位 `AIEffectProcessEffect.swift` 中 ViewEffect 枚举定义
- [ ] 新增 `case requestCreateCamera(taskID: String, publisherInfo: TTVPPublisherInfo, effectModel: TSVEffectStickerModel?, cameraConfig: TTRecordVideoCameraConfig)`
- [ ] 在 `handleViewEffect` switch 中添加该 case 的转发（空实现，T-03 填充）
- [ ] 验证：编译通过

### T-02: VC 视图树补齐

- [ ] 在 VC 中声明 `private lazy var previewContainerView: UIView`（背景透明）
- [ ] 在 VC 中声明 `private lazy var previewMaskView: UIView`（背景黑色）
- [ ] 在 `addSubviews()` 中 insertSubview previewContainerView below backgroundImageView
- [ ] 在 `addSubviews()` 中 insertSubview previewMaskView above previewContainerView
- [ ] 在 `setupConstraints()` 中约束两者 edges == backgroundImageView
- [ ] 验证：运行时 frame 正确、层级正确

### T-03: VC 响应 ViewEffect 创建 camera

- [ ] 读取 `TTRecordVideoCamera.h` 确认初始化签名
- [ ] 在 `onViewEffect` 的 switch 中添加 `requestCreateCamera` 处理
- [ ] 创建 `TTRecordVideoCamera(cameraView: previewContainerView, cameraConfig:, publisherInfo:, initializedBlock: nil)`
- [ ] 设置 `setPreviewModeType(.preserveAspectRatioAndFill)`
- [ ] 调用 `applyAIEffect(effectModel, withTaskID: taskID)`
- [ ] 回传 `viewModel.provideCamera(camera, taskID: taskID)`
- [ ] 验证：编译通过；断点确认 ViewEffect 触达

### T-04: VM provideCamera

- [ ] 新增 `func provideCamera(_ camera: TTRecordVideoCamera, taskID: String)`
- [ ] guard self.taskID == taskID，不一致则 return + log
- [ ] self.camera = camera, self.cameraTaskID = taskID
- [ ] camera.delegate = self
- [ ] currentTask?.msgBridge = camera as? AIEffectMsgBridgeProtocol
- [ ] 调用现有 startGenerationByRenderingCoverImage()（内部改为 camera.renderPicImage）
- [ ] 验证：idle 启动生成流程走通

### T-05: VM cleanupCamera 收敛

- [ ] 新增 `private func cleanupCamera(reason: String)`：stopCapture + delegate=nil + msgBridge=nil + camera=nil
- [ ] deinit 中调用 cleanupCamera(reason: "deinit")
- [ ] moveToBackground 中替换 generator.destroyCamera 为 cleanupCamera(reason: "background")
- [ ] retry 流程中替换为 cleanupCamera(reason: "retry")
- [ ] cleanupTask 中替换为 cleanupCamera(reason: "cleanupTask")
- [ ] 验证：各路径均走单点销毁

### T-06: VM delegate 实现

- [ ] 新增 `extension AIEffectProcessViewModel: TTRecordVideoCameraDelegate`
- [ ] 实现 `recordVideoCamera(_:didFinishRecordingWithURL:orVideo:)` → currentTask?.onRecordingFinished
- [ ] 实现 `recordVideoCamera(_:didReceiveAIGCFailed:)` → currentTask?.onRecordingFailed
- [ ] 验证：编译通过；回调能正确转发

### T-07: Generator 下线

- [ ] 删除 `private let cameraService = AIEffectCameraService()`
- [ ] 删除 setupCamera/startCapture/stopCapture/renderImage/destroyCamera 方法
- [ ] 修改 cleanupTask：移除 destroyCamera 调用
- [ ] 全工程搜索 generator.setupCamera / generator.destroyCamera 确认 0 结果
- [ ] 验证：编译通过

### T-08: 可选清理

- [ ] 全工程搜索 AIEffectCameraService / AICameraBuilder 引用
- [ ] 若无引用 → 删除文件
- [ ] 若有外部引用 → 保留，仅确保 Process/Generator 不再依赖
- [ ] 验证：编译通过

---

## 5. 审查修正记录

### Round 1: PM（完整性与价值）
- Feature 划分覆盖了完整的迁移链路：契约层 → Owner 转移 → 旧能力下线。
- 补充了关键验收：`.idle` 新建任务必须仍能通过 render cover image 启动生成链路。
- 所有 Feature 均已分配到 Batch。

### Round 2: 架构师（结构与解耦）
- 任务按物理文件拆分（1 Task = 1 目标文件），没有"同时改多个文件"的 God Task。
- VM 不触达 UIKit：camera 创建行为发生在 VC（UI 层）且通过回传让 VM 成为 owner，满足依赖倒置。
- Batch 编排遵循依赖链，未跨层级聚合。

### Round 3: 结对伙伴（可执行性与细节）
- 每个任务给出明确签名与落点，执行时只需按 Vision 打开对应文件即可开工。
- T-01 的目标文件路径需要在执行前通过搜索确定 `AIEffectProcessEffect` 实际定义文件名。
- Batch 编排合理，单个 Batch 内 Task 上下文可控。
