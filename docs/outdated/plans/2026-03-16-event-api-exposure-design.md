# 2026-03-16 Event API Exposure Design

## Context

The current registrar model exposes side-specific tool events:

- `RegisterCommonMcpToolsEvent`
- `RegisterClientMcpToolsEvent`
- `RegisterServerMcpToolsEvent`

Those events can currently register only `McpToolProvider` instances.

At the same time, several public extension hooks still require direct access to `ModMcpApi` or `ModDevMCP`:

- `registerUiDriver`
- `registerInventoryDriver`
- `registerInputController`
- `registerUiInteractionStateResolver`
- `registerUiOffscreenCaptureProvider`
- `registerUiFramebufferCaptureProvider`
- `eventPublisher`

That forces downstream integrations to depend on a concrete `ModDevMCP` instance even when they already run inside a registrar callback.

## Decision

Promote the side-specific registrar events into the primary extension surface.

Each side-specific event should:

- retain direct tool provider registration
- expose `ModMcpApi` through `api()`
- expose the runtime event publisher through `eventPublisher()`
- provide direct helper methods for the registrations that are valid on that side

## Side-Specific Helper Surface

### Common

Expose:

- `register(McpToolProvider provider)`
- `registerToolProvider(McpToolProvider provider)`
- `api()`
- `eventPublisher()`
- `publishEvent(EventEnvelope event)`

Do not add client-only helper shortcuts on the common event.

### Client

Expose everything from common, plus:

- `registerUiDriver(UiDriver driver)`
- `registerInventoryDriver(InventoryDriver driver)`
- `registerInputController(InputController controller)`
- `registerUiInteractionStateResolver(UiInteractionStateResolver resolver)`
- `registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider)`
- `registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider)`

### Server

Expose the same surface as common.

The current public `ModMcpApi` does not contain server-only runtime adapters beyond tool registration, so no extra server helpers are required yet.

## Compatibility

Keep `register(McpToolProvider provider)` on existing events so current registrar tests and downstream code continue to compile.

Add `registerToolProvider(...)` as the clearer alias for new code.

## Testing

Add integration tests that prove:

- client registrar events can register client runtime adapters without a direct `ModDevMCP` instance
- events expose `api()` and `eventPublisher()`
- common and server registrars can still register tools through the event surface
