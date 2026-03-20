package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiLocator;
import dev.vfyjxf.moddev.api.runtime.UiResolveRequest;
import dev.vfyjxf.moddev.api.runtime.UiTargetReference;
import dev.vfyjxf.moddev.api.ui.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VanillaScreenUiDriverTest {

    @Test
    void snapshotIncludesExtractedButtonTargetsAndMarksHoveredTargetFromPointer() {
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(
                        buttonTarget(context, "button-singleplayer", "Singleplayer", 113, 93, 200, 20),
                        buttonTarget(context, "button-multiplayer", "Multiplayer", 113, 117, 200, 20)
                )
        );

        var snapshot = driver.snapshot(new TestUiContext(
                "net.minecraft.client.gui.screens.TitleScreen",
                "minecraft",
                150,
                120
        ), SnapshotOptions.DEFAULT);

        assertEquals("button-multiplayer", snapshot.hoveredTargetId());
        assertEquals(3, snapshot.targets().size());
        var hovered = snapshot.targets().stream()
                .filter(target -> target.targetId().equals("button-multiplayer"))
                .findFirst()
                .orElseThrow();
        assertTrue(hovered.state().hovered());
        assertFalse(hovered.state().focused());
    }

    @Test
    void queryMatchesExtractedButtonTargetsByIdAndText() {
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(
                        buttonTarget(context, "button-singleplayer", "Singleplayer", 113, 93, 200, 20),
                        buttonTarget(context, "button-multiplayer", "Multiplayer", 113, 117, 200, 20)
                )
        );
        var context = new TestUiContext(
                "net.minecraft.client.gui.screens.TitleScreen",
                "minecraft",
                0,
                0
        );

        var byId = driver.query(context, TargetSelector.builder().id("button-singleplayer").build());
        var byText = driver.query(context, TargetSelector.builder().text("Multiplayer").build());

        assertEquals(1, byId.size());
        assertEquals("Singleplayer", byId.getFirst().text());
        assertEquals(1, byText.size());
        assertEquals("button-multiplayer", byText.getFirst().targetId());
    }

    @Test
    void interactionStateReturnsHoveredExtractedButtonAndPointerCoordinates() {
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(
                        buttonTarget(context, "button-singleplayer", "Singleplayer", 113, 93, 200, 20),
                        buttonTarget(context, "button-multiplayer", "Multiplayer", 113, 117, 200, 20)
                )
        );

        var state = driver.interactionState(new TestUiContext(
                "net.minecraft.client.gui.screens.TitleScreen",
                "minecraft",
                150,
                120
        ));

        assertEquals(150, state.cursorX());
        assertEquals(120, state.cursorY());
        assertNotNull(state.hoveredTarget());
        assertEquals("button-multiplayer", state.hoveredTarget().targetId());
        assertEquals("Multiplayer", state.hoveredTarget().text());
    }

    @Test
    void clickActionDelegatesToExecutorUsingTargetCenter() {
        var executor = new RecordingUiActionExecutor();
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(buttonTarget(context, "button-singleplayer", "Singleplayer", 113, 93, 200, 20)),
                executor
        );
        var context = new TestUiContext(
                "net.minecraft.client.gui.screens.TitleScreen",
                "minecraft",
                0,
                0
        );

        var result = driver.action(context, new UiActionRequest(
                TargetSelector.builder().id("button-singleplayer").build(),
                "click",
                Map.of()
        ));

        assertTrue(result.accepted());
        assertEquals(1, executor.invocationCount.get());
        assertEquals("click", executor.lastAction.get());
        assertEquals(213, executor.lastX.get());
        assertEquals(103, executor.lastY.get());
    }

    @Test
    void hoverActionDelegatesToExecutorWithHoverDelay() {
        var executor = new RecordingUiActionExecutor();
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(buttonTarget(context, "button-multiplayer", "Multiplayer", 113, 117, 200, 20)),
                executor
        );
        var context = new TestUiContext(
                "net.minecraft.client.gui.screens.TitleScreen",
                "minecraft",
                0,
                0
        );

        var result = driver.action(context, new UiActionRequest(
                TargetSelector.builder().id("button-multiplayer").build(),
                "hover",
                Map.of("hoverDelayMs", 250)
        ));

        assertTrue(result.accepted());
        assertEquals(1, executor.invocationCount.get());
        assertEquals("hover", executor.lastAction.get());
        assertEquals(213, executor.lastX.get());
        assertEquals(127, executor.lastY.get());
        assertEquals(250, executor.lastHoverDelayMs.get());
    }

    @Test
    void resolveSupportsContainsTextWithIndexOverWidgetTargets() {
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(
                        buttonTarget(context, "button-create-world", "Create New World", 113, 93, 200, 20),
                        buttonTarget(context, "button-create-experimental-world", "Create Experimental World", 113, 117, 200, 20),
                        buttonTarget(context, "button-cancel", "Cancel", 113, 141, 200, 20)
                )
        );

        var result = driver.resolve(new TestUiContext(
                "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
                "minecraft",
                0,
                0
        ), new UiResolveRequest(
                UiTargetReference.locator(new UiLocator("button", null, "Create", null, 1, null)),
                false,
                false,
                false
        ));

        assertEquals("resolved", result.status());
        assertEquals("button-create-experimental-world", result.primary().targetId());
    }

    @Test
    void inspectSummaryExcludesScreenRootNoise() {
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of(
                        buttonTarget(context, "button-singleplayer", "Singleplayer", 113, 93, 200, 20),
                        buttonTarget(context, "button-multiplayer", "Multiplayer", 113, 117, 200, 20)
                )
        );

        var result = driver.inspect(new TestUiContext(
                "net.minecraft.client.gui.screens.TitleScreen",
                "minecraft",
                150,
                120
        ), SnapshotOptions.DEFAULT);

        assertEquals(2, result.summary().get("targetCount"));
        assertEquals(2, result.summary().get("actionableCount"));
        assertEquals("button-multiplayer", result.interaction().get("hoveredTargetId"));
    }


    @Test
    void matchesCustomModdedScreensButNotUnknownFallbackOrInventory() {
        var driver = new VanillaScreenUiDriver(
                new UiSessionStateRegistry(),
                BuiltinUiInteractionResolvers.newRegistry(),
                context -> List.of()
        );

        assertTrue(driver.matches(new TestUiContext(
                "dev.vfyjxf.testmod.client.TestModDebugScreen",
                "minecraft",
                0,
                0
        )));
        assertFalse(driver.matches(new TestUiContext(
                "custom.UnknownScreen",
                "minecraft",
                0,
                0
        )));
        assertFalse(driver.matches(new TestUiContext(
                "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                "minecraft",
                0,
                0
        )));
    }
    private UiTarget buttonTarget(UiContext context, String targetId, String text, int x, int y, int width, int height) {
        return new UiTarget(
                targetId,
                "vanilla-screen",
                context.screenClass(),
                context.modId(),
                "button",
                text,
                new Bounds(x, y, width, height),
                UiTargetState.defaultState(),
                List.of("click", "hover"),
                Map.of()
        );
    }

    private record TestUiContext(
            String screenClass,
            String modId,
            int mouseX,
            int mouseY
    ) implements UiContext {
    }

    private static final class RecordingUiActionExecutor implements VanillaScreenUiDriver.UiActionExecutor {

        private final AtomicInteger invocationCount = new AtomicInteger();
        private final AtomicReference<String> lastAction = new AtomicReference<>(null);
        private final AtomicInteger lastX = new AtomicInteger();
        private final AtomicInteger lastY = new AtomicInteger();
        private final AtomicInteger lastHoverDelayMs = new AtomicInteger();

        @Override
        public dev.vfyjxf.moddev.api.model.OperationResult<Map<String, Object>> execute(UiContext context, UiTarget target, UiActionRequest request) {
            invocationCount.incrementAndGet();
            lastAction.set(request.action());
            lastX.set(target.bounds().x() + (target.bounds().width() / 2));
            lastY.set(target.bounds().y() + (target.bounds().height() / 2));
            lastHoverDelayMs.set(((Number) request.arguments().getOrDefault("hoverDelayMs", 0)).intValue());
            return dev.vfyjxf.moddev.api.model.OperationResult.success(Map.of(
                    "driverId", "vanilla-screen",
                    "action", request.action(),
                    "performed", true
            ));
        }
    }
}


