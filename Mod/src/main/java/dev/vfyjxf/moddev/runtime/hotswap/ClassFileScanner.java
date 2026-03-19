package dev.vfyjxf.moddev.runtime.hotswap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class ClassFileScanner {

    private ClassFileScanner() {
    }

    public static Map<String, Long> scan(Path classOutputDir) {
        Map<String, Long> result = new HashMap<>();
        if (!Files.isDirectory(classOutputDir)) {
            return result;
        }
        try (Stream<Path> walk = Files.walk(classOutputDir)) {
            walk.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        String relative = classOutputDir.relativize(p).toString().replace('\\', '/');
                        try {
                            result.put(relative, Files.getLastModifiedTime(p).toMillis());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public static Map<String, byte[]> readChanged(Path classOutputDir, Map<String, Long> baseline) {
        Map<String, byte[]> changed = new HashMap<>();
        Map<String, Long> current = scan(classOutputDir);
        for (var entry : current.entrySet()) {
            String relativePath = entry.getKey();
            long currentTimestamp = entry.getValue();
            Long baselineTimestamp = baseline.get(relativePath);
            if (baselineTimestamp == null || currentTimestamp > baselineTimestamp) {
                try {
                    byte[] bytes = Files.readAllBytes(classOutputDir.resolve(relativePath));
                    changed.put(relativePath, bytes);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return changed;
    }

    public static String classFileToClassName(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        return normalized.replace('/', '.');
    }
}

