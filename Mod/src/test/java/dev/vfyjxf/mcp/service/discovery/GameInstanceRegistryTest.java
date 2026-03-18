package dev.vfyjxf.mcp.service.discovery;

import dev.vfyjxf.mcp.server.transport.JsonCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameInstanceRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void upsertWritesClientAndServerEntriesWithValidJson() throws Exception {
        var registryPath = tempDir.resolve("build/moddevmcp/game-instances.json");
        var registry = new GameInstanceRegistry(registryPath);

        var client = new GameInstanceRecord("http://127.0.0.1:47812", 47812, 1001L, Instant.parse("2026-03-18T01:00:00Z"), Instant.parse("2026-03-18T01:00:30Z"));
        var server = new GameInstanceRecord("http://127.0.0.1:47813", 47813, 1002L, Instant.parse("2026-03-18T01:01:00Z"), Instant.parse("2026-03-18T01:01:30Z"));

        registry.upsert("client", client);
        registry.upsert("server", server);

        assertEquals(client, registry.find("client").orElseThrow());
        assertEquals(server, registry.find("server").orElseThrow());

        var parsed = new JsonCodec().parseObject(Files.readAllBytes(registryPath));
        assertTrue(parsed.containsKey("instances"));
        assertEquals(tempDir.toAbsolutePath().normalize().toString(), parsed.get("projectPath"));
        assertTrue(parsed.get("updatedAt") instanceof String);

        @SuppressWarnings("unchecked")
        var instances = (Map<String, Object>) parsed.get("instances");
        @SuppressWarnings("unchecked")
        var parsedClient = (Map<String, Object>) instances.get("client");
        @SuppressWarnings("unchecked")
        var parsedServer = (Map<String, Object>) instances.get("server");
        assertEquals("http://127.0.0.1:47812", parsedClient.get("baseUrl"));
        assertEquals(47812, ((Number) parsedClient.get("port")).intValue());
        assertEquals(1001L, ((Number) parsedClient.get("pid")).longValue());
        assertEquals("2026-03-18T01:00:00Z", parsedClient.get("startedAt"));
        assertEquals("2026-03-18T01:00:30Z", parsedClient.get("lastSeen"));
        assertEquals("http://127.0.0.1:47813", parsedServer.get("baseUrl"));
        assertEquals(47813, ((Number) parsedServer.get("port")).intValue());
        assertEquals(1002L, ((Number) parsedServer.get("pid")).longValue());
        assertEquals("2026-03-18T01:01:00Z", parsedServer.get("startedAt"));
        assertEquals("2026-03-18T01:01:30Z", parsedServer.get("lastSeen"));
    }

    @Test
    void upsertOverwritesSameSideEntry() {
        var registry = new GameInstanceRegistry(tempDir.resolve("build/moddevmcp/game-instances.json"));

        var initial = new GameInstanceRecord("http://127.0.0.1:47812", 47812, 2001L, Instant.parse("2026-03-18T02:00:00Z"), Instant.parse("2026-03-18T02:00:10Z"));
        var updated = new GameInstanceRecord("http://127.0.0.1:57812", 57812, 2002L, Instant.parse("2026-03-18T02:01:00Z"), Instant.parse("2026-03-18T02:01:10Z"));

        registry.upsert("client", initial);
        registry.upsert("client", updated);

        assertEquals(updated, registry.find("client").orElseThrow());
    }

    @Test
    void removeDeletesOnlyRequestedSide() {
        var registry = new GameInstanceRegistry(tempDir.resolve("build/moddevmcp/game-instances.json"));
        var client = new GameInstanceRecord("http://127.0.0.1:47812", 47812, 3001L, Instant.parse("2026-03-18T03:00:00Z"), Instant.parse("2026-03-18T03:00:20Z"));
        var server = new GameInstanceRecord("http://127.0.0.1:47813", 47813, 3002L, Instant.parse("2026-03-18T03:01:00Z"), Instant.parse("2026-03-18T03:01:20Z"));

        registry.upsert("client", client);
        registry.upsert("server", server);

        registry.remove("client");

        assertFalse(registry.find("client").isPresent());
        assertEquals(server, registry.find("server").orElseThrow());
    }

    @Test
    void removeIfSamePreservesNewerSameSideEntry() {
        var registry = new GameInstanceRegistry(tempDir.resolve("build/moddevmcp/game-instances.json"));
        var older = new GameInstanceRecord("http://127.0.0.1:47812", 47812, 4001L, Instant.parse("2026-03-18T04:00:00Z"), Instant.parse("2026-03-18T04:00:20Z"));
        var newer = new GameInstanceRecord("http://127.0.0.1:57812", 57812, 4002L, Instant.parse("2026-03-18T04:01:00Z"), Instant.parse("2026-03-18T04:01:20Z"));

        registry.upsert("client", older);
        registry.upsert("client", newer);

        assertFalse(registry.removeIfSame("client", older));
        assertEquals(newer, registry.find("client").orElseThrow());
        assertTrue(registry.removeIfSame("client", newer));
        assertTrue(registry.find("client").isEmpty());
    }

    @Test
    void findReturnsEmptyWhenEntryPayloadIsMalformed() throws Exception {
        var registryPath = tempDir.resolve("build/moddevmcp/game-instances.json");
        Files.createDirectories(registryPath.getParent());
        Files.writeString(registryPath, """
                {
                  "projectPath":"%s",
                  "updatedAt":"2026-03-18T03:30:00Z",
                  "instances":{
                    "client":{
                      "baseUrl":"http://127.0.0.1:47812",
                      "port":"bad",
                      "pid":1,
                      "startedAt":"2026-03-18T03:00:00Z",
                      "lastSeen":"2026-03-18T03:00:20Z"
                    }
                  }
                }
                """.formatted(tempDir.toAbsolutePath().normalize().toString().replace("\\", "\\\\")));

        var registry = new GameInstanceRegistry(registryPath);
        assertTrue(registry.find("client").isEmpty());
    }

    @Test
    void findOnMissingRegistryDoesNotCreateArtifacts() {
        var registryPath = tempDir.resolve("build/moddevmcp/game-instances.json");
        var registry = new GameInstanceRegistry(registryPath);

        assertTrue(registry.find("client").isEmpty());
        assertFalse(Files.exists(registryPath));
        assertFalse(Files.exists(registryPath.resolveSibling("game-instances.lock")));
    }

    @Test
    void upsertRejectsPathsOutsideBuildModdevmcpLayout() {
        var invalidPath = tempDir.resolve("moddevmcp/game-instances.json");
        var registry = new GameInstanceRegistry(invalidPath);
        var record = new GameInstanceRecord("http://127.0.0.1:47812", 47812, 1L, Instant.parse("2026-03-18T01:00:00Z"), Instant.parse("2026-03-18T01:00:30Z"));

        assertThrows(IllegalStateException.class, () -> registry.upsert("client", record));
    }
}
