package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class OperationsEndpoint implements HttpServiceServer.Endpoint {

    private final OperationRegistry operationRegistry;

    public OperationsEndpoint(OperationRegistry operationRegistry) {
        this.operationRegistry = Objects.requireNonNull(operationRegistry, "operationRegistry");
    }

    @Override
    public void register(HttpServer server) {
        server.createContext("/api/v1/operations", this::handle);
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.sendMethodNotAllowed(exchange);
            return;
        }

        var operationPayloads = new ArrayList<Object>(operationRegistry.all().size());
        for (var operation : operationRegistry.all()) {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("operationId", operation.operationId());
            payload.put("categoryId", operation.categoryId());
            payload.put("title", operation.title());
            payload.put("summary", operation.summary());
            payload.put("supportsTargetSide", operation.supportsTargetSide());
            payload.put("availableTargetSides", operation.availableTargetSides());
            payload.put("inputSchema", operation.inputSchema());
            payload.put("exampleRequest", operation.exampleRequest());
            operationPayloads.add(payload);
        }

        var response = new LinkedHashMap<String, Object>();
        response.put("operations", operationPayloads);
        HttpJson.sendJson(exchange, 200, response);
    }
}
