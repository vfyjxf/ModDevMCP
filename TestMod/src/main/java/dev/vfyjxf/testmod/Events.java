package dev.vfyjxf.testmod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TestModEntrypoint.MOD_ID)
public class Events {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("test_cmd")
                .executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal(testMethod(1, 2) + ""), true);
            return 0;
        }));
    }

    public static int testMethod(int a, int b) {
        return a + b + 1;
    }
}
