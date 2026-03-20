package dev.vfyjxf.moddev.service.operation;

import dev.vfyjxf.moddev.service.category.CategoryDefinition;
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
        var skillIds = List.of("moddev-usage", "ui-snapshot");
        var operationIds = List.of("ui.snapshot", "ui.query");
        var definition = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                skillIds,
                operationIds
        );

        assertEquals(List.of("moddev-usage", "ui-snapshot"), definition.skillIds());
        assertEquals(List.of("ui.snapshot", "ui.query"), definition.operationIds());
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
    void registryApiRejectsBlankIdsAndNullCategoryValidationInput() {
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

        assertThrows(IllegalArgumentException.class, () -> registry.findById(null));
        assertThrows(IllegalArgumentException.class, () -> registry.findById(" "));
        assertThrows(IllegalArgumentException.class, () -> registry.findById(" ui.snapshot "));
        assertThrows(IllegalArgumentException.class, () -> registry.findByCategoryId(null));
        assertThrows(IllegalArgumentException.class, () -> registry.findByCategoryId(" "));
        assertThrows(IllegalArgumentException.class, () -> registry.findByCategoryId(" ui "));
        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(null));
    }

    @Test
    void categoryDefinitionRejectsBlankOrNullMembers() {
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of(" "),
                List.of("ui.snapshot")
        ));
        var operationIds = new java.util.ArrayList<String>();
        operationIds.add("ui.snapshot");
        operationIds.add(null);
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of("ui-snapshot"),
                operationIds
        ));
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                " ui ",
                "UI",
                "Screen and interaction tools.",
                List.of("ui-snapshot"),
                List.of("ui.snapshot")
        ));
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of(" ui-snapshot "),
                List.of("ui.snapshot")
        ));
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of("ui-snapshot", "ui-snapshot"),
                List.of("ui.snapshot")
        ));
        assertThrows(IllegalArgumentException.class, () -> new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of("ui-snapshot"),
                List.of("ui.snapshot", "ui.snapshot")
        ));
    }

    @Test
    void metadataIncludesTargetSideAndCategoryOwnership() {
        var availableTargetSides = new java.util.LinkedHashSet<String>();
        availableTargetSides.add("server");
        availableTargetSides.add("client");
        var definition = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                availableTargetSides,
                Map.of("type", "object"),
                Map.of("operationId", "ui.snapshot", "targetSide", "client")
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
    void metadataRejectsNestedSetValue() {
        var input = new LinkedHashMap<String, Object>();
        input.put("tags", Set.of("first", "second"));
        var request = new LinkedHashMap<String, Object>();
        request.put("operationId", "ui.snapshot");
        request.put("input", input);

        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                request
        ));
    }

    @Test
    void metadataRejectsNonJsonLeafValue() {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("custom", new Object());

        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                schema,
                Map.of()
        ));
    }

    @Test
    void metadataRejectsBlankMapKeys() {
        var schemaWithBlankKey = new LinkedHashMap<String, Object>();
        schemaWithBlankKey.put(" ", "x");
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                schemaWithBlankKey,
                Map.of()
        ));

        var nested = new LinkedHashMap<String, Object>();
        nested.put(" ", 1);
        var schemaWithNestedBlank = new LinkedHashMap<String, Object>();
        schemaWithNestedBlank.put("props", nested);
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                schemaWithNestedBlank,
                Map.of()
        ));
    }

    @Test
    void metadataSupportsNullElementsInsideListsWithoutNpeLeak() {
        var input = new LinkedHashMap<String, Object>();
        input.put("values", java.util.Arrays.asList(null, "ok"));
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

        var frozenValues = (List<Object>) ((Map<String, Object>) definition.exampleRequest().get("input")).get("values");
        assertEquals(2, frozenValues.size());
        assertEquals(null, frozenValues.get(0));
        assertEquals("ok", frozenValues.get(1));
        assertThrows(UnsupportedOperationException.class, () -> frozenValues.add("x"));
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
    void exampleRequestRejectsInvalidTopLevelKeyShape() {
        var nonStringKey = new LinkedHashMap<Object, Object>();
        nonStringKey.put(1, "value");
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                (Map<String, Object>) (Map<?, ?>) nonStringKey
        ));

        var blankKey = new LinkedHashMap<Object, Object>();
        blankKey.put(" ", "value");
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                (Map<String, Object>) (Map<?, ?>) blankKey
        ));

        var nullKey = new LinkedHashMap<Object, Object>();
        nullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                (Map<String, Object>) (Map<?, ?>) nullKey
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
    void exampleRequestRejectsBlankRequestId() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("operationId", "ui.snapshot", "requestId", " ")
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
    void metadataRejectsNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of("value", Double.NaN),
                Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of("operationId", "ui.snapshot", "input", Map.of("threshold", Double.POSITIVE_INFINITY))
        ));
    }

    @Test
    void metadataRejectsUnsupportedNumberSubtype() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of("value", new java.util.concurrent.atomic.AtomicInteger(1)),
                Map.of()
        ));
    }

    @Test
    void operationDefinitionRejectsPaddedIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                " ui.snapshot ",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                " ui ",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
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
                List.of("ui-snapshot"),
                List.of("ui.missing")
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
        var reversedOperationIds = List.of("ui.query", "ui.snapshot");
        var category = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of("ui-snapshot"),
                reversedOperationIds
        );

        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(category));
    }

    @Test
    void validateDeclaredCategoriesRejectsOrphanOperationCategoryId() {
        var operation = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        );
        var registry = new OperationRegistry(List.of(operation));
        var categories = List.of(new CategoryDefinition(
                "status",
                "Status",
                "Service status.",
                List.of("moddev-usage"),
                List.of()
        ));

        assertThrows(IllegalArgumentException.class, () -> registry.validateDeclaredCategories(categories));
    }

    @Test
    void validateDeclaredCategoriesRejectsOwnershipMismatchWithoutSeparateCall() {
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
        var categories = List.of(new CategoryDefinition(
                "ui",
                "UI",
                "Screen tools.",
                List.of("ui-snapshot"),
                List.of("ui.snapshot")
        ));

        assertThrows(IllegalArgumentException.class, () -> registry.validateDeclaredCategories(categories));
    }

    @Test
    void validateDeclaredCategoriesRejectsDuplicateCategoryIds() {
        var operation = new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client"),
                Map.of(),
                Map.of()
        );
        var registry = new OperationRegistry(List.of(operation));
        var categories = List.of(
                new CategoryDefinition("ui", "UI", "Screen tools.", List.of("ui-snapshot"), List.of("ui.snapshot")),
                new CategoryDefinition("ui", "UI Duplicate", "Duplicate.", List.of("ui-snapshot"), List.of("ui.snapshot"))
        );

        assertThrows(IllegalArgumentException.class, () -> registry.validateDeclaredCategories(categories));
    }

    @Test
    void exampleRequestRequiresTargetSideWhenOperationSupportsMultipleSides() {
        assertThrows(IllegalArgumentException.class, () -> new OperationDefinition(
                "ui.snapshot",
                "ui",
                "UI Snapshot",
                "Capture UI metadata.",
                true,
                Set.of("client", "server"),
                Map.of(),
                Map.of("operationId", "ui.snapshot")
        ));
    }
}


