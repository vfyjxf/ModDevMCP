package dev.vfyjxf.mcp.service.http;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OperationRequestEndpointTest {

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
    void rejectsTargetSideWhenOperationDoesNotSupportSideSelection() throws Exception {
        var operationRegistry = new OperationRegistry(
                java.util.List.of(operation("status.check", false, Set.of()))
        );

        startServer(new RequestsEndpoint(
                operationRegistry,
                () -> java.util.List.of("client"),
                (request, resolvedTargetSide) -> Map.of("ok", true)
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"requestId\":\"req-1\",\"operationId\":\"status.check\",\"targetSide\":\"client\",\"input\":{}}"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":\"req-1\",\"operationId\":\"status.check\",\"targetSide\":null,\"status\":\"error\",\"output\":{},\"errorCode\":\"target_side_not_supported\",\"errorMessage\":\"operation does not support target side selection\"}",
                response.body()
        );
    }

    @Test
    void executionFailureReturnsStructuredErrorCodeAndMessage() throws Exception {
        var operationRegistry = new OperationRegistry(
                java.util.List.of(operation("world.inspect", true, Set.of("client")))
        );

        startServer(new RequestsEndpoint(
                operationRegistry,
                () -> java.util.List.of("client"),
                (request, resolvedTargetSide) -> {
                    throw new IllegalStateException("operation crashed");
                }
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"requestId\":\"req-2\",\"operationId\":\"world.inspect\",\"input\":{}}"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":\"req-2\",\"operationId\":\"world.inspect\",\"targetSide\":\"client\",\"status\":\"error\",\"output\":{},\"errorCode\":\"operation_execution_failed\",\"errorMessage\":\"operation crashed\"}",
                response.body()
        );
        assertFalse(response.body().contains("disconnected"));
    }

    @Test
    void invalidRequestUsesStandardErrorEnvelope() throws Exception {
        var operationRegistry = new OperationRegistry(List.of());
        startServer(new RequestsEndpoint(
                operationRegistry,
                List::of,
                (request, resolvedTargetSide) -> Map.of()
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"operationId\":123,\"input\":[]}"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":null,\"operationId\":null,\"targetSide\":null,\"status\":\"error\",\"output\":{},\"errorCode\":\"invalid_request\",\"errorMessage\":\"operationId must be a string\"}",
                response.body()
        );
    }

    @Test
    void malformedJsonUsesStandardErrorEnvelope() throws Exception {
        var operationRegistry = new OperationRegistry(List.of());
        startServer(new RequestsEndpoint(
                operationRegistry,
                List::of,
                (request, resolvedTargetSide) -> Map.of()
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"operationId\":\"world.inspect\",\"input\":"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":null,\"operationId\":null,\"targetSide\":null,\"status\":\"error\",\"output\":{},\"errorCode\":\"invalid_request\",\"errorMessage\":\"request body must be a valid JSON object\"}",
                response.body()
        );
    }

    @Test
    void inputAllowsNullValues() throws Exception {
        var operationRegistry = new OperationRegistry(
                java.util.List.of(operation("world.inspect", true, Set.of("client")))
        );

        startServer(new RequestsEndpoint(
                operationRegistry,
                () -> java.util.List.of("client"),
                (request, resolvedTargetSide) -> {
                    var output = new java.util.LinkedHashMap<String, Object>();
                    output.put("targetSide", resolvedTargetSide);
                    output.put("echo", request.input());
                    return output;
                }
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"requestId\":\"req-3\",\"operationId\":\"world.inspect\",\"input\":{\"foo\":null,\"nested\":{\"bar\":null}}}"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":\"req-3\",\"operationId\":\"world.inspect\",\"targetSide\":\"client\",\"status\":\"ok\",\"output\":{\"targetSide\":\"client\",\"echo\":{\"foo\":null,\"nested\":{\"bar\":null}}},\"errorCode\":null,\"errorMessage\":null}",
                response.body()
        );
    }

    @Test
    void ambiguousOmittedTargetSideReturnsStructuredError() throws Exception {
        var operationRegistry = new OperationRegistry(
                java.util.List.of(operation("world.inspect", true, Set.of("client", "server")))
        );

        startServer(new RequestsEndpoint(
                operationRegistry,
                () -> java.util.List.of("client", "server"),
                (request, resolvedTargetSide) -> Map.of("ok", true)
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"requestId\":\"req-4\",\"operationId\":\"world.inspect\",\"input\":{}}"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":\"req-4\",\"operationId\":\"world.inspect\",\"targetSide\":null,\"status\":\"error\",\"output\":{},\"errorCode\":\"target_side_required\",\"errorMessage\":\"targetSide is required when multiple sides are eligible\"}",
                response.body()
        );
    }

    @Test
    void successfulOutputAllowsNullValues() throws Exception {
        var operationRegistry = new OperationRegistry(
                java.util.List.of(operation("world.inspect", true, Set.of("client")))
        );

        startServer(new RequestsEndpoint(
                operationRegistry,
                () -> java.util.List.of("client"),
                (request, resolvedTargetSide) -> {
                    var output = new java.util.LinkedHashMap<String, Object>();
                    output.put("result", null);
                    output.put("nested", java.util.Collections.singletonMap("value", null));
                    return output;
                }
        ));

        var response = post(
                baseUri().resolve("/api/v1/requests"),
                "{\"requestId\":\"req-5\",\"operationId\":\"world.inspect\",\"input\":{}}"
        );

        assertEquals(200, response.statusCode());
        assertEquals(
                "{\"requestId\":\"req-5\",\"operationId\":\"world.inspect\",\"targetSide\":\"client\",\"status\":\"ok\",\"output\":{\"result\":null,\"nested\":{\"value\":null}},\"errorCode\":null,\"errorMessage\":null}",
                response.body()
        );
    }

    private void startServer(RequestsEndpoint requestsEndpoint) throws IOException {
        server = new HttpServiceServer(
                new ServiceConfig("127.0.0.1", findOpenPort(), tempDir),
                statusEndpoint(),
                new CategoriesEndpoint(List.of()),
                new SkillsEndpoint(new SkillRegistry(List.of(entrySkill()))),
                new OperationsEndpoint(new OperationRegistry(List.of())),
                requestsEndpoint
        );
        server.start();
    }

    private URI baseUri() {
        return server.baseUri();
    }

    private static HttpResponse<String> post(URI uri, String body) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static OperationDefinition operation(String operationId, boolean supportsTargetSide, Set<String> sides) {
        return new OperationDefinition(
                operationId,
                "status",
                "Operation",
                "Operation summary.",
                supportsTargetSide,
                sides,
                Map.of("type", "object"),
                supportsTargetSide
                        ? Map.of("operationId", operationId, "targetSide", sides.iterator().next())
                        : Map.of("operationId", operationId)
        );
    }

    private static SkillDefinition entrySkill() {
        return new SkillDefinition(
                "moddev-entry",
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

    private StatusEndpoint statusEndpoint() {
        return new StatusEndpoint(new StaticStatusProvider());
    }

    private final class StaticStatusProvider implements StatusEndpoint.StatusProvider {

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
            return tempDir;
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
