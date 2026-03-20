package dev.vfyjxf.moddev.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;

final class RepoRootLocator {

    private RepoRootLocator() {
    }

    static Path repoRoot() {
        var current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle"))
                    && Files.exists(current.resolve("README.md"))
                    && Files.exists(current.resolve("Mod"))
                    && Files.exists(current.resolve("TestMod"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root from current working directory");
    }
}
