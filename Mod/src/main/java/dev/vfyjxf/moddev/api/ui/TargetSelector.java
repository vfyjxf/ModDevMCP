package dev.vfyjxf.moddev.api.ui;

import java.util.List;

/**
 * Declarative selector used to find matching targets inside a UI snapshot.
 */
public record TargetSelector(
        String scope,
        String screen,
        String modId,
        String text,
        String role,
        String id,
        Bounds bounds,
        Integer index,
        List<TargetSelector> exclude
) {
    public TargetSelector {
        exclude = exclude == null ? List.of() : List.copyOf(exclude);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for assembling a {@link TargetSelector} incrementally.
     */
    public static final class Builder {
        private String scope;
        private String screen;
        private String modId;
        private String text;
        private String role;
        private String id;
        private Bounds bounds;
        private Integer index;
        private List<TargetSelector> exclude = List.of();

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder screen(String screen) {
            this.screen = screen;
            return this;
        }

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder bounds(Bounds bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder index(Integer index) {
            this.index = index;
            return this;
        }

        public Builder exclude(List<TargetSelector> exclude) {
            this.exclude = exclude;
            return this;
        }

        public TargetSelector build() {
            return new TargetSelector(scope, screen, modId, text, role, id, bounds, index, exclude);
        }
    }
}

