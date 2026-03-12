# Docs Structure Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** reorganize `docs/plans` into topic folders with fixed filenames, while cleaning `docs/guides` for clarity and sanitizing user-facing paths.

**Architecture:** keep `docs/guides` as a flat guide directory, but normalize each guide's structure and path examples. Move `docs/plans` from suffix-based filenames to `docs/plans/<date-topic>/<type>.md`, update all in-repo references, and do the migration in batches so broken links are caught quickly.

**Tech Stack:** Markdown docs, PowerShell file moves, ripgrep reference scans, existing JUnit tests that assert doc contents/paths.

---

### Task 1: Migrate the actively maintained 2026-03-12 plan sets to folder layout

**Files:**
- Move: `docs/plans/2026-03-12-embedded-game-mcp-cleanup-plan.md`
- Move: `docs/plans/2026-03-12-embedded-game-mcp-cleanup-checklist.md`
- Move: `docs/plans/2026-03-12-embedded-game-mcp-cleanup-impl.md`
- Move: `docs/plans/2026-03-12-server-hosted-embedded-game-mcp-plan.md`
- Move: `docs/plans/2026-03-12-server-hosted-embedded-game-mcp-checklist.md`
- Move: `docs/plans/2026-03-12-server-hosted-embedded-game-mcp-impl.md`
- Move: `docs/plans/2026-03-12-playwright-style-ui-automation-checklist.md`
- Move: `docs/plans/2026-03-12-playwright-style-ui-automation-impl.md`
- Modify: moved file internal references
- Modify: `README.md`

**Step 1: Create topic directories**

Create:
- `docs/plans/2026-03-12-embedded-game-mcp-cleanup/`
- `docs/plans/2026-03-12-server-hosted-embedded-game-mcp/`
- `docs/plans/2026-03-12-playwright-style-ui-automation/`

**Step 2: Move files to fixed names**

Map:
- `*-plan.md` -> `plan.md`
- `*-checklist.md` -> `checklist.md`
- `*-impl.md` -> `impl.md`

**Step 3: Update first-order references**

Update README and the moved docs so links use folder paths.

**Step 4: Run a reference scan**

Run: `rg -n "2026-03-12-.*(plan|checklist|impl)\\.md" README.md docs Mod\\src\\test\\java Server\\src\\test\\java`

Expected: no stale references to the old 2026-03-12 filenames outside historical text.

### Task 2: Migrate the current primary-path 2026-03-11 game-mcp plan set

**Files:**
- Move: `docs/plans/2026-03-11-game-mcp-design.md`
- Move: `docs/plans/2026-03-11-game-mcp-plan.md`
- Move: `docs/plans/2026-03-11-game-mcp-checklist.md`
- Move: `docs/plans/2026-03-11-game-mcp-impl.md`
- Modify: moved docs and any cross-links

**Step 1: Create the topic directory**

Create:
- `docs/plans/2026-03-11-game-mcp/`

**Step 2: Move files to fixed names**

Map to:
- `design.md`
- `plan.md`
- `checklist.md`
- `impl.md`

**Step 3: Update references**

Update current docs that point to these files.

**Step 4: Run a reference scan**

Run: `rg -n "2026-03-11-game-mcp-(design|plan|checklist|impl)\\.md" README.md docs Mod\\src\\test\\java Server\\src\\test\\java`

Expected: only historical prose should remain.

### Task 3: Normalize active guides and sanitize paths

**Files:**
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Modify: `docs/guides/2026-03-11-agent-prompt-templates.md`
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Modify: `README.md`

**Step 1: Standardize active guide structure**

Use a consistent section order where applicable:
- Purpose
- Workflow / Setup
- Verification / First Call
- Notes / Failure Handling

**Step 2: Sanitize user-facing paths**

Replace absolute machine paths with:
- `<repo>\...`
- `TestMod\...`
- `Mod\build\...`

**Step 3: Remove stale wording**

Keep only current primary-path wording in active guides.

### Task 4: Update doc-path assertions in tests

**Files:**
- Modify: `Mod/src/test/java/dev/vfyjxf/mcp/bootstrap/EmbeddedModDevMcpStdioMainTest.java`
- Modify any server-side doc assertion tests that reference moved plan files

**Step 1: Adjust tests to new plan directory layout**

Where tests assert doc paths or contents, point them at the new folder structure.

**Step 2: Run focused test verification**

Run:
- `.\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon`
- `.\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon`

Expected: pass.

### Task 5: Continue migration batch-by-batch and record results

**Files:**
- Move remaining `docs/plans/*.md` into topic folders as needed
- Add: `docs/plans/2026-03-12-docs-structure-refactor/impl.md`
- Add: `docs/plans/2026-03-12-docs-structure-refactor/checklist.md`

**Step 1: Migrate the next batches**

After the first batches are stable, continue with the rest of the plan documents by topic.

**Step 2: Run broad verification**

Run:
- `.\gradlew.bat :Server:test :Mod:test --no-daemon`
- `.\TestMod\gradlew.bat compileJava --no-daemon`

**Step 3: Record exact outcomes**

If any file-lock, TLS, or repository issue happens, classify it separately from broken doc references or broken tests.
