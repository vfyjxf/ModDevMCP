# TestMod Composite Build Design

Date: 2026-03-11

## Goal

Add a standalone `TestMod/` Gradle project inside this repository so real Minecraft integration tests run through that project's Gradle `runClient` flow, while the main repository continues to own the MCP implementation.

## Current Context

- The current repository contains `Server` and `Mod`.
- `Mod` is the MCP-side Minecraft runtime implementation.
- Stable-server lifecycle and reconnect behavior already exist.
- Current real-game verification still starts from `:Mod:runClient`.
- The user wants future game testing to run through a separate test mod project, not through ad hoc scripts.

## Requirements

- `TestMod/` must be an independent Gradle project.
- `TestMod/` must use composite build dependency on this repository.
- Future real-game startup should primarily use `TestMod`'s Gradle `runClient`.
- The solution should preserve the existing stable-server architecture.
- Manual batch launch scripts should no longer be the primary validation path.

## Recommended Approach

Create a root-level `TestMod/` project with its own `settings.gradle` and `build.gradle`, and wire it to this repository using `includeBuild("..")`.

`TestMod` will:

- apply NeoForge ModDevGradle on its own
- declare a dependency on the MCP mod project from the composite build
- own its own `runClient`
- pass the same stable-server config properties into the client run
- optionally expose a small helper task for generating or validating shared stable-server config before launch

This keeps responsibilities clean:

- `Server` and `Mod` stay focused on MCP implementation
- `TestMod` becomes the real integration harness

## Alternatives Considered

### Option 1: Root-level `TestMod/` independent project

Pros:

- matches the user's requested structure
- keeps test harness isolated from the main multi-project build
- easy to document with a dedicated working directory and simple `runClient`

Cons:

- requires some composite dependency plumbing
- duplicates a small amount of NeoForge project configuration

### Option 2: Add another subproject under the main `settings.gradle`

Pros:

- simpler dependency wiring
- less Gradle indirection

Cons:

- not an independent Gradle project
- does not satisfy the user's requirement for composite build

### Option 3: Keep using `:Mod:runClient` and only add helper tasks

Pros:

- smallest code change

Cons:

- does not create the requested external integration harness
- does not separate MCP implementation from integration testing

## Architecture

### Project Layout

New top-level directory:

- `TestMod/settings.gradle`
- `TestMod/build.gradle`
- `TestMod/gradle.properties`
- `TestMod/src/main/java/...`
- `TestMod/src/main/resources/...`

The main repository remains unchanged as the included build target.

### Composite Build Wiring

`TestMod/settings.gradle` should:

- declare plugin repositories
- declare the `TestMod` root project name
- use `includeBuild("..")`

Dependency resolution should map the MCP implementation project from the included build into `TestMod`'s runtime/dev environment.

### Runtime Contract

`TestMod:runClient` should:

- start Minecraft using ModDevGradle
- load the MCP mod/runtime from the included build
- point at the shared stable-server config path
- rely on the same fixed-port reconnect architecture already implemented

This preserves the existing server/mod split:

- stable server remains separate and reconnectable
- game runtime still attaches over network
- Java-level coupling remains only through dev/runtime dependency inclusion, not process embedding

### Verification Flow

Primary real-game verification should become:

1. generate or validate shared stable-server launch/config artifacts
2. run `TestMod`'s Gradle `runClient`
3. connect MCP client to the stable server
4. perform screenshot / UI / runtime checks

This replaces hand-maintained wrapper scripts as the normal workflow, while existing generated scripts can remain as lower-level debug artifacts if still needed.

## Error Handling

- If composite dependency resolution fails, report it as a build/config issue.
- If Gradle dependency download fails, classify it separately as environment/repository/TLS failure.
- If `runClient` launches but MCP runtime is unavailable, classify it as runtime attach/state propagation issue.

## Testing Strategy

### Automated

- Gradle build logic tests where practical for generated config and docs assertions
- focused tests for any new config resolver or helper task logic

### Real Integration

- run `TestMod:runClient`
- verify stable server port is live
- verify lifecycle file updates
- verify MCP calls against the live game

## Documentation Changes

Need updates in:

- `README.md`
- new plan/checklist/impl docs under `docs/plans`
- likely a guide for `TestMod` startup and validation flow

## Decision

Proceed with Option 1:

- add `TestMod/` as an independent root-level Gradle project
- use composite build with `includeBuild("..")`
- make `TestMod:runClient` the default real-game verification path going forward
