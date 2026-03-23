package com.hackmod.mixin;

import com.hackmod.common.config.ModConfig;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Intercepts the FOV calculation.
     * - If zoom is active (key held): returns a narrow 10° FOV for sniper-style zoom.
     * - If custom FOV is enabled: returns the user-defined FOV value.
     */
    @ModifyVariable(method = "getFov", at = @At("RETURN"), ordinal = 0)
    private float hackmod_modifyFov(float original) {
        if (ModConfig.zoomEnabled.get() && ModConfig.zoomActive) {
            return 10f;
        }
        if (ModConfig.customFovEnabled) {
            return (float) ModConfig.customFov;
        }
        return original;
    }
}
