package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.mcp.service.operation.OperationDefinition;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OperationRequestEndpointTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
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
                "{\"requestId\":\"req-1\",\"operationId\":\"status.check\",\"targetSide\":null,\"status\":\"error\",\"output\":null,\"errorCode\":\"target_side_not_supported\",\"errorMessage\":\"operation does not support target side selection\"}",
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
                "{\"requestId\":\"req-2\",\"operationId\":\"world.inspect\",\"targetSide\":\"client\",\"status\":\"error\",\"output\":null,\"errorCode\":\"operation_execution_failed\",\"errorMessage\":\"operation crashed\"}",
                response.body()
        );
        assertFalse(response.body().contains("disconnected"));
    }

    private void startServer(RequestsEndpoint endpoint) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        endpoint.register(server);
        server.start();
    }

    private URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
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
}
