package com.hackmod.client.render;

import com.hackmod.common.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class PlayerEspRenderer {

    public static void render(WorldRenderContext ctx) {
        if (!ModConfig.playerEsp.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Vec3d cam = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();
        int color = ModConfig.playerEspColor;
        float r=((color>>16)&0xFF)/255f, g=((color>>8)&0xFF)/255f, b=(color&0xFF)/255f;
        float pt = ctx.tickCounter().getTickDelta(true);

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        Tessellator tess = Tessellator.getInstance();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double px=player.prevX+(player.getX()-player.prevX)*pt-cam.x;
            double py=player.prevY+(player.getY()-player.prevY)*pt-cam.y;
            double pz=player.prevZ+(player.getZ()-player.prevZ)*pt-cam.z;
            float w=player.getWidth()/2f+0.05f, h=player.getHeight()+0.1f;
            Box box = new Box(px-w,py,pz-w,px+w,py+h,pz+w);
            ms.push();
            Matrix4f m = ms.peek().getPositionMatrix();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            ChestEspRenderer.fillBox(buf,m,box,r,g,b,0.08f);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1.6f);
            buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            ChestEspRenderer.wireBox(buf,m,box,r,g,b,0.85f);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            ms.pop();
        }
        RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        RenderSystem.disableBlend(); RenderSystem.lineWidth(1.0f);
    }
}
