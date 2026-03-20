package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaWidgetIntrospectionTest {

    @Test
    void extractTargetsTraversesNestedChildrenAndIncludesTabButtons() {
        var context = new TestUiContext(
                "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
                "minecraft",
                0,
                0
        );
        var screen = new FakeScreen(List.of(
                new FakeTabNavigationBar(List.of(
                        new FakeWidget("Game", 110, 0, 60, 24),
                        new FakeWidget("World", 172, 0, 60, 24),
                        new FakeWidget("More", 234, 0, 60, 24)
                )),
                new FakeWidget("Create New World", 92, 492, 150, 20)
        ));

        var targets = VanillaWidgetIntrospection.extractTargets(screen, context, "vanilla-screen");

        assertEquals(4, targets.size());
        assertTrue(targets.stream().anyMatch(target -> target.text().equals("Game")));
        assertTrue(targets.stream().anyMatch(target -> target.text().equals("World")));
        assertTrue(targets.stream().anyMatch(target -> target.text().equals("More")));
        assertTrue(targets.stream().anyMatch(target -> target.text().equals("Create New World")));
    }

    @Test
    void invokeButtonPressTraversesNestedChildren() {
        var tab = new PressableFakeWidget("World", 172, 0, 60, 24);
        var screen = new FakeScreen(List.of(
                new FakeTabNavigationBar(List.of(tab))
        ));

        var handled = VanillaWidgetPressSupport.invokeButtonPress(
                screen,
                VanillaWidgetIntrospection.extractTargets(
                        screen,
                        new TestUiContext("custom.Screen", "minecraft", 0, 0),
                        "vanilla-screen"
                ).getFirst()
        );

        assertTrue(handled);
        assertTrue(tab.pressed);
    }

    private record TestUiContext(
            String screenClass,
            String modId,
            int mouseX,
            int mouseY
    ) implements UiContext {
    }

    private record FakeText(String value) {
        public String getString() {
            return value;
        }
    }

    private static class FakeScreen {

        private final List<Object> children;

        private FakeScreen(List<Object> children) {
            this.children = children;
        }

        public List<Object> children() {
            return children;
        }
    }

    private static final class FakeTabNavigationBar extends FakeScreen {

        private FakeTabNavigationBar(List<Object> children) {
            super(children);
        }
    }

    private static class FakeWidget {

        public boolean visible = true;
        public boolean active = true;
        private final String message;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private FakeWidget(String message, int x, int y, int width, int height) {
            this.message = message;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public FakeText getMessage() {
            return new FakeText(message);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean isFocused() {
            return false;
        }
    }

    private static final class PressableFakeWidget extends FakeWidget {

        private boolean pressed;

        private PressableFakeWidget(String message, int x, int y, int width, int height) {
            super(message, x, y, width, height);
        }

        public void onPress() {
            this.pressed = true;
        }
    }
}

