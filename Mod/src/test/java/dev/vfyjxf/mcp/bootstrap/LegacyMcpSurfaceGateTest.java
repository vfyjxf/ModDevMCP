package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMcpSurfaceGateTest {

    private static final List<String> LEGACY_PRODUCTION_TOKENS = List.of(
            "McpToolProvider",
            "McpToolRegistry",
            "ToolResult",
            "ToolCallContext",
            "McpProtocolDispatcher"
    );

    private static final List<String> LEGACY_DOC_TOKENS = List.of(
            "tools/call",
            "tools/list",
            "jsonrpc",
            "stdio mcp"
    );

    @Test
    void productionCodeMustNotReferenceLegacyMcpToolSurface() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var productionRoot = rootDir.resolve("Mod/src/main/java");

        var firstMatch = findFirstMatch(productionRoot, LEGACY_PRODUCTION_TOKENS);
        assertTrue(
                firstMatch.isEmpty(),
                () -> "legacy production token found: " + firstMatch.orElseThrow()
        );
    }

    @Test
    void activeDocsAndSkillsMustNotReferenceLegacyStdioToolFlow() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var roots = List.of(
                rootDir.resolve("README.md"),
                rootDir.resolve("README.zh.md"),
                rootDir.resolve("skills"),
                rootDir.resolve("docs/guides"),
                rootDir.resolve("Mod/src/main/resources/moddev-service")
        );

        for (var root : roots) {
            var firstMatch = findFirstMatch(root, LEGACY_DOC_TOKENS);
            assertTrue(
                    firstMatch.isEmpty(),
                    () -> "legacy docs token found under " + root + ": " + firstMatch.orElseThrow()
            );
        }
    }

    private static Optional<String> findFirstMatch(Path root, List<String> tokens) throws IOException {
        if (!Files.exists(root)) {
            return Optional.empty();
        }
        if (Files.isRegularFile(root)) {
            var content = Files.readString(root);
            for (var token : tokens) {
                if (content.toLowerCase().contains(token.toLowerCase())) {
                    return Optional.of(root + " -> " + token);
                }
            }
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.walk(root)) {
            var candidates = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> hasSupportedSuffix(path.getFileName().toString()))
                    .sorted()
                    .toList();
            var matches = new ArrayList<String>();
            for (var path : candidates) {
                var content = readText(path);
                for (var token : tokens) {
                    if (content.toLowerCase().contains(token.toLowerCase())) {
                        matches.add(path + " -> " + token);
                    }
                }
            }
            return matches.stream().findFirst();
        }
    }

    private static String readText(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException("failed reading " + path, exception);
        }
    }

    private static boolean hasSupportedSuffix(String fileName) {
        return fileName.endsWith(".java")
                || fileName.endsWith(".md")
                || fileName.endsWith(".txt")
                || fileName.endsWith(".json");
    }
}
