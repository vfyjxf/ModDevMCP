# UI Capture Artifact And Resource Design

**Date:** 2026-03-09

**Goal:** make `ui_capture` produce a real PNG artifact with both local debug access and a formal server-side resource read path.

## Scope

This slice adds a first runnable capture artifact pipeline built around placeholder rendering instead of real Minecraft framebuffer capture.

Included:

- `ui_capture` returns `imageRef`, `imagePath`, `imageResourceUri`, and `imageMeta`
- `Mod` renders a placeholder PNG from `UiSnapshot` data
- `Mod` stores capture artifacts on disk and exposes them by `imageRef`
- `Server` adds a minimal read-only resource registry for capture artifacts

Excluded:

- real framebuffer grabbing
- persistent resource catalogs
- generic MCP resource framework beyond the minimal read-only surface needed here
- non-image resources

## Architecture

### Mod-side capture pipeline

`UiToolProvider` keeps owning the public tool behavior, but `ui_capture` now calls two runtime services:

- `UiCaptureRenderer`
  - renders a debug PNG from the structured snapshot and matched target sets
- `UiCaptureArtifactStore`
  - writes the PNG to disk
  - generates `imageRef`
  - tracks `imageRef -> path, uri, metadata, bytes`

The first renderer does not try to mirror Minecraft pixels. It renders enough information to debug selector and capture behavior:

- screen background
- target rectangles
- labels
- focus and selection highlights
- excluded region overlays

### Server-side resource read path

`Server` gains a minimal read-only resource surface:

- `McpResourceRegistry`
- `McpResourceProvider`
- `McpResource`

This is intentionally small. It only needs to support:

- register providers
- resolve a URI
- read resource content and metadata

Capture artifacts use the URI format:

- `moddev://capture/<imageRef>`

`ModDevMCP` registers a built-in capture resource provider backed by `UiCaptureArtifactStore`.

## Tool contract

`moddev.ui_capture` keeps existing structured fields and adds:

- `imageRef`
- `imagePath`
- `imageResourceUri`
- `imageMeta`

Suggested `imageMeta` fields:

- `width`
- `height`
- `format`
- `driverId`
- `createdAt`

## Testing

### Mod

- `UiToolInvocationTest`
  - capture returns image artifact fields
  - created file exists
- `UiCaptureArtifactStoreTest`
  - stored artifact is readable by `imageRef`
  - metadata is preserved

### Server

- resource registry stores and resolves providers
- server reads a `moddev://capture/<imageRef>` resource through the provider

## Rationale

This gives a complete runnable artifact chain now without blocking on actual game rendering internals. Later real capture support can replace only the renderer input, while `imageRef`, local file output, and server resource reads remain stable.
