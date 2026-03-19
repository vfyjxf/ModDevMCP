package dev.vfyjxf.moddev.bootstrap;

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
            assertTrue(content.contains("moddev-usage"), guide + " should point to the entry skill");
            assertTrue(content.contains("/api/v1/requests"), guide + " should describe request execution");
            assertTrue(content.contains("build/moddevmcp/game-instances.json"), guide + " should document project-local discovery fallback");
            assertTrue(
                    content.contains("default probe")
                            || content.contains("默认探测"),
                    guide + " should document default probe first"
            );
            assertTrue(
                    content.contains("project-local fallback")
                            || content.contains("项目级回退"),
                    guide + " should document project-local fallback"
            );
            assertTrue(
                    content.contains("separate ports")
                            || content.contains("独立端口"),
                    guide + " should document separate client/server ports"
            );
            assertFalse(content.contains("createMcpClientFiles"), guide + " should not mention generated MCP client files");
            assertFalse(content.contains("moddev.status"), guide + " should not mention legacy tool names");
            assertFalse(content.contains("moddev.ui_get_live_screen"), guide + " should not mention legacy tool names");
            assertFalse(content.contains("dev.vfyjxf.moddevmcp"), guide + " should not mention the removed Gradle plugin");
            assertFalse(content.contains("modDevMcp {"), guide + " should not mention the removed Gradle extension");
            assertFalse(content.toLowerCase().contains("jsonrpc"), guide + " should not mention JSON-RPC flow");
            assertFalse(content.contains("tools/call"), guide + " should not mention tools/call");
            assertFalse(content.contains("tools/list"), guide + " should not mention tools/list");
        }
    }

    @Test
    void uiGuidesDocumentDriverFilteringAndRawInputBoundary() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var guides = List.of(
                rootDir.resolve("docs/guides/2026-03-11-live-screen-tool-guide.md"),
                rootDir.resolve("docs/guides/2026-03-12-playwright-style-ui-automation-guide.md"),
                rootDir.resolve("docs/guides/2026-03-15-third-party-mod-integration-guide.md"),
                rootDir.resolve("docs/guides/2026-03-11-live-screen-tool-guide.zh.md"),
                rootDir.resolve("docs/guides/2026-03-12-playwright-style-ui-automation-guide.zh.md"),
                rootDir.resolve("docs/guides/2026-03-15-third-party-mod-integration-guide.zh.md")
        );

        for (var guide : guides) {
            var content = Files.readString(guide);
            assertTrue(content.contains("includeDrivers"), guide + " should mention includeDrivers");
            assertTrue(content.contains("excludeDrivers"), guide + " should mention excludeDrivers");
            assertFalse(content.toLowerCase().contains("jsonrpc"), guide + " should not mention JSON-RPC flow");
            assertFalse(content.contains("tools/call"), guide + " should not mention tools/call");
            assertFalse(content.contains("tools/list"), guide + " should not mention tools/list");
        }

        var liveScreenGuide = Files.readString(rootDir.resolve("docs/guides/2026-03-11-live-screen-tool-guide.md"));
        assertTrue(liveScreenGuide.contains("drivers[]"), "live screen guide should describe drivers[]");

        var liveScreenGuideZh = Files.readString(rootDir.resolve("docs/guides/2026-03-11-live-screen-tool-guide.zh.md"));
        assertTrue(liveScreenGuideZh.contains("drivers[]"), "zh live screen guide should describe drivers[]");

        var playwrightGuide = Files.readString(rootDir.resolve("docs/guides/2026-03-12-playwright-style-ui-automation-guide.md"));
        assertTrue(playwrightGuide.contains("moddev.input_action"), "playwright guide should point raw input to moddev.input_action");
        assertTrue(playwrightGuide.contains("moddev.ui_query"), "playwright guide should mention moddev.ui_query for multi-driver flows");
        assertTrue(playwrightGuide.contains("moddev.ui_action"), "playwright guide should mention moddev.ui_action for multi-driver flows");

        var playwrightGuideZh = Files.readString(rootDir.resolve("docs/guides/2026-03-12-playwright-style-ui-automation-guide.zh.md"));
        assertTrue(playwrightGuideZh.contains("moddev.input_action"), "zh playwright guide should point raw input to moddev.input_action");
        assertTrue(playwrightGuideZh.contains("moddev.ui_query"), "zh playwright guide should mention moddev.ui_query for multi-driver flows");
        assertTrue(playwrightGuideZh.contains("moddev.ui_action"), "zh playwright guide should mention moddev.ui_action for multi-driver flows");
    }
}

