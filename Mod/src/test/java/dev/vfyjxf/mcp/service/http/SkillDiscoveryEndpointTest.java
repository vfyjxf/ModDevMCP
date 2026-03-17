package dev.vfyjxf.mcp.service.http;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.operation.OperationDefinition;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillDiscoveryEndpointTest {

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
    void discoveryEndpointsReturnStableJsonMetadata() throws Exception {
        var category = new CategoryDefinition(
                "status",
                "Status",
                "Service status and discovery.",
                List.of("moddev-entry", "status-check"),
                List.of("status.check")
        );
        var skillRegistry = new SkillRegistry(List.of(
                new SkillDefinition(
                        "moddev-entry",
                        "status",
                        SkillKind.GUIDANCE,
                        "Entry",
                        "Start here.",
                        null,
                        Set.of("entry", "status"),
                        false,
                        "# Entry\n"
                ),
                new SkillDefinition(
                        "status-check",
                        "status",
                        SkillKind.ACTION,
                        "Status Check",
                        "Inspect status.",
                        "status.check",
                        Set.of("status", "check"),
                        false,
                        "# Status\n"
                )
        ));
        var operationRegistry = new OperationRegistry(List.of(
                new OperationDefinition(
                        "status.check",
                        "status",
                        "Status Check",
                        "Inspect service status.",
                        false,
                        Set.of(),
                        Map.of("type", "object"),
                        orderedExampleRequest()
                )
        ));

        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                statusEndpoint(tempDir),
                new CategoriesEndpoint(List.of(category)),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(operationRegistry)
        );
        server.start();

        var categoriesResponse = get(server.baseUri().resolve("/api/v1/categories"));
        var skillsResponse = get(server.baseUri().resolve("/api/v1/skills"));
        var operationsResponse = get(server.baseUri().resolve("/api/v1/operations"));

        assertEquals(200, categoriesResponse.statusCode());
        assertEquals(
                "{\"categories\":[{\"categoryId\":\"status\",\"title\":\"Status\",\"summary\":\"Service status and discovery.\",\"skillIds\":[\"moddev-entry\",\"status-check\"],\"operationIds\":[\"status.check\"]}]}",
                categoriesResponse.body()
        );

        assertEquals(200, skillsResponse.statusCode());
        assertEquals(
                "{\"skills\":[{\"skillId\":\"moddev-entry\",\"categoryId\":\"status\",\"kind\":\"guidance\",\"title\":\"Entry\",\"summary\":\"Start here.\",\"operationId\":null,\"tags\":[\"entry\",\"status\"],\"requiresGame\":false},{\"skillId\":\"status-check\",\"categoryId\":\"status\",\"kind\":\"action\",\"title\":\"Status Check\",\"summary\":\"Inspect status.\",\"operationId\":\"status.check\",\"tags\":[\"check\",\"status\"],\"requiresGame\":false}]}",
                skillsResponse.body()
        );

        assertEquals(200, operationsResponse.statusCode());
        assertEquals(
                "{\"operations\":[{\"operationId\":\"status.check\",\"categoryId\":\"status\",\"title\":\"Status Check\",\"summary\":\"Inspect service status.\",\"supportsTargetSide\":false,\"availableTargetSides\":[],\"inputSchema\":{\"type\":\"object\"},\"exampleRequest\":{\"operationId\":\"status.check\",\"input\":{\"verbose\":true}}}]}",
                operationsResponse.body()
        );
    }

    @Test
    void skillMarkdownEndpointReturnsRawMarkdown() throws Exception {
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
                        "# Entry\nUse /api/v1/status\n"
                )
        ));

        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                statusEndpoint(tempDir),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(new OperationRegistry(List.of()))
        );
        server.start();

        var response = get(server.baseUri().resolve("/api/v1/skills/moddev-entry/markdown"));

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("none").startsWith("text/markdown"));
        assertEquals("# Entry\nUse /api/v1/status\n", response.body());
    }

    @Test
    void skillMarkdownPathDecodingKeepsPlusCharacter() throws Exception {
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
                ),
                new SkillDefinition(
                        "status+tips",
                        "status",
                        SkillKind.GUIDANCE,
                        "Status Tips",
                        "Keep plus in id.",
                        null,
                        Set.of("status"),
                        false,
                        "# Plus Id\n"
                )
        ));

        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                statusEndpoint(tempDir),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(new OperationRegistry(List.of()))
        );
        server.start();

        var response = get(server.baseUri().resolve("/api/v1/skills/status+tips/markdown"));

        assertEquals(200, response.statusCode());
        assertEquals("# Plus Id\n", response.body());
    }

    @Test
    void unknownSkillMarkdownReturnsNotFound() throws Exception {
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

        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                statusEndpoint(tempDir),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(new OperationRegistry(List.of()))
        );
        server.start();

        var response = get(server.baseUri().resolve("/api/v1/skills/missing/markdown"));

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("not_found"));
    }

    @Test
    void markdownEndpointRejectsNonGetMethod() throws Exception {
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

        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                statusEndpoint(tempDir),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(skillRegistry),
                new OperationsEndpoint(new OperationRegistry(List.of()))
        );
        server.start();

        var response = request(server.baseUri().resolve("/api/v1/skills/moddev-entry/markdown"), "POST");

        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("Allow").orElse("missing"));
    }

    private static HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        return request(uri, "GET");
    }

    private static HttpResponse<String> request(URI uri, String method) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(uri).method(method, HttpRequest.BodyPublishers.noBody()).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> orderedExampleRequest() {
        var exampleRequest = new LinkedHashMap<String, Object>();
        exampleRequest.put("operationId", "status.check");
        exampleRequest.put("input", Map.of("verbose", true));
        return exampleRequest;
    }

    private static StatusEndpoint statusEndpoint(Path exportRoot) {
        return new StatusEndpoint(new StaticStatusProvider(exportRoot));
    }

    private static final class StaticStatusProvider implements StatusEndpoint.StatusProvider {
        private final Path exportRoot;

        private StaticStatusProvider(Path exportRoot) {
            this.exportRoot = exportRoot;
        }

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
        public String entrySkillId() {
            return "moddev-entry";
        }

        @Override
        public Path exportRoot() {
            return exportRoot;
        }

        @Override
        public String lastError() {
            return null;
        }
    }

    private static int findOpenPort() {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to allocate test port", exception);
        }
    }
}
