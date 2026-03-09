package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevUiCaptureVerificationRunnerTest {

    @Test
    void captureOnceInvokesUiCaptureTwiceAndWritesVerificationReport() throws IOException {
        var mod = new ModDevMCP(new ModDevMcpServer(new McpToolRegistry()));
        var reportRoot = Files.createTempDirectory("dev-ui-capture-report");
        var runner = new DevUiCaptureVerificationRunner(mod, reportRoot);

        var executed = runner.captureOnce("custom.UnknownScreen", "minecraft", 12, 34);

        assertTrue(executed);
        var reportPath = runner.lastReportPath().orElseThrow();
        assertTrue(Files.exists(reportPath));

        var properties = new Properties();
        try (var input = Files.newInputStream(reportPath)) {
            properties.load(input);
        }

        assertEquals("custom.UnknownScreen", properties.getProperty("screenClass"));
        assertEquals("placeholder", properties.getProperty("offscreen.imageMeta.source"));
        assertEquals("placeholder", properties.getProperty("framebuffer.imageMeta.source"));
        assertTrue(Files.exists(java.nio.file.Path.of(properties.getProperty("offscreen.imagePath"))));
        assertTrue(Files.exists(java.nio.file.Path.of(properties.getProperty("framebuffer.imagePath"))));
    }

    @Test
    void captureOnceSkipsLaterScreensAfterFirstSuccessfulRun() throws IOException {
        var mod = new ModDevMCP(new ModDevMcpServer(new McpToolRegistry()));
        var reportRoot = Files.createTempDirectory("dev-ui-capture-report");
        var runner = new DevUiCaptureVerificationRunner(mod, reportRoot);

        assertTrue(runner.captureOnce("custom.UnknownScreen", "minecraft", 0, 0));
        assertFalse(runner.captureOnce("other.Screen", "minecraft", 0, 0));
    }
}
