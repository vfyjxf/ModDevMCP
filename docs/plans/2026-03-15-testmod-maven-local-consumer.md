# TestMod Maven Local Consumer Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make `TestMod` consume ModDevMCP through `mavenLocal()` like a real consumer project instead of using composite-build substitution.

**Architecture:** Remove `includeBuild("..")` substitution from `TestMod` so both `dev.vfyjxf:moddevmcp` and `dev.vfyjxf:moddevmcp-server` resolve through repositories. Keep `mavenLocal()` first so local-published artifacts are used during development.

**Tech Stack:** Gradle, mavenLocal publishing, NeoForge consumer build, Gradle plugin resolution

---

### Task 1: Switch TestMod to repository-based consumption

**Files:**
- Modify: `TestMod/settings.gradle`
- Modify: `TestMod/build.gradle`

**Step 1: Remove composite-build dependency substitution**

- Delete the `includeBuild("..")` block that substitutes `:Mod` and `:Server`.

**Step 2: Keep plugin resolution repository-based**

- Retain `mavenLocal()` in plugin management so the published plugin can be resolved locally.

### Task 2: Publish local artifacts for verification

**Files:**
- No source changes required

**Step 1: Publish producer modules to local Maven**

- Publish `Plugin`, `Mod`, and `Server` to `mavenLocal`.

**Step 2: Regenerate MCP client files in TestMod**

- Run `createMcpClientFiles` in `TestMod`.

### Task 3: Verify resolved server origin

**Files:**
- No source changes required

**Step 1: Inspect generated classpath**

- Confirm `server` resolves from Gradle cache / local Maven instead of `Server/build/libs`.

**Step 2: Report exact result**

- If resolution still points at composite-build output, identify the remaining source.
