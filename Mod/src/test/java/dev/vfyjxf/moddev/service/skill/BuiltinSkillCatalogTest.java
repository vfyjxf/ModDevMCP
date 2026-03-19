package dev.vfyjxf.moddev.service.skill;

import dev.vfyjxf.moddev.service.config.ServiceConfig;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;
import dev.vfyjxf.moddev.service.operation.OperationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinSkillCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void alwaysIncludesModdevEntrySkill() {
        var catalog = catalog(sampleOperations());

        var entrySkill = catalog.skillRegistry().findById("moddev-usage").orElseThrow();
        assertEquals(SkillKind.GUIDANCE, entrySkill.kind());
        assertNull(entrySkill.operationId());
        assertTrue(entrySkill.markdown().contains("http://127.0.0.1:47812/api/v1/status"));
        assertTrue(entrySkill.markdown().contains("build/moddevmcp/game-instances.json"));
        assertTrue(entrySkill.markdown().contains("Probe each candidate"));
        assertTrue(entrySkill.markdown().contains("required only when both eligible sides are live"));
        assertTrue(entrySkill.markdown().contains("Do not use shell scripts"));
    }

    @Test
    void categorySkillMarkdownLoadsFromBundledResources() {
        var catalog = catalog(sampleOperations());

        var categorySkill = catalog.skillRegistry().findById("status").orElseThrow();
        assertEquals(SkillKind.GUIDANCE, categorySkill.kind());
        assertTrue(categorySkill.markdown().contains("Status skills describe how to inspect service readiness"));
        assertTrue(categorySkill.markdown().contains("curl"));
        assertTrue(categorySkill.markdown().contains("/api/v1/status"));
    }

    @Test
    void guidanceOnlySkillsDoNotNeedOperationIds() {
        var catalog = catalog(sampleOperations());

        var entrySkill = catalog.skillRegistry().findById("moddev-usage").orElseThrow();
        assertEquals(SkillKind.GUIDANCE, entrySkill.kind());
        assertNull(entrySkill.operationId());
    }

    @Test
    void hybridOperationSkillsEmbedOperationIdsAndCurlExamples() {
        var catalog = catalog(sampleOperations());

        var operationSkill = catalog.skillRegistry().findById("command.execute").orElseThrow();
        assertEquals(SkillKind.HYBRID, operationSkill.kind());
        assertEquals("command.execute", operationSkill.operationId());
        assertTrue(operationSkill.markdown().contains("Operation id: `command.execute`"));
        assertTrue(operationSkill.markdown().contains("curl -X POST"));
        assertTrue(operationSkill.markdown().contains("\"operationId\":\"command.execute\""));
        assertTrue(operationSkill.markdown().contains("Send `targetSide` when both client and server are connected."));
        assertTrue(operationSkill.markdown().contains("do not replace it with shell-driven keyboard input"));
    }

    @Test
    void reusableUsageSkillDocumentsProjectLocalDiscoveryFlow() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var usageSkill = Files.readString(rootDir.resolve("skills/moddevmcp-usage/SKILL.md"));
        assertTrue(usageSkill.contains("GET http://127.0.0.1:47812/api/v1/status"));
        assertTrue(usageSkill.contains("<gradleProject>/build/moddevmcp/game-instances.json"));
        assertTrue(usageSkill.contains("Probe each candidate `baseUrl` from the registry with `GET /api/v1/status`"));
        assertTrue(usageSkill.contains("targetSide` is required only when both eligible sides are live"));
        assertTrue(usageSkill.contains("1. Try the default probe `GET http://127.0.0.1:47812/api/v1/status`."));
        assertTrue(usageSkill.contains("5. After you have a live `baseUrl`, read the exported entry skill"));
        assertTrue(usageSkill.contains("3. pick a live `baseUrl`"));
        assertTrue(usageSkill.contains("4. `GET <baseUrl>/api/v1/skills/moddev-usage/markdown`"));
        assertFalse(usageSkill.contains("1. Read the exported entry skill"));
    }

    @Test
    void entryMarkdownKeepsDefaultProbeWhenRenderedWithFallbackBaseUri() {
        var markdown = new SkillMarkdownLoader().loadEntryMarkdown("http://127.0.0.1:57999");
        assertTrue(markdown.contains("curl http://127.0.0.1:47812/api/v1/status"));
        assertFalse(markdown.contains("curl http://127.0.0.1:57999/api/v1/status"));
    }

    private BuiltinSkillCatalog.Catalog catalog(OperationRegistry operationRegistry) {
        return new BuiltinSkillCatalog(new SkillMarkdownLoader())
                .build(new ServiceConfig("127.0.0.1", 47812, tempDir), operationRegistry);
    }

    private static OperationRegistry sampleOperations() {
        return new OperationRegistry(java.util.List.of(
                new OperationDefinition(
                        "status.get",
                        "status",
                        "Get Status",
                        "Returns current service and game readiness.",
                        false,
                        Set.of(),
                        Map.of(),
                        Map.of("operationId", "status.get")
                ),
                new OperationDefinition(
                        "command.execute",
                        "command",
                        "Execute Command",
                        "Executes a command on the selected connected side.",
                        true,
                        Set.of("client", "server"),
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "command", Map.of("type", "string")
                                ),
                                "required", java.util.List.of("command")
                        ),
                        Map.of(
                                "operationId", "command.execute",
                                "targetSide", "server",
                                "input", Map.of("command", "/say hi")
                        )
                )
        ));
    }
}


