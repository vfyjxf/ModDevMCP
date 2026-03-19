package dev.vfyjxf.moddev.server.runtime;

import dev.vfyjxf.moddev.server.api.McpResource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpResourceRegistryTest {

    @Test
    void registryStoresProvidersAndReadsResourcesByUri() {
        var registry = new McpResourceRegistry();
        registry.registerProvider(uri -> uri.equals("moddev://capture/ui-1")
                ? java.util.Optional.of(new McpResource(uri, "image/png", "Capture ui-1", Map.of("width", 32), new byte[]{1, 2, 3}))
                : java.util.Optional.empty());

        var resource = registry.read("moddev://capture/ui-1").orElseThrow();

        assertEquals("image/png", resource.mimeType());
        assertEquals(3, resource.content().length);
        assertTrue(registry.read("moddev://capture/missing").isEmpty());
    }
}

