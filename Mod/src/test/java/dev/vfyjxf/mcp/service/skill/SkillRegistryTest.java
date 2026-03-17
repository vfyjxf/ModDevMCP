package dev.vfyjxf.mcp.service.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
                "ui.snapshot",
                Set.of("ui", "snapshot"),
                true,
                "Use this skill to capture UI metadata."
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
                null,
                Set.of("entry"),
                false,
                "This is the entry skill."
        );

        assertEquals("moddev-entry", definition.skillId());
        assertEquals(Set.of("entry"), definition.tags());
        assertTrue(definition.markdown().contains("entry"));
    }

    @Test
    void actionSkillRequiresOperationId() {
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "ui-snapshot",
                "ui",
                SkillKind.ACTION,
                "UI Snapshot",
                "Capture UI metadata.",
                null,
                Set.of("ui"),
                true,
                "Capture a snapshot."
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
                "   ",
                Set.of("guide"),
                true,
                "Guide users and execute one operation."
        ));
    }

    @Test
    void skillRequiresNonBlankMarkdownAndNonNullTags() {
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                null,
                false,
                "This is the entry skill."
        ));
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of("entry"),
                false,
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
                null,
                Set.of("entry"),
                false,
                "This is the entry skill."
        );
        var action = new SkillDefinition(
                "ui-snapshot",
                "ui",
                SkillKind.ACTION,
                "UI Snapshot",
                "Capture UI metadata.",
                "ui.snapshot",
                Set.of("ui"),
                true,
                "Capture UI metadata."
        );
        var registry = new SkillRegistry(List.of(entry, action));

        assertEquals(2, registry.all().size());
        assertTrue(registry.findById("moddev-entry").isPresent());
        assertThrows(UnsupportedOperationException.class, () -> registry.all().add(entry));
    }
}
