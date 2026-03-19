package dev.vfyjxf.mcp.mixin.client;

import dev.vfyjxf.mcp.runtime.input.VirtualModifierQueries;
import dev.vfyjxf.mcp.runtime.input.VirtualModifierState;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
final class ScreenModifierMixin {

    // Merge virtual modifier holds at return so physical keyboard state remains authoritative input.
    @Inject(method = "hasShiftDown", at = @At("RETURN"), cancellable = true)
    private static void moddevmcp$mergeVirtualShiftState(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(VirtualModifierQueries.merge(
                cir.getReturnValueZ(),
                VirtualModifierState.global().shiftActive()
        ));
    }

    @Inject(method = "hasControlDown", at = @At("RETURN"), cancellable = true)
    private static void moddevmcp$mergeVirtualControlState(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(VirtualModifierQueries.merge(
                cir.getReturnValueZ(),
                VirtualModifierState.global().controlActive()
        ));
    }

    @Inject(method = "hasAltDown", at = @At("RETURN"), cancellable = true)
    private static void moddevmcp$mergeVirtualAltState(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(VirtualModifierQueries.merge(
                cir.getReturnValueZ(),
                VirtualModifierState.global().altActive()
        ));
    }
}
