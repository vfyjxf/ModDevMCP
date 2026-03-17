package dev.vfyjxf.mcp.service.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceConfigTest {

    @Test
    void loadResolvedUsesDefaultHostPortAndExportRoot() {
        withClearedProperty("moddev.service.host", () ->
                withClearedProperty("moddev.service.port", () ->
                        withClearedProperty("moddev.skill.exportRoot", () -> {
                            var config = ServiceConfig.loadResolved();

                            assertEquals("127.0.0.1", config.host());
                            assertEquals(47812, config.port());
                            assertEquals(
                                    Path.of(System.getProperty("user.home"), ".moddev", "skills").toAbsolutePath().normalize(),
                                    config.exportRoot()
                            );
                        })));
    }

    @Test
    void loadResolvedNormalizesRelativeExportRootOverride() {
        withClearedProperty("moddev.service.host", () ->
                withClearedProperty("moddev.service.port", () -> {
                    var override = Path.of("build", "..", "tmp", "skills").toString();
                    withProperty("moddev.skill.exportRoot", override, () -> {
                        var config = ServiceConfig.loadResolved();
                        assertEquals(Path.of(override).toAbsolutePath().normalize(), config.exportRoot());
                    });
                }));
    }

    @Test
    void constructorNormalizesExportRootInvariant() {
        var config = new ServiceConfig("127.0.0.1", 47812, Path.of("build", "..", "tmp", "skills"));
        assertEquals(Path.of("build", "..", "tmp", "skills").toAbsolutePath().normalize(), config.exportRoot());
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

    private static void withProperty(String key, String value, Runnable testBody) {
        var previous = System.getProperty(key);
        try {
            System.setProperty(key, value);
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
