package dev.vfyjxf.mcp.runtime.input;

import java.lang.reflect.Field;

/**
 * Small reflective helper that tolerates anonymous widget subclasses by walking the class
 * hierarchy until the requested field is found.
 */
final class ReflectiveFieldAccess {

    private ReflectiveFieldAccess() {
    }

    static boolean readBooleanField(Object target, String fieldName) {
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getBoolean(target);
            } catch (NoSuchFieldException ignored) {
                // Anonymous subclasses like ChatScreen$1 often inherit the stateful field from EditBox.
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to read " + fieldName + " on " + target.getClass().getName(), exception);
            }
        }
        throw new IllegalStateException("Failed to read " + fieldName + " on " + target.getClass().getName());
    }
}
