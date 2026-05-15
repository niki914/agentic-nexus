# Reviewer Prompt Filled Example

> **说明**：这是占位符全部替换后的 reviewer prompt 样例（基于 camera 迁移场景全量 Review），供派发 subagent 前参照确认所有占位符已正确替换。

---

## 完整替换后的 Prompt

```
Task tool:
  description: "Review all implemented code for project ai_effect_camera_migration"
  prompt: |
    你正在审查项目 ai_effect_camera_migration 的全量实现代码。

    ## 实现范围

    ### Feature F-01: ViewEffect 契约层扩展 + UI 补齐
    - T-01: 新增 ViewEffect case → AC: 编译通过；VM 能发出该 effect；VC 能收到
    - T-02: VC 视图树补齐 → AC: frame 与 backgroundImageView 一致；层级正确
    - T-03: VC 响应 ViewEffect 创建 camera → AC: idle 新建任务时 VC 能创建 camera 并回传

    ### Feature F-02: VM Camera Owner
    - T-04: VM 接收 camera → AC: idle 启动生成不再调用 generator camera API；VM 持有 camera
    - T-05: VM cleanupCamera 收敛 → AC: 各路径均走 cleanupCamera
    - T-06: VM delegate 实现 → AC: camera 回调能驱动 task 回调

    ### Feature F-03: Generator 下线 + 清理
    - T-07: Generator 下线 camera → AC: 编译通过；全工程不再引用 generator camera API
    - T-08: 可选清理 → AC: 无引用、构建通过

    ## tech_design.md 关键设计决策
    - VM 成为 camera owner，VC 负责创建后通过 provideCamera 回传
    - camera 生命周期统一收敛到 VM.cleanupCamera(reason:)
    - Generator 完全移除 camera 相关能力

    ## Implementer 报告汇总

    ### B-01 (F-01: T-01, T-02, T-03)
    - **Status:** DONE
    - T-01: 新增了 requestCreateCamera case，handleViewEffect 已覆盖
    - T-02: 新增了 previewContainerView/previewMaskView，约束已设置
    - T-03: 在 onViewEffect 中处理 requestCreateCamera，创建 camera 并回传 VM
    - 自审：无问题

    ### B-02 (F-02: T-04, T-05, T-06)
    - **Status:** DONE
    - T-04: provideCamera 方法实现，含 taskID 校验、delegate/bridge 绑定
    - T-05: cleanupCamera 统一入口，4 个调用点已替换
    - T-06: TTRecordVideoCameraDelegate extension 实现
    - 自审：renderPicImage 传入的 coverImage 来源未变，确认是否正确
    - 疑虑：renderPicImage 的 coverImage 来源

    ### B-03 (F-03: T-07, T-08)
    - **Status:** DONE
    - T-07: 删除了 cameraService 及 5 个 camera 方法，cleanupTask 已修改
    - T-08: AIEffectCameraService 和 AICameraBuilder 无外部引用，已删除
    - 自审：无问题

    ## 重要：不要信任报告

    Implementer 的报告可能不完整或过于乐观。你 MUST 独立验证一切。

    **NEVER：**
    - 相信 implementer 的完成声明
    - 仅凭报告判断合规性
    - 跳过实际代码阅读

    **MUST：**
    - 读取实际代码文件
    - 逐条对比 AC
    - 独立运行校验命令

    ## 审查维度 A：规格合规

    逐 Feature、逐 Task 验证：

    **F-01:**
    - T-01: requestCreateCamera case 签名是否与 tech_design 一致？handleViewEffect 是否覆盖？
    - T-02: previewContainerView/previewMaskView 层级是否正确？约束是否 edges == backgroundImageView？
    - T-03: 创建 camera 的参数是否完整？是否调用了 setPreviewModeType + applyAIEffect？回传路径是否正确？

    **F-02:**
    - T-04: provideCamera 的 taskID 校验是否用 guard + return？camera/delegate/bridge 绑定是否完整？
    - T-05: cleanupCamera 是否覆盖 deinit/background/retry/cleanupTask 全部 4 个路径？
    - T-06: delegate 方法签名是否与 TTRecordVideoCamera.h 一致？回调是否转发到 currentTask？

    **F-03:**
    - T-07: 搜索 generator.setupCamera/destroyCamera 确认 0 结果？
    - T-08: 搜索 AIEffectCameraService/AICameraBuilder 确认 0 结果？

    **跨 Feature 集成：**
    - T-01 定义的 case 签名 == T-03 使用的签名？
    - T-03 回传的 provideCamera 调用 == T-04 的方法签名？
    - T-04 的 camera.delegate = self == T-06 的 delegate extension？

    ## 审查维度 B：代码质量

    **命名与风格：**
    - provideCamera/cleanupCamera 命名是否与现有 VM 方法风格一致？
    - 日志是否使用了 AIEffectMonitor.shared.log？

    **基础设施复用：**
    - bridge 绑定方式是否与原 generator 中的方式一致？

    **职责单一：**
    - 每个 Task 是否只修改了其目标文件？
    - provideCamera 是否只做了绑定 + 触发生成？

    ## 报告格式

    ## Review Result

    **Status:** ✅ 全部通过 | ❌ 有问题

    ### 规格合规
    - [逐 Feature/Task AC 检查结果]

    ### 代码质量
    - **优点**: [具体优点]
    - **问题**: [Critical/Important/Minor 分级，附 file:line 引用]

    ### 跨 Feature 集成检查
    - [接口一致性、调用链完整性]

    ### 校验结果
    - 编译: PASS/FAIL
    - generator camera API 搜索: 期望 0 处残留

    ### 结论
    [全部通过 / 需要修复（列出具体修复项）]
```

---

## 自检要点

上面的 prompt 中：
- ✅ `{PROJECT_NAME}` → `ai_effect_camera_migration`
- ✅ `{ALL_FEATURES_AND_TASKS}` → 列出了 3 个 Feature、8 个 Task 及其 AC
- ✅ `{DESIGN_DECISIONS}` → 列出了 3 条关键设计决策
- ✅ `{ALL_IMPLEMENTER_REPORTS}` → 粘贴了 3 个 Batch 的完整报告
- ✅ 逐个搜索 4 个占位符原文，确认全部已替换
