package dev.vfyjxf.mcp.service.category;

import java.util.Set;

public record CategoryDefinition(
        String categoryId,
        String title,
        String summary,
        Set<String> skillIds,
        Set<String> operationIds
) {

    public CategoryDefinition {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (skillIds == null) {
            throw new IllegalArgumentException("skillIds must not be null");
        }
        if (operationIds == null) {
            throw new IllegalArgumentException("operationIds must not be null");
        }
        skillIds = Set.copyOf(skillIds);
        operationIds = Set.copyOf(operationIds);
    }
}
