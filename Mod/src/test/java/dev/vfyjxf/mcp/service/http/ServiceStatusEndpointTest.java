package dev.vfyjxf.mcp.service.http;

import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
import dev.vfyjxf.mcp.service.skill.SkillDefinition;
import dev.vfyjxf.mcp.service.skill.SkillKind;
import dev.vfyjxf.mcp.service.skill.SkillRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceStatusEndpointTest {

    @TempDir
    Path tempDir;

    private HttpServiceServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getStatusReturnsServiceReadinessAndDiscoveryPointers() throws Exception {
        var skillRegistry = new SkillRegistry(List.of(
                new SkillDefinition(
                        "moddev-usage",
                        "status",
                        SkillKind.GUIDANCE,
                        "Entry",
                        "Start here.",
                        null,
                        Set.of("entry"),
                        false,
                        "# Entry\n"
                )
        ));
        var status = new MutableStatusProvider(tempDir);
        status.gameReady = true;
        status.connectedSides = List.of("client");
        status.lastError = "no game instance";

        var config = new ServiceConfig("127.0.0.1", findOpenPort(), tempDir);
        server = new HttpServiceServer(
                config,
                new StatusEndpoint(status),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(new OperationRegistry(List.of())),
                requestsEndpoint()
        );
        server.start();

        var response = get(server.baseUri().resolve("/api/v1/status"));

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("none").startsWith("application/json"));
        assertEquals(
                "{\"serviceReady\":true,\"gameReady\":true,\"connectedSides\":[\"client\"],\"usageSkillId\":\"moddev-usage\",\"exportRoot\":\"" + tempDir.toAbsolutePath().normalize().toString().replace("\\", "\\\\") + "\",\"lastError\":\"no game instance\"}",
                response.body()
        );
    }

    @Test
    void getStatusReflectsLiveStateAcrossRequests() throws Exception {
        var status = new MutableStatusProvider(tempDir);
        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                new StatusEndpoint(status),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(new SkillRegistry(List.of(entrySkill()))),
                new OperationsEndpoint(new OperationRegistry(List.of())),
                requestsEndpoint()
        );
        server.start();

        var first = get(server.baseUri().resolve("/api/v1/status"));
        assertEquals(
                "{\"serviceReady\":true,\"gameReady\":false,\"connectedSides\":[],\"usageSkillId\":\"moddev-usage\",\"exportRoot\":\"" + tempDir.toAbsolutePath().normalize().toString().replace("\\", "\\\\") + "\",\"lastError\":null}",
                first.body()
        );

        status.serviceReady = false;
        status.gameReady = true;
        status.connectedSides = List.of("server");
        status.usageSkillId = "moddev-usage-alt";
        status.exportRoot = tempDir.resolve("next");
        status.lastError = "updated";

        var second = get(server.baseUri().resolve("/api/v1/status"));
        assertEquals(
                "{\"serviceReady\":false,\"gameReady\":true,\"connectedSides\":[\"server\"],\"usageSkillId\":\"moddev-usage-alt\",\"exportRoot\":\"" + tempDir.resolve("next").toAbsolutePath().normalize().toString().replace("\\", "\\\\") + "\",\"lastError\":\"updated\"}",
                second.body()
        );
    }

    @Test
    void baseUriWrapsIpv6HostWithBrackets() {
        var uri = HttpServiceServer.buildBaseUri("::1", 47812);
        assertEquals("http://[::1]:47812", uri.toString());
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

    private static final class MutableStatusProvider implements StatusEndpoint.StatusProvider {
        private boolean serviceReady = true;
        private boolean gameReady;
        private List<String> connectedSides = List.of();
        private String usageSkillId = "moddev-usage";
        private Path exportRoot;
        private String lastError;

        private MutableStatusProvider(Path exportRoot) {
            this.exportRoot = exportRoot;
        }

        @Override
        public boolean serviceReady() {
            return serviceReady;
        }

        @Override
        public boolean gameReady() {
            return gameReady;
        }

        @Override
        public List<String> connectedSides() {
            return connectedSides;
        }

        @Override
        public String usageSkillId() {
            return usageSkillId;
        }

        @Override
        public Path exportRoot() {
            return exportRoot;
        }

        @Override
        public String lastError() {
            return lastError;
        }
    }

    private static HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(uri).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static RequestsEndpoint requestsEndpoint() {
        return new RequestsEndpoint(
                new OperationRegistry(List.of()),
                List::of,
                (request, resolvedTargetSide) -> java.util.Map.of()
        );
    }

    private static int findOpenPort() {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to allocate test port", exception);
        }
    }
}


