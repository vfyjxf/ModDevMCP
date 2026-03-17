package dev.vfyjxf.mcp.service.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OperationRegistry {

    private final List<OperationDefinition> all;
    private final Map<String, OperationDefinition> byId;
    private final Map<String, List<OperationDefinition>> byCategoryId;

    public OperationRegistry(Collection<OperationDefinition> definitions) {
        var idMap = new LinkedHashMap<String, OperationDefinition>();
        var categoryMap = new LinkedHashMap<String, List<OperationDefinition>>();
        var ordered = new ArrayList<OperationDefinition>();
        if (definitions != null) {
            for (var definition : definitions) {
                var previous = idMap.putIfAbsent(definition.operationId(), definition);
                if (previous != null) {
                    throw new IllegalArgumentException("duplicate operationId: " + definition.operationId());
                }
                ordered.add(definition);
                categoryMap.computeIfAbsent(definition.categoryId(), ignored -> new ArrayList<>()).add(definition);
            }
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
        return Optional.ofNullable(byId.get(operationId));
    }

    public List<OperationDefinition> findByCategoryId(String categoryId) {
        return byCategoryId.getOrDefault(categoryId, List.of());
    }
}
