package dev.vfyjxf.mcp.service.export;

import dev.vfyjxf.mcp.service.category.CategoryDefinition;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.skill.SkillRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SkillExportService {

    private final ServiceConfig config;
    private final List<CategoryDefinition> categories;
    private final SkillRegistry skillRegistry;
    private final SkillExportLayout layout;

    public SkillExportService(
            ServiceConfig config,
            List<CategoryDefinition> categories,
            SkillRegistry skillRegistry
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.layout = new SkillExportLayout(config.exportRoot());
    }

    public ExportResult exportAll() {
        try {
            recreateManagedTree();
            writeSkills();
            writeCategories();
            writeIndexes();
            writeManifest();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to export skills", exception);
        }
        return new ExportResult(true, layout.root(), categories.size(), skillRegistry.all().size());
    }

    public SkillExportLayout layout() {
        return layout;
    }

    private void recreateManagedTree() throws IOException {
        Files.createDirectories(layout.root());
        deleteIfExists(layout.skillsDir());
        deleteIfExists(layout.categoriesDir());
        deleteIfExists(layout.indexesDir());
        Files.deleteIfExists(layout.manifestPath());
        Files.createDirectories(layout.skillsDir());
        Files.createDirectories(layout.categoriesDir());
        Files.createDirectories(layout.indexesDir());
    }

    private void writeSkills() throws IOException {
        for (var skill : skillRegistry.all()) {
            Files.writeString(layout.skillMarkdownPath(skill.skillId()), skill.markdown(), StandardCharsets.UTF_8);
        }
    }

    private void writeCategories() throws IOException {
        for (var category : categories) {
            var categorySkill = skillRegistry.findById(category.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("missing category skill for categoryId: " + category.categoryId()));
            Files.writeString(layout.categoryMarkdownPath(category.categoryId()), categorySkill.markdown(), StandardCharsets.UTF_8);
        }
    }

    private void writeIndexes() throws IOException {
        var skillsIndex = new StringBuilder("# Skills\n\n");
        for (var skill : skillRegistry.all()) {
            skillsIndex.append("- `").append(skill.skillId()).append("` - ").append(skill.summary()).append('\n');
        }
        Files.writeString(layout.skillsIndexPath(), skillsIndex.toString(), StandardCharsets.UTF_8);

        var categoriesIndex = new StringBuilder("# Categories\n\n");
        for (var category : categories) {
            categoriesIndex.append("- `").append(category.categoryId()).append("` - ").append(category.summary()).append('\n');
        }
        Files.writeString(layout.categoriesIndexPath(), categoriesIndex.toString(), StandardCharsets.UTF_8);
    }

    private void writeManifest() throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("entrySkillId", "moddev-usage");
        payload.put("exportRoot", config.exportRoot().toAbsolutePath().normalize().toString());
        payload.put("skills", skillRegistry.all().stream().map(skill -> skill.skillId()).toList());
        payload.put("categories", categories.stream().map(category -> category.categoryId()).toList());
        Files.writeString(layout.manifestPath(), toJson(payload), StandardCharsets.UTF_8);
    }

    private static void deleteIfExists(java.nio.file.Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(entry -> {
                try {
                    Files.deleteIfExists(entry);
                } catch (IOException exception) {
                    throw new IllegalStateException("failed to delete export path: " + entry, exception);
                }
            });
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escape(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            var builder = new StringBuilder();
            builder.append('{');
            var iterator = mapValue.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                builder.append(toJson(String.valueOf(entry.getKey())));
                builder.append(':');
                builder.append(toJson(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof List<?> listValue) {
            var builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < listValue.size(); i++) {
                builder.append(toJson(listValue.get(i)));
                if (i + 1 < listValue.size()) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        throw new IllegalArgumentException("unsupported manifest value type: " + value.getClass().getName());
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record ExportResult(
            boolean exported,
            java.nio.file.Path exportRoot,
            int categoryCount,
            int skillCount
    ) {
    }
}

