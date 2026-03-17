package dev.vfyjxf.mcp.service.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SkillRegistry {

    private static final String ENTRY_SKILL_ID = "moddev-entry";

    private final List<SkillDefinition> all;
    private final Map<String, SkillDefinition> byId;
    private final Map<String, List<SkillDefinition>> byCategoryId;

    public SkillRegistry(Collection<SkillDefinition> definitions) {
        var idMap = new LinkedHashMap<String, SkillDefinition>();
        var categoryMap = new LinkedHashMap<String, List<SkillDefinition>>();
        var ordered = new ArrayList<SkillDefinition>();
        if (definitions != null) {
            for (var definition : definitions) {
                var previous = idMap.putIfAbsent(definition.skillId(), definition);
                if (previous != null) {
                    throw new IllegalArgumentException("duplicate skillId: " + definition.skillId());
                }
                ordered.add(definition);
                categoryMap.computeIfAbsent(definition.categoryId(), ignored -> new ArrayList<>()).add(definition);
            }
        }
        if (!idMap.containsKey(ENTRY_SKILL_ID)) {
            throw new IllegalArgumentException("required skill missing: " + ENTRY_SKILL_ID);
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
        return Optional.ofNullable(byId.get(skillId));
    }

    public List<SkillDefinition> findByCategoryId(String categoryId) {
        return byCategoryId.getOrDefault(categoryId, List.of());
    }
}
