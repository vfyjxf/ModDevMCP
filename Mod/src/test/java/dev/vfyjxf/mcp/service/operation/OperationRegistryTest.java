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
        var skillIds = new java.util.LinkedHashSet<String>();
        skillIds.add("moddev-entry");
        skillIds.add("ui-snapshot");
        var operationIds = new java.util.LinkedHashSet<String>();
        operationIds.add("ui.snapshot");
        operationIds.add("ui.query");
        var definition = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                skillIds,
                operationIds
        );

        assertEquals(List.of("moddev-entry", "ui-snapshot"), List.copyOf(definition.skillIds()));
        assertEquals(List.of("ui.snapshot", "ui.query"), List.copyOf(definition.operationIds()));
    }

    @Test
    void registryRejectsNullDefinitionsCollectionAndNullMembers() {
        assertThrows(IllegalArgumentException.class, () -> new OperationRegistry(null));
        var withNullMember = new java.util.ArrayList<OperationDefinition>();
        withNullMember.add(new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        ));
        withNullMember.add(null);
        assertThrows(IllegalArgumentException.class, () -> new OperationRegistry(withNullMember));
    }

    @Test
    void categoryDefinitionRejectsBlankOrNullMembers() {
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                Set.of(" "),
                Set.of("ui.snapshot")
        ));
        var operationIds = new java.util.LinkedHashSet<String>();
        operationIds.add("ui.snapshot");
        operationIds.add(null);
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                Set.of("ui-snapshot"),
                operationIds
        ));
    }

    @Test
    void metadataIncludesTargetSideAndCategoryOwnership() {
        var availableTargetSides = new java.util.LinkedHashSet<String>();
        availableTargetSides.add("client");
        availableTargetSides.add("server");
        var definition = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                availableTargetSides,
                Map.of("type", "object"),
                Map.of("operationId", "ui.snapshot")
        );

        assertEquals("ui.snapshot", definition.operationId());
        assertEquals("ui", definition.categoryId());
        assertTrue(definition.supportsTargetSide());
        assertEquals(List.of("client", "server"), List.copyOf(definition.availableTargetSides()));
    }

    @Test
    void nestedInputSchemaAndExampleRequestAreDeeplyImmutable() {
        var nestedSchema = new LinkedHashMap<String, Object>();
        nestedSchema.put("type", "object");
        var schema = new LinkedHashMap<String, Object>();
        schema.put("properties", nestedSchema);
        var nestedList = new java.util.ArrayList<>(List.of("client"));
        var input = new LinkedHashMap<String, Object>();
        input.put("values", nestedList);
        var request = new LinkedHashMap<String, Object>();
        request.put("operationId", "ui.snapshot");
        request.put("input", input);

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
        assertThrows(UnsupportedOperationException.class, () -> ((List<String>) ((Map<String, Object>) definition.exampleRequest().get("input")).get("values")).add("server"));

        nestedSchema.put("postConstruct", "changed");
        nestedList.add("server");

        assertEquals(Map.of("type", "object"), definition.inputSchema().get("properties"));
        assertEquals(List.of("client"), ((Map<String, Object>) definition.exampleRequest().get("input")).get("values"));
    }

    @Test
    void nestedSetInExampleRequestIsDeeplyImmutableAndSourceIsolated() {
        var tags = new java.util.LinkedHashSet<String>();
        tags.add("first");
        tags.add("second");
        var input = new LinkedHashMap<String, Object>();
        input.put("tags", tags);
        var request = new LinkedHashMap<String, Object>();
        request.put("operationId", "ui.snapshot");
        request.put("input", input);

        var definition = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                request
        );

        var frozenTags = (Set<String>) ((Map<String, Object>) definition.exampleRequest().get("input")).get("tags");
        assertEquals(List.of("first", "second"), List.copyOf(frozenTags));
        assertThrows(UnsupportedOperationException.class, () -> frozenTags.add("third"));

        tags.add("third");
        assertEquals(List.of("first", "second"), List.copyOf(frozenTags));
    }

    @Test
    void exampleRequestRejectsNestedMapWithNonStringOrNullKey() {
        var badInput = new java.util.LinkedHashMap<Object, Object>();
        badInput.put(1, "bad");
        var badRequest = new LinkedHashMap<String, Object>();
        badRequest.put("operationId", "ui.snapshot");
        badRequest.put("input", badInput);
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                badRequest
        ));

        var nullKeyInput = new java.util.LinkedHashMap<Object, Object>();
        nullKeyInput.put(null, "bad");
        var nullKeyRequest = new LinkedHashMap<String, Object>();
        nullKeyRequest.put("operationId", "ui.snapshot");
        nullKeyRequest.put("input", nullKeyInput);
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                nullKeyRequest
        ));
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
        example.put("operationId", "ui.snapshot");
        var input = new LinkedHashMap<String, Object>();
        input.put("k2", "v2");
        input.put("k1", "v1");
        example.put("input", input);

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
        assertEquals(List.of("requestId", "operationId", "input"), List.copyOf(definition.exampleRequest().keySet()));
        assertEquals(List.of("k2", "k1"), List.copyOf(((Map<String, Object>) definition.exampleRequest().get("input")).keySet()));
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
    void operationRejectsUnsupportedTargetSideValue() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("proxy"),
                Map.of(),
                Map.of()
        ));
    }

    @Test
    void operationRejectsBlankOrNullAvailableTargetSideMembers() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of(" "),
                Map.of(),
                Map.of()
        ));
        var sides = new java.util.LinkedHashSet<String>();
        sides.add("client");
        sides.add(null);
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                sides,
                Map.of(),
                Map.of()
        ));
    }

    @Test
    void exampleRequestRejectsUnknownTopLevelKey() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("requestId", "r-1", "unknown", "value")
        ));
    }

    @Test
    void exampleRequestRejectsMismatchedOperationId() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("operationId", "world.list")
        ));
    }

    @Test
    void exampleRequestRejectsMissingOperationIdWhenNonEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("requestId", "r-1")
        ));
    }

    @Test
    void exampleRequestRejectsNonStringOperationId() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("operationId", 42)
        ));
    }

    @Test
    void exampleRequestRejectsInvalidTargetSideShapeAndValue() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("targetSide", List.of("client"))
        ));
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("targetSide", "server")
        ));
    }

    @Test
    void exampleRequestRejectsNonStringRequestId() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("operationId", "ui.snapshot", "requestId", 123)
        ));
    }

    @Test
    void exampleRequestRejectsNonMapInput() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("operationId", "ui.snapshot", "input", "not-an-object")
        ));
    }

    @Test
    void exampleRequestRejectsTargetSideWhenOperationDoesNotSupportIt() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "world.list",
                "world",
                "List Worlds",
                "List available worlds.",
                false,
                Set.of(),
                Map.of(),
                Map.of("targetSide", "server")
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

    @Test
    void validateCategoryOwnershipRejectsMismatchedOrdering() {
        var first = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        );
        var second = new OperationDefinition(
                "ui.query",
                "ui",
                "UI Query",
                "Query UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        );
        var registry = new OperationRegistry(List.of(first, second));
        var reversedOperationIds = new java.util.LinkedHashSet<String>();
        reversedOperationIds.add("ui.query");
        reversedOperationIds.add("ui.snapshot");
        var category = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                Set.of("ui-snapshot"),
                reversedOperationIds
        );

        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(category));
    }
}
