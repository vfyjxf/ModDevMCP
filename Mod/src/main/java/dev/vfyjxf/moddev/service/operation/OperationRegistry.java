package dev.vfyjxf.moddev.service.operation;

import dev.vfyjxf.moddev.service.category.CategoryDefinition;

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
        validateLookupId(operationId, "operationId");
        return Optional.ofNullable(byId.get(operationId));
    }

    public List<OperationDefinition> findByCategoryId(String categoryId) {
        validateLookupId(categoryId, "categoryId");
        return byCategoryId.getOrDefault(categoryId, List.of());
    }

    public void validateCategoryOwnership(CategoryDefinition categoryDefinition) {
        if (categoryDefinition == null) {
            throw new IllegalArgumentException("categoryDefinition must not be null");
        }
        var categoryId = categoryDefinition.categoryId();
        validateLookupId(categoryId, "categoryDefinition.categoryId");
        var expectedOperationIds = byCategoryId
                .getOrDefault(categoryId, List.of())
                .stream()
                .map(OperationDefinition::operationId)
                .toList();
        if (!expectedOperationIds.equals(List.copyOf(categoryDefinition.operationIds()))) {
            throw new IllegalArgumentException("operation ownership mismatch for categoryId: " + categoryId);
        }
    }

    public void validateDeclaredCategories(List<CategoryDefinition> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }
        var declaredById = new java.util.LinkedHashMap<String, CategoryDefinition>();
        for (var category : categories) {
            if (category == null) {
                throw new IllegalArgumentException("categories must not contain null members");
            }
            if (declaredById.putIfAbsent(category.categoryId(), category) != null) {
                throw new IllegalArgumentException("duplicate declared categoryId: " + category.categoryId());
            }
        }
        for (var category : declaredById.values()) {
            validateCategoryOwnership(category);
        }
        for (var operation : all) {
            if (!declaredById.containsKey(operation.categoryId())) {
                throw new IllegalArgumentException("orphan operation categoryId: " + operation.categoryId());
            }
        }
    }

    private static void validateLookupId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not include leading or trailing whitespace");
        }
    }
}

