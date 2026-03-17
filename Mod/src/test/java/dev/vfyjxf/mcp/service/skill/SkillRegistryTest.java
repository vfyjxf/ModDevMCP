package dev.vfyjxf.mcp.service.skill;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import dev.vfyjxf.mcp.service.operation.OperationDefinition;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
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
    void registryRejectsExecutableModdevEntrySkill() {
        var executableEntry = new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.ACTION,
                "Entry",
                "Start here.",
                "ui.snapshot",
                Set.of("entry"),
                false,
                "This is the entry skill."
        );

        assertThrows(IllegalArgumentException.class, () -> new SkillRegistry(List.of(executableEntry)));
    }

    @Test
    void registryRejectsGameRequiredModdevEntrySkill() {
        var gameRequiredEntry = new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of("entry"),
                true,
                "This is the entry skill."
        );

        assertThrows(IllegalArgumentException.class, () -> new SkillRegistry(List.of(gameRequiredEntry)));
    }

    @Test
    void registryRejectsNullDefinitionsCollectionAndNullMembers() {
        assertThrows(IllegalArgumentException.class, () -> new SkillRegistry(null));
        var withNullMember = new java.util.ArrayList<SkillDefinition>();
        withNullMember.add(new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of("entry"),
                false,
                "This is the entry skill."
        ));
        withNullMember.add(null);
        assertThrows(IllegalArgumentException.class, () -> new SkillRegistry(withNullMember));
    }

    @Test
    void registryApiRejectsBlankIdsAndNullCategoryValidationInput() {
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
        var registry = new SkillRegistry(List.of(entry));

        assertThrows(IllegalArgumentException.class, () -> registry.findById(null));
        assertThrows(IllegalArgumentException.class, () -> registry.findById(" "));
        assertThrows(IllegalArgumentException.class, () -> registry.findById(" moddev-entry "));
        assertThrows(IllegalArgumentException.class, () -> registry.findByCategoryId(null));
        assertThrows(IllegalArgumentException.class, () -> registry.findByCategoryId(" "));
        assertThrows(IllegalArgumentException.class, () -> registry.findByCategoryId(" status "));
        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(null));
    }

    @Test
    void guidanceSkillDoesNotRequireOperationId() {
        var markdown = "  # Entry Skill\n";
        var tags = new java.util.LinkedHashSet<String>();
        tags.add("guide");
        tags.add("entry");
        var definition = new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                tags,
                false,
                markdown
        );

        assertEquals("moddev-entry", definition.skillId());
        assertEquals(List.of("entry", "guide"), List.copyOf(definition.tags()));
        assertTrue(definition.markdown().contains("Entry"));
        assertEquals(markdown, definition.markdown());
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
    void skillRejectsBlankOrNullTagMembers() {
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of(" "),
                false,
                "This is the entry skill."
        ));
        var tagsWithNull = new java.util.LinkedHashSet<String>();
        tagsWithNull.add("entry");
        tagsWithNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                tagsWithNull,
                false,
                "This is the entry skill."
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

    @Test
    void validateCategoryOwnershipRejectsMismatch() {
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
        var brokenCategory = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                List.of("ui-missing"),
                List.of("ui.snapshot")
        );

        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(brokenCategory));
    }

    @Test
    void validateCategoryOwnershipRejectsMismatchedOrdering() {
        var entry = new SkillDefinition(
                "moddev-entry",
                "ui",
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
        var reversedSkillIds = List.of("ui-snapshot", "moddev-entry");
        var category = new CategoryDefinition(
                "ui",
                "UI",
                "Screen and interaction tools.",
                reversedSkillIds,
                List.of("ui.snapshot")
        );

        assertThrows(IllegalArgumentException.class, () -> registry.validateCategoryOwnership(category));
    }

    @Test
    void validateOperationBindingsRejectsMissingOperationReference() {
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
        var skillRegistry = new SkillRegistry(List.of(entry, action));
        var operationRegistry = new OperationRegistry(List.of());

        assertThrows(IllegalArgumentException.class, () -> skillRegistry.validateOperationBindings(operationRegistry));
    }

    @Test
    void validateOperationBindingsRejectsCrossCategoryReference() {
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
        var skillRegistry = new SkillRegistry(List.of(entry, action));
        var operationRegistry = new OperationRegistry(List.of(
                new OperationDefinition(
                        "ui.snapshot",
                        "status",
                        "UI Snapshot",
                        "Capture UI metadata.",
                        true,
                        Set.of("client"),
                        java.util.Map.of(),
                        java.util.Map.of()
                )
        ));

        assertThrows(IllegalArgumentException.class, () -> skillRegistry.validateOperationBindings(operationRegistry));
    }

    @Test
    void skillDefinitionRejectsPaddedIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                " moddev-entry",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of("entry"),
                false,
                "This is the entry skill."
        ));
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "moddev-entry",
                " status ",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of("entry"),
                false,
                "This is the entry skill."
        ));
        assertThrows(IllegalArgumentException.class, () -> new SkillDefinition(
                "ui-snapshot",
                "ui",
                SkillKind.ACTION,
                "UI Snapshot",
                "Capture UI metadata.",
                " ui.snapshot ",
                Set.of("ui"),
                true,
                "Capture UI metadata."
        ));
    }
}
