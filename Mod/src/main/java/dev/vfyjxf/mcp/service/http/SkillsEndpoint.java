package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.mcp.service.skill.SkillRegistry;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class SkillsEndpoint implements HttpServiceServer.Endpoint {

    private static final String BASE_PATH = "/api/v1/skills";

    private final SkillRegistry skillRegistry;

    public SkillsEndpoint(SkillRegistry skillRegistry) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
    }

    @Override
    public void register(HttpServer server) {
        server.createContext(BASE_PATH, this::handle);
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.sendMethodNotAllowed(exchange);
            return;
        }

        var path = exchange.getRequestURI().getPath();
        if (BASE_PATH.equals(path) || (BASE_PATH + "/").equals(path)) {
            handleList(exchange);
            return;
        }
        if (!path.startsWith(BASE_PATH + "/")) {
            HttpJson.sendNotFound(exchange);
            return;
        }

        var remainder = path.substring((BASE_PATH + "/").length());
        var parts = remainder.split("/");
        if (parts.length == 2 && "markdown".equals(parts[1])) {
            handleMarkdown(exchange, URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
            return;
        }
        HttpJson.sendNotFound(exchange);
    }

    private void handleList(HttpExchange exchange) throws IOException {
        var skillPayloads = new ArrayList<Object>(skillRegistry.all().size());
        for (var skill : skillRegistry.all()) {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("skillId", skill.skillId());
            payload.put("categoryId", skill.categoryId());
            payload.put("kind", skill.kind().value());
            payload.put("title", skill.title());
            payload.put("summary", skill.summary());
            payload.put("operationId", skill.operationId());
            payload.put("tags", skill.tags());
            payload.put("requiresGame", skill.requiresGame());
            skillPayloads.add(payload);
        }
        var response = new LinkedHashMap<String, Object>();
        response.put("skills", skillPayloads);
        HttpJson.sendJson(exchange, 200, response);
    }

    private void handleMarkdown(HttpExchange exchange, String skillId) throws IOException {
        try {
            var skill = skillRegistry.findById(skillId);
            if (skill.isEmpty()) {
                HttpJson.sendNotFound(exchange);
                return;
            }
            HttpJson.sendMarkdown(exchange, 200, skill.get().markdown());
        } catch (IllegalArgumentException ignored) {
            HttpJson.sendNotFound(exchange);
        }
    }
}
