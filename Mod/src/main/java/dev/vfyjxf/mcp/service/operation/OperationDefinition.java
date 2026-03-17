package dev.vfyjxf.mcp.service.operation;

import java.util.Map;
import java.util.Set;

public record OperationDefinition(
        String operationId,
        String categoryId,
        String title,
        String summary,
        boolean supportsTargetSide,
        Set<String> availableTargetSides,
        Map<String, Object> inputSchema,
        Map<String, Object> exampleRequest
) {

    public OperationDefinition {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }

        availableTargetSides = availableTargetSides == null ? Set.of() : Set.copyOf(availableTargetSides);
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
        exampleRequest = exampleRequest == null ? Map.of() : Map.copyOf(exampleRequest);

        if (supportsTargetSide && availableTargetSides.isEmpty()) {
            throw new IllegalArgumentException("availableTargetSides must not be empty when supportsTargetSide is true");
        }
        if (!supportsTargetSide && !availableTargetSides.isEmpty()) {
            throw new IllegalArgumentException("availableTargetSides must be empty when supportsTargetSide is false");
        }
    }
}
