package dev.vfyjxf.mcp.service.skill;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SkillRegistry {

    private static final String ENTRY_SKILL_ID = "moddev-entry";

    private final List<SkillDefinition> all;
    private final Map<String, SkillDefinition> byId;
    private final Map<String, List<SkillDefinition>> byCategoryId;

    public SkillRegistry(List<SkillDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("definitions must not be null");
        }
        var idMap = new LinkedHashMap<String, SkillDefinition>();
        var categoryMap = new LinkedHashMap<String, List<SkillDefinition>>();
        var ordered = new ArrayList<SkillDefinition>();
        for (var definition : definitions) {
            if (definition == null) {
                throw new IllegalArgumentException("definitions must not contain null members");
            }
            var previous = idMap.putIfAbsent(definition.skillId(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate skillId: " + definition.skillId());
            }
            ordered.add(definition);
            categoryMap.computeIfAbsent(definition.categoryId(), ignored -> new ArrayList<>()).add(definition);
        }
        var entrySkill = idMap.get(ENTRY_SKILL_ID);
        if (entrySkill == null) {
            throw new IllegalArgumentException("required skill missing: " + ENTRY_SKILL_ID);
        }
        if (entrySkill.kind() != SkillKind.GUIDANCE || entrySkill.operationId() != null || entrySkill.requiresGame()) {
            throw new IllegalArgumentException(ENTRY_SKILL_ID + " must be guidance-only and not require game");
        }
        this.all = List.copyOf(ordered);
        this.byId = Map.copyOf(idMap);
        var frozenCategoryMap = new LinkedHashMap<String, List<SkillDefinition>>();
        for (var entry : categoryMap.entrySet()) {
            frozenCategoryMap.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.byCategoryId = Map.copyOf(frozenCategoryMap);
    }

    public List<SkillDefinition> all() {
        return all;
    }

    public Optional<SkillDefinition> findById(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        return Optional.ofNullable(byId.get(skillId));
    }

    public List<SkillDefinition> findByCategoryId(String categoryId) {
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
        var expectedSkillIds = byCategoryId
                .getOrDefault(categoryId, List.of())
                .stream()
                .map(SkillDefinition::skillId)
                .toList();
        if (!expectedSkillIds.equals(List.copyOf(categoryDefinition.skillIds()))) {
            throw new IllegalArgumentException("skill ownership mismatch for categoryId: " + categoryId);
        }
    }
}
