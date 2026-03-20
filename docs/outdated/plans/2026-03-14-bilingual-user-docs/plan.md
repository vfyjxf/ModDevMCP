# Bilingual User Docs Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rewrite user-facing docs into English-first documentation with Chinese mirrors while keeping README focused on user onboarding only.

**Architecture:** Keep `README.md` as the short English entrypoint, add `README.zh.md` as the Chinese mirror, and rewrite each user guide into a clean English version plus a matching `.zh.md` file. Leave `docs/plans` as internal implementation history.

**Tech Stack:** Markdown, Git, PowerShell validation commands

---

### Task 1: Add plan docs for the bilingual documentation refresh

**Files:**
- Create: `docs/plans/2026-03-14-bilingual-user-docs/design.md`
- Create: `docs/plans/2026-03-14-bilingual-user-docs/plan.md`
- Create: `docs/plans/2026-03-14-bilingual-user-docs/checklist.md`
- Create: `docs/plans/2026-03-14-bilingual-user-docs/impl.md`

**Step 1: Write the docs**

Document the README contraction, English-first guide policy, and Chinese mirror policy.

**Step 2: Verify the plan directory**

Run: `Get-ChildItem docs\plans\2026-03-14-bilingual-user-docs`
Expected: the directory contains `design.md`, `plan.md`, `checklist.md`, and `impl.md`

### Task 2: Rewrite README into a user-only entrypoint

**Files:**
- Modify: `README.md`
- Create: `README.zh.md`

**Step 1: Replace the current README**

Keep only the project summary, quick start, readiness check, and user guide index.

**Step 2: Add the Chinese mirror**

Provide a content-equivalent `README.zh.md`.

**Step 3: Verify README files**

Run: `Get-ChildItem README*`
Expected: both `README.md` and `README.zh.md` exist

### Task 3: Rewrite user guides in English and add Chinese mirrors

**Files:**
- Modify: `docs/guides/2026-03-11-simple-agent-install-guide.md`
- Modify: `docs/guides/2026-03-11-game-mcp-guide.md`
- Modify: `docs/guides/2026-03-11-testmod-runclient-guide.md`
- Modify: `docs/guides/2026-03-11-agent-preflight-checklist.md`
- Modify: `docs/guides/2026-03-11-agent-prompt-templates.md`
- Modify: `docs/guides/2026-03-11-codex-screenshot-demo-guide.md`
- Modify: `docs/guides/2026-03-11-live-screen-tool-guide.md`
- Modify: `docs/guides/2026-03-12-playwright-style-ui-automation-guide.md`
- Create matching `.zh.md` files for all of the above

**Step 1: Rewrite the English guides**

Keep them user-facing and suitable for agent consumption.

**Step 2: Add Chinese mirrors**

Keep content aligned with the English guides.

**Step 3: Verify file coverage**

Run: `Get-ChildItem docs\guides\*.zh.md`
Expected: one Chinese mirror exists for each rewritten user guide

### Task 4: Validate and record the real documentation changes

**Files:**
- Modify: `docs/plans/2026-03-14-bilingual-user-docs/checklist.md`
- Modify: `docs/plans/2026-03-14-bilingual-user-docs/impl.md`

**Step 1: Inspect the diff**

Run: `git diff --stat`
Expected: README plus user guide docs are updated, no unrelated source files changed

**Step 2: Inspect the working tree**

Run: `git status --short`
Expected: only the intended documentation files appear

**Step 3: Record actual verification**

Write the real command outputs and results into `impl.md`
