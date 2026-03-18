package dev.vfyjxf.mcp.service.skill;

import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.operation.OperationDefinition;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

