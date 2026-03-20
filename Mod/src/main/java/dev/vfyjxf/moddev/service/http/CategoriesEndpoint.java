package dev.vfyjxf.moddev.service.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.moddev.service.category.CategoryDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public final class CategoriesEndpoint implements HttpServiceServer.Endpoint {

    private final List<CategoryDefinition> categories;

    public CategoriesEndpoint(List<CategoryDefinition> categories) {
        this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
    }

    @Override
    public void register(HttpServer server) {
        server.createContext("/api/v1/categories", this::handle);
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpJson.sendMethodNotAllowed(exchange);
            return;
        }

        var categoryPayloads = new ArrayList<Object>(categories.size());
        for (var category : categories) {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("categoryId", category.categoryId());
            payload.put("title", category.title());
            payload.put("summary", category.summary());
            payload.put("skillIds", category.skillIds());
            payload.put("operationIds", category.operationIds());
            categoryPayloads.add(payload);
        }

        var response = new LinkedHashMap<String, Object>();
        response.put("categories", categoryPayloads);
        HttpJson.sendJson(exchange, 200, response);
    }
}

