# Annotation Registrar Design

**Date:** 2026-03-15

## Goal

Replace manual and static MCP tool registration with NeoForge annotation-based registrar discovery that cleanly separates common, client, and server registration.

## Problem

The previous static global registration approach can introduce ordering issues and unnecessary class loading. It also exposes a manual registration workflow to other mods when ModDevMCP should instead discover extension points itself.

## Chosen Design

Use NeoForge mod scan data to discover registrar classes marked by side-specific annotations.

- Common registrar annotation: `@CommonMcpRegistrar`
- Client registrar annotation: `@ClientMcpRegistrar`
- Server registrar annotation: `@ServerMcpRegistrar`

Each annotation maps to a dedicated registrar interface:

- `CommonMcpToolRegistrar`
- `ClientMcpToolRegistrar`
- `ServerMcpToolRegistrar`

Each registrar receives a dedicated event object and registers one or more `McpToolProvider` instances through that event.

## Side Safety

Side safety is enforced before class loading:

- common registrars are scanned and loaded only from the common path
- client registrars are scanned and loaded only from the client path
- server registrars are scanned and loaded only from the server path

This avoids loading client-only registrar classes on dedicated servers.

## Discovery Flow

- `ModDevMCP.registerCommonProviders()` discovers and runs common registrars
- `ClientRuntimeBootstrap.registerClientProviders()` discovers and runs client registrars
- `ServerRuntimeBootstrap.registerServerProviders()` discovers and runs server registrars

The lookup uses `ModList.get().getAllScanData()` and filters annotation metadata first, then loads only matching registrar classes.

## Testing

- Unit test the annotation lookup using synthetic `ModFileScanData`
- Integration test `ModDevMCP` with injected registrar suppliers so common/client/server registration timing can be verified without requiring a real mod scan environment
