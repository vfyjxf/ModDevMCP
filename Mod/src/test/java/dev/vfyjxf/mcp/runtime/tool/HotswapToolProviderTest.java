package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.hotswap.HotswapRuntimeConfig;
import dev.vfyjxf.mcp.runtime.hotswap.HotswapService;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotswapToolProviderTest {

    @Test
    void hotswapToolExposesCompileToggleInInputSchema() {
        var registry = new McpToolRegistry();
        var config = new HotswapRuntimeConfig(Path.of("."), ":Mod:compileJava", Path.of("Mod/build/classes/java/main"));

        new HotswapToolProvider(new HotswapService(config)).register(registry);

        var definition = registry.findTool("moddev.hotswap").orElseThrow().definition();
        assertEquals("object", definition.inputSchema().get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
        assertTrue(properties.containsKey("compile"));
    }
}
