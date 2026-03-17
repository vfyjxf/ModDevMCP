package dev.vfyjxf.mcp.service.skill;

import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.ArrayList;

public record SkillDefinition(
        String skillId,
        String categoryId,
        SkillKind kind,
        String title,
        String summary,
        String operationId,
        Set<String> tags,
        boolean requiresGame,
        String markdown
) {

    public SkillDefinition {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        ensureNoPadding(skillId, "skillId");
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        ensureNoPadding(categoryId, "categoryId");
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (tags == null) {
            throw new IllegalArgumentException("tags must not be null");
        }
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("markdown must not be blank");
        }
        validateTagMembers(tags);

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
        tags = freezeTags(tags);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        ensureNoPadding(value, "operationId");
        return value;
    }

    private static void validateTagMembers(Set<String> tags) {
        for (var tag : tags) {
            if (tag == null || tag.isBlank()) {
                throw new IllegalArgumentException("tags must not contain null or blank members");
            }
        }
    }

    private static Set<String> freezeTags(Set<String> tags) {
        var ordered = new ArrayList<>(tags);
        Collections.sort(ordered);
        return Collections.unmodifiableSet(new LinkedHashSet<>(ordered));
    }

    private static void ensureNoPadding(String value, String fieldName) {
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not include leading or trailing whitespace");
        }
    }
}
