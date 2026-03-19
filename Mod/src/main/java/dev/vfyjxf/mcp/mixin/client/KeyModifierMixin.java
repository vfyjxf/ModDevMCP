package dev.vfyjxf.mcp.mixin.client;

import dev.vfyjxf.mcp.runtime.input.VirtualModifierQueries;
import dev.vfyjxf.mcp.runtime.input.VirtualModifierState;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "net.neoforged.neoforge.client.settings.KeyModifier$1",
        "net.neoforged.neoforge.client.settings.KeyModifier$2",
        "net.neoforged.neoforge.client.settings.KeyModifier$3",
        "net.neoforged.neoforge.client.settings.KeyModifier$4"
})
abstract class KeyModifierMixin {

    // KeyModifier is an enum with per-constant anonymous subclasses, so the mixin targets
    // those generated classes directly and merges virtual state after NeoForge computes activity.
    @Inject(method = "isActive", at = @At("RETURN"), cancellable = true)
    private void moddevmcp$mergeVirtualModifierState(
            IKeyConflictContext conflictContext,
            CallbackInfoReturnable<Boolean> cir
    ) {
        var modifier = (KeyModifier) (Object) this;
        var state = VirtualModifierState.global();
        var merged = switch (modifier) {
            case SHIFT -> VirtualModifierQueries.merge(cir.getReturnValueZ(), state.shiftActive());
            case CONTROL -> VirtualModifierQueries.controlActive(
                    cir.getReturnValueZ(),
                    state.controlActive(),
                    state.superActive(),
                    Minecraft.ON_OSX
            );
            case ALT -> VirtualModifierQueries.merge(cir.getReturnValueZ(), state.altActive());
            case NONE -> cir.getReturnValueZ();
        };
        cir.setReturnValue(merged);
    }
}
