# MCP Client Config Generation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Regenerate only officially confirmed MCP client config files, and stop emitting unverified client formats.

**Architecture:** Keep the launcher/bootstrap generation unchanged, but split client config emitters by actual client schema instead of reusing one shared JSON payload. Update the install guide to describe per-client config targets and remove unsupported generated files.

**Tech Stack:** Gradle task API, Java, JUnit 5, Gradle TestKit fixtures

---

### Task 1: Narrow generated client outputs

**Files:**
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/CreateMcpClientFilesTask.java`
- Modify: `Plugin/src/main/java/dev/vfyjxf/mcp/gradle/McpLaunchFiles.java`

**Step 1: Remove legacy shared output assumptions**

- Stop generating `mcp-servers.json`, `claude-desktop.mcp.json`, `cline_mcp_settings.json`, `windsurf-mcp_config.json`, and `goose-setup.md`.
- Keep launcher files unchanged.

**Step 2: Emit only confirmed client config files**

- Keep `codex.toml`.
- Emit dedicated JSON snippets for Claude Code, Cursor, VS Code, and Gemini CLI using their own confirmed root keys and field layout.

**Step 3: Rewrite install guide**

- Document exact generated files and target config locations.
- Remove claims that one shared JSON file works across multiple clients.

### Task 2: Update tests to match the new contract

**Files:**
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/CreateMcpClientFilesTaskTest.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/McpLaunchFilesTest.java`
- Modify: `Plugin/src/test/java/dev/vfyjxf/mcp/gradle/McpGatewayLaunchFilesTest.java` only if needed

**Step 1: Update file existence expectations**

- Assert kept files exist.
- Assert removed legacy files no longer exist.

**Step 2: Add schema-specific assertions**

- Claude Code and Cursor should use `mcpServers`.
- VS Code should use `servers`.
- Gemini CLI should use its own generated snippet format instead of reusing the old shared payload.

### Task 3: Verify with real test execution

**Files:**
- No source changes required

**Step 1: Run Plugin tests covering the generator**

- Run the focused `Plugin:test` task or the specific test classes.

**Step 2: Report actual results**

- If tests fail because of code, fix them.
- If tests fail because of environment or dependency/network issues, report the exact failure point.
