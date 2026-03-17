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
                        "moddev-entry",
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

        var config = new ServiceConfig("127.0.0.1", findOpenPort(), tempDir);
        server = new HttpServiceServer(
                config,
                new StatusEndpoint(true, List.of("client"), "moddev-entry", tempDir, "no game instance"),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(new OperationRegistry(List.of()))
        );
        server.start();

        var response = get(server.baseUri().resolve("/api/v1/status"));

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("none").startsWith("application/json"));
        assertEquals(
                "{\"serviceReady\":true,\"gameReady\":true,\"connectedSides\":[\"client\"],\"entrySkillId\":\"moddev-entry\",\"exportRoot\":\"" + tempDir.toAbsolutePath().normalize().toString().replace("\\", "\\\\") + "\",\"lastError\":\"no game instance\"}",
                response.body()
        );
    }

    private static HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(uri).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static int findOpenPort() {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to allocate test port", exception);
        }
    }
}
