package dev.vfyjxf.mcp.service.skill;

public enum SkillKind {
    GUIDANCE("guidance"),
    ACTION("action"),
    HYBRID("hybrid");

    private final String value;

    SkillKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
