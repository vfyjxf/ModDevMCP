package dev.vfyjxf.mcp.service.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRegistryTest {

    @Test
    void registryRequiresModdevEntrySkill() {
        var definitions = List.of(new SkillDefinition(
                "ui-snapshot",
                "ui",
                SkillKind.ACTION,
                "UI Snapshot",
                "Capture UI metadata.",
                "ui.snapshot"
        ));

        assertThrows(IllegalArgumentException.class, () -> new SkillRegistry(definitions));
    }

    @Test
    void guidanceSkillDoesNotRequireOperationId() {
        var definition = new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null
        );

        assertEquals("moddev-entry", definition.skillId());
    }

    @Test
    void actionSkillRequiresOperationId() {
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "ui-snapshot",
                "ui",
                SkillKind.ACTION,
                "UI Snapshot",
                "Capture UI metadata.",
                null
        ));
    }

    @Test
    void hybridSkillRequiresOperationId() {
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "guide-and-run",
                "ui",
                SkillKind.HYBRID,
                "Guide and Run",
                "Explain and run.",
                "   "
        ));
    }

    @Test
    void registryExposesReadOnlyDefinitions() {
        var entry = new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null
        );
        var action = new SkillDefinition(
                "ui-snapshot",
                "ui",
                SkillKind.ACTION,
                "UI Snapshot",
                "Capture UI metadata.",
                "ui.snapshot"
        );
        var registry = new SkillRegistry(List.of(entry, action));

        assertEquals(2, registry.all().size());
        assertTrue(registry.findById("moddev-entry").isPresent());
        assertThrows(UnsupportedOperationException.class, () -> registry.all().add(entry));
    }
}
