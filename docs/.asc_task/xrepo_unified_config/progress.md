# XRepo Unified Config Progress

## Current Phase
Phase 3 - Ready for Batch Implementation

## Project
- Name: `xrepo_unified_config`
- Goal: 新增 `XRepo` 强类型统一配置读写门面，并分批迁移 MCP、Tool、Runtime、UI 调用方。

## Confirmed Decisions
| 决策 | 选择 | 说明 |
|:-----|:-----|:-----|
| 底层读写 | A | `XRepo` 直接调用 `XService`，内部加进程内 `Mutex` |
| API 组织 | A | 使用子域 API，避免 `XRepo` 本体膨胀 |
| 迁移顺序 | A | 先 `XRepo + tests`，再 MCP/cache，再 tool，再 UI/runtime |

## Files
| 文件 | 状态 |
|:-----|:-----|
| `tech_survey.md` | Completed v1.0 |
| `tech_design.md` | Completed v1.0 |
| `plan.md` | Completed v1.0 |

## Phase Checklist
| 阶段 | 状态 |
|:-----|:-----|
| Phase 0 - 代码摸底 | Completed |
| Phase 0 - 决策确认 | Completed |
| Phase 0 - 技术调研文档 | Completed |
| Phase 1 - 架构设计 | Completed |
| Phase 2 - 任务规划 | Completed |
| Phase 3 - 分批实现 | Pending |

## Context
- `XRepo` 包位置：`com.niki914.nexus.agentic.repo`
- 底层保留：`XService`、`XIpcBridge`、`XIpcStoreRepository`
- 第一批不动：`App.kt` 调试 seed 逻辑
- 迁移重点：隐藏 `LocalSettings.props`、`JsonObject`、`JsonArray`，外部只用强类型 API

## Batch Pause Mode
Manual validation after each Batch

## Batch Progress
| Batch ID | Feature | 状态 | 前置 Batch |
|:---------|:--------|:-----|:-----------|
| B-01 | F-01 | Completed | - |
| B-02 | F-02 | Completed | B-01 |
| B-03 | F-03 | Completed | B-01 |
| B-04 | F-04 | Pending | B-02, B-03 |

## Task Progress
| Task ID | Batch ID | 目标文件 | 状态 |
|:--------|:---------|:---------|:-----|
| T-01 | B-01 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt` | Completed |
| T-02 | B-01 | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsStore.kt` | Completed |
| T-03 | B-01 | `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt` | Completed |
| T-04 | B-01 | `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt` | Completed |
| T-05 | B-01 | `app/src/test/java/com/niki914/nexus/agentic/repo/LocalSettingsCodecTest.kt` | Completed |
| T-06 | B-01 | `app/src/test/java/com/niki914/nexus/agentic/repo/XRepoTest.kt` | Completed |
| T-07 | B-02 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt` | Completed |
| T-08 | B-02 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt` | Completed |
| T-09 | B-02 | `app/src/test/java/com/niki914/nexus/agentic/chat/v2/ToolManagerTest.kt` | Completed |
| T-10 | B-02 | `app/src/test/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStoreTest.kt` | Completed |
| T-11 | B-03 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt` | Completed |
| T-12 | B-03 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt` | Completed |
| T-13 | B-03 | `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt` | Completed |
| T-14 | B-03 | `app/src/test/java/com/niki914/nexus/agentic/chat/v2/CommandToolManagerTest.kt`, `app/src/test/java/com/niki914/nexus/agentic/chat/v2/BuiltinToolSettingsManagerTest.kt`, `app/src/test/java/com/niki914/nexus/agentic/chat/v2/CreateCommandToolBuiltinTest.kt` | Completed |
| T-15 | B-04 | `app/src/main/java/com/niki914/nexus/agentic/chat/LLMController.kt` | Pending |
| T-16 | B-04 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt` | Pending |
| T-17 | B-04 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsState.kt` | Pending |
| T-18 | B-04 | `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/CustomToolsSettingsContent.kt` | Pending |
| T-19 | B-04 | `app/src/main/java/com/niki914/nexus/agentic/app/MainActivity.kt` | Pending |
| T-20 | B-04 | `app/src/test/java/com/niki914/nexus/agentic/app/ui/nexus/model/McpSettingsViewModelTest.kt` | Pending |

## Batch Implementation Notes

### B-01
- Status: Completed
- Modified files:
  - `app/src/main/java/com/niki914/nexus/agentic/repo/XRepoModels.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsStore.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/repo/LocalSettingsCodec.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/repo/XRepo.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/repo/LocalSettingsCodecTest.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/repo/XRepoTest.kt`
- Verification:
  - `./gradlew :app:testDebugUnitTest --tests 'com.niki914.nexus.agentic.repo.*'` passed.
  - IDE diagnostics for edited repo files returned no diagnostics.
- Notes:
  - 初次验证发现 `XRepoTest` 使用 `android.test.mock.MockContext` 导致 JVM test compile 失败；已改为 `ContextWrapper(null)` 测试 Context，并保持 `LocalSettingsStore` 的 non-null `Context` 签名与设计一致。

### B-03
- Status: Completed
- Modified files:
  - `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/custom/CustomToolManager.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/BuiltinToolSettingsManager.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/buildin/impl/CreateCustomToolBuiltin.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/chat/v2/CommandToolManagerTest.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/chat/v2/BuiltinToolSettingsManagerTest.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/chat/v2/CreateCommandToolBuiltinTest.kt`
- Verification:
  - `./gradlew :app:testDebugUnitTest --tests 'com.niki914.nexus.agentic.chat.v2.CustomToolManagerTest' --tests 'com.niki914.nexus.agentic.chat.v2.BuiltinToolSettingsManagerTest' --tests 'com.niki914.nexus.agentic.chat.v2.CreateCustomToolBuiltinTest'` passed.
  - IDE diagnostics for edited production/test files returned no diagnostics.
  - Grep confirmed B-03 target production files no longer reference `XService`, `getLocalSettings`, `putLocalSettings`, or raw custom/builtin JSON builder helpers.
- Notes:
  - 现有测试文件位于 `app/src/test/java/com/niki914/nexus/agentic/chat/v2/`，不是计划中的 `chat/agentic/custom/CustomToolManagerTest.kt` 路径；本批按实际测试布局扩展。
  - `BuiltinToolRegistry.default()` 当前包含 `notify`，本批测试断言已按源码事实更新。
  - Controller 合并验收已通过，包含 B-02/B-03 目标测试同跑与 IDE diagnostics 检查。

### B-02
- Status: Completed
- Modified files:
  - `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/ToolManager.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStore.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/chat/v2/ToolManagerTest.kt`
  - `app/src/test/java/com/niki914/nexus/agentic/chat/agentic/mcp/McpDiscoveryCacheStoreTest.kt`
- Verification:
  - `./gradlew :app:testDebugUnitTest --tests 'com.niki914.nexus.agentic.chat.v2.ToolManagerTest' --tests 'com.niki914.nexus.agentic.chat.agentic.mcp.McpDiscoveryCacheStoreTest' --tests 'com.niki914.nexus.agentic.chat.v2.CustomToolManagerTest' --tests 'com.niki914.nexus.agentic.chat.v2.BuiltinToolSettingsManagerTest' --tests 'com.niki914.nexus.agentic.chat.v2.CreateCustomToolBuiltinTest'` passed.
  - IDE diagnostics for B-02/B-03 edited production/test files returned no diagnostics.
  - Grep confirmed `McpDiscoveryCacheStore.kt` no longer references `XService`, `getLocalSettings`, or `putLocalSettings`.
- Notes:
  - 并行实现时 B-02 曾因检测到 B-03 文件变更暂停；确认来源为并行 B-03 后继续收尾。
  - Controller 验收发现 `ToolManager.resolve(settings)` 一度丢失 builtin `defaultEnabled` 兼容行为；已补派 B-02 修复，并新增测试覆盖空 flags 使用默认启用与显式 false 覆盖默认启用。
