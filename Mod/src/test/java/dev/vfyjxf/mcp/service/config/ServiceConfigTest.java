package dev.vfyjxf.mcp.service.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceConfigTest {

    @Test
    void loadResolvedUsesDefaultHostPortAndExportRoot() {
        withClearedProperty("moddev.service.host", () ->
                withClearedProperty("moddev.service.port", () ->
                        withClearedProperty("moddev.service.exportRoot", () -> {
                            var config = ServiceConfig.loadResolved();

                            assertEquals("127.0.0.1", config.host());
                            assertEquals(47812, config.port());
                            assertEquals(Path.of(System.getProperty("user.home"), ".moddev", "skills"), config.exportRoot());
                        })));
    }

    private static void withClearedProperty(String key, Runnable testBody) {
        var previous = System.getProperty(key);
        try {
            System.clearProperty(key);
            testBody.run();
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }
}
