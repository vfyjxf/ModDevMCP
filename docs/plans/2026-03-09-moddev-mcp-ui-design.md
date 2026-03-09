# ModDevMCP GUI Control Surface Design

**Date:** 2026-03-09

**Goal:** establish a layered MCP architecture for Minecraft mod development, with phase one focused on client-side GUI observation and control tools.

## Scope

Phase one builds the framework for `common`, `client`, and `server`, but only ships GUI-oriented MCP tools on the client side:

- `ui.snapshot`
- `ui.screenshot`
- `ui.observe`
- `ui.describe_target`
- `ui.click`
- `ui.type`
- `ui.key_press`
- `ui.scroll`
- `ui.open`
- `ui.close`
- `ui.switch`
- `ui.toggle`
- `ui.wait`
- `ui.assert`

World, entity, block, and `BlockEntity` data remain out of scope for phase one, but the bridge and package structure must leave room for them.

## Constraints

- The MCP protocol layer must use the official `modelcontextprotocol/java-sdk`.
- The Gradle modules are intentionally split:
  - `Server` is the MCP protocol/runtime module.
  - `Mod` depends on `Server` and packages it into the NeoForge mod.
- A new `Common` module should hold shared protocol models, bridge interfaces, and the public API for other mods.
- Many mods wrap custom UI frameworks inside `Screen`; vanilla widget traversal is not sufficient.
- JEI and similar overlays must be classifiable instead of globally ignored.
- JEI add-ons must be distinguishable from JEI itself.
- Other mods must be able to extend the recognition pipeline through `api` package contracts.
- `Mod` test setup should follow the same NeoForge unit-test pattern used by `CloudLib-Standalone`.

## Module Layout

### `:Common`

Shared, non-Minecraft-specific contracts and models.

Primary packages:

- `dev.vfyjxf.mcp.common.api.ui`
  - stable extension API for third-party mods
- `dev.vfyjxf.mcp.common.protocol.ui`
  - DTOs shared by MCP handlers and in-game bridge implementations
- `dev.vfyjxf.mcp.common.bridge`
  - service interfaces used by `Server` and implemented by `Mod`
- `dev.vfyjxf.mcp.common.model`
  - enums/value objects such as bounds, capture scope, owner refs
- `dev.vfyjxf.mcp.common.internal`
  - internal helpers not intended for third-party consumption

### `:Server`

Plain Java MCP server built on the official Java SDK.

Responsibilities:

- define MCP tools and handlers
- validate tool inputs
- map MCP requests onto bridge interfaces from `:Common`
- manage observation tokens and request context
- stay free of NeoForge and Minecraft runtime types

Framework choice:

- use the official `modelcontextprotocol/java-sdk`
- prefer the `io.modelcontextprotocol.sdk:mcp` convenience artifact unless implementation detail forces a narrower dependency later
- keep transport pluggable so the embedded server can start with local stream-based operation and evolve later

### `:Mod`

NeoForge runtime implementation.

Responsibilities:

- depend on `:Server` and `:Common`
- instantiate and wire the MCP server into the game process
- implement the client-side GUI bridge
- register client lifecycle hooks, screenshot capture, input dispatch, and adapter registry
- host future dedicated-server/world bridge implementations

Package layout:

- `dev.vfyjxf.mcp`
  - shared mod bootstrap base
- `dev.vfyjxf.mcp.client.*`
  - GUI capture, pipeline, adapters, transport bridge
- `dev.vfyjxf.mcp.servergame.*`
  - future dedicated server or world data bridge implementations

## Layered GUI Pipeline

The first-phase GUI system uses a layered pipeline instead of assuming that widget trees are reliable:

1. raw screen/frame capture
2. generic region detection and naming
3. overlay classification
4. adapter-provided semantic enrichment
5. action exposure and resolution
6. MCP response assembly

This gives useful baseline behavior without an adapter and better behavior when a specific mod registers one.

## Public API for Other Mods

The public extension surface lives in `dev.vfyjxf.mcp.common.api.ui`.

Planned API contracts:

- `UiAdapter`
  - semantic inspection for a screen or UI host
- `OverlayClassifier`
  - classify regions as `main_content`, `overlay`, `tooltip`, `addon_owned`, or `unknown`
- `CaptureRegionProvider`
  - publish named logical capture regions
- `UiActionProvider`
  - expose stable `open`, `close`, `switch`, and `toggle` actions
- `UiAdapterRegistrar`
  - registration entry point
- `UiApi`
  - stable facade for mod-side registration or capability discovery

Compatibility rules:

- API interfaces stay small and versionable.
- API consumers depend on `:Common`, not `:Server`.
- Internal pipeline helpers stay out of the API package.

## Region and Ownership Model

Each `UiRegion` should carry enough ownership data for filtering and targeted screenshots:

- `id`
- `bounds`
- `role`
- `ownerModId`
- `hostModId`
- `tags`
- `interactive`
- `occludesUnderlay`
- `children`

Examples:

- a JEI recipe panel: `ownerModId=jei`
- an add-on region hosted inside JEI: `ownerModId=<addon>`, `hostModId=jei`
- the host screen's main content: `ownerModId=<host mod>`, `role=main_content`

This makes “exclude JEI”, “include JEI add-ons”, and “capture only host content” first-class operations instead of custom hacks.

## Phase One Tool Behavior

### Observation tools

- `ui.snapshot`
  - returns structured screen, region, element, focus, action, and adapter metadata
- `ui.screenshot`
  - supports `full_screen`, `main_content_only`, `overlay_only`, named `region_ids`, and `custom_rect`
- `ui.observe`
  - tracks GUI changes with a pollable token-based observation session
- `ui.describe_target`
  - resolves one region or element into ownership, role, actions, and capture recommendations

### Interaction tools

- `ui.click`
- `ui.type`
- `ui.key_press`
- `ui.scroll`

These must accept both semantic targets and explicit coordinates where possible.

### Navigation and test-stability tools

- `ui.open`
- `ui.close`
- `ui.switch`
- `ui.toggle`
- `ui.wait`
- `ui.assert`

These are required because many GUI actions are not robustly represented as raw clicks. Adapters can expose stable semantic actions for tabs, sidebars, overlays, filters, and host-specific navigation.

## Error Model

Tool failures should distinguish:

- `unsupported`
- `not_found`
- `stale_context`

This avoids collapsing all GUI failures into generic internal errors and gives an agent enough feedback to recover.

## Testing Strategy

### `:Common`

- pure JUnit 5 unit tests
- ownership model, filtering, capture scope resolution, action request validation

### `:Server`

- pure JUnit 5 unit tests
- MCP handler mapping, parameter validation, error mapping, observation token logic

### `:Mod`

- NeoForge unit tests enabled, following the pattern used by `CloudLib-Standalone`
- focus on pipeline assembly, adapter precedence, capture scope handling, and GUI action dispatch
- keep true screenshot pixel-perfect verification minimal in phase one

## Build and Runtime Decisions

- add a new `:Common` module and make both `:Server` and `:Mod` depend on it
- replace the current `Server` dependency on `io.modelcontextprotocol.sdk:mcp-core:1.0.0` with the official Java SDK setup centered on the `io.modelcontextprotocol.sdk` artifacts
- enable JUnit 5 in both plain Java modules
- enable NeoForge unit tests in `:Mod`, mirroring the `CloudLib-Standalone` pattern

## Out of Scope for Phase One

- world/block/entity/`BlockEntity` MCP tools
- dedicated server world inspection tools
- drag-and-drop and container-slot-specialized GUI actions
- real-time streaming observe transport beyond stable token polling

## Notes

- `D:\ProjectDir\AgentFarm\ModDevMCP` is not currently a git repository, so this design doc cannot be committed from the present workspace state.
