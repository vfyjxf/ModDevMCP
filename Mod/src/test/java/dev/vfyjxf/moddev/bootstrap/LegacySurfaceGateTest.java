package dev.vfyjxf.moddev.bootstrap;

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

class LegacySurfaceGateTest {

    private static final List<String> LEGACY_PRODUCTION_TOKENS = List.of(
            "RegisterMcpToolsEvent",
            "RegisterCommonMcpToolsEvent",
            "RegisterClientMcpToolsEvent",
            "RegisterServerMcpToolsEvent",
            "CommonMcpToolRegistrar",
            "ClientMcpToolRegistrar",
            "ServerMcpToolRegistrar",
            "CommonMcpRegistrar",
            "ClientMcpRegistrar",
            "ServerMcpRegistrar",
            "AnnotationMcpRegistrarLookup",
            "ModMcpApi",
            "McpToolProvider",
            "McpToolRegistry",
            "ToolCallContext",
            "ToolResult",
            "McpProtocolDispatcher"
    );

    private static final List<String> LEGACY_DOC_TOKENS = List.of(
            "tools/call",
            "tools/list",
            "jsonrpc",
            "stdio mcp"
    );

    @Test
    void productionSourceMustNotContainLegacyMcpOrToolBridgeTokens() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var productionRoots = List.of(
                rootDir.resolve("Mod/src/main/java"),
                rootDir.resolve("Mod/src/main/resources/moddev-service")
        );

        for (var root : productionRoots) {
            var firstMatch = findFirstMatch(root, LEGACY_PRODUCTION_TOKENS, List.of());
            assertTrue(
                    firstMatch.isEmpty(),
                    () -> "legacy production token found under " + root + ": " + firstMatch.orElseThrow()
            );
        }
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
            var firstMatch = findFirstMatch(root, LEGACY_DOC_TOKENS, List.of(rootDir.resolve("docs/outdated")));
            assertTrue(
                    firstMatch.isEmpty(),
                    () -> "legacy docs token found under " + root + ": " + firstMatch.orElseThrow()
            );
        }
    }

    private static Optional<String> findFirstMatch(Path root, List<String> tokens, List<Path> excludedRoots) throws IOException {
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
                    .filter(path -> !isExcluded(path, excludedRoots))
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

    private static boolean isExcluded(Path path, List<Path> excludedRoots) {
        for (var excludedRoot : excludedRoots) {
            if (path.normalize().startsWith(excludedRoot.normalize())) {
                return true;
            }
        }
        return false;
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
                || fileName.endsWith(".json")
                || fileName.endsWith(".properties");
    }
}
