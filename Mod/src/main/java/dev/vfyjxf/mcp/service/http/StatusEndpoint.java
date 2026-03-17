package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public final class StatusEndpoint implements HttpServiceServer.Endpoint {

    private final boolean gameReady;
    private final List<String> connectedSides;
    private final String entrySkillId;
    private final Path exportRoot;
    private final String lastError;

    public StatusEndpoint(
            boolean gameReady,
            List<String> connectedSides,
            String entrySkillId,
            Path exportRoot,
            String lastError
    ) {
        this.gameReady = gameReady;
        this.connectedSides = List.copyOf(Objects.requireNonNull(connectedSides, "connectedSides"));
        this.entrySkillId = Objects.requireNonNull(entrySkillId, "entrySkillId");
        this.exportRoot = Objects.requireNonNull(exportRoot, "exportRoot").toAbsolutePath().normalize();
        this.lastError = lastError;
    }

    @Override
    public void register(HttpServer server) {
        server.createContext("/api/v1/status", this::handle);
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.sendMethodNotAllowed(exchange);
            return;
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("serviceReady", true);
        payload.put("gameReady", gameReady);
        payload.put("connectedSides", connectedSides);
        payload.put("entrySkillId", entrySkillId);
        payload.put("exportRoot", exportRoot.toString());
        payload.put("lastError", lastError);
        HttpJson.sendJson(exchange, 200, payload);
    }
}
