package dev.vfyjxf.testmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TestModDebugScreen extends Screen {

    private static final Component TITLE = Component.literal("TestMod Ctrl+Y");

    public TestModDebugScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = (this.height / 2) + 20;
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(x, y, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, TITLE, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}