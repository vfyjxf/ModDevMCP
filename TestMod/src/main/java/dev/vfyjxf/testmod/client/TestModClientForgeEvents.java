package dev.vfyjxf.testmod.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class TestModClientForgeEvents {

    private TestModClientForgeEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TestModClientForgeEvents::onClientTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        for (var binding : TestModClientBindings.bindings()) {
            var mapping = binding.mapping();
            while (mapping.consumeClick()) {
                binding.action().run();
            }
        }
    }
}