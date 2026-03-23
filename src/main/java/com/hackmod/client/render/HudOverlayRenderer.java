package com.hackmod.client.render;

import com.hackmod.common.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HudOverlayRenderer implements HudRenderCallback {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean armorAlertVisible = false;

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        boolean anyHud = ModConfig.coordsHud.get() || ModConfig.biomeHud.get() || ModConfig.clockHud.get();
        if (anyHud) drawInfoPanel(ctx, mc);
        if (ModConfig.armorAlert.get()) checkArmorAlert(ctx, mc);
    }

    private void drawInfoPanel(DrawContext ctx, MinecraftClient mc) {
        PlayerEntity p = mc.player;
        List<String> lines = new ArrayList<>();

        if (ModConfig.coordsHud.get()) {
            lines.add("§2// XYZ");
            lines.add(String.format("§aX §f%.1f  §aY §f%.1f  §aZ §f%.1f", p.getX(), p.getY(), p.getZ()));
            lines.add("§a➤ §f" + p.getHorizontalFacing().getName().toUpperCase());
        }

        if (ModConfig.biomeHud.get()) {
            String biome = "unknown";
            try {
                var entry = mc.world.getBiome(p.getBlockPos());
                var key = entry.getKey();
                if (key.isPresent()) {
                    biome = key.get().getValue().getPath().replace("_", " ").toUpperCase();
                }
            } catch (Exception ignored) {}
            lines.add("§aBIOME §f" + biome);
        }

        if (ModConfig.clockHud.get()) {
            long worldTime = mc.world.getTimeOfDay() % 24000;
            int hours   = (int)((worldTime / 1000 + 6) % 24);
            int minutes = (int)((worldTime % 1000) * 60 / 1000);
            String realTime = LocalTime.now().format(TIME_FMT);
            lines.add(String.format("§aMC §f%02d:%02d  §aIRL §f%s", hours, minutes, realTime));
        }

        if (lines.isEmpty()) return;
        int pw = 220, ph = lines.size() * 10 + 8;
        ctx.fill(4, 4, 4+pw, 4+ph, 0xCC050F0A);
        ctx.fill(4, 4, 4+pw, 5, 0xFF00CC33);
        ctx.fill(4, 4, 5, 4+ph, 0xFF00CC33);
        for (int i = 0; i < lines.size(); i++)
            ctx.drawText(mc.textRenderer, lines.get(i), 8, 8 + i*10, 0xFFAAFFAA, false);
    }

    public static void renderArmorHud(DrawContext ctx, MinecraftClient mc) {
        if (!ModConfig.armorHud.get()) return;
        PlayerEntity p = mc.player;
        if (p == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = {"H","C","L","B"};
        int baseY = mc.getWindow().getScaledHeight() - 60;
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = p.getEquippedStack(slots[i]);
            if (stack.isEmpty()) continue;
            int maxDur = stack.getMaxDamage();
            if (maxDur == 0) continue;
            int dur = maxDur - stack.getDamage();
            float pct = (float) dur / maxDur;
            int color = pct > 0.5f ? 0xFF00FF41 : pct > 0.25f ? 0xFFFFCC00 : 0xFFFF3C00;
            ctx.drawText(mc.textRenderer, names[i]+":"+dur+"/"+maxDur, 6, baseY - i*11, color, true);
        }
    }

    private void checkArmorAlert(DrawContext ctx, MinecraftClient mc) {
        PlayerEntity p = mc.player;
        if (p == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        boolean low = false;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = p.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            int maxDur = stack.getMaxDamage();
            if (maxDur > 0 && (float)(maxDur - stack.getDamage()) / maxDur < 0.15f) { low = true; break; }
        }
        if (low) {
            armorAlertVisible = (System.currentTimeMillis() / 600) % 2 == 0;
            if (armorAlertVisible) {
                String msg = "⚠ ARMOR CRITICAL ⚠";
                int sw = mc.textRenderer.getWidth(msg);
                int sx = (mc.getWindow().getScaledWidth() - sw) / 2;
                int sy = mc.getWindow().getScaledHeight() / 2 + 30;
                ctx.fill(sx-4, sy-2, sx+sw+4, sy+11, 0xCC1A0000);
                ctx.drawText(mc.textRenderer, msg, sx, sy, 0xFFFF3C00, false);
            }
        }
    }
}
