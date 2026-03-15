# 2026-03-15 Third-Party Mod Integration Guide

Date: 2026-03-15 17:10 CST

## Purpose

- explain how another NeoForge mod should extend ModDevMCP
- separate the stable extension points from the internal runtime APIs
- show the side boundaries so client-only code does not leak into dedicated server paths

## Who Should Read This

Use this guide if you maintain another mod and want ModDevMCP to understand that mod better.

Typical cases:

- expose new MCP tools for your mod
- route tool calls to mod-specific client or server logic
- add richer UI inspection or capture support for a custom screen framework

## Choose the Right Extension Path

Use this rule first:

- add a tool registrar when you want to expose new MCP tools
- add runtime adapters when you need ModDevMCP's existing UI, inventory, input, or capture tools to understand your mod's runtime objects

Current recommendation:

- for most downstream mods, start with tool registrars
- use runtime adapters only when you need deep integration with `ui_snapshot`, `ui_query`, `ui_capture`, `ui_action`, or related tools

## Side Model

ModDevMCP now splits tool registration by side:

- `common`: safe on both physical sides
- `client`: client-only runtime logic, screens, input, rendering, capture
- `server`: dedicated or integrated server runtime logic

Choose the narrowest side that matches your code:

- if a class touches `Minecraft`, screens, or rendering classes, it belongs on `client`
- if a class touches `MinecraftServer`, command dispatchers, or server-world state, it belongs on `server`
- only use `common` when the provider is truly side-neutral

## Preferred Path: Annotation-Scanned Tool Registrars

The stable third-party extension point today is the registrar model.

Available annotations:

- `@CommonMcpRegistrar`
- `@ClientMcpRegistrar`
- `@ServerMcpRegistrar`

Matching interfaces:

- `CommonMcpToolRegistrar`
- `ClientMcpToolRegistrar`
- `ServerMcpToolRegistrar`

Matching registration events:

- `RegisterCommonMcpToolsEvent`
- `RegisterClientMcpToolsEvent`
- `RegisterServerMcpToolsEvent`

Each registrar:

- must be a concrete class with a public no-arg constructor
- should implement the matching registrar interface
- should only reference classes that are valid on that side

### Minimal Common Tool Example

```java
package com.example.examplemod.mcp;

import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;
import dev.vfyjxf.mcp.api.registrar.CommonMcpRegistrar;
import dev.vfyjxf.mcp.api.registrar.CommonMcpToolRegistrar;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

@CommonMcpRegistrar
public final class ExampleCommonRegistrar implements CommonMcpToolRegistrar {

    @Override
    public void register(RegisterCommonMcpToolsEvent event) {
        event.register(new ExampleToolProvider());
    }

    private static final class ExampleToolProvider implements McpToolProvider {
        @Override
        public void register(McpToolRegistry registry) {
            registry.registerTool(
                    new McpToolDefinition(
                            "examplemod.ping",
                            "Example Ping",
                            "Returns a simple payload from Example Mod.",
                            Map.of("type", "object"),
                            Map.of("type", "object"),
                            List.of("example"),
                            "common",
                            false,
                            false,
                            "public",
                            "public"
                    ),
                    (context, arguments) -> ToolResult.success(Map.of(
                            "ok", true,
                            "mod", "examplemod",
                            "runtimeSide", context.side()
                    ))
            );
        }
    }
}
```

### Minimal Client Tool Example

```java
package com.example.examplemod.mcp;

import dev.vfyjxf.mcp.api.event.RegisterClientMcpToolsEvent;
import dev.vfyjxf.mcp.api.registrar.ClientMcpRegistrar;
import dev.vfyjxf.mcp.api.registrar.ClientMcpToolRegistrar;

@ClientMcpRegistrar
public final class ExampleClientRegistrar implements ClientMcpToolRegistrar {

    @Override
    public void register(RegisterClientMcpToolsEvent event) {
        event.register(new ExampleClientToolProvider());
    }
}
```

Use the same pattern for `server` with `@ServerMcpRegistrar`.

## Naming and Contract Advice

When you add tools for your mod:

- use your own namespace, for example `examplemod.some_tool`
- keep schemas narrow and agent-friendly
- return structured payloads, not log dumps
- keep side routing explicit when one tool can act on both client and server runtime paths

If your tool is really a mod-specific operation, prefer a new tool instead of overloading a generic ModDevMCP tool.

## Runtime Adapter APIs

ModDevMCP also exposes runtime adapter registration through `ModMcpApi`.

Current public methods include:

- `registerUiDriver(UiDriver driver)`
- `registerInventoryDriver(InventoryDriver driver)`
- `registerInputController(InputController controller)`
- `registerToolProvider(McpToolProvider provider)`
- `registerUiInteractionStateResolver(UiInteractionStateResolver resolver)`
- `registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider)`
- `registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider)`

This layer is useful when you want the built-in ModDevMCP UI and capture tools to understand your mod directly instead of adding separate custom tools.

### Minimal `UiDriver` Shape

```java
public final class ExampleScreenUiDriver implements UiDriver {

    @Override
    public DriverDescriptor descriptor() {
        return new DriverDescriptor("examplemod:screen", "Example Screen Driver", List.of("examplemod"));
    }

    @Override
    public boolean matches(UiContext context) {
        return context.screen() instanceof ExampleScreen;
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        // Build a stable target tree for your screen here.
        throw new UnsupportedOperationException("example");
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets();
    }
}
```

### Minimal Capture Provider Shape

```java
public final class ExampleOffscreenCaptureProvider implements UiOffscreenCaptureProvider {

    @Override
    public String providerId() {
        return "examplemod:offscreen";
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public boolean matches(UiContext context, UiSnapshot snapshot) {
        return context.screen() instanceof ExampleScreen;
    }

    @Override
    public UiCaptureImage capture(
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        throw new UnsupportedOperationException("example");
    }
}
```

## Important Limitation Today

Tool registrars are the first-class third-party integration path today.

The runtime adapter APIs exist, but they are currently instance-scoped through `ModMcpApi`. That means they are best suited to:

- code that runs inside ModDevMCP-owned bootstrap paths
- tightly integrated compatibility modules
- upstream contributions that extend the built-in runtime bootstrap

If your mod is completely external and you only need agent-facing operations, prefer a registrar plus dedicated tools first.

## Suggested Integration Strategy

Use this order:

1. add a registrar and a focused tool provider for the mod-specific capability
2. validate the tool contract from a real MCP session
3. only add a `UiDriver` or capture provider when the generic UI tools need to understand your screen framework directly

This keeps the integration small and avoids over-coupling your mod to ModDevMCP internals too early.

## Side-Safety Checklist

- do not reference client-only classes from `common` or `server` registrars
- do not put dedicated-server logic in `client` registrars
- keep integrated-server support in mind when a tool runs on the client but acts on server state
- keep constructors public and no-arg for scanned registrar classes

## Verification Flow

Recommended downstream verification:

1. add the published `dev.vfyjxf:moddevmcp` dependency and the `dev.vfyjxf.moddevmcp` plugin
2. generate MCP client files with `createMcpClientFiles`
3. start `runClient`
4. connect an MCP client and call `moddev.status`
5. call your new tool
6. if you added UI adapters, also verify `moddev.ui_get_live_screen`, `moddev.ui_snapshot`, and `moddev.ui_capture`

If Gradle resolution fails, treat repository, TLS, or network failures separately from code failures.
