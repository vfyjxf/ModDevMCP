package dev.vfyjxf.moddev.runtime.tool;

import dev.vfyjxf.moddev.runtime.hotswap.HotswapRuntimeConfig;
import dev.vfyjxf.moddev.runtime.hotswap.HotswapService;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotswapToolProviderTest {

    @Test
    void hotswapToolExposesCompileToggleAndTargetSideInInputSchema() {
        var registry = new McpToolRegistry();
        var config = new HotswapRuntimeConfig(Path.of("."), Path.of("."), ":compileJava", Path.of("build/classes/java/main"));

        new HotswapToolProvider(new HotswapService(config)).register(registry);

        var definition = registry.findTool("moddev.hotswap").orElseThrow().definition();
        assertEquals("common", definition.side());
        assertEquals("object", definition.inputSchema().get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
        assertEquals(List.of(), definition.inputSchema().get("required"));
        assertTrue(properties.containsKey("compile"));
        assertTrue(properties.containsKey("targetSide"));

        @SuppressWarnings("unchecked")
        Map<String, Object> targetSide = (Map<String, Object>) properties.get("targetSide");
        assertEquals(List.of("client", "server"), targetSide.get("enum"));
        assertEquals(
                "Optional runtime routing hint. Only needed when both client and server runtimes are connected to the same gateway.",
                targetSide.get("description")
        );
    }
}

