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
- At the time this recovery prompt was written, ASC-01 had finished Phase 2 and was waiting for explicit approval before implementation.

## Exact files for ASC-01

If the active ASC is `asc_01_strings_baseline`, read:

- `docs/.asc_task/asc_01_strings_baseline/progress.md`
- `docs/.asc_task/asc_01_strings_baseline/tech_survey.md`
- `docs/.asc_task/asc_01_strings_baseline/tech_design.md`
- `docs/.asc_task/asc_01_strings_baseline/plan.md`

### ASC-01 implementation boundary

- In scope:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
- Out of scope by default:
  - Kotlin source changes
  - page structure changes
  - global string key rename
  - English resource rebuild

## Negative examples

Bad recovery:

- “I remember we wanted cleaner naming, so I will rename all `nexus_*` keys now.”
- “Phase 2 is done, so I can start editing immediately.”
- “The deleted PRD probably had the intended navigation, I should reconstruct it.”

Good recovery:

- “I will read `AGENTS.md`, `ASC_may_25.md`, and the active ASC documents before deciding anything.”
- “Implementation is next, but I still need explicit user approval before Phase 3.”
- “This ASC only changes resource files, so Kotlin changes stay out of scope unless verification disproves that.”

## Recovery response template

After reading the required files, respond to the user with a short Chinese status update:

1. state which ASC is active
2. state which phase is currently active
3. state the exact implementation boundary
4. ask only if a real ambiguity remains

Do not dump the entire recovery chain to the user.

当前进度：asc-01 phase2 finished。即将进入 3。回答语言：中文