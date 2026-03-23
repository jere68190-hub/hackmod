package com.hackmod.client.render;

import com.hackmod.common.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class FeatureTickHandler {

    private static double originalGamma = -1.0;
    public static KeyBinding zoomKey;

    public static void register() {
        // Zoom key (hold C)
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hackmod.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.hackmod"
        ));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            handleAutoSprint(client.player);
            handleNoFall(client.player);
            handleFullbright(client);
            handleZoom(client);
        });
    }

    private static void handleAutoSprint(PlayerEntity p) {
        if (ModConfig.autoSprint.get() && p.forwardSpeed > 0) p.setSprinting(true);
    }

    private static void handleNoFall(PlayerEntity p) {
        if (ModConfig.noFall.get()) p.fallDistance = 0f;
    }

    private static void handleFullbright(MinecraftClient mc) {
        if (ModConfig.fullbright.get()) {
            if (originalGamma < 0) originalGamma = mc.options.getGamma().getValue();
            if (mc.options.getGamma().getValue() < 15.0) mc.options.getGamma().setValue(15.0);
        } else if (originalGamma >= 0) {
            mc.options.getGamma().setValue(originalGamma);
            originalGamma = -1.0;
        }
    }

    private static void handleZoom(MinecraftClient mc) {
        if (!ModConfig.zoomEnabled.get()) {
            ModConfig.zoomActive = false;
            return;
        }
        boolean held = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_C);
        ModConfig.zoomActive = held;
    }
}
