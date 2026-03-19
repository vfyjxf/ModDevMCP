package dev.vfyjxf.moddev.service.export;

import java.nio.file.Path;
import java.util.Objects;

public record SkillExportLayout(Path root) {

    public SkillExportLayout {
        root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    public Path manifestPath() {
        return root.resolve("manifest.json");
    }

    public Path skillsDir() {
        return root.resolve("skills");
    }

    public Path categoriesDir() {
        return root.resolve("categories");
    }

    public Path indexesDir() {
        return root.resolve("indexes");
    }

    public Path skillMarkdownPath(String skillId) {
        return skillsDir().resolve(skillId + ".md");
    }

    public Path categoryMarkdownPath(String categoryId) {
        return categoriesDir().resolve(categoryId + ".md");
    }

    public Path skillsIndexPath() {
        return indexesDir().resolve("skills.md");
    }

    public Path categoriesIndexPath() {
        return indexesDir().resolve("categories.md");
    }
}

