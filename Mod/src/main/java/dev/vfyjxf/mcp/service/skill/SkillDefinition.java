package dev.vfyjxf.mcp.service.skill;

public record SkillDefinition(
        String skillId,
        String categoryId,
        SkillKind kind,
        String title,
        String summary,
        String operationId
) {

    public SkillDefinition {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }

        var normalizedOperationId = normalize(operationId);
        switch (kind) {
            case GUIDANCE -> {
                if (normalizedOperationId != null) {
                    throw new IllegalArgumentException("guidance skill must not define operationId");
                }
            }
            case ACTION, HYBRID -> {
                if (normalizedOperationId == null) {
                    throw new IllegalArgumentException(kind.value() + " skill must define operationId");
                }
            }
        }

        operationId = normalizedOperationId;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
