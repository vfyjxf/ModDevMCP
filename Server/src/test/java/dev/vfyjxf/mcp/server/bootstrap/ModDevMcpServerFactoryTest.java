package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpResource;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.ToolResult;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevMcpServerFactoryTest {

    @Test
    void factoryBuildsSdkServerWithRegisteredToolsAndResources() {
        var domainServer = new ModDevMcpServer();
        domainServer.registerProvider(registry -> registry.registerTool(
                new McpToolDefinition(
                        "demo.echo",
                        "Echo",
                        "Echoes the input",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("demo"),
                        "either",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> ToolResult.success(arguments)
        ));
        domainServer.registerResourceProvider(new DemoResourceProvider());

        McpSyncServer sdkServer = ModDevMcpServerFactory.createSdkServer(
                domainServer,
                new NoopServerTransport()
        );

        assertFalse(sdkServer.getServerCapabilities().tools() == null);
        assertFalse(sdkServer.getServerCapabilities().resources() == null);
        assertEquals(2, sdkServer.listTools().size());
        assertTrue(sdkServer.listTools().stream().anyMatch(tool -> tool.name().equals("demo.echo")));
        assertTrue(sdkServer.listTools().stream().anyMatch(tool -> tool.name().equals("moddev.status")));
        assertEquals(1, sdkServer.listResources().size());
        assertEquals("moddev://capture/ui-1", sdkServer.listResources().getFirst().uri());

        var result = ModDevMcpServerFactory.toSdkTool(domainServer.registry().findTool("demo.echo").orElseThrow())
                .callHandler()
                .apply(null, new McpSchema.CallToolRequest("demo.echo", Map.of("message", "hello")));

        assertFalse(result.isError());
        assertInstanceOf(Map.class, result.structuredContent());
        assertEquals("hello", ((Map<?, ?>) result.structuredContent()).get("message"));
    }

    private static final class DemoResourceProvider implements dev.vfyjxf.mcp.server.api.McpResourceProvider {

        @Override
        public java.util.Optional<McpResource> read(String uri) {
            return uri.equals("moddev://capture/ui-1")
                    ? java.util.Optional.of(new McpResource(
                    uri,
                    "image/png",
                    "Capture ui-1",
                    Map.of("width", 32),
                    "png".getBytes(StandardCharsets.UTF_8)
            ))
                    : java.util.Optional.empty();
        }

        @Override
        public List<McpResource> list() {
            return List.of(new McpResource(
                    "moddev://capture/ui-1",
                    "image/png",
                    "Capture ui-1",
                    Map.of("width", 32),
                    "png".getBytes(StandardCharsets.UTF_8)
            ));
        }
    }
}
