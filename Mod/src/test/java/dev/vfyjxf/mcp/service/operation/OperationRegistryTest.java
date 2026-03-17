package dev.vfyjxf.mcp.service.operation;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationRegistryTest {

    @Test
    void categoryDefinitionOwnsSkillIdsAndOperationIds() {
        var definition = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                Set.of("moddev-entry", "ui-snapshot"),
                Set.of("ui.snapshot")
        );

        assertEquals(Set.of("moddev-entry", "ui-snapshot"), definition.skillIds());
        assertEquals(Set.of("ui.snapshot"), definition.operationIds());
    }

    @Test
    void metadataIncludesTargetSideAndCategoryOwnership() {
        var definition = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of("type", "object"),
                Map.of("operationId", "ui.snapshot")
        );

        assertEquals("ui.snapshot", definition.operationId());
        assertEquals("ui", definition.categoryId());
        assertTrue(definition.supportsTargetSide());
        assertEquals(Set.of("client"), definition.availableTargetSides());
    }

    @Test
    void nestedInputSchemaAndExampleRequestAreDeeplyImmutable() {
        var nestedSchema = new LinkedHashMap<String, Object>();
        nestedSchema.put("type", "object");
        var schema = new LinkedHashMap<String, Object>();
        schema.put("properties", nestedSchema);
        var nestedList = new java.util.ArrayList<>(List.of("client"));
        var request = new LinkedHashMap<String, Object>();
        request.put("targetSides", nestedList);

        var definition = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                schema,
                request
        );

        assertThrows(UnsupportedOperationException.class, () -> ((Map<String, Object>) definition.inputSchema().get("properties")).put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> ((List<String>) definition.exampleRequest().get("targetSides")).add("server"));

        nestedSchema.put("postConstruct", "changed");
        nestedList.add("server");

        assertEquals(Map.of("type", "object"), definition.inputSchema().get("properties"));
        assertEquals(List.of("client"), definition.exampleRequest().get("targetSides"));
    }

    @Test
    void frozenMetadataPreservesInsertionOrderForNestedMaps() {
        var nestedSchema = new LinkedHashMap<String, Object>();
        nestedSchema.put("beta", 2);
        nestedSchema.put("alpha", 1);
        var schema = new LinkedHashMap<String, Object>();
        schema.put("zeta", 26);
        schema.put("eta", nestedSchema);
        var example = new LinkedHashMap<String, Object>();
        example.put("requestId", "r-1");
        example.put("input", new LinkedHashMap<>(Map.of("k2", "v2", "k1", "v1")));

        var definition = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                schema,
                example
        );

        assertEquals(List.of("zeta", "eta"), List.copyOf(definition.inputSchema().keySet()));
        assertEquals(List.of("beta", "alpha"), List.copyOf(((Map<String, Object>) definition.inputSchema().get("eta")).keySet()));
        assertEquals(List.of("requestId", "input"), List.copyOf(definition.exampleRequest().keySet()));
    }

    @Test
    void registryGroupsOperationsByCategoryAndIsReadOnly() {
        var snapshot = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        );
        var worldList = new OperationDefinition(
                "world.list",
                "world",
                "List Worlds",
                "List available worlds.",
                false,
                Set.of(),
                Map.of(),
                Map.of()
        );
        var registry = new OperationRegistry(List.of(snapshot, worldList));

        assertEquals(2, registry.all().size());
        assertEquals(List.of(snapshot), registry.findByCategoryId("ui"));
        assertTrue(registry.findById("world.list").isPresent());
        assertThrows(UnsupportedOperationException.class, () -> registry.all().add(snapshot));
    }

    @Test
    void nonTargetedOperationDoesNotAllowAvailableTargetSides() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "world.list",
                "world",
                "List Worlds",
                "List available worlds.",
                false,
                Set.of("server"),
                Map.of(),
                Map.of()
        ));
    }

    @Test
    void validateCategoryOwnershipRejectsMismatch() {
        var snapshot = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        );
        var registry = new OperationRegistry(List.of(snapshot));
        var brokenCategory = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                Set.of("ui-snapshot"),
                Set.of("ui.missing")
        );

        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(brokenCategory));
    }
}
