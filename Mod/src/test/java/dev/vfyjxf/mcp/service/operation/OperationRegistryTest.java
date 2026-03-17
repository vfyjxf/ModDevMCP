package dev.vfyjxf.mcp.service.operation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationRegistryTest {

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
}
