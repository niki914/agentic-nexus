# ASC Compression Recovery Prompt

Read this file immediately after context compression if you only remember that work was done here but do not remember the details.

## Core objective

Recover the exact execution state with minimal drift, then continue the current ASC workflow without inventing missing context.

## Mandatory first actions

- `asc-director`
- `nexus-lookup`

## Mandatory files to read first

Always read these files in this order:

1. `AGENTS.md`
2. `ASC_may_25.md`
3. The active ASC progress file under `docs/.asc_task/*/progress.md`

If there is an active ASC, also read:

4. `docs/.asc_task/<active_asc>/tech_survey.md`
5. `docs/.asc_task/<active_asc>/tech_design.md`
6. `docs/.asc_task/<active_asc>/plan.md`

If the user has already pointed at a specific file, class, or page, read that exact file before broad exploration.

## How to find the active ASC

1. Inspect `docs/.asc_task/`
2. Prefer the ASC folder whose `progress.md` says the phase is not finished yet
3. If all existing ASC folders are complete, use `ASC_may_25.md` to identify the next pending ASC in the queue
4. Do not trust memory over the files

## Compression timing A

Use this branch when compression happened after an ASC finished Phase 2 and was about to enter Phase 3.

### Recovery steps

1. Read `progress.md`
2. Confirm the current phase says implementation is next
3. Read `tech_survey.md`, `tech_design.md`, and `plan.md`
4. Re-state the execution boundary to yourself:
   - what files may change
   - what files must not change
   - whether Kotlin changes are in scope
5. Wait for explicit user approval before implementation

### Do not do

- Do not infer permission to implement just because the plan is complete
- Do not skip re-reading `plan.md`
- Do not widen scope beyond the current ASC
- Do not silently replace the agreed migration strategy with a cleaner-looking one

## Compression timing B

Use this branch when compression happened after one ASC was completed and accepted, and work is about to move to the next ASC.

### Recovery steps

1. Read `ASC_may_25.md`
2. Read the most recently completed ASC `progress.md`
3. Identify the next ASC from the queue in `ASC_may_25.md`
4. Treat the next ASC as a new planning cycle
5. Invoke the skills required for the next phase instead of assuming implementation is next

### Do not do

- Do not reopen completed ASC work unless the user explicitly asks
- Do not drag leftover “cleanup” into the next ASC without approval
- Do not assume old PRD files are authoritative

## Repository-specific rules that must survive compression

- Source code is the truth. If docs and code conflict, trust code.
- The current UI navigation structure is final for this cycle. Do not reintroduce deleted PRD navigation.
- `ASC_may_25.md` is the active project-level constraint file for UI and ASC ordering.
- For strings:
  - Do not add new `nexus_*` keys
  - UI text uses `ui_*` domains
  - module-facing visible text uses module domains such as `mcp_*`, `custom_tool_*`, `builtin_tool_*`
- Do not add page ViewModels for purely presentational pages just for symmetry.
- Use Chinese for user-facing communication, plans, todos, and code comments unless the file is explicitly an agent-facing prompt.
- Do not compile or run the app unless the user explicitly asks.
- Do not treat `docs/.asc_task/` as proof that code is already implemented.

## Current known project facts

- `UI-PRD.md` was intentionally deleted because it was outdated and misleading.
- `ASC_may_25.md` replaced it as the current execution constraint summary.
- ASC-01 is `asc_01_strings_baseline`.
- ASC-01 has been implemented, reviewed by the user, and accepted.
- ASC-01 removed all `nexus_*` string resource keys from `app/src/main`.
- ASC-02 is `asc_02_onboarding_form`.
- ASC-02 has been implemented through Phase 3.
- ASC-02 turned `ConfigurePage` into a real provider-aware onboarding form page.
- The next ASC in the queue is ASC-03, which focuses on de-shelling `DonePage`.

## ASC-01 completion files

If the user reopens ASC-01 or asks for verification, read:

- `docs/.asc_task/asc_01_strings_baseline/progress.md`
- `docs/.asc_task/asc_01_strings_baseline/tech_survey.md`
- `docs/.asc_task/asc_01_strings_baseline/tech_design.md`
- `docs/.asc_task/asc_01_strings_baseline/plan.md`

### ASC-01 final boundary

- In scope:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - Kotlin references that had to be updated to match the renamed resource keys
- Out of scope:
  - page structure changes
  - business logic changes
  - English resource rebuild

## ASC-02 completion files

If the user reopens ASC-02 or asks for verification, read:

- `docs/.asc_task/asc_02_onboarding_form/progress.md`
- `docs/.asc_task/asc_02_onboarding_form/tech_survey.md`
- `docs/.asc_task/asc_02_onboarding_form/tech_design.md`
- `docs/.asc_task/asc_02_onboarding_form/plan.md`

### ASC-02 final boundary

- In scope:
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/NexusPages.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/content/ConfigurePageContent.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ConfigureState.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/nexus/model/ProviderSpec.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/LiquidSecretTextField.kt`
  - `app/src/main/java/com/niki914/nexus/agentic/mod/SettingModels.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values/colors.xml`
  - `docs/.asc_task/asc_02_onboarding_form/progress.md`
- Out of scope:
  - `DonePage` de-shelling
  - `ChatOnly` path fixes
  - provider screen background skinning
  - `values-en/strings.xml` rebuild

## ASC-03 recovery entry

If ASC-02 is complete enough to leave behind, and the user wants to proceed, treat ASC-03 as the next active work item.

### ASC-03 recovery steps

1. Read `ASC_may_25.md`
2. Read `docs/.asc_task/asc_02_onboarding_form/progress.md`
3. Confirm ASC-02 implementation is already landed
4. Identify ASC-03 from the queue as the next planning target
5. Start ASC-03 at Phase 0 rather than assuming any design or implementation already exists

### ASC-03 initial constraints

- Goal: give `DonePage` its own content and visual structure instead of reusing `ConfigurePageContent`
- Navigation structure stays as-is
- `DonePage` remains a presentational page; do not add a page ViewModel just for symmetry
- Do not bundle `ChatOnly` path fixes or settings work into ASC-03 unless the user explicitly expands scope

## Negative examples

Bad recovery:

- “I remember we wanted cleaner naming, so I will rename all `nexus_*` keys now.”
- “Phase 2 is done, so I can start editing immediately.”
- “The deleted PRD probably had the intended navigation, I should reconstruct it.”
- “ASC-02 is implemented, so ASC-03 coding must be next.”

Good recovery:

- “I will read `AGENTS.md`, `ASC_may_25.md`, and the active ASC documents before deciding anything.”
- “Implementation is next, but I still need explicit user approval before Phase 3.”
- “This ASC only changes resource files, so Kotlin changes stay out of scope unless verification disproves that.”
- “ASC-02 is already landed, so ASC-03 should begin from Phase 0 planning unless the files show otherwise.”

## Recovery response template

After reading the required files, respond to the user with a short Chinese status update:

1. state which ASC is active
2. state which phase is currently active
3. state the exact implementation boundary
4. ask only if a real ambiguity remains

Do not dump the entire recovery chain to the user.

当前进度：asc-02 phase3 finished；下一步进入 asc-03 phase0。回答语言：中文
