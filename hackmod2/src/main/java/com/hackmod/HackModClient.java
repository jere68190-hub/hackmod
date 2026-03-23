package com.hackmod;

import com.hackmod.client.render.*;
import com.hackmod.server.BrowserLauncher;
import com.hackmod.server.HackWebServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HackModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hackmod");
    private static final HackWebServer SERVER = new HackWebServer();

    @Override
    public void onInitializeClient() {
        LOGGER.info("[HackMod] Starting v2.0...");

        SERVER.start();
        BrowserLauncher.launch();

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ChestEspRenderer::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(PlayerEspRenderer::render);

        HudRenderCallback.EVENT.register(new HudOverlayRenderer());
        HudRenderCallback.EVENT.register((ctx, tc) -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null) HudOverlayRenderer.renderArmorHud(ctx, mc);
        });

        FeatureTickHandler.register();

        Runtime.getRuntime().addShutdownHook(new Thread(SERVER::stop, "hackmod-shutdown"));
        LOGGER.info("[HackMod] Ready — http://localhost:{}", HackWebServer.PORT);
    }
}
