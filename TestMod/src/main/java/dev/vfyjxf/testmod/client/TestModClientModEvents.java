package dev.vfyjxf.testmod.client;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class TestModClientModEvents {

    private TestModClientModEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (var binding : TestModClientBindings.bindings()) {
            event.register(binding.mapping());
        }
    }
}
