# UI Real Capture Providers Design

**Date:** 2026-03-09

**Goal:** add two real image capture paths for `ui_capture`: off-screen GUI re-rendering and current main framebuffer grabbing, while preserving the existing artifact and resource pipeline.

## Scope

Included:

- `ui_capture` accepts `source=auto|offscreen|framebuffer|placeholder`
- `Mod` exposes registration-based capture provider APIs
- runtime supports:
  - off-screen capture providers
  - framebuffer capture providers
- built-in providers for vanilla client screens
- placeholder capture remains the final fallback

Excluded:

- pixel-perfect integration tests for the live Minecraft renderer
- full third-party framework support beyond the registration API
- replacing the existing artifact store or resource URI contract

## Capture Modes

### `offscreen`

Re-renders the current screen into an off-screen FBO and reads back the pixels.

Use when:

- a clean semantic screenshot is preferred
- overlays should be controlled by the provider
- target crop and exclude masking should be more deterministic

### `framebuffer`

Reads pixels from the current main render target.

Use when:

- the final player-visible result is needed
- overlays and post-processing should be preserved

### `placeholder`

Uses the existing debug renderer based on structured `UiSnapshot`.

Use when:

- no real provider matches
- the capture is running in test or headless-like environments

### `auto`

Default resolution order:

1. offscreen
2. framebuffer
3. placeholder

This keeps the cleanest available result as the default while preserving a guaranteed fallback.

## Mod API

Add two registration-facing APIs:

- `UiOffscreenCaptureProvider`
- `UiFramebufferCaptureProvider`

Both return a shared result record, for example:

- `providerId`
- `source`
- `pngBytes`
- `width`
- `height`
- `metadata`

This lets the existing `UiCaptureArtifactStore` remain the single place that writes files and exposes resources.

## Runtime Registries

Add two provider registries with priority-based selection:

- `UiOffscreenCaptureProviderRegistry`
- `UiFramebufferCaptureProviderRegistry`

Each registry selects the highest-priority matching provider for the current context and snapshot.

## Built-in Providers

### Built-in off-screen provider

Reference implementation based on `CloudLib-Standalone`:

- `dev.vfyjxf.cloudlib.api.ui.dump.SceneCapture`
- `dev.vfyjxf.cloudlib.api.ui.dump.UICapture`

Key behaviors to replicate:

- off-screen `TextureTarget`
- orthographic GUI projection
- model-view reset and depth translation
- `GuiGraphics` render pass
- `NativeImage` pixel download and vertical flip

The first built-in implementation should only claim vanilla/current client screens that can be re-rendered safely.

### Built-in framebuffer provider

Reads the current `Minecraft` main render target into a `NativeImage`.

This path is intentionally closer to “what the player currently sees”.

## Tool Contract

`ui_capture` returns existing fields plus:

- `imageRef`
- `imagePath`
- `imageResourceUri`
- `imageMeta.source`
- `imageMeta.providerId`

If no real provider matches, `imageMeta.source=placeholder`.

## Testing Strategy

### Unit-level

- provider selection for `source=auto`
- explicit source selection
- offscreen preferred over framebuffer under `auto`
- fallback to placeholder when no provider matches

### Runtime-level

Built-in provider code should be written to work in-game, but unit tests only verify:

- selection behavior
- artifact output
- metadata source/provider tagging

This avoids unstable render-thread pixel assertions in the current test environment.

## Rationale

The artifact store and resource layer are already working. By inserting real capture providers ahead of the existing placeholder renderer, we can add true image capture now without breaking the current MCP contract. Off-screen and framebuffer captures serve different purposes, so both are first-class rather than forcing one to emulate the other.
