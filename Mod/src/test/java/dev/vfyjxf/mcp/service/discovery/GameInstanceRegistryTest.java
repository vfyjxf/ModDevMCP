package dev.vfyjxf.mcp.service.discovery;

import dev.vfyjxf.mcp.server.transport.JsonCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        var content = Files.readString(registryPath);
        assertTrue(content.contains("\"instances\""));
        assertTrue(content.contains("\"client\""));
        assertTrue(content.contains("\"server\""));
        assertTrue(content.contains("\"projectPath\""));
        assertTrue(content.contains("\"updatedAt\""));
        assertTrue(content.startsWith("{"));
        assertTrue(content.endsWith("}"));

        var parsed = new JsonCodec().parseObject(Files.readAllBytes(registryPath));
        assertTrue(parsed.containsKey("instances"));
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
}
