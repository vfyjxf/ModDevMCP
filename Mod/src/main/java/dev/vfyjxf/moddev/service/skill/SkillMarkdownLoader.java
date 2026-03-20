package dev.vfyjxf.moddev.service.skill;

import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillMarkdownLoader {
    private static final String PROJECT_REGISTRY_PATH_HINT = "<gradleProject>/build/moddevmcp/game-instances.json";

    public String loadEntryMarkdown(String baseUri) {
        return renderResource("moddev-service/skills/moddev-usage.md", Map.of(
                "baseUri", baseUri,
                "projectRegistryPathHint", PROJECT_REGISTRY_PATH_HINT
        ));
    }

    public String loadCategoryMarkdown(
            String categoryId,
            String title,
            String summary,
            List<OperationDefinition> operations,
            String baseUri
    ) {
        return renderResource("moddev-service/categories/" + categoryId + ".md", Map.of(
                "baseUri", baseUri,
                "projectRegistryPathHint", PROJECT_REGISTRY_PATH_HINT,
                "categoryId", categoryId,
                "title", title,
                "summary", summary,
                "operationIds", formatOperationIds(operations),
                "curlExample", firstCurlExample(operations, baseUri)
        ));
    }

    public String loadOperationMarkdown(OperationDefinition operation, String baseUri) {
        return renderResource("moddev-service/skills/operation.md", Map.of(
                "baseUri", baseUri,
                "projectRegistryPathHint", PROJECT_REGISTRY_PATH_HINT,
                "operationId", operation.operationId(),
                "categoryId", operation.categoryId(),
                "title", operation.title(),
                "summary", operation.summary(),
                "targetSideRule", targetSideRule(operation),
                "commonFailureCodes", commonFailureCodes(operation),
                "curlExample", curlExample(operation, baseUri)
        ));
    }

    private String renderResource(String resourcePath, Map<String, String> replacements) {
        var template = readResource(resourcePath);
        for (var entry : replacements.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private String readResource(String resourcePath) {
        try (InputStream stream = SkillMarkdownLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("missing bundled skill markdown resource: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read bundled skill markdown resource: " + resourcePath, exception);
        }
    }

    private static String formatOperationIds(List<OperationDefinition> operations) {
        if (operations.isEmpty()) {
            return "- none";
        }
        return operations.stream()
                .map(operation -> "- `" + operation.operationId() + "`")
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String firstCurlExample(List<OperationDefinition> operations, String baseUri) {
        if (operations.isEmpty()) {
            return "No request example is available because this category currently exposes guidance only.";
        }
        return curlExample(operations.getFirst(), baseUri);
    }

    private static String targetSideRule(OperationDefinition operation) {
        if (!operation.supportsTargetSide()) {
            return "Do not send `targetSide` for this operation.";
        }
        if (operation.availableTargetSides().size() == 1) {
            return "This operation supports `targetSide`, but omission is safe when only `" + operation.availableTargetSides().iterator().next() + "` can handle it.";
        }
        return "Send `targetSide` when both client and server are connected. Omit it only when exactly one eligible side is connected.";
    }

    private static String commonFailureCodes(OperationDefinition operation) {
        var codes = new java.util.ArrayList<String>();
        codes.add("- `invalid_request`");
        if (operation.supportsTargetSide()) {
            codes.add("- `target_side_required`");
            codes.add("- `target_side_unsupported`");
            codes.add("- `target_side_disconnected`");
        } else {
            codes.add("- `target_side_not_supported`");
        }
        codes.add("- `operation_execution_failed`");
        return String.join("\n", codes);
    }

    private static String curlExample(OperationDefinition operation, String baseUri) {
        return """
                ```bash
                curl -X POST %s/api/v1/requests \\
                  -H "Content-Type: application/json" \\
                  -d '%s'
                ```
                """.formatted(baseUri, jsonLiteral(operation.exampleRequest()));
    }

    private static String jsonLiteral(Map<String, Object> payload) {
        return toJson(freezeForJson(payload));
    }

    private static Map<String, Object> freezeForJson(Map<String, Object> payload) {
        var normalized = new LinkedHashMap<String, Object>();
        normalized.putAll(payload);
        if (!normalized.containsKey("input")) {
            normalized.put("input", Map.of());
        }
        return Map.copyOf(normalized);
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escape(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            var builder = new StringBuilder();
            builder.append('{');
            var iterator = mapValue.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                builder.append(toJson(String.valueOf(entry.getKey())));
                builder.append(':');
                builder.append(toJson(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof List<?> listValue) {
            var builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < listValue.size(); i++) {
                builder.append(toJson(listValue.get(i)));
                if (i + 1 < listValue.size()) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        throw new IllegalArgumentException("unsupported JSON example value type: " + value.getClass().getName());
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}


