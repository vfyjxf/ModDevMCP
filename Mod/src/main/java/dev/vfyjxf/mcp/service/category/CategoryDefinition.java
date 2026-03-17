package dev.vfyjxf.mcp.service.category;

import java.util.List;

public record CategoryDefinition(
        String categoryId,
        String title,
        String summary,
        List<String> skillIds,
        List<String> operationIds
) {

    public CategoryDefinition {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        ensureNoPadding(categoryId, "categoryId");
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
        validateMembers(skillIds, "skillIds");
        validateMembers(operationIds, "operationIds");
        validateNoDuplicates(skillIds, "skillIds");
        validateNoDuplicates(operationIds, "operationIds");
        skillIds = List.copyOf(skillIds);
        operationIds = List.copyOf(operationIds);
    }

    private static void validateMembers(List<String> values, String fieldName) {
        for (var value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not contain null or blank members");
            }
            ensureNoPadding(value, fieldName);
        }
    }

    private static void validateNoDuplicates(List<String> values, String fieldName) {
        var seen = new java.util.HashSet<String>();
        for (var value : values) {
            if (!seen.add(value)) {
                throw new IllegalArgumentException(fieldName + " must not contain duplicates");
            }
        }
    }

    private static void ensureNoPadding(String value, String fieldName) {
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not contain padded identifiers");
        }
    }
}
