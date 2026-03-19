package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.ModDevMCP;
import dev.vfyjxf.moddev.server.ModDevMcpServer;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("false", properties.getProperty("offscreen.success"));
        assertEquals("false", properties.getProperty("framebuffer.success"));
        assertTrue(properties.getProperty("offscreen.error").contains("capture_unavailable"));
        assertTrue(properties.getProperty("framebuffer.error").contains("capture_unavailable"));
        assertEquals("", properties.getProperty("offscreen.imagePath"));
        assertEquals("", properties.getProperty("framebuffer.imagePath"));
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

