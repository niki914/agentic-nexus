# 任务规划清单 v1.1

## 1. Feature 列表

| Feature ID | 功能描述 | 预估 LOC | 依赖 Feature |
|:-----------|:---------|:---------|:-------------|
| F-01 | 新模块与 Gradle 骨架 | ~90 | - |
| F-02 | runtime shared settings 契约层 | ~120 | F-01 |
| F-07 | 预创建目标迁移目录 | ~20 | F-02 |
| F-03 | app/repo 接入与模型下沉收口 | ~245 | F-02 |
| F-04 | UI Infra 物理迁移到 composebase | ~3454 | F-01 |
| F-05 | Chat Runtime 物理迁移到 agent-runtime | ~2492 | F-02 |
| F-06 | 迁移后 runtime 源码去 app 依赖 | ~245 | F-03 + F-05 |

## 2. Batch 编排表

| Batch ID | 包含 Feature | 预估总 LOC | 前置 Batch | 可并行 |
|:---------|:------------|:-----------|:-----------|:-------|
| B-01 | F-01 | ~90 | - | - |
| B-02 | F-02 | ~120 | B-01 | 与 B-07 并行 |
| B-07 | F-07 | ~20 | B-02 | - |
| B-03 | F-04 | ~3454 | B-01 | 与 B-02 并行 |
| B-04 | F-03 | ~245 | B-02 | 与 B-05 并行 |
| B-05 | F-05 | ~2492 | B-02 | 与 B-04 并行 |
| B-06 | F-06 | ~245 | B-04 + B-05 | - |

> **编排说明**：`F-04` 与 `F-05` 的代码量都远超 500 LOC，必须独占 Batch。新增 `B-07` 是辅助 Batch，在你第一次 AS 迁移前预创建目标目录树，不改变源码语义。`F-03` 依赖 `F-02` 的 shared contract 与 gateway，`F-06` 依赖 `F-03` 和 `F-05`，因此运行时去耦修正必须放在 chat 文件完成物理迁移之后。

## 3. 任务清单 (Task List)

### Feature F-01: 新模块与 Gradle 骨架

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-01 | Config | Config | 在 `settings.gradle.kts` 增加 `include(":agent-runtime")`；保持现有模块声明顺序稳定；不要改动其他 module 名称。 | `settings.gradle.kts` | `settings.gradle.kts`, `docs/.asc_task/module_refactor/tech_design.md` | - | L | ~5 lines | Gradle settings 中出现 `:agent-runtime`；现有模块 include 保持不变。 |
| T-02 | Config | Infra | 新建 `agent-runtime/build.gradle.kts`；按设计稿声明 Android library、Kotlin Android、serialization 插件；补齐 runtime 所需依赖。 | `agent-runtime/build.gradle.kts` | `app/build.gradle.kts`, `composebase/build.gradle.kts`, `docs/.asc_task/module_refactor/tech_design.md` | - | M | ~40 lines | `agent-runtime` 可作为 Android library 被 `app` 引用；依赖列表覆盖 `h`、`s3ss10n`、`okhttp`、`serialization`、`coroutines`。 |
| T-03 | Config | Infra | 新建 `agent-runtime/src/main/AndroidManifest.xml`；仅保留 library module 所需最小声明；不注册组件。 | `agent-runtime/src/main/AndroidManifest.xml` | `agent-runtime/build.gradle.kts` | - | L | ~5 lines | Manifest 存在；package/namespace 与模块配置一致；未额外声明 Activity、Service、Receiver。 |
| T-04 | Config | Config | 修改 `app/build.gradle.kts`；新增 `implementation(project(":agent-runtime"))`；删除迁入 runtime 的依赖；保留 `composebase`、`h`、`ipc`。 | `app/build.gradle.kts` | `app/build.gradle.kts`, `agent-runtime/build.gradle.kts`, `docs/.asc_task/module_refactor/tech_design.md` | - | M | ~20 lines | `app` 直接依赖 `agent-runtime`；runtime 专属依赖从 `app` 移除；现有基础模块依赖保持存在。 |
| T-05 | Config | Config | 修改 `composebase/build.gradle.kts`；补齐承接 `ui/infra` 所需的 Compose/Lifecycle/视觉效果依赖；禁止新增对 `app` 的依赖。 | `composebase/build.gradle.kts` | `composebase/build.gradle.kts`, `docs/.asc_task/module_refactor/tech_design.md` | - | M | ~20 lines | `composebase` 具备承载 `ui/infra` 的依赖集合；文件中不存在对 `:app` 的依赖。 |

### Feature F-02: runtime shared settings 契约层

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-06 | Contracts | Contract | 新增 runtime shared settings DTO；定义 `RuntimeLlmConfig`、`RuntimeMcpServer`、`RuntimeMcpTool`、`RuntimeCustomTool`、`RuntimeBuiltinToolSetting`、`RuntimeCustomToolValidation`。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | `docs/.asc_task/module_refactor/tech_design.md`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt` | - | M | ~70 lines | 6 个共享 DTO 完整存在；字段覆盖设计稿；不再依赖 `app/repo` 包。 |
| T-07 | Contracts | Contract | 新增 `RuntimeSettingsGateway` 接口；完整声明 runtime 读取/写入配置所需的方法签名。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeSettingsGateway.kt` | `docs/.asc_task/module_refactor/tech_design.md`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | M | ~30 lines | 接口方法集合与设计稿一致；返回值与参数全部使用 runtime shared models。 |
| T-08 | Infra | Infra | 新增 `RuntimeEnvironment`；提供 `install()`、`requireSettingsGateway()`、`clearForTest()`；作为 runtime 全局 gateway 安装点。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeSettingsGateway.kt`, `docs/.asc_task/module_refactor/tech_design.md` | - | L | ~25 lines | `RuntimeEnvironment` 可保存一个已安装 gateway；未安装时 `requireSettingsGateway()` 会明确失败。 |

### Feature F-07: 预创建目标迁移目录

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-78 | Infra | Config | 新建 `mkdir_targets.sh` 脚本并执行；用 `mkdir -p` 预创建 `composebase` 与 `agent-runtime` 下承接机械迁移所需的全部目录；脚本保留在任务目录下供后续复用。 | `docs/.asc_task/module_refactor/mkdir_targets.sh` | `docs/.asc_task/module_refactor/plan.md`, `composebase/src/main/java`, `agent-runtime/src/main/java` | - | L | ~20 lines | 脚本存在且可执行；`composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra` 及其子目录、`agent-runtime/src/main/java/com/niki914/nexus/agentic/chat` 及其子目录均已存在；不改动任何源码文件。 |

### Feature F-03: app/repo 接入与模型下沉收口

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-09 | Infra | Logic | 新增 `XRepoRuntimeGateway` 适配器；把 `XRepo` 暴露成 `RuntimeSettingsGateway`；完成 DTO 映射与缓存操作透传。 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoRuntimeGateway.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeSettingsGateway.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | H | ~90 lines | `XRepoRuntimeGateway` 覆盖 gateway 的全部方法；不引入 runtime 到 app 的反向实现依赖。 |
| T-10 | Logic | Logic | 修改 `LocalSettingsCodec.kt`；把旧 repo models import 与函数签名切到 runtime shared models；保持序列化字段与存储 key 不变。 | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | M | ~40 lines | `LocalSettingsCodec` 只依赖 runtime shared models；编码解码结果与迁移前兼容。 |
| T-11 | Logic | Logic | 修改 `XRepo.kt`；用 runtime shared models 替代 `XRepoModels.kt`；保留现有对外 API 名称与语义。 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | H | ~80 lines | `XRepo` 编译期不再依赖 `XRepoModels.kt`；`llm()`、`mcp.*`、`customTools.*`、`builtinTools.*` 的语义保持一致。 |
| T-12 | Infra | Infra | 删除旧的 `XRepoModels.kt`；避免共享模型在 `app/repo` 和 `agent-runtime` 双份存在。 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt` | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | L | ~0 lines | `XRepoModels.kt` 已删除；工程内无文件继续引用该路径。 |
| T-13 | Infra | Infra | 修改 `App.kt`；在主进程启动时安装 `RuntimeEnvironment.install(XRepoRuntimeGateway())`。 | `app/src/main/java/com/niki914/nexus/agentic/app/App.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/App.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoRuntimeGateway.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt` | - | L | ~10 lines | `App.onCreate()` 中完成 runtime gateway 安装；不影响既有初始化顺序。 |
| T-14 | Infra | Infra | 修改 `Entrance.kt`；在宿主进程 `XRepo.init(ctx)` 后安装 `RuntimeEnvironment.install(XRepoRuntimeGateway())`。 | `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt` | `app/src/main/java/a0/a0/a0/a0/a0/a0/Entrance.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoRuntimeGateway.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt` | - | M | ~10 lines | Hook 进程启动后 runtime gateway 可用；安装时机位于 `XRepo.init(ctx)` 之后。 |
| T-15 | Logic | Logic | 修改 `ConfigureState.kt`；把 `LlmConfig` import 切换到 runtime shared models；保持 ViewModel 读写 `XRepo.llm()` 的方式不变。 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | L | ~5 lines | `ConfigureState.kt` 不再 import `com.niki914.nexus.agentic.repo.LlmConfig`；页面逻辑不变。 |
| T-16 | Logic | Logic | 修改 `McpSettingsState.kt`；把 `McpServer` import 切换到 runtime shared models；保持页面内 `toItem()` 与 `toRepo()` 转换逻辑不变。 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsState.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsState.kt`, `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | L | ~5 lines | `McpSettingsState.kt` 只引用 runtime `McpServer`；现有转换函数与存储调用保持一致。 |
| T-17 | Logic | Logic | 修改 `CustomToolsSettingsContent.kt`；把 `CustomTool` import 切换到 runtime shared models；保持 UI 展示与 item 映射逻辑不变。 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | L | ~5 lines | `CustomToolsSettingsContent.kt` 不再引用 repo 包下的 `CustomTool`；UI item 映射字段保持不变。 |

### Feature F-04: UI Infra 物理迁移到 composebase

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-18 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-19 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-20 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-21 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-22 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidViewportAvoidance.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidViewportAvoidance.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidViewportAvoidance.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidViewportAvoidance.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-23 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-24 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-25 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-26 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-27 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-28 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggleStateMachine.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggleStateMachine.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggleStateMachine.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggleStateMachine.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-29 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-30 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextCard.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextCard.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextCard.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextCard.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-31 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextItem.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextItem.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextItem.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextItem.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-32 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingNavigationItem.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingNavigationItem.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingNavigationItem.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingNavigationItem.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-33 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsGroupCard.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsGroupCard.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsGroupCard.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsGroupCard.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-34 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-35 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsToggleListItemCard.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsToggleListItemCard.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsToggleListItemCard.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsToggleListItemCard.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-36 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingToggleItem.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingToggleItem.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingToggleItem.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingToggleItem.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-37 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-38 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-39 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-40 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-41 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-42 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-43 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Navigator.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Navigator.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Navigator.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Navigator.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-44 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-45 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/PageViewModel.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/PageViewModel.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/PageViewModel.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/PageViewModel.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-46 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/preview/SettingsInfraPreview.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/preview/SettingsInfraPreview.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/preview/SettingsInfraPreview.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/preview/SettingsInfraPreview.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |
| T-47 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt` 迁到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt`；保持 `package com.niki914.nexus.agentic.app.ui.infra...` 不变；不要 rename 符号。 | `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt` | `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt`, `composebase/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `composebase`；package 声明未改名；AS 自动修正必要 import/path。 |

### Feature F-05: Chat Runtime 物理迁移到 agent-runtime

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-48 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-49 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-50 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-51 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-52 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-53 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-54 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-55 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-56 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-57 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinTool.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinTool.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinTool.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinTool.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-58 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolExecutor.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolExecutor.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolExecutor.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolExecutor.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-59 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-60 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-61 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-62 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/NotifyBuiltin.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/NotifyBuiltin.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/NotifyBuiltin.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/NotifyBuiltin.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-63 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/RunCommandBuildin_WIP_SAFE.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/RunCommandBuildin_WIP_SAFE.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/RunCommandBuildin_WIP_SAFE.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/RunCommandBuildin_WIP_SAFE.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-64 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolExecutor.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolExecutor.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolExecutor.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolExecutor.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-65 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-66 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-67 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpInterceptorHttpEngine.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpInterceptorHttpEngine.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpInterceptorHttpEngine.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpInterceptorHttpEngine.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-68 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandRunner.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandRunner.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandRunner.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandRunner.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-69 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandSafetyPolicy.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandSafetyPolicy.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandSafetyPolicy.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandSafetyPolicy.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-70 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/LlmStreamEventMapper.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/LlmStreamEventMapper.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/LlmStreamEventMapper.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/LlmStreamEventMapper.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |
| T-71 | Move | Infra | 【用户-AS】在 Android Studio 中执行 Move；把 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/ToolEventFormatter.kt` 迁到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/ToolEventFormatter.kt`；保持 `package com.niki914.nexus.agentic.chat...` 不变；不要顺手改类名或拆文件。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/ToolEventFormatter.kt` | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/ToolEventFormatter.kt`, `agent-runtime/build.gradle.kts` | - | L | ~moved file | 文件物理位置迁到 `agent-runtime`；package 声明未改名；迁移仅限路径变化。 |

### Feature F-06: 迁移后 runtime 源码去 app 依赖

| ID | 阶段 | 类型 | 任务详情（含伪代码签名与实现步骤） | 目标文件 | 视野（依赖文件） | 匹配 Skill | 复杂度 | 预估规模 | 验收标准 (AC) |
|:---|:-----|:-----|:-------------------------------|:---------|:--------------|:-----------|:-------|:---------|:-------------|
| T-72 | Logic | Logic | 修改迁移后的 `LLMController.kt`；移除 `XRepo` 直接依赖；通过 `RuntimeEnvironment.requireSettingsGateway()` 读取 llm、mcp、custom、builtin 配置。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | H | ~80 lines | `LLMController.kt` 中不再 import `XRepo`；刷新配置路径全部经由 gateway。 |
| T-73 | Logic | Logic | 修改迁移后的 `BuiltinToolSettingsManager.kt`；把 builtin 设置的读取与写回切到 gateway。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt` | - | M | ~30 lines | `BuiltinToolSettingsManager.kt` 中不再依赖 `XRepo` 或 repo model；load/setEnabled 均走 gateway。 |
| T-74 | Logic | Logic | 修改迁移后的 `CustomToolManager.kt`；把自定义工具列表、保存、删除、启停切到 gateway；保留校验语义。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | H | ~60 lines | `CustomToolManager.kt` 不再依赖 `XRepo`；load/saveAll/delete/setEnabled 全部通过 gateway；校验失败返回值保持兼容。 |
| T-75 | Logic | Logic | 修改迁移后的 `CreateCustomToolBuiltin.kt`；去掉对 `XRepo` 的直接调用；改成通过 gateway 保存自定义工具。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | M | ~30 lines | `CreateCustomToolBuiltin.kt` 中不再 import `XRepo`；保存 custom tool 的结果仍能回传给 builtin tool 调用方。 |
| T-76 | Logic | Logic | 修改迁移后的 `McpDiscoveryCacheStore.kt`；发现到的 tools 缓存写入改经由 gateway；不再直接调用 `XRepo`。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/RuntimeEnvironment.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | M | ~30 lines | `McpDiscoveryCacheStore.kt` 中不存在 `XRepo` 直接访问；缓存持久化调用通过 gateway 完成。 |
| T-77 | Logic | Logic | 修改迁移后的 `ToolManager.kt`；把共享模型 import 调整到 runtime settings models；保持 tool 装配顺序与会话绑定行为不变。 | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` | `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`, `agent-runtime/src/main/java/com/niki914/nexus/agentic/runtime/settings/model/RuntimeSettingsModels.kt` | - | L | ~15 lines | `ToolManager.kt` 不再依赖 repo model 包；对外行为与原来一致。 |

## 4. 实施步骤 (Steps per Task)

### T-01: 新模块与 Gradle 骨架 / settings.gradle.kts

- [ ] 定位 `settings.gradle.kts` 的 module include 区域。
- [ ] 插入 `include(":agent-runtime")`，不重排既有模块。
- [ ] 核对文件中仅新增目标模块声明。

### T-02: 新模块与 Gradle 骨架 / build.gradle.kts

- [ ] 创建 `agent-runtime/build.gradle.kts`。
- [ ] 填写 plugins、android namespace、dependencies。
- [ ] 对照设计稿检查版本与依赖名称。

### T-03: 新模块与 Gradle 骨架 / AndroidManifest.xml

- [ ] 创建 `src/main/AndroidManifest.xml`。
- [ ] 写入最小 library manifest 根节点。
- [ ] 检查未引入多余组件声明。

### T-04: 新模块与 Gradle 骨架 / build.gradle.kts

- [ ] 定位 `dependencies` 区域。
- [ ] 新增 `implementation(project(":agent-runtime"))`。
- [ ] 删除转移到 runtime 的第三方依赖。

### T-05: 新模块与 Gradle 骨架 / build.gradle.kts

- [ ] 定位 `composebase` 现有依赖列表。
- [ ] 补齐 Capsule、backdrop、haze、lifecycle-viewmodel-compose。
- [ ] 确认没有引入 `project(":app")`。

### T-06: runtime shared settings 契约层 / RuntimeSettingsModels.kt

- [ ] 创建 shared settings model 文件。
- [ ] 按设计稿迁移旧 DTO 结构。
- [ ] 核对字段默认值与旧模型一致。

### T-07: runtime shared settings 契约层 / RuntimeSettingsGateway.kt

- [ ] 创建 gateway 接口文件。
- [ ] 录入设计稿中的全部方法签名。
- [ ] 检查没有残留 `app/repo` 类型。

### T-08: runtime shared settings 契约层 / RuntimeEnvironment.kt

- [ ] 创建 `RuntimeEnvironment`。
- [ ] 加入 `@Volatile` gateway 持有字段。
- [ ] 实现安装、读取、测试清理入口。

### T-78: 预创建目标迁移目录 / mkdir_targets.sh

- [ ] 在 `docs/.asc_task/module_refactor/` 下创建 `mkdir_targets.sh`。
- [ ] 在脚本中显式列出 `composebase` 承接 `ui/infra` 的目录和 `agent-runtime` 承接 `chat` 的目录，并使用 `mkdir -p` 创建。
- [ ] 执行脚本，确认所有迁移目标目录已存在。

### T-09: app/repo 接入与模型下沉收口 / XRepoRuntimeGateway.kt

- [ ] 创建 `XRepoRuntimeGateway`。
- [ ] 逐个实现 gateway 方法并委托给 `XRepo`。
- [ ] 补齐 repo model 与 runtime model 的双向映射。

### T-10: app/repo 接入与模型下沉收口 / LocalSettingsCodec.kt

- [ ] 打开 `LocalSettingsCodec.kt`。
- [ ] 替换模型 import 与签名。
- [ ] 核对 JSON 字段名与旧实现一致。

### T-11: app/repo 接入与模型下沉收口 / XRepo.kt

- [ ] 替换顶部 import。
- [ ] 更新返回值与参数类型。
- [ ] 核对 builtin/custom/mcp 各子对象 API 行为保持不变。

### T-12: app/repo 接入与模型下沉收口 / XRepoModels.kt

- [ ] 确认 `XRepo.kt` 与 UI 文件已切换到新模型包。
- [ ] 删除 `XRepoModels.kt`。
- [ ] 全局搜索旧模型路径确认无残留引用。

### T-13: app/repo 接入与模型下沉收口 / App.kt

- [ ] 定位 `App.onCreate()`。
- [ ] 插入 runtime gateway 安装语句。
- [ ] 检查插入点在需要用到 runtime 之前。

### T-14: app/repo 接入与模型下沉收口 / Entrance.kt

- [ ] 定位 `XRepo.init(ctx)` 调用点。
- [ ] 在其后插入 runtime gateway 安装语句。
- [ ] 确认未改动宿主匹配与 Hook 初始化流程。

### T-15: app/repo 接入与模型下沉收口 / ConfigureState.kt

- [ ] 打开 `ConfigureState.kt`。
- [ ] 替换模型 import。
- [ ] 确认 `XRepo.llm()` 的调用点无需额外逻辑调整。

### T-16: app/repo 接入与模型下沉收口 / McpSettingsState.kt

- [ ] 打开 `McpSettingsState.kt`。
- [ ] 替换 `McpServer` import。
- [ ] 确认 `toItem()` / `toRepo()` 使用的新类型字段一致。

### T-17: app/repo 接入与模型下沉收口 / CustomToolsSettingsContent.kt

- [ ] 打开 `CustomToolsSettingsContent.kt`。
- [ ] 替换 `CustomTool` import。
- [ ] 核对 `toItem()` 所需字段全部可用。

### T-18: UI Infra 物理迁移到 composebase / ActionBarButton.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/ActionBarButton.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-19: UI Infra 物理迁移到 composebase / LiquidScreen.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreen.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-20: UI Infra 物理迁移到 composebase / LiquidScreenState.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenState.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-21: UI Infra 物理迁移到 composebase / LiquidScreenSwipeContent.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidScreenSwipeContent.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-22: UI Infra 物理迁移到 composebase / LiquidViewportAvoidance.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidViewportAvoidance.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/LiquidViewportAvoidance.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-23: UI Infra 物理迁移到 composebase / LiquidButton.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidButton.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-24: UI Infra 物理迁移到 composebase / LiquidSecretTextField.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-25: UI Infra 物理迁移到 composebase / LiquidTextField.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextField.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-26: UI Infra 物理迁移到 composebase / LiquidTextFieldContainer.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidTextFieldContainer.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-27: UI Infra 物理迁移到 composebase / LiquidToggle.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggle.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-28: UI Infra 物理迁移到 composebase / LiquidToggleStateMachine.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggleStateMachine.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidToggleStateMachine.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-29: UI Infra 物理迁移到 composebase / MaterialTintLiquidButton.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/MaterialTintLiquidButton.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-30: UI Infra 物理迁移到 composebase / SettingExpandableTextCard.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextCard.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextCard.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-31: UI Infra 物理迁移到 composebase / SettingExpandableTextItem.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextItem.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingExpandableTextItem.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-32: UI Infra 物理迁移到 composebase / SettingNavigationItem.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingNavigationItem.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingNavigationItem.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-33: UI Infra 物理迁移到 composebase / SettingsGroupCard.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsGroupCard.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsGroupCard.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-34: UI Infra 物理迁移到 composebase / SettingsListPageContent.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsListPageContent.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-35: UI Infra 物理迁移到 composebase / SettingsToggleListItemCard.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsToggleListItemCard.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsToggleListItemCard.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-36: UI Infra 物理迁移到 composebase / SettingToggleItem.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingToggleItem.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingToggleItem.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-37: UI Infra 物理迁移到 composebase / TintLiquidButton.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/TintLiquidButton.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-38: UI Infra 物理迁移到 composebase / DragGestureInspector.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/DragGestureInspector.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-39: UI Infra 物理迁移到 composebase / InteractiveHighlight.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/InteractiveHighlight.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-40: UI Infra 物理迁移到 composebase / LiquidInteractiveLayer.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveLayer.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-41: UI Infra 物理迁移到 composebase / LiquidInteractiveStyle.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/interaction/LiquidInteractiveStyle.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-42: UI Infra 物理迁移到 composebase / NavigationController.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/NavigationController.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-43: UI Infra 物理迁移到 composebase / Navigator.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Navigator.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Navigator.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-44: UI Infra 物理迁移到 composebase / Page.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/Page.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-45: UI Infra 物理迁移到 composebase / PageViewModel.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/PageViewModel.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/nav/PageViewModel.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-46: UI Infra 物理迁移到 composebase / SettingsInfraPreview.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/preview/SettingsInfraPreview.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/preview/SettingsInfraPreview.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-47: UI Infra 物理迁移到 composebase / G2Shapes.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt`。
- [ ] 执行 Refactor > Move 到 `composebase/src/main/java/com/niki914/nexus/agentic/app/ui/infra/shape/G2Shapes.kt` 对应目录。
- [ ] 确认 package 保持不变，仅接受机械性路径修正。

### T-48: Chat Runtime 物理迁移到 agent-runtime / ConversationJournal.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationJournal.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-49: Chat Runtime 物理迁移到 agent-runtime / ConversationTurnState.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/ConversationTurnState.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-50: Chat Runtime 物理迁移到 agent-runtime / LLMController.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-51: Chat Runtime 物理迁移到 agent-runtime / LlmModels.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LlmModels.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-52: Chat Runtime 物理迁移到 agent-runtime / LlmStreamEvent.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/LlmStreamEvent.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-53: Chat Runtime 物理迁移到 agent-runtime / PromptComposer.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/PromptComposer.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-54: Chat Runtime 物理迁移到 agent-runtime / SessionToolBinder.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/SessionToolBinder.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-55: Chat Runtime 物理迁移到 agent-runtime / ToolCallDispatcher.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolCallDispatcher.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-56: Chat Runtime 物理迁移到 agent-runtime / ToolManager.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-57: Chat Runtime 物理迁移到 agent-runtime / BuiltinTool.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinTool.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinTool.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-58: Chat Runtime 物理迁移到 agent-runtime / BuiltinToolExecutor.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolExecutor.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolExecutor.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-59: Chat Runtime 物理迁移到 agent-runtime / BuiltinToolRegistry.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolRegistry.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-60: Chat Runtime 物理迁移到 agent-runtime / BuiltinToolSettingsManager.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-61: Chat Runtime 物理迁移到 agent-runtime / CreateCustomToolBuiltin.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-62: Chat Runtime 物理迁移到 agent-runtime / NotifyBuiltin.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/NotifyBuiltin.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/NotifyBuiltin.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-63: Chat Runtime 物理迁移到 agent-runtime / RunCommandBuildin_WIP_SAFE.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/RunCommandBuildin_WIP_SAFE.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/RunCommandBuildin_WIP_SAFE.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-64: Chat Runtime 物理迁移到 agent-runtime / CustomToolExecutor.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolExecutor.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolExecutor.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-65: Chat Runtime 物理迁移到 agent-runtime / CustomToolManager.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-66: Chat Runtime 物理迁移到 agent-runtime / McpDiscoveryCacheStore.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-67: Chat Runtime 物理迁移到 agent-runtime / McpInterceptorHttpEngine.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpInterceptorHttpEngine.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpInterceptorHttpEngine.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-68: Chat Runtime 物理迁移到 agent-runtime / ShellCommandRunner.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandRunner.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandRunner.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-69: Chat Runtime 物理迁移到 agent-runtime / ShellCommandSafetyPolicy.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandSafetyPolicy.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/shell/ShellCommandSafetyPolicy.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-70: Chat Runtime 物理迁移到 agent-runtime / LlmStreamEventMapper.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/LlmStreamEventMapper.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/LlmStreamEventMapper.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-71: Chat Runtime 物理迁移到 agent-runtime / ToolEventFormatter.kt

- [ ] 在 Android Studio 中选中 `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/ToolEventFormatter.kt`。
- [ ] 执行 Refactor > Move 到 `agent-runtime/src/main/java/com/niki914/nexus/agentic/chat/agentic/stream/ToolEventFormatter.kt` 对应目录。
- [ ] 确认 package 不变，不做额外重构。

### T-72: 迁移后 runtime 源码去 app 依赖 / LLMController.kt

- [ ] 打开已迁移的 `LLMController.kt`。
- [ ] 替换 repo imports 为 runtime gateway/model imports。
- [ ] 将 refresh 相关配置读取改成 gateway 调用。

### T-73: 迁移后 runtime 源码去 app 依赖 / BuiltinToolSettingsManager.kt

- [ ] 打开 `BuiltinToolSettingsManager.kt`。
- [ ] 替换 load/setEnabled 的数据源。
- [ ] 确认返回值转换仍符合原调用方期望。

### T-74: 迁移后 runtime 源码去 app 依赖 / CustomToolManager.kt

- [ ] 打开 `CustomToolManager.kt`。
- [ ] 把数据读取和写入改到 gateway。
- [ ] 核对 validation 与 builtin 冲突检查逻辑未丢失。

### T-75: 迁移后 runtime 源码去 app 依赖 / CreateCustomToolBuiltin.kt

- [ ] 打开 `CreateCustomToolBuiltin.kt`。
- [ ] 替换保存入口为 gateway。
- [ ] 检查错误消息映射保持原样。

### T-76: 迁移后 runtime 源码去 app 依赖 / McpDiscoveryCacheStore.kt

- [ ] 打开 `McpDiscoveryCacheStore.kt`。
- [ ] 把保存 discovered tools 的分支改成 gateway 调用。
- [ ] 确认 URL/headers/tool list 的映射字段不变。

### T-77: 迁移后 runtime 源码去 app 依赖 / ToolManager.kt

- [ ] 打开 `ToolManager.kt`。
- [ ] 替换共享模型 import。
- [ ] 检查构造出来的 tool 列表顺序未改变。

## 5. 审查修正记录

### Round 1: PM
- 原始版本遗漏了模型下沉对 `LocalSettingsCodec.kt` 与 3 个 UI 状态文件 import 的影响；现已补入 F-03。
- 明确把用户在 AS 的机械 Move 与 ASC 的代码/Gradle 改造拆成不同 Task，避免执行期互相等待。
- 所有 Feature 都已分配到 Batch，没有孤立需求点。

### Round 2: 架构师
- 坚持 1 Task = 1 目标文件；`ui/infra` 与 `chat` 的物理迁移被逐文件展开，没有目录级 God Task。
- `F-06` 被放到 `F-05` 之后，确保 runtime 去耦修正发生在文件已经位于 `agent-runtime` 的上下文中。
- `F-04` 与 `F-05` 各自独占 Batch，符合大体量迁移不得跨依赖层聚合的规则。

### Round 3: 结对伙伴
- 所有任务都给出了明确目标文件、依赖视野和可执行 AC；文档中没有保留占位文本。
- 对手工 AS 迁移任务，实施步骤统一限制为机械 Move，显式禁止顺手 rename，避免把逻辑改造掺进去。
- 新增 `B-07` 作为辅助 Batch，在人工迁移前先创建目录树，减少 AS Move 时的目录准备成本。
- `B-02`、`B-07`、`B-04`、`B-05` 的顺序已经覆盖 gateway 先建、目录预创建、chat 后迁、runtime 再去耦的依赖链。
