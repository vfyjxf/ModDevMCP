package dev.vfyjxf.moddev.service.config;

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

    @Test
    void loadResolvedTrimsNonBlankOverrideValues() {
        withProperty("moddev.service.host", " localhost ", () ->
                withProperty("moddev.service.port", " 47813 ", () ->
                        withProperty("moddev.skill.exportRoot", "  build/skills  ", () -> {
                            var config = ServiceConfig.loadResolved();
                            assertEquals("localhost", config.host());
                            assertEquals(47813, config.port());
                            assertEquals(Path.of("build/skills").toAbsolutePath().normalize(), config.exportRoot());
                        })));
    }

    @Test
    void loadResolvedRejectsNonLoopbackHostOverride() {
        withProperty("moddev.service.host", " 0.0.0.0 ", () ->
                withClearedProperty("moddev.service.port", () ->
                        withClearedProperty("moddev.skill.exportRoot", () ->
                                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, ServiceConfig::loadResolved))));
    }

    @Test
    void loadResolvedDerivesGameInstancesPathFromProjectRootProperty() {
        withProperty("moddevmcp.project.root", " D:/workspace/consumer ", () ->
                withClearedProperty("moddev.service.host", () ->
                        withClearedProperty("moddev.service.port", () ->
                                withClearedProperty("moddev.skill.exportRoot", () -> {
                                    var config = ServiceConfig.loadResolved();

                                    assertEquals(
                                            Path.of("D:/workspace/consumer", "build", "moddevmcp", "game-instances.json")
                                                    .toAbsolutePath()
                                                    .normalize(),
                                            config.gameInstancesPath()
                                    );
                                    assertEquals(
                                            Path.of("D:/workspace/consumer").toAbsolutePath().normalize(),
                                            config.projectRoot()
                                    );
                                }))));
    }

    @Test
    void loadResolvedDerivesGameInstancesPathFromUserDirWhenProjectRootMissing() {
        withClearedProperty("moddevmcp.project.root", () ->
                withProperty("user.dir", "D:/workspace/current-module", () ->
                        withClearedProperty("moddev.service.host", () ->
                                withClearedProperty("moddev.service.port", () ->
                                        withClearedProperty("moddev.skill.exportRoot", () -> {
                                            var config = ServiceConfig.loadResolved();

                                            assertEquals(
                                                    Path.of("D:/workspace/current-module", "build", "moddevmcp", "game-instances.json")
                                                            .toAbsolutePath()
                                                            .normalize(),
                                                    config.gameInstancesPath()
                                            );
                                            assertEquals(
                                                    Path.of("D:/workspace/current-module").toAbsolutePath().normalize(),
                                                    config.projectRoot()
                                            );
                                        })))));
    }

    @Test
    void loadResolvedKeepsStableProjectRootAfterCreation() {
        withProperty("moddevmcp.project.root", "D:/workspace/first", () -> {
            var config = ServiceConfig.loadResolved();
            withProperty("moddevmcp.project.root", "D:/workspace/second", () -> {
                assertEquals(Path.of("D:/workspace/first").toAbsolutePath().normalize(), config.projectRoot());
                assertEquals(
                        Path.of("D:/workspace/first", "build", "moddevmcp", "game-instances.json").toAbsolutePath().normalize(),
                        config.gameInstancesPath()
                );
            });
        });
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

