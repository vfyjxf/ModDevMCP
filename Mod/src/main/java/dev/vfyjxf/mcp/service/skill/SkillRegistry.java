package dev.vfyjxf.mcp.service.skill;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;

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
        validateLookupId(skillId, "skillId");
        return Optional.ofNullable(byId.get(skillId));
    }

    public List<SkillDefinition> findByCategoryId(String categoryId) {
        validateLookupId(categoryId, "categoryId");
        return byCategoryId.getOrDefault(categoryId, List.of());
    }

    public void validateCategoryOwnership(CategoryDefinition categoryDefinition) {
        if (categoryDefinition == null) {
            throw new IllegalArgumentException("categoryDefinition must not be null");
        }
        var categoryId = categoryDefinition.categoryId();
        validateLookupId(categoryId, "categoryDefinition.categoryId");
        var expectedSkillIds = byCategoryId
                .getOrDefault(categoryId, List.of())
                .stream()
                .map(SkillDefinition::skillId)
                .toList();
        if (!expectedSkillIds.equals(List.copyOf(categoryDefinition.skillIds()))) {
            throw new IllegalArgumentException("skill ownership mismatch for categoryId: " + categoryId);
        }
    }

    public void validateOperationBindings(OperationRegistry operationRegistry) {
        if (operationRegistry == null) {
            throw new IllegalArgumentException("operationRegistry must not be null");
        }
        for (var skill : all) {
            if (skill.kind() == SkillKind.GUIDANCE) {
                continue;
            }
            var operation = operationRegistry.findById(skill.operationId())
                    .orElseThrow(() -> new IllegalArgumentException("missing operationId for skillId: " + skill.skillId()));
            if (!operation.categoryId().equals(skill.categoryId())) {
                throw new IllegalArgumentException("operation category mismatch for skillId: " + skill.skillId());
            }
        }
    }

    public void validateDeclaredCategories(List<CategoryDefinition> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }
        var declared = new java.util.HashSet<String>();
        for (var category : categories) {
            if (category == null) {
                throw new IllegalArgumentException("categories must not contain null members");
            }
            declared.add(category.categoryId());
        }
        for (var skill : all) {
            if (!declared.contains(skill.categoryId())) {
                throw new IllegalArgumentException("orphan skill categoryId: " + skill.categoryId());
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
