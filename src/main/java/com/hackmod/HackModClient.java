package com.hackmod;

import com.hackmod.client.render.*;
import com.hackmod.common.config.ModConfig;
import com.hackmod.common.seed.WorldSeedManager;
import com.hackmod.server.BrowserLauncher;
import com.hackmod.server.HackWebServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HackModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hackmod");
    private static final HackWebServer SERVER = new HackWebServer();

    @Override
    public void onInitializeClient() {
        LOGGER.info("[HackMod] Starting v3.0...");

        // Embedded HTTP + WebSocket
        SERVER.start();
        BrowserLauncher.launch();

        // World ESP
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ChestEspRenderer::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(PlayerEspRenderer::render);

        // HUD
        HudRenderCallback.EVENT.register(new HudOverlayRenderer());
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null) HudOverlayRenderer.renderArmorHud(ctx, mc);
        });

        // Per-tick: game features + seed scanner
        FeatureTickHandler.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModConfig.seedTracker.get()) WorldSeedManager.tick();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(SERVER::stop, "hackmod-shutdown"));
        LOGGER.info("[HackMod] Ready — http://localhost:{}", HackWebServer.PORT);
    }
}
