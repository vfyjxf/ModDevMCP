package dev.vfyjxf.mcp.api.runtime;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Minimal runtime view of the currently active UI.
 *
 * <p>The context intentionally stays lightweight so drivers can be matched in unit tests,
 * metadata-only flows, and live client flows. When available, {@link #screenHandle()} exposes the
 * live screen object as a weakly typed handle.
 */
public interface UiContext {

    /**
     * Returns the runtime class name of the active or requested screen.
     */
    String screenClass();

    /**
     * Returns the live screen object when a real client screen is available, or {@code null}
     * otherwise.
     */
    @Nullable
    default Object screenHandle() {
        return null;
    }

    /**
     * Returns the logical mod namespace associated with the active screen.
     */
    default String modId() {
        return "minecraft";
    }

    /**
     * Returns the current pointer X coordinate in UI space when known.
     */
    default int mouseX() {
        return 0;
    }

    /**
     * Returns the current pointer Y coordinate in UI space when known.
     */
    default int mouseY() {
        return 0;
    }

    /**
     * Returns optional driver- or runtime-specific attributes associated with this context.
     */
    default Map<String, Object> attributes() {
        return Map.of();
    }
}
