package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.mcp.service.export.SkillExportService;
import dev.vfyjxf.mcp.service.skill.SkillRegistry;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class SkillsEndpoint implements HttpServiceServer.Endpoint {

    private static final String BASE_PATH = "/api/v1/skills";

    private final SkillRegistry skillRegistry;
    private final SkillExportService skillExportService;

    public SkillsEndpoint(SkillRegistry skillRegistry) {
        this(skillRegistry, null);
    }

    public SkillsEndpoint(SkillRegistry skillRegistry, SkillExportService skillExportService) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.skillExportService = skillExportService;
    }

    @Override
    public void register(HttpServer server) {
        server.createContext(BASE_PATH, this::handle);
    }

    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        var rawPath = exchange.getRequestURI().getRawPath();
        if ("POST".equals(exchange.getRequestMethod()) && (BASE_PATH + "/export").equals(path)) {
            handleExport(exchange);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.sendMethodNotAllowed(exchange);
            return;
        }
        if (BASE_PATH.equals(path) || (BASE_PATH + "/").equals(path)) {
            handleList(exchange);
            return;
        }
        if (!path.startsWith(BASE_PATH + "/")) {
            HttpJson.sendNotFound(exchange);
            return;
        }

        var remainder = rawPath.substring((BASE_PATH + "/").length());
        var parts = remainder.split("/");
        if (parts.length == 2 && "markdown".equals(parts[1])) {
            handleMarkdown(exchange, decodePathSegment(parts[0]));
            return;
        }
        HttpJson.sendNotFound(exchange);
    }

    private void handleExport(HttpExchange exchange) throws IOException {
        if (skillExportService == null) {
            HttpJson.sendNotFound(exchange);
            return;
        }
        var result = skillExportService.exportAll();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("exported", result.exported());
        payload.put("exportRoot", result.exportRoot().toString());
        payload.put("categoryCount", result.categoryCount());
        payload.put("skillCount", result.skillCount());
        HttpJson.sendJson(exchange, 200, payload);
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

    private static String decodePathSegment(String rawSegment) {
        var bytes = new ByteArrayOutputStream(rawSegment.length());
        for (int i = 0; i < rawSegment.length(); i++) {
            var ch = rawSegment.charAt(i);
            if (ch == '%') {
                if (i + 2 >= rawSegment.length()) {
                    throw new IllegalArgumentException("incomplete percent escape in path segment");
                }
                var hi = Character.digit(rawSegment.charAt(i + 1), 16);
                var lo = Character.digit(rawSegment.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw new IllegalArgumentException("invalid percent escape in path segment");
                }
                bytes.write((hi << 4) + lo);
                i += 2;
            } else {
                var encoded = String.valueOf(ch).getBytes(StandardCharsets.UTF_8);
                bytes.write(encoded, 0, encoded.length);
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
