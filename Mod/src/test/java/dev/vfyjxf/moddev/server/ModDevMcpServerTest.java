package dev.vfyjxf.moddev.server;

import dev.vfyjxf.moddev.server.api.McpResource;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevMcpServerTest {

    @Test
    void serverRegistersProviderToolsIntoRegistry() {
        var registry = new McpToolRegistry();
        var server = new ModDevMcpServer(registry);

        server.registerProvider(registry1 -> registry1.registerTool(
                new McpToolDefinition("demo.echo", "Echo", "Echo tool", Map.of(), Map.of(), List.of(), "either", false, false, "public", "public"),
                (context, args) -> ToolResult.success(args)
        ));

        assertTrue(registry.findTool("demo.echo").isPresent());
    }

    @Test
    void serverRegistersResourceProvidersIntoResourceRegistry() {
        var registry = new McpToolRegistry();
        var server = new ModDevMcpServer(registry);

        server.registerResourceProvider(uri -> uri.equals("moddev://capture/ui-1")
                ? java.util.Optional.of(new McpResource(uri, "image/png", "Capture", Map.of(), new byte[]{1}))
                : java.util.Optional.empty());

        assertEquals("image/png", server.resourceRegistry().read("moddev://capture/ui-1").orElseThrow().mimeType());
    }
}

