package dev.vfyjxf.moddev.runtime.input;

/**
 * Utility helpers that merge physical modifier query results with virtual modifier state.
 */
public final class VirtualModifierQueries {

    private VirtualModifierQueries() {
    }

    /**
     * Returns true when either physical input or virtual state marks the modifier as active.
     */
    public static boolean merge(boolean originalActive, boolean virtualActive) {
        return originalActive || virtualActive;
    }

    /**
     * Merges control modifier activity and optional super-as-control platform semantics.
     */
    public static boolean controlActive(
            boolean originalActive,
            boolean virtualControlActive,
            boolean virtualSuperActive,
            boolean superActsAsControl
    ) {
        return originalActive
                || virtualControlActive
                || (superActsAsControl && virtualSuperActive);
    }
}

