# Third-Party Mod Integration Docs Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a user-facing guide that explains how other NeoForge mods should extend ModDevMCP.

**Architecture:** Keep the documentation as one primary guide plus one Chinese mirror. The guide should clearly separate the first-class extension path for MCP tools from the current instance-scoped runtime adapter APIs so external mod authors do not infer capabilities that the codebase does not yet expose automatically.

**Tech Stack:** Markdown, existing ModDevMCP public API, README guide index

---

### Task 1: Add the guide plan record

**Files:**
- Create: `docs/plans/2026-03-15-third-party-mod-integration-docs.md`

**Step 1: Record the agreed documentation scope**

- Keep the scope to one integration guide for third-party mod authors.
- Cover tool registrars, side boundaries, runtime adapter APIs, and current limitations.

### Task 2: Write the English integration guide

**Files:**
- Create: `docs/guides/2026-03-15-third-party-mod-integration-guide.md`

**Step 1: Explain who the guide is for**

- Target NeoForge mod authors who want ModDevMCP to understand their mod.

**Step 2: Explain the two extension layers**

- First-class path: annotation-scanned MCP tool registrars.
- Runtime adapter path: `ModMcpApi` registration for UI, input, inventory, and capture integrations.

**Step 3: Add minimal examples**

- Include `@CommonMcpRegistrar`, `@ClientMcpRegistrar`, and `@ServerMcpRegistrar` examples.
- Include a small `McpToolProvider` example.
- Include a small `UiDriver` / capture provider example outline.

**Step 4: Document limitations and side safety**

- Explain common/client/server separation.
- Explain that runtime adapter APIs are instance-scoped today and are better suited to tightly integrated modules.

### Task 3: Write the Chinese mirror

**Files:**
- Create: `docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md`

**Step 1: Mirror the English guide faithfully**

- Keep the technical meaning aligned with the English guide.
- Keep examples and limitations consistent.

### Task 4: Link the new guide from README files

**Files:**
- Modify: `README.md`
- Modify: `README.zh.md`

**Step 1: Add the new guide to the guide list**

- Add the English guide near the other 2026-03-15 guides.
- Add the Chinese mirror in the Chinese section.

### Task 5: Verify the docs update

**Files:**
- Verify: `README.md`
- Verify: `README.zh.md`
- Verify: `docs/guides/2026-03-15-third-party-mod-integration-guide.md`
- Verify: `docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md`

**Step 1: Check the new files and links**

Run: `rg -n "third-party-mod-integration-guide|@CommonMcpRegistrar|ModMcpApi" README.md README.zh.md docs/guides`

Expected:
- the new guide files exist
- both README files link to them
- the guide mentions registrar annotations and `ModMcpApi`
