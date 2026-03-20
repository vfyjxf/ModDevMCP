package dev.vfyjxf.testmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.runtime.input.MinecraftInputController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Client-only smoke test that validates virtual modifiers reach NeoForge key modifiers
 * and `KeyMapping` matching in a real launched client.
 *
 * <p>The hook is gated behind an environment variable so normal `runClient` sessions do
 * not auto-exit. When enabled, the test runs once on the title screen, writes a result
 * file under `build/smoke-tests`, and then terminates the client.</p>
 */
public final class ClientSmokeTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ENABLED_ENV = "MODDEVMCP_TESTMOD_AUTO_SMOKETEST";
    private static final String RESULT_FILE = "build/smoke-tests/virtual-modifier-keymapping.txt";
    private static final InputConstants.Key TEST_PRIMARY_KEY = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_P);
    private static boolean attached;
    private static boolean completed;

    private ClientSmokeTest() {
    }

    /**
     * Attaches the runtime tick hook when the smoke test is explicitly enabled.
     */
    public static void attach() {
        if (!enabled() || attached) {
            return;
        }
        attached = true;
        NeoForge.EVENT_BUS.addListener(ClientSmokeTest::onClientTickPost);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (!enabled()) {
            return;
        }
        event.register(smokeTestKeyMapping());
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        if (!enabled() || completed) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            return;
        }
        completed = true;
        try {
            var report = runSmokeTest();
            writeResult("SUCCESS\n" + report);
            LOGGER.info("TestMod virtual modifier smoke test passed");
            minecraft.stop();
        } catch (Throwable throwable) {
            var stackTrace = stackTrace(throwable);
            writeResult("FAILURE\n" + stackTrace);
            LOGGER.error("TestMod virtual modifier smoke test failed", throwable);
            minecraft.stop();
            System.exit(1);
        }
    }

    private static String runSmokeTest() {
        var controller = new MinecraftInputController();
        var keyMapping = smokeTestKeyMapping();
        var conflictContext = KeyConflictContext.UNIVERSAL;

        releaseModifier(controller, GLFW.GLFW_KEY_LEFT_SHIFT);

        expect(!KeyModifier.SHIFT.isActive(conflictContext), "SHIFT should start inactive");
        expect(!keyMapping.isActiveAndMatches(TEST_PRIMARY_KEY), "SHIFT+P mapping should start inactive");

        pressModifier(controller, GLFW.GLFW_KEY_LEFT_SHIFT);

        expect(KeyModifier.SHIFT.isActive(conflictContext), "virtual SHIFT should activate KeyModifier");
        expect(keyMapping.isActiveAndMatches(TEST_PRIMARY_KEY), "virtual SHIFT should activate SHIFT+P mapping");

        releaseModifier(controller, GLFW.GLFW_KEY_LEFT_SHIFT);

        expect(!KeyModifier.SHIFT.isActive(conflictContext), "SHIFT should be inactive after release");
        expect(!keyMapping.isActiveAndMatches(TEST_PRIMARY_KEY), "SHIFT+P mapping should be inactive after release");

        return String.join(System.lineSeparator(),
                "modifierActiveDuringHold=true",
                "mappingActiveDuringHold=true",
                "modifierReleased=true"
        ) + System.lineSeparator();
    }

    private static void pressModifier(MinecraftInputController controller, int keyCode) {
        var result = controller.perform("key_down", Map.of("keyCode", keyCode));
        assertSuccess(result, "pressing modifier " + keyCode);
    }

    private static void releaseModifier(MinecraftInputController controller, int keyCode) {
        var result = controller.perform("key_up", Map.of("keyCode", keyCode));
        assertSuccess(result, "releasing modifier " + keyCode);
    }

    private static void assertSuccess(OperationResult<Void> result, String action) {
        expect(result.accepted() && result.performed(), action + " should succeed but was: " + result.reason());
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void writeResult(String content) {
        try {
            var resultPath = resultPath();
            Files.createDirectories(resultPath.getParent());
            Files.writeString(resultPath, content);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write smoke-test result", exception);
        }
    }

    private static Path resultPath() {
        return Path.of(System.getProperty("moddevmcp.project.root", ".")).resolve(RESULT_FILE);
    }

    private static String stackTrace(Throwable throwable) {
        var buffer = new StringWriter();
        try (var writer = new PrintWriter(buffer)) {
            throwable.printStackTrace(writer);
        }
        return buffer.toString();
    }

    private static boolean enabled() {
        return Boolean.parseBoolean(System.getenv(ENABLED_ENV));
    }

    /**
     * Uses a lazy holder so the keybinding is only created in explicit smoke-test runs.
     */
    private static KeyMapping smokeTestKeyMapping() {
        return SmokeTestKeyMappingHolder.INSTANCE;
    }

    private static final class SmokeTestKeyMappingHolder {

        private static final KeyMapping INSTANCE = new KeyMapping(
                "key.test_mod.virtual_modifier_smoke",
                KeyConflictContext.UNIVERSAL,
                KeyModifier.SHIFT,
                TEST_PRIMARY_KEY,
                KeyMapping.CATEGORY_MISC
        );

        private SmokeTestKeyMappingHolder() {
        }
    }
}
