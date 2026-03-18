package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.mcp.server.transport.JsonCodec;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;
import dev.vfyjxf.mcp.service.request.OperationError;
import dev.vfyjxf.mcp.service.request.OperationExecutionException;
import dev.vfyjxf.mcp.service.request.OperationRequest;
import dev.vfyjxf.mcp.service.request.OperationResponse;
import dev.vfyjxf.mcp.service.runtime.TargetSideResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RequestsEndpoint implements HttpServiceServer.Endpoint {

    private static final String BASE_PATH = "/api/v1/requests";

    public interface ConnectedSidesProvider {
        List<String> connectedSides();
    }

    public interface OperationExecutor {
        Map<String, Object> execute(OperationRequest request, String resolvedTargetSide) throws Exception;
    }

    private final OperationRegistry operationRegistry;
    private final ConnectedSidesProvider connectedSidesProvider;
    private final OperationExecutor executor;
    private final TargetSideResolver targetSideResolver;
    private final JsonCodec jsonCodec;

    public RequestsEndpoint(
            OperationRegistry operationRegistry,
            ConnectedSidesProvider connectedSidesProvider,
            OperationExecutor executor
    ) {
        this(operationRegistry, connectedSidesProvider, executor, new TargetSideResolver(), new JsonCodec());
    }

    RequestsEndpoint(
            OperationRegistry operationRegistry,
            ConnectedSidesProvider connectedSidesProvider,
            OperationExecutor executor,
            TargetSideResolver targetSideResolver,
            JsonCodec jsonCodec
    ) {
        this.operationRegistry = Objects.requireNonNull(operationRegistry, "operationRegistry");
        this.connectedSidesProvider = Objects.requireNonNull(connectedSidesProvider, "connectedSidesProvider");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.targetSideResolver = Objects.requireNonNull(targetSideResolver, "targetSideResolver");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
    }

    @Override
    public void register(HttpServer server) {
        server.createContext(BASE_PATH, this::handle);
    }

    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        if (!BASE_PATH.equals(path) && !(BASE_PATH + "/").equals(path)) {
            HttpJson.sendNotFound(exchange);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Allow", "POST");
            HttpJson.sendJson(exchange, 405, Map.of("error", "method_not_allowed"));
            return;
        }

        OperationRequest request;
        try {
            request = OperationRequest.fromPayload(jsonCodec.parseObject(exchange.getRequestBody().readAllBytes()));
        } catch (RuntimeException exception) {
            send(exchange, OperationResponse.error(
                    null,
                    null,
                    null,
                    invalidRequestError(exception)
            ));
            return;
        }

        var operation = operationRegistry.findById(request.operationId()).orElse(null);
        if (operation == null) {
            send(exchange, OperationResponse.error(
                    request.requestId(),
                    request.operationId(),
                    null,
                    new OperationError("operation_not_found", "operation was not found")
            ));
            return;
        }

        String resolvedTargetSide;
        try {
            resolvedTargetSide = targetSideResolver.resolve(
                    operation,
                    request.targetSide(),
                    connectedSidesProvider.connectedSides()
            );
        } catch (TargetSideResolver.TargetSideResolutionException exception) {
            send(exchange, OperationResponse.error(
                    request.requestId(),
                    request.operationId(),
                    null,
                    exception.error()
            ));
            return;
        }

        try {
            var output = executor.execute(request, resolvedTargetSide);
            send(exchange, OperationResponse.success(
                    request.requestId(),
                    request.operationId(),
                    resolvedTargetSide,
                    output
            ));
        } catch (OperationExecutionException exception) {
            send(exchange, OperationResponse.error(
                    request.requestId(),
                    request.operationId(),
                    resolvedTargetSide,
                    exception.error()
            ));
        } catch (Exception exception) {
            var message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            send(exchange, OperationResponse.error(
                    request.requestId(),
                    request.operationId(),
                    resolvedTargetSide,
                    new OperationError("operation_execution_failed", message)
            ));
        }
    }

    private static void send(HttpExchange exchange, OperationResponse response) throws IOException {
        HttpJson.sendJson(exchange, 200, response.toPayload());
    }

    private static OperationError invalidRequestError(RuntimeException exception) {
        if (!(exception instanceof IllegalArgumentException)) {
            return new OperationError("invalid_request", "request body must be a valid JSON object");
        }
        var message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "request body must be a valid JSON object";
        }
        return new OperationError("invalid_request", message);
    }
}
