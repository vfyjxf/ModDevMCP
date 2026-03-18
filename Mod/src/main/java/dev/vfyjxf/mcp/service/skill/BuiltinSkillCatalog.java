package dev.vfyjxf.mcp.service.skill;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.http.HttpServiceServer;
import dev.vfyjxf.mcp.service.operation.OperationDefinition;
import dev.vfyjxf.mcp.service.operation.OperationRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BuiltinSkillCatalog {

    private static final List<CategorySpec> CATEGORY_SPECS = List.of(
            new CategorySpec("status", "Status", "Inspect local service readiness and troubleshooting state."),
            new CategorySpec("ui", "UI", "Inspect screens, refs, and interactive state inside the running game."),
            new CategorySpec("command", "Command", "Discover, suggest, and execute Minecraft commands."),
            new CategorySpec("world", "World", "List, create, and join local worlds from the client runtime."),
            new CategorySpec("hotswap", "Hotswap", "Compile or reload changed classes into the running game.")
    );

    private final SkillMarkdownLoader markdownLoader;

    public BuiltinSkillCatalog() {
        this(new SkillMarkdownLoader());
    }

    public BuiltinSkillCatalog(SkillMarkdownLoader markdownLoader) {
        this.markdownLoader = Objects.requireNonNull(markdownLoader, "markdownLoader");
    }

    public Catalog build(ServiceConfig config, OperationRegistry operationRegistry) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(operationRegistry, "operationRegistry");

        var baseUri = HttpServiceServer.buildBaseUri(config.host(), config.port()).toString();
        var skills = new ArrayList<SkillDefinition>();
        var categories = new ArrayList<CategoryDefinition>();

        skills.add(new SkillDefinition(
                "moddev-usage",
                "status",
                SkillKind.GUIDANCE,
                "ModDev Entry",
                "Start here for local service discovery and request conventions.",
                null,
                Set.of("entry", "status"),
                false,
                markdownLoader.loadEntryMarkdown(baseUri)
        ));

        for (var spec : CATEGORY_SPECS) {
            var operations = operationRegistry.findByCategoryId(spec.categoryId());
            skills.add(new SkillDefinition(
                    spec.categoryId(),
                    spec.categoryId(),
                    SkillKind.GUIDANCE,
                    spec.title(),
                    spec.summary(),
                    null,
                    Set.of("category", spec.categoryId()),
                    false,
                    markdownLoader.loadCategoryMarkdown(spec.categoryId(), spec.title(), spec.summary(), operations, baseUri)
            ));

            for (var operation : operations) {
                skills.add(operationSkill(operation, baseUri));
            }

            categories.add(categoryDefinition(spec, operations));
        }

        var skillRegistry = new SkillRegistry(skills);
        skillRegistry.validateDeclaredCategories(categories);
        skillRegistry.validateOperationBindings(operationRegistry);
        operationRegistry.validateDeclaredCategories(categories);
        return new Catalog(List.copyOf(categories), skillRegistry);
    }

    private SkillDefinition operationSkill(OperationDefinition operation, String baseUri) {
        return new SkillDefinition(
                operation.operationId(),
                operation.categoryId(),
                SkillKind.HYBRID,
                operation.title(),
                operation.summary(),
                operation.operationId(),
                operationTags(operation),
                requiresGame(operation),
                markdownLoader.loadOperationMarkdown(operation, baseUri)
        );
    }

    private CategoryDefinition categoryDefinition(CategorySpec spec, List<OperationDefinition> operations) {
        var skillIds = new ArrayList<String>();
        skillIds.add(spec.categoryId());
        skillIds.addAll(operations.stream().map(OperationDefinition::operationId).toList());
        if ("status".equals(spec.categoryId())) {
            skillIds.addFirst("moddev-usage");
        }
        return new CategoryDefinition(
                spec.categoryId(),
                spec.title(),
                spec.summary(),
                skillIds,
                operations.stream().map(OperationDefinition::operationId).toList()
        );
    }

    private static Set<String> operationTags(OperationDefinition operation) {
        var tags = new LinkedHashSet<String>();
        tags.add(operation.categoryId());
        tags.add("operation");
        if (operation.supportsTargetSide()) {
            tags.add("target-side");
        }
        return Set.copyOf(tags);
    }

    private static boolean requiresGame(OperationDefinition operation) {
        return !"status".equals(operation.categoryId());
    }

    public record Catalog(
            List<CategoryDefinition> categories,
            SkillRegistry skillRegistry
    ) {
    }

    private record CategorySpec(
            String categoryId,
            String title,
            String summary
    ) {
    }
}

