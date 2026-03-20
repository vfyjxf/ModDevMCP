package dev.vfyjxf.testmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.testmod.TestModEntrypoint;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class TestModClientBindings {

    public static final String CATEGORY = "key.categories." + TestModEntrypoint.MOD_ID;

    private static final List<Binding> BINDINGS = new CopyOnWriteArrayList<>();

    static {
        register(
                new KeyMapping(
                        "key.test_mod.open_ctrl_y",
                        KeyConflictContext.IN_GAME,
                        KeyModifier.CONTROL,
                        InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Y),
                        CATEGORY
                ),
                TestModDebugScreen::new
        );
    }

    private TestModClientBindings() {
    }

    // External mods may register additional bindings during client init.
    public static void register(KeyMapping mapping, Supplier<? extends Screen> screenSupplier) {
        register(mapping, () -> {
            var minecraft = Minecraft.getInstance();
            minecraft.setScreen(screenSupplier.get());
        });
    }

    public static void register(KeyMapping mapping, Runnable action) {
        BINDINGS.add(new Binding(mapping, action));
    }

    public static List<Binding> bindings() {
        return List.copyOf(BINDINGS);
    }

    public record Binding(KeyMapping mapping, Runnable action) {
    }
}