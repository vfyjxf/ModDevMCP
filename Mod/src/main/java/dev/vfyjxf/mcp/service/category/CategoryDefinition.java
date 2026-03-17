package dev.vfyjxf.mcp.service.category;

public record CategoryDefinition(
        String categoryId,
        String title,
        String summary
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
    }
}
