package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.mcp.api.runtime.ClientScreenProbe;
import net.minecraft.client.Minecraft;

public final class LiveClientScreenProbe implements ClientScreenProbe {

    @Override
    public ClientScreenMetrics metrics() {
        var minecraft = Minecraft.getInstance();
        return new ClientScreenMetrics(
                minecraft.screen == null ? null : minecraft.screen.getClass().getName(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight()
        );
    }
}
