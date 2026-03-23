package com.hackmod.mixin;

import com.hackmod.common.config.ModConfig;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    /**
     * Reduce mouse sensitivity while zoom is active so camera movement feels smooth.
     */
    @ModifyVariable(method = "updateMouse", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double hackmod_zoomSensitivity(double sensitivity) {
        if (ModConfig.zoomEnabled.get() && ModConfig.zoomActive) {
            return sensitivity * 0.15;
        }
        return sensitivity;
    }
}
