# ModDevMCP Framework Design

**Date:** 2026-03-09

## Goal

Build a Minecraft-focused MCP stack for NeoForge 1.21.1 mod development with:

- a pure `Server` module that hosts the MCP server and exposes extensible tools
- a `Mod` module that runs inside Minecraft and implements data collection and control
- a shared `Common` contract layer for cross-module APIs, drivers, requests, snapshots, and events

The first functional slice focuses on `ui`, `input`, `inventory`, and `event` domains. The design prioritizes extensibility over a hardcoded widget model so different UI frameworks and mods can register their own runtime implementations.

## Module Layout

### `Common`

Purpose:

- stable contracts shared by `Server`, `Mod`, and third-party integrations
- request/response models for MCP-facing operations
- runtime SPI for UI, inventory, input, and event providers

Owns:

- selectors, action requests, capture requests
- snapshots, targets, tooltips, operation results
- event models and cursors
- driver registry interfaces
- bridge interfaces consumed by the MCP server

Must not depend on Minecraft client-only or NeoForge runtime-only classes.

### `Server`

Purpose:

- pure MCP server implementation
- registration and dispatch of MCP tools, resources, and subscriptions
- no direct dependency on Minecraft runtime classes

Owns:

- MCP server bootstrap
- tool definition and registration API
- tool invocation dispatch
- embedded and remote bridge modes

### `Mod`

Purpose:

- Minecraft-side implementation of data capture and action execution
- built-in runtime drivers for vanilla UI and inventory
- bridge implementation consumed by `Server`

Owns:

- NeoForge entrypoints
- UI, inventory, input, and event runtime services
- screenshot capture
- driver discovery and registration
- embedded server wiring and remote bridge endpoint

## Runtime Modes

### Embedded

The MCP server runs inside the Minecraft process. `Server` calls the in-process game bridge implemented by `Mod`.

Use cases:

- lowest setup overhead
- local development
- single Minecraft instance workflows

### Remote

The MCP server runs as a separate process and communicates with the game runtime through a local transport owned by `Mod`.

Use cases:

- external orchestration
- multiple instances
- future remote debugging or sandboxing

The bridge contract must be identical across both modes.

## Domain Model

The first version includes these domains:

- `ui`
- `input`
- `inventory`
- `event`

These domains must be designed as registrable providers rather than one fixed implementation.

## UI Model

The UI layer intentionally avoids a rigid global widget taxonomy. Many Minecraft mods render custom interfaces inside a `Screen` without exposing compatible widget trees.

The MCP-facing model is based on three generic concepts:

### Selector

Describes what to inspect, capture, or operate on.

Suggested fields:

- `scope`: `screen | region | element | slot | overlay | focused | hovered | active`
- `screen`
- `modId`
- `text`
- `role`
- `id`
- `bounds`
- `index`
- `exclude[]`

### Capture

Describes which rendered output to return.

Supported forms:

- full screen
- current screen
- matched targets
- arbitrary region
- include-only masking
- exclude masking
- overlay include or exclude policy

### Action

Describes what to do with a selected target.

First version actions:

- `click`
- `right_click`
- `double_click`
- `hover`
- `focus`
- `type`
- `key_press`
- `scroll`
- `drag`
- `open`
- `close`
- `switch`

## MCP Tool Surface

### UI Tools

#### `ui_snapshot`

Returns the current UI context.

Capabilities:

- current screen summary
- visible targets
- visible slots
- overlays or auxiliary regions
- focused, selected, hovered, and active target ids
- `driverId`
- `extensions`

#### `ui_query`

Finds targets using a selector.

Returns matched targets with:

- `targetId`
- `driverId`
- `screenClass`
- `modId`
- `role`
- `text`
- `bounds`
- `state`
- `actions[]`
- `extensions`

#### `ui_capture`

Captures rendered output by selector or region.

Supports:

- full capture
- cropped region
- target-based capture
- include mask
- exclude mask
- overlay filtering

Returns:

- `imageRef`
- matched targets
- excluded targets
- `snapshotRef`

#### `ui_action`

Executes a semantic action on a selected target.

Returns:

- `accepted`
- `performed`
- `reason`
- `postSnapshotRef`

#### `ui_wait`

Blocks until the UI meets a condition.

First version conditions:

- selector appeared
- selector disappeared
- screen changed
- text changed
- focus changed
- screen stable for a duration

#### `ui_inspect_at`

Resolves the targets under a given point.

Returns:

- matched targets
- topmost target
- `driverId`
- `modId`
- `role`
- `text`
- `bounds`
- `extensions`

#### `ui_get_tooltip`

Returns tooltip data for a selector or a point.

#### `ui_get_interaction_state`

Returns the current interaction state.

Fields:

- `focusedTarget`
- `selectedTarget`
- `hoveredTarget`
- `activeTarget`
- `cursorPosition`
- `textInputActive`
- `selectionSource`
- `driverId`

#### `ui_get_target_details`

Returns expanded detail for a target.

If a provided target selector matches nothing, return a `not_found` style tool failure instead of a synthetic empty detail object.

Suggested content:

- base target data
- hierarchy path
- supported actions
- related capture region
- overlay flag
- inventory slot or text-input metadata
- `extensions`

#### Historical `ui_open`

This section is historical only.

Current active API guidance uses `moddev.ui_run_intent` for explicit high-level entry actions such as:

- `inventory`
- `chat`
- `pause_menu`

The generic `ui_open` tool is no longer part of the active public API because it implied behavior the runtime could not honestly guarantee.

#### `ui_close`

Semantically closes:

- current screen
- active overlay
- focused or active target
- arbitrary selector

#### `ui_switch`

Switches active interaction target by selector.

Modes:

- `focus`
- `cycle`
- `activate`

### Input Tools

#### `input_action`

Low-level escape hatch for raw input operations:

- mouse move
- mouse click
- key press
- type text
- scroll
- drag

### Inventory Tools

#### `inventory_snapshot`

Returns the currently open container or player inventory state.

#### `inventory_action`

Executes higher-level inventory operations on selected targets.

First version actions:

- `pickup`
- `quick_move`
- `throw`
- `swap`
- `split`
- `drag`

### Event Tools

#### `event_poll`

Polls recent events by domain and cursor.

#### `event_subscribe`

Subscribes to streamed events using a selector and domain filter.

## Common Runtime API

The `Mod` runtime must expose a registration-based SPI for third-party integrations.

### UI Driver API

`UiDriver` is the main extension point for custom UIs.

Expected responsibilities:

- decide if the driver matches the current UI context
- produce snapshots
- resolve selectors
- capture regions or targets
- execute semantic actions
- inspect targets at coordinates
- return tooltips
- return interaction state
- wait for UI conditions

Required semantic methods:

- `descriptor()`
- `matches(UiContext context)`
- `snapshot(UiContext context, SnapshotOptions options)`
- `query(UiContext context, TargetSelector selector)`
- `capture(UiContext context, CaptureRequest request)`
- `action(UiContext context, UiActionRequest request)`
- `inspectAt(UiContext context, int x, int y)`
- `tooltip(UiContext context, TargetSelector selector)`
- `interactionState(UiContext context)`
- `waitFor(UiContext context, UiWaitRequest request)`

### Inventory Driver API

Separate SPI for container-specific logic:

- `matches(InventoryContext context)`
- `snapshot(InventoryContext context, InventoryOptions options)`
- `querySlots(InventoryContext context, TargetSelector selector)`
- `action(InventoryContext context, InventoryActionRequest request)`

### Input Controller API

Low-level input execution:

- `keyPress(KeyPressRequest request)`
- `typeText(TypeTextRequest request)`
- `mouseClick(MouseClickRequest request)`
- `mouseMove(MouseMoveRequest request)`
- `scroll(ScrollRequest request)`
- `drag(DragRequest request)`

### Event API

Runtime event publication and polling:

- `publish(EventEnvelope event)`
- `subscribe(EventSelector selector, EventSink sink)`
- `poll(EventSelector selector, EventCursor cursor, int limit)`

### Registration API

The mod-side API must support explicit registration and event-based registration.

Suggested registration entrypoints:

- `registerUiDriver(UiDriver driver)`
- `registerInventoryDriver(InventoryDriver driver)`
- `registerInputController(InputController controller)`
- `registerEventSource(EventSource source)`

Suggested NeoForge registration events:

- `RegisterUiDriversEvent`
- `RegisterInventoryDriversEvent`
- `RegisterMcpToolsEvent`

## Server Extension API

Other mods must be able to register their own MCP tools into the server.

### `McpToolProvider`

Declares a namespace and a set of tools.

### `McpToolDefinition`

Describes tool metadata:

- `name`
- `title`
- `description`
- `inputSchema`
- `outputSchema`
- `tags`
- `side`
- `requiresWorld`
- `requiresPlayer`
- `availability`
- `exposurePolicy`

### `McpToolHandler`

Executes a tool call using:

- `ToolCallContext`
- JSON-like arguments

Returns:

- `ToolResult`

### `ToolCallContext`

Should expose:

- current side
- world or player context when available
- `GameBridge`
- event publisher
- logger
- call metadata
- timeout or cancellation information

### `McpToolRegistry`

Registration surface:

- `registerProvider(McpToolProvider provider)`
- `registerTool(McpToolDefinition definition, McpToolHandler handler)`
- `listTools()`
- `findTool(String name)`

Tool names must be namespaced, for example:

- `moddev.ui_snapshot`
- `moddev.inventory_action`
- `jei.open_recipe_view`
- `mymod.machine_get_status`

## Bridge Contract

`Server` must depend only on a bridge API implemented by `Mod`.

Suggested bridge surface:

- `uiSnapshot`
- `uiCapture`
- `uiQuery`
- `uiAction`
- `uiInteractionState`
- `uiTooltip`
- `inventorySnapshot`
- `inventoryAction`
- `pollEvents`

This contract must remain stable across embedded and remote server modes.

## Built-In Runtime Implementations

First version should include:

- `VanillaScreenUiDriver`
- `VanillaContainerUiDriver`
- `FallbackRegionUiDriver`
- `VanillaInventoryDriver`
- `MinecraftInputController`
- `RuntimeEventPublisher`

These provide a baseline implementation and examples for third-party integrations.

## Design Constraints

- prioritize extensibility over a rigid universal widget model
- keep MCP-facing tools uniform even when runtime drivers differ
- do not force third-party mods to adopt one UI framework
- allow strong structured behavior when a custom driver exists
- allow degraded but usable behavior through region capture and raw input fallback
- keep `Server` free from direct Minecraft runtime dependencies
- support both embedded and remote deployment modes

## Out of Scope for First Version

- world and entity domain implementations
- recipe reasoning helpers
- long-running macro scripting semantics
- replay or audit pipelines
- a universal compatibility layer for every third-party UI without a registered driver
