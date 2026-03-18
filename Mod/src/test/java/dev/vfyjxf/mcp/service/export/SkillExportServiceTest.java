package dev.vfyjxf.mcp.service.export;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.skill.SkillDefinition;
import dev.vfyjxf.mcp.service.skill.SkillKind;
import dev.vfyjxf.mcp.service.skill.SkillRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exportWritesManifestSkillsCategoriesAndIndexes() throws Exception {
        var service = exportService();

        service.exportAll();

        assertTrue(Files.exists(tempDir.resolve("manifest.json")));
        assertTrue(Files.exists(tempDir.resolve("skills").resolve("moddev-usage.md")));
        assertTrue(Files.exists(tempDir.resolve("skills").resolve("status.md")));
        assertTrue(Files.exists(tempDir.resolve("categories").resolve("status.md")));
        assertTrue(Files.exists(tempDir.resolve("indexes").resolve("skills.md")));
        assertTrue(Files.exists(tempDir.resolve("indexes").resolve("categories.md")));
    }

    @Test
    void exportAlwaysRegeneratesFromInMemoryRegistries() throws Exception {
        var service = exportService();

        service.exportAll();
        Files.writeString(tempDir.resolve("skills").resolve("moddev-usage.md"), "stale");

        service.exportAll();

        assertEquals("# Entry\n", Files.readString(tempDir.resolve("skills").resolve("moddev-usage.md")));
    }

    private SkillExportService exportService() {
        return new SkillExportService(
                new ServiceConfig("127.0.0.1", 47812, tempDir),
                categories(),
                skills()
        );
    }

    private static List<CategoryDefinition> categories() {
        return List.of(new CategoryDefinition(
                "status",
                "Status",
                "Service status and discovery.",
                List.of("moddev-usage", "status"),
                List.of()
        ));
    }

    private static SkillRegistry skills() {
        return new SkillRegistry(List.of(
                new SkillDefinition(
                        "moddev-usage",
                        "status",
                        SkillKind.GUIDANCE,
                        "Entry",
                        "Start here.",
                        null,
                        Set.of("entry"),
                        false,
                        "# Entry\n"
                ),
                new SkillDefinition(
                        "status",
                        "status",
                        SkillKind.GUIDANCE,
                        "Status",
                        "Inspect status.",
                        null,
                        Set.of("status"),
                        false,
                        "# Status\n"
                )
        ));
    }
}

