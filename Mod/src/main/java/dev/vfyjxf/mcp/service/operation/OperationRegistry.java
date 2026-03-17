package dev.vfyjxf.mcp.service.operation;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OperationRegistry {

    private final List<OperationDefinition> all;
    private final Map<String, OperationDefinition> byId;
    private final Map<String, List<OperationDefinition>> byCategoryId;

    public OperationRegistry(List<OperationDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("definitions must not be null");
        }
        var idMap = new LinkedHashMap<String, OperationDefinition>();
        var categoryMap = new LinkedHashMap<String, List<OperationDefinition>>();
        var ordered = new ArrayList<OperationDefinition>();
        for (var definition : definitions) {
            if (definition == null) {
                throw new IllegalArgumentException("definitions must not contain null members");
            }
            var previous = idMap.putIfAbsent(definition.operationId(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate operationId: " + definition.operationId());
            }
            ordered.add(definition);
            categoryMap.computeIfAbsent(definition.categoryId(), ignored -> new ArrayList<>()).add(definition);
        }
        this.all = List.copyOf(ordered);
        this.byId = Map.copyOf(idMap);
        var frozenCategoryMap = new LinkedHashMap<String, List<OperationDefinition>>();
        for (var entry : categoryMap.entrySet()) {
            frozenCategoryMap.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.byCategoryId = Map.copyOf(frozenCategoryMap);
    }

    public List<OperationDefinition> all() {
        return all;
    }

    public Optional<OperationDefinition> findById(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        return Optional.ofNullable(byId.get(operationId));
    }

    public List<OperationDefinition> findByCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        return byCategoryId.getOrDefault(categoryId, List.of());
    }

    public void validateCategoryOwnership(CategoryDefinition categoryDefinition) {
        if (categoryDefinition == null) {
            throw new IllegalArgumentException("categoryDefinition must not be null");
        }
        var categoryId = categoryDefinition.categoryId();
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryDefinition.categoryId must not be blank");
        }
        var expectedOperationIds = byCategoryId
                .getOrDefault(categoryId, List.of())
                .stream()
                .map(OperationDefinition::operationId)
                .toList();
        if (!expectedOperationIds.equals(List.copyOf(categoryDefinition.operationIds()))) {
            throw new IllegalArgumentException("operation ownership mismatch for categoryId: " + categoryId);
        }
    }
}
