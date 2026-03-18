package dev.vfyjxf.mcp.service.http;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.discovery.GameInstanceRegistry;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
import dev.vfyjxf.mcp.service.skill.SkillDefinition;
import dev.vfyjxf.mcp.service.skill.SkillKind;
import dev.vfyjxf.mcp.service.skill.SkillRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpServiceServerLifecycleTest {

    @TempDir
    Path tempDir;

    private final List<HttpServiceServer> httpServers = new ArrayList<>();
    private final List<ModDevMCP> mods = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (int i = mods.size() - 1; i >= 0; i--) {
            mods.get(i).stopHttpService();
        }
        for (int i = httpServers.size() - 1; i >= 0; i--) {
            httpServers.get(i).stop();
        }
    }

    @Test
    void bindsToNextAvailablePortWhenPreferredPortIsOccupied() throws Exception {
        var preferredPort = findOpenPort();
        try (var blocker = new ServerSocket(preferredPort)) {
            var config = new ServiceConfig("127.0.0.1", preferredPort, tempDir.resolve("skills"), tempDir.resolve("project"));
            var server = new HttpServiceServer(
                    config,
                    statusEndpoint(tempDir),
                    new CategoriesEndpoint(List.of()),
                    new SkillsEndpoint(new SkillRegistry(List.of(entrySkill()))),
                    new OperationsEndpoint(new OperationRegistry(List.of())),
                    requestsEndpoint()
            );
            httpServers.add(server);
            server.start();

            assertNotEquals(preferredPort, server.baseUri().getPort());
            assertTrue(server.baseUri().getPort() > preferredPort);
        }
    }

    @Test
    void modLifecycleRegistersClientAndServerOnDifferentPortsAndCleansUpRegistry() {
        var projectRoot = tempDir.resolve("project");
        var exportRoot = tempDir.resolve("skills");
        var preferredPort = findOpenPort();

        withProperties(Map.of(
                ServiceConfig.HOST_PROPERTY, "127.0.0.1",
                ServiceConfig.PORT_PROPERTY, String.valueOf(preferredPort),
                ServiceConfig.EXPORT_ROOT_PROPERTY, exportRoot.toString(),
                ServiceConfig.PROJECT_ROOT_PROPERTY, projectRoot.toString()
        ), () -> {
            var config = ServiceConfig.loadResolved();
            var registryPath = config.gameInstancesPath();
            var registry = new GameInstanceRegistry(registryPath);

            var client = new ModDevMCP(new ModDevMcpServer());
            var server = new ModDevMCP(new ModDevMcpServer());
            mods.add(client);
            mods.add(server);

            client.activateClientSide();
            server.activateServerSide();

            client.startHttpService("client");
            server.startHttpService("server");

            var clientRecord = registry.find("client").orElseThrow();
            var serverRecord = registry.find("server").orElseThrow();
            assertNotEquals(clientRecord.port(), serverRecord.port());
            assertTrue(readString(registryPath).contains("\"client\""));
            assertTrue(readString(registryPath).contains("\"server\""));

            client.stopHttpService();
            assertFalse(registry.find("client").isPresent());
            assertTrue(registry.find("server").isPresent());

            server.stopHttpService();
            assertFalse(Files.exists(registryPath));
        });
    }

    @Test
    void olderClientShutdownDoesNotRemoveNewerClientRegistration() {
        var projectRoot = tempDir.resolve("project-same-side");
        var exportRoot = tempDir.resolve("skills-same-side");
        var preferredPort = findOpenPort();

        withProperties(Map.of(
                ServiceConfig.HOST_PROPERTY, "127.0.0.1",
                ServiceConfig.PORT_PROPERTY, String.valueOf(preferredPort),
                ServiceConfig.EXPORT_ROOT_PROPERTY, exportRoot.toString(),
                ServiceConfig.PROJECT_ROOT_PROPERTY, projectRoot.toString()
        ), () -> {
            var config = ServiceConfig.loadResolved();
            var registry = new GameInstanceRegistry(config.gameInstancesPath());

            var olderClient = new ModDevMCP(new ModDevMcpServer());
            var newerClient = new ModDevMCP(new ModDevMcpServer());
            mods.add(olderClient);
            mods.add(newerClient);

            olderClient.activateClientSide();
            newerClient.activateClientSide();

            olderClient.startHttpService("client");
            var olderRecord = registry.find("client").orElseThrow();

            newerClient.startHttpService("client");
            var newerRecord = registry.find("client").orElseThrow();
            assertNotEquals(olderRecord.baseUrl(), newerRecord.baseUrl());

            olderClient.stopHttpService();
            var remaining = registry.find("client").orElse(null);
            assertNotNull(remaining);
            assertTrue(remaining.baseUrl().equals(newerRecord.baseUrl()));
            assertTrue(remaining.startedAt().equals(newerRecord.startedAt()));

            newerClient.stopHttpService();
            assertFalse(Files.exists(config.gameInstancesPath()));
        });
    }

    @Test
    void noArgStartHttpServiceRequiresSingleActiveSide() {
        var mod = new ModDevMCP(new ModDevMcpServer());
        mods.add(mod);
        assertThrows(IllegalStateException.class, mod::startHttpService);

        mod.activateClientSide();
        mod.activateServerSide();
        assertThrows(IllegalStateException.class, mod::startHttpService);
    }

    private static SkillDefinition entrySkill() {
        return new SkillDefinition(
                "moddev-usage",
                "status",
                SkillKind.GUIDANCE,
                "Entry",
                "Start here.",
                null,
                Set.of("entry"),
                false,
                "# Entry\n"
        );
    }

    private static StatusEndpoint statusEndpoint(Path exportRoot) {
        return new StatusEndpoint(new StatusEndpoint.StatusProvider() {
            @Override
            public boolean serviceReady() {
                return true;
            }

            @Override
            public boolean gameReady() {
                return false;
            }

            @Override
            public List<String> connectedSides() {
                return List.of();
            }

            @Override
            public String usageSkillId() {
                return "moddev-usage";
            }

            @Override
            public Path exportRoot() {
                return exportRoot;
            }

            @Override
            public String lastError() {
                return null;
            }
        });
    }

    private static RequestsEndpoint requestsEndpoint() {
        return new RequestsEndpoint(
                new OperationRegistry(List.of()),
                List::of,
                (request, resolvedTargetSide) -> Map.of()
        );
    }

    private static int findOpenPort() {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to allocate test port", exception);
        }
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read file: " + path, exception);
        }
    }

    private static void withProperties(Map<String, String> properties, Runnable task) {
        var previous = new LinkedHashMap<String, String>();
        for (var entry : properties.entrySet()) {
            previous.put(entry.getKey(), System.getProperty(entry.getKey()));
            System.setProperty(entry.getKey(), entry.getValue());
        }
        try {
            task.run();
        } finally {
            for (var entry : previous.entrySet()) {
                if (entry.getValue() == null) {
                    System.clearProperty(entry.getKey());
                } else {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
