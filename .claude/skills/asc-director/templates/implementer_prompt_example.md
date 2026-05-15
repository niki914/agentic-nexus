# Implementer Prompt Filled Example

> **说明**：这是占位符全部替换后的 implementer prompt 样例（基于 camera 迁移场景 B-01），供派发 subagent 前参照确认所有占位符已正确替换。

---

## 完整替换后的 Prompt

```
Task tool:
  description: "Implement Batch B-01: ViewEffect 契约层扩展 + UI 补齐"
  prompt: |
    你正在实现 Batch B-01，包含以下 Feature/Task：

    ## Batch 概览
    - Feature F-01: ViewEffect 契约层扩展 + UI 视图树补齐 + VC 响应逻辑
      - T-01: 新增 ViewEffect case
      - T-02: VC 视图树补齐 previewContainerView + previewMaskView
      - T-03: VC 响应 ViewEffect 创建 camera 并回传

    ## 各 Task 详细描述

    ### T-01: 新增 ViewEffect case

    | T-01 | Contracts | Contract | 新增 ViewEffect case：在 `AIEffectProcessEffect.ViewEffect` 增加 `case requestCreateCamera(...)` | `.../AIEffectProcessEffect.swift` | `AIEffectProcessViewModel.swift` | - | L | ~20-40 LOC | 编译通过；VM 能发出该 effect；VC 能收到并进入对应处理分支 |

    实施步骤：
    - [ ] 定位 `AIEffectProcessEffect.swift` 中 ViewEffect 枚举定义
    - [ ] 新增 `case requestCreateCamera(taskID: String, publisherInfo: TTVPPublisherInfo, effectModel: TSVEffectStickerModel?, cameraConfig: TTRecordVideoCameraConfig)`
    - [ ] 在 `handleViewEffect` switch 中添加该 case 的转发（空实现，T-03 填充）
    - [ ] 验证：编译通过

    ### T-02: VC 视图树补齐

    | T-02 | UI | UI | VC 视图树补齐 previewContainerView + previewMaskView | `AIEffectProcessViewController.swift` | `AIEffectProcessViewController.swift` | - | L | ~40-80 LOC | previewContainerView/previewMaskView frame 与 backgroundImageView 一致；层级正确 |

    实施步骤：
    - [ ] 在 VC 中声明 `private lazy var previewContainerView: UIView`（背景透明）
    - [ ] 在 VC 中声明 `private lazy var previewMaskView: UIView`（背景黑色）
    - [ ] 在 `addSubviews()` 中 insertSubview previewContainerView below backgroundImageView
    - [ ] 在 `addSubviews()` 中 insertSubview previewMaskView above previewContainerView
    - [ ] 在 `setupConstraints()` 中约束两者 edges == backgroundImageView
    - [ ] 验证：运行时 frame 正确、层级正确

    ### T-03: VC 响应 ViewEffect 创建 camera

    | T-03 | UI | UI | VC 响应 ViewEffect 创建 camera 并回传 | `AIEffectProcessViewController.swift` | `TTRecordVideoCamera.h` | - | M | ~30-70 LOC | idle 新建任务时 VC 能创建 camera 并回传；VM 不持有 UIView |

    实施步骤：
    - [ ] 读取 `TTRecordVideoCamera.h` 确认初始化签名
    - [ ] 在 `onViewEffect` 的 switch 中添加 `requestCreateCamera` 处理
    - [ ] 创建 TTRecordVideoCamera，设置 preview mode，apply AI effect
    - [ ] 回传 `viewModel.provideCamera(camera, taskID: taskID)`
    - [ ] 验证：编译通过；断点确认 ViewEffect 触达

    ## 验收标准 (AC) 汇总
    - T-01: 编译通过；VM 能发出该 effect；VC 能收到并进入对应处理分支
    - T-02: previewContainerView/previewMaskView frame 与 backgroundImageView 一致；层级正确
    - T-03: idle 新建任务时 VC 能创建 camera 并回传；VM 不持有 UIView

    ## API 规范（来自 tech_design.md）

    ### Enum: `AIEffectProcessEffect.ViewEffect`
    - 新增 `case requestCreateCamera(taskID: String, publisherInfo: TTVPPublisherInfo, effectModel: TSVEffectStickerModel?, cameraConfig: TTRecordVideoCameraConfig)`

    ### Class: `AIEffectProcessViewController`
    - 新增 `previewContainerView: UIView`、`previewMaskView: UIView`
    - 在 `onViewEffect` 处理 `requestCreateCamera`

    ## 现有代码上下文

    ### AIEffectProcessViewController.swift (关键片段)
    ```swift
    class AIEffectProcessViewController: TTMVIBaseViewController<...> {
        private lazy var backgroundImageView: UIImageView = { ... }()
        
        func addSubviews() { ... }
        func setupConstraints() { ... }
        func bindViewModel() {
            viewModel.onViewEffect = { [weak self] effect in
                switch effect {
                // 现有 case 处理
                }
            }
        }
    }
    ```

    ### TTRecordVideoCamera.h (关键接口)
    ```objc
    @interface TTRecordVideoCamera : NSObject
    - (instancetype)initWithCameraView:(UIView *)cameraView 
                          cameraConfig:(TTRecordVideoCameraConfig *)cameraConfig 
                         publisherInfo:(TTVPPublisherInfo *)publisherInfo 
                      initializedBlock:(void(^)(void))block;
    - (void)setPreviewModeType:(TTRecordPreviewModeType)type;
    - (void)renderPicImage:(UIImage *)image;
    @property (nonatomic, weak) id<TTRecordVideoCameraDelegate> delegate;
    @end
    ```

    ## 目标文件当前内容

    [粘贴 AIEffectProcessEffect.swift 和 AIEffectProcessViewController.swift 的完整当前内容]

    ## 项目目录结构

    ```
    Module/TTVideoPublisherBusiness/.../RecordVideo/
    ├── AIEffect/Core/AIEffectGenerator.swift
    └── AIEffectProcess/
        ├── View/AIEffectProcessViewController.swift  ← T-02, T-03 目标
        ├── ViewModel/AIEffectProcessViewModel.swift
        └── XXX/AIEffectProcessEffect.swift           ← T-01 目标
    ```

    ## 开始前
    如果对以下内容有疑问，**立即提问**。提问总比猜测好。

    ## 你的工作
    1. 按 Task 顺序（T-01 → T-02 → T-03）逐个实现
    2. 文件路径、命名风格 MUST 与 tech_design.md 一致
    3. MUST 复用现有基础设施。NEVER 重新发明轮子
    4. 所有 Task 完成后执行自审
    5. 报告结果

    禁止事项：
    - 除非用户明确要求，否则不得进行 git commit

    工作目录: ./Module/TTVideoPublisherBusiness/.../AIEffectProcess/

    ## 校验策略
    - 不跑完整编译/link/联调
    - 仅做 AI Code Review 自审

    ## 自审清单
    **完整性**：每个 Task 的 AC 每条都实现了？
    **质量**：命名清晰？风格一致？复用工具类？
    **纪律**：只做要求的？每个 Task 只改了其目标文件？
    **跨 Task 一致性**：T-01 定义的 case 和 T-03 使用的 case 签名一致？

    ## 报告格式
    - **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
    - 逐 Task 报告
    - 整体校验结果
    - 自审发现
    - 疑虑
```

---

## 自检要点

上面的 prompt 中：
- ✅ `{BATCH_ID}` → `B-01`
- ✅ `{BATCH_SUMMARY}` → `ViewEffect 契约层扩展 + UI 补齐`
- ✅ `{BATCH_OVERVIEW}` → 列出 F-01 下的 T-01/T-02/T-03
- ✅ `{ALL_TASKS_TEXT}` → 完整粘贴了 3 个 Task 的详情和步骤
- ✅ `{ALL_ACCEPTANCE_CRITERIA}` → 汇总了 3 个 Task 的 AC
- ✅ `{RELEVANT_API_SPEC}` → 粘贴了 ViewEffect enum 和 VC 相关规范
- ✅ `{CODE_CONTEXT}` → 粘贴了 VC 和 TTRecordVideoCamera.h 关键片段
- ✅ `{EXISTING_FILES_CONTENT}` → 标注为"粘贴完整当前内容"
- ✅ `{DIRECTORY_STRUCTURE}` → 粘贴了目录树
- ✅ `{WORKING_DIR}` → 设置了工作目录
- ✅ 逐个搜索 10 个占位符原文，确认全部已替换
