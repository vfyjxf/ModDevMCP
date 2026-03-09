package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.api.ToolCallContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public final class DevUiCaptureVerificationRunner {

    private final ModDevMCP mod;
    private final Path reportRoot;
    private Path lastReportPath;
    private boolean completed;

    public DevUiCaptureVerificationRunner(ModDevMCP mod, Path reportRoot) {
        this.mod = mod;
        this.reportRoot = reportRoot.toAbsolutePath().normalize();
    }

    public synchronized boolean captureOnce(String screenClass, String modId, int mouseX, int mouseY) {
        if (completed) {
            return false;
        }
        ensureUiCaptureToolRegistered();
        var tool = mod.server().registry().findTool("moddev.ui_capture").orElseThrow();
        var offscreen = invokeCapture(tool, screenClass, modId, mouseX, mouseY, "offscreen");
        var framebuffer = invokeCapture(tool, screenClass, modId, mouseX, mouseY, "framebuffer");
        lastReportPath = writeReport(screenClass, offscreen, framebuffer);
        completed = true;
        return true;
    }

    public synchronized Optional<Path> lastReportPath() {
        return Optional.ofNullable(lastReportPath);
    }

    public synchronized boolean completed() {
        return completed;
    }

    private void ensureUiCaptureToolRegistered() {
        if (mod.server().registry().findTool("moddev.ui_capture").isEmpty()) {
            mod.registerBuiltinProviders();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeCapture(
            dev.vfyjxf.mcp.server.runtime.McpToolRegistry.RegisteredTool tool,
            String screenClass,
            String modId,
            int mouseX,
            int mouseY,
            String source
    ) {
        var result = tool.handler().handle(ToolCallContext.empty(), Map.of(
                "screenClass", screenClass,
                "modId", modId,
                "mouseX", mouseX,
                "mouseY", mouseY,
                "source", source
        ));
        if (!result.success()) {
            throw new IllegalStateException("ui_capture failed for source=" + source + ": " + result.error());
        }
        return (Map<String, Object>) result.value();
    }

    private Path writeReport(String screenClass, Map<String, Object> offscreen, Map<String, Object> framebuffer) {
        try {
            Files.createDirectories(reportRoot);
            var reportPath = reportRoot.resolve("ui-capture-" + sanitize(screenClass) + ".properties");
            var properties = new Properties();
            properties.setProperty("screenClass", screenClass);
            putCaptureProperties(properties, "offscreen", offscreen);
            putCaptureProperties(properties, "framebuffer", framebuffer);
            try (var output = Files.newOutputStream(reportPath)) {
                properties.store(output, "ModDevMCP dev ui capture verification");
            }
            return reportPath;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void putCaptureProperties(Properties properties, String prefix, Map<String, Object> payload) {
        properties.setProperty(prefix + ".driverId", String.valueOf(payload.getOrDefault("driverId", "")));
        properties.setProperty(prefix + ".imagePath", String.valueOf(payload.getOrDefault("imagePath", "")));
        properties.setProperty(prefix + ".imageRef", String.valueOf(payload.getOrDefault("imageRef", "")));
        properties.setProperty(prefix + ".imageResourceUri", String.valueOf(payload.getOrDefault("imageResourceUri", "")));
        var imageMeta = payload.get("imageMeta") instanceof Map<?, ?> rawImageMeta
                ? (Map<String, Object>) rawImageMeta
                : Map.<String, Object>of();
        for (var entry : flattenMap("imageMeta", imageMeta).entrySet()) {
            properties.setProperty(prefix + "." + entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> flattenMap(String prefix, Map<String, Object> map) {
        var flattened = new LinkedHashMap<String, String>();
        for (var entry : map.entrySet()) {
            var key = prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) {
                flattened.putAll(flattenMap(key, (Map<String, Object>) nested));
            } else {
                flattened.put(key, String.valueOf(entry.getValue()));
            }
        }
        return flattened;
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
