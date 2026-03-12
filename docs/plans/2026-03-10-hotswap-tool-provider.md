# Plan: Class Hot-Reload (Hotswap) Tool Provider

**Date:** 2026-03-10

## Context

ModDevMCP is an MCP server for Minecraft mod TDD/debugging. Agents edit source files but currently have no way to reload changes into the running game. This feature adds MCP tools that trigger recompilation (Gradle) and class redefinition (JVM Instrumentation API), closing the edit-compile-test loop.

## Architecture

Three components:

1. **Agent subproject** (`Agent/`) — minimal Java agent JAR that captures `Instrumentation` via `premain`
2. **HotswapService** — orchestrates compilation (ProcessBuilder -> Gradle) and reload (Instrumentation.redefineClasses)
3. **HotswapToolProvider** — MCP tool provider exposing `moddev.hotswap` and `moddev.compile` tools

## Component Overview

### Agent Subproject

A standalone JAR with no dependencies. Contains:
- `HotswapAgent` — captures `Instrumentation` instance via `premain`
- `HotswapCapabilities` — detects DCEVM/JBR and probes actual JVM capabilities

### HotswapService

Orchestrates the compile-reload cycle:
- `compile()` — shells out to `gradlew :Mod:compileJava` via ProcessBuilder
- `reload()` — scans for changed .class files, redefines via Instrumentation API
- `snapshotTimestamps()` — maintains baseline for change detection

### HotswapToolProvider

Exposes two MCP tools:
- `moddev.compile` — compile mod source, returns exit code + output
- `moddev.hotswap` — compile (optional) + reload changed classes

## Design Decisions

- **ProcessBuilder for Gradle**: simple, no extra dependencies, works with any Gradle setup
- **Timestamp-based change detection**: avoids full bytecode comparison, fast enough for dev workflow
- **DCEVM/JBR detection**: capabilities reported in tool response so agents know what changes are safe
- **Agent as separate subproject**: minimal JAR, clean separation, loaded via `-javaagent`

## Limitations

- New classes (not yet loaded) cannot be redefined — they use new version on first load
- Standard JVM only supports method body changes; structural changes require DCEVM/JBR
- Gradle daemon contention possible if game's Gradle daemon conflicts
