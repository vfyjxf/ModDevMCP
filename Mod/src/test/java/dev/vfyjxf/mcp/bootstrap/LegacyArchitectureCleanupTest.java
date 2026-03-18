package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyArchitectureCleanupTest {

    @Test
    void standaloneServerAndPluginModulesAreDeleted() {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();

        assertFalse(Files.exists(rootDir.resolve("Server")), "Server module directory should be deleted");
        assertFalse(Files.exists(rootDir.resolve("Plugin")), "Plugin module directory should be deleted");
    }

    @Test
    void coreGuidesUseServiceFirstFlow() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var guides = List.of(
                rootDir.resolve("docs/guides/2026-03-11-game-mcp-guide.md"),
                rootDir.resolve("docs/guides/2026-03-11-game-mcp-guide.zh.md"),
                rootDir.resolve("docs/guides/2026-03-11-simple-agent-install-guide.md"),
                rootDir.resolve("docs/guides/2026-03-11-simple-agent-install-guide.zh.md"),
                rootDir.resolve("docs/guides/2026-03-11-testmod-runclient-guide.md"),
                rootDir.resolve("docs/guides/2026-03-11-testmod-runclient-guide.zh.md")
        );

        for (var guide : guides) {
            var content = Files.readString(guide);
            assertTrue(content.contains("/api/v1/status"), guide + " should point to the status endpoint");
            assertTrue(content.contains("moddev-entry"), guide + " should point to the entry skill");
            assertTrue(content.contains("/api/v1/requests"), guide + " should describe request execution");
            assertFalse(content.contains("createMcpClientFiles"), guide + " should not mention generated MCP client files");
            assertFalse(content.contains("moddev.status"), guide + " should not mention legacy tool names");
            assertFalse(content.contains("moddev.ui_get_live_screen"), guide + " should not mention legacy tool names");
            assertFalse(content.contains("dev.vfyjxf.moddevmcp"), guide + " should not mention the removed Gradle plugin");
            assertFalse(content.contains("modDevMcp {"), guide + " should not mention the removed Gradle extension");
        }
    }
}
