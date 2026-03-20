package dev.vfyjxf.moddev.runtime.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

final class ModifiedKeybindingDispatch {

    private final Supplier<List<Binding>> bindings;
    private final Supplier<Integer> persistentModifierBits;

    ModifiedKeybindingDispatch(Supplier<List<Binding>> bindings) {
        this(bindings, () -> 0);
    }

    ModifiedKeybindingDispatch(Supplier<List<Binding>> bindings, Supplier<Integer> persistentModifierBits) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.persistentModifierBits = Objects.requireNonNull(persistentModifierBits, "persistentModifierBits");
    }

    static ModifiedKeybindingDispatch live() {
        return live(() -> 0);
    }

    static ModifiedKeybindingDispatch live(Supplier<Integer> persistentModifierBits) {
        return new ModifiedKeybindingDispatch(
                () -> allKeyMappings().stream().map(LiveBinding::new).map(Binding.class::cast).toList(),
                persistentModifierBits
        );
    }

    boolean dispatch(int keyCode, int modifiers, Runnable dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        // Route matching through merged modifiers so persistent held virtual modifiers
        // can satisfy modified keybindings even when a command carries no one-shot bits.
        var effectiveModifiers = modifiers | persistentModifierBits.get();
        if (effectiveModifiers == 0 || isModifierKey(keyCode)) {
            return false;
        }
        var matchingBindings = bindings.get().stream()
                .filter(binding -> binding.matches(keyCode, effectiveModifiers))
                .toList();
        if (matchingBindings.isEmpty()) {
            return false;
        }
        runWithNeutralizedModifiers(matchingBindings, 0, dispatcher);
        return true;
    }

    private void runWithNeutralizedModifiers(List<Binding> matchingBindings, int index, Runnable dispatcher) {
        if (index >= matchingBindings.size()) {
            dispatcher.run();
            return;
        }
        matchingBindings.get(index).runWithNeutralModifier(() ->
                runWithNeutralizedModifiers(matchingBindings, index + 1, dispatcher)
        );
    }

    @SuppressWarnings("unchecked")
    private static Collection<KeyMapping> allKeyMappings() {
        try {
            Field field = findAllKeyMappingsField();
            return ((Map<String, KeyMapping>) field.get(null)).values();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to read KeyMapping.ALL", exception);
        }
    }

    private static Field findAllKeyMappingsField() {
        try {
            var field = KeyMapping.class.getDeclaredField("ALL");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to locate KeyMapping.ALL", exception);
        }
    }

    private static boolean isModifierKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
                    GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
                    GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
                    GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    interface Binding {

        boolean matches(int keyCode, int modifiers);

        void runWithNeutralModifier(Runnable runnable);
    }

    private static final class LiveBinding implements Binding {

        private final KeyMapping keyMapping;

        private LiveBinding(KeyMapping keyMapping) {
            this.keyMapping = keyMapping;
        }

        @Override
        public boolean matches(int keyCode, int modifiers) {
            if (!keyMapping.getKeyConflictContext().isActive()) {
                return false;
            }
            var key = keyMapping.getKey();
            if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() != keyCode) {
                return false;
            }
            var keyModifier = keyMapping.getKeyModifier();
            if (keyModifier == KeyModifier.NONE) {
                return false;
            }
            return switch (keyModifier) {
                case CONTROL -> (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
                case SHIFT -> (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
                case ALT -> (modifiers & GLFW.GLFW_MOD_ALT) != 0;
                case NONE -> false;
            };
        }

        @Override
        public void runWithNeutralModifier(Runnable runnable) {
            var originalKey = keyMapping.getKey();
            var originalModifier = keyMapping.getKeyModifier();
            keyMapping.setKeyModifierAndCode(KeyModifier.NONE, originalKey);
            try {
                runnable.run();
            } finally {
                keyMapping.setKeyModifierAndCode(originalModifier, originalKey);
            }
        }
    }
}

