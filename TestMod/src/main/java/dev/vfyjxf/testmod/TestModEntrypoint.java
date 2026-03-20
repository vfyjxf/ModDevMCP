package dev.vfyjxf.testmod;

import dev.vfyjxf.testmod.client.TestModClientForgeEvents;
import dev.vfyjxf.testmod.client.TestModClientModEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(TestModEntrypoint.MOD_ID)
public final class TestModEntrypoint {

    public static final String MOD_ID = "test_mod";

    public TestModEntrypoint(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(TestModClientModEvents::registerKeyMappings);
            TestModClientForgeEvents.register();
        }
    }
}
