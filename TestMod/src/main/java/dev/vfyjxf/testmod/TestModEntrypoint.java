package dev.vfyjxf.testmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(TestModEntrypoint.MOD_ID)
public final class TestModEntrypoint {

    public static final String MOD_ID = "test_mod";

    public TestModEntrypoint(IEventBus modEventBus) {
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ClientSmokeTest::registerKeyMappings);
            ClientSmokeTest.attach();
        }
    }

}
