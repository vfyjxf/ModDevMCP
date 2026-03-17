package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public final class StatusEndpoint implements HttpServiceServer.Endpoint {

    public interface StatusProvider {
        boolean serviceReady();

        boolean gameReady();

        List<String> connectedSides();

        String entrySkillId();

        Path exportRoot();

        String lastError();
    }

    private final StatusProvider statusProvider;

    public StatusEndpoint(StatusProvider statusProvider) {
        this.statusProvider = Objects.requireNonNull(statusProvider, "statusProvider");
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

        var connectedSides = List.copyOf(Objects.requireNonNull(statusProvider.connectedSides(), "connectedSides"));
        var entrySkillId = Objects.requireNonNull(statusProvider.entrySkillId(), "entrySkillId");
        var exportRoot = Objects.requireNonNull(statusProvider.exportRoot(), "exportRoot").toAbsolutePath().normalize();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("serviceReady", statusProvider.serviceReady());
        payload.put("gameReady", statusProvider.gameReady());
        payload.put("connectedSides", connectedSides);
        payload.put("entrySkillId", entrySkillId);
        payload.put("exportRoot", exportRoot.toString());
        payload.put("lastError", statusProvider.lastError());
        HttpJson.sendJson(exchange, 200, payload);
    }
}
