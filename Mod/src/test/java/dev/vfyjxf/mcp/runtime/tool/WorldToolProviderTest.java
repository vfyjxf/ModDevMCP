package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.world.WorldCreateRequest;
import dev.vfyjxf.mcp.runtime.world.WorldCreateResult;
import dev.vfyjxf.mcp.runtime.world.WorldDescriptor;
import dev.vfyjxf.mcp.runtime.world.WorldJoinRequest;
import dev.vfyjxf.mcp.runtime.world.WorldJoinResult;
import dev.vfyjxf.mcp.runtime.world.WorldListResult;
import dev.vfyjxf.mcp.runtime.world.WorldService;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldToolProviderTest {

    @Test
    void worldProviderRegistersExpectedToolNames() {
        var registry = new McpToolRegistry();
        new WorldToolProvider(new RecordingWorldService()).register(registry);

        assertTrue(registry.findTool("moddev.world_list").isPresent());
        assertTrue(registry.findTool("moddev.world_create").isPresent());
        assertTrue(registry.findTool("moddev.world_join").isPresent());
    }

    @Test
    void worldProviderDefinesClientSchemas() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        new WorldToolProvider(new RecordingWorldService()).register(server.registry());

        var list = server.registry().findTool("moddev.world_list").orElseThrow().definition();
        var create = server.registry().findTool("moddev.world_create").orElseThrow().definition();
        var join = server.registry().findTool("moddev.world_join").orElseThrow().definition();

        assertEquals("client", list.side());
        assertTrue(((Map<?, ?>) list.outputSchema().get("properties")).containsKey("worlds"));

        assertEquals("client", create.side());
        @SuppressWarnings("unchecked")
        var createProperties = (Map<String, Object>) create.inputSchema().get("properties");
        assertTrue(createProperties.containsKey("name"));
        assertTrue(createProperties.containsKey("joinAfterCreate"));
        assertTrue(createProperties.containsKey("worldType"));
        assertTrue(createProperties.containsKey("difficulty"));
        assertTrue(createProperties.containsKey("bonusChest"));
        assertTrue(createProperties.containsKey("generateStructures"));
        assertTrue(((List<?>) ((Map<?, ?>) createProperties.get("worldType")).get("enum")).contains("flat"));
        assertTrue(((Map<?, ?>) create.outputSchema().get("properties")).containsKey("joined"));

        assertEquals("client", join.side());
        assertTrue(((Map<?, ?>) join.inputSchema().get("properties")).containsKey("id"));
        assertTrue(((Map<?, ?>) join.inputSchema().get("properties")).containsKey("name"));
        assertTrue(((Map<?, ?>) join.outputSchema().get("properties")).containsKey("worldId"));
    }

    @Test
    void worldCreateDefaultsJoinAfterCreateToTrue() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var worldService = new RecordingWorldService();
        new WorldToolProvider(worldService).register(server.registry());

        var result = server.registry().findTool("moddev.world_create").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of("name", "Test World"));

        assertTrue(result.success());
        assertEquals(Boolean.TRUE, worldService.lastCreateRequest.joinAfterCreate());
        assertEquals("default", worldService.lastCreateRequest.worldType());
        assertEquals("normal", worldService.lastCreateRequest.difficulty());
        assertEquals(Boolean.FALSE, worldService.lastCreateRequest.bonusChest());
        assertEquals(Boolean.TRUE, worldService.lastCreateRequest.generateStructures());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("world-1", payload.get("worldId"));
        assertEquals(true, payload.get("joined"));
    }

    @Test
    void worldCreatePassesExtendedOptionsToService() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var worldService = new RecordingWorldService();
        new WorldToolProvider(worldService).register(server.registry());

        var result = server.registry().findTool("moddev.world_create").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")), Map.of(
                        "name", "Flat Test",
                        "gameMode", "creative",
                        "allowCheats", true,
                        "worldType", "flat",
                        "difficulty", "peaceful",
                        "bonusChest", true,
                        "generateStructures", false,
                        "seed", "1234",
                        "joinAfterCreate", true
                ));

        assertTrue(result.success());
        assertEquals("flat", worldService.lastCreateRequest.worldType());
        assertEquals("peaceful", worldService.lastCreateRequest.difficulty());
        assertEquals(Boolean.TRUE, worldService.lastCreateRequest.bonusChest());
        assertEquals(Boolean.FALSE, worldService.lastCreateRequest.generateStructures());
    }

    @Test
    void worldJoinPrefersIdOverName() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var worldService = new RecordingWorldService();
        new WorldToolProvider(worldService).register(server.registry());

        var result = server.registry().findTool("moddev.world_join").orElseThrow()
                .handler()
                .handle(new dev.vfyjxf.mcp.server.api.ToolCallContext("client", Map.of("runtimeId", "client-runtime")),
                        Map.of("id", "world-7", "name", "Ignored Name"));

        assertTrue(result.success());
        assertEquals("world-7", worldService.lastJoinRequest.id());
        assertEquals("Ignored Name", worldService.lastJoinRequest.name());
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) result.value();
        assertEquals("world-7", payload.get("worldId"));
    }

    private static final class RecordingWorldService implements WorldService {
        private WorldCreateRequest lastCreateRequest;
        private WorldJoinRequest lastJoinRequest;

        @Override
        public WorldListResult listWorlds() {
            return new WorldListResult(List.of(
                    new WorldDescriptor("world-1", "Test World", 123L, "survival", false, true)
            ));
        }

        @Override
        public WorldCreateResult createWorld(WorldCreateRequest request) {
            this.lastCreateRequest = request;
            return new WorldCreateResult("world-1", request.name(), true, request.joinAfterCreate());
        }

        @Override
        public WorldJoinResult joinWorld(WorldJoinRequest request) {
            this.lastJoinRequest = request;
            return new WorldJoinResult(request.id() == null ? "world-from-name" : request.id(),
                    request.name() == null ? "Resolved World" : request.name(),
                    true);
        }
    }
}
