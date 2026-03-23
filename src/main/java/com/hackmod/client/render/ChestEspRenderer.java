package com.hackmod.client.render;

import com.hackmod.common.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ChestEspRenderer {

    public static void render(WorldRenderContext ctx) {
        if (!ModConfig.chestEsp.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        int radiusBlocks = ModConfig.chestEspRadius * 16;
        BlockPos origin = mc.player.getBlockPos();
        List<BlockEntity> found = new ArrayList<>();
        for (BlockEntity be : mc.world.getBlockEntities()) {
            if (!(be instanceof ChestBlockEntity) && !(be instanceof TrappedChestBlockEntity) && !(be instanceof EnderChestBlockEntity)) continue;
            if (be.getPos().getManhattanDistance(origin) <= radiusBlocks) found.add(be);
        }
        if (found.isEmpty()) return;

        Vec3d cam = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();
        int color = ModConfig.chestEspColor;
        float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        Tessellator tess = Tessellator.getInstance();

        for (BlockEntity be : found) {
            boolean isEnder = be instanceof EnderChestBlockEntity;
            float cr = isEnder ? 0.6f : r, cg = isEnder ? 0f : g, cb = isEnder ? 1f : b;
            BlockPos pos = be.getPos();
            double dx = pos.getX() - cam.x, dy = pos.getY() - cam.y, dz = pos.getZ() - cam.z;
            Box box = new Box(dx+.05, dy+.05, dz+.05, dx+.95, dy+.95, dz+.95);
            ms.push();
            Matrix4f m = ms.peek().getPositionMatrix();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            fillBox(buf, m, box, cr, cg, cb, 0.10f);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.lineWidth(1.8f);
            buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            wireBox(buf, m, box, cr, cg, cb, 0.9f);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            ms.pop();
        }
        RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        RenderSystem.disableBlend(); RenderSystem.lineWidth(1.0f);
    }

    public static void fillBox(BufferBuilder b, Matrix4f m, Box bx, float r, float g, float bv, float a) {
        float x0=(float)bx.minX,y0=(float)bx.minY,z0=(float)bx.minZ,x1=(float)bx.maxX,y1=(float)bx.maxY,z1=(float)bx.maxZ;
        q(b,m,x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,r,g,bv,a); q(b,m,x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,r,g,bv,a);
        q(b,m,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0,r,g,bv,a); q(b,m,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,r,g,bv,a);
        q(b,m,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,r,g,bv,a); q(b,m,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1,r,g,bv,a);
    }

    public static void wireBox(BufferBuilder b, Matrix4f m, Box bx, float r, float g, float bv, float a) {
        float x0=(float)bx.minX,y0=(float)bx.minY,z0=(float)bx.minZ,x1=(float)bx.maxX,y1=(float)bx.maxY,z1=(float)bx.maxZ;
        ln(b,m,x0,y0,z0,x1,y0,z0,r,g,bv,a);ln(b,m,x1,y0,z0,x1,y0,z1,r,g,bv,a);ln(b,m,x1,y0,z1,x0,y0,z1,r,g,bv,a);ln(b,m,x0,y0,z1,x0,y0,z0,r,g,bv,a);
        ln(b,m,x0,y1,z0,x1,y1,z0,r,g,bv,a);ln(b,m,x1,y1,z0,x1,y1,z1,r,g,bv,a);ln(b,m,x1,y1,z1,x0,y1,z1,r,g,bv,a);ln(b,m,x0,y1,z1,x0,y1,z0,r,g,bv,a);
        ln(b,m,x0,y0,z0,x0,y1,z0,r,g,bv,a);ln(b,m,x1,y0,z0,x1,y1,z0,r,g,bv,a);ln(b,m,x1,y0,z1,x1,y1,z1,r,g,bv,a);ln(b,m,x0,y0,z1,x0,y1,z1,r,g,bv,a);
    }

    private static void q(BufferBuilder b,Matrix4f m,float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,float r,float g,float bv,float a){
        b.vertex(m,x0,y0,z0).color(r,g,bv,a);b.vertex(m,x1,y1,z1).color(r,g,bv,a);b.vertex(m,x2,y2,z2).color(r,g,bv,a);b.vertex(m,x3,y3,z3).color(r,g,bv,a);
    }
    private static void ln(BufferBuilder b,Matrix4f m,float x0,float y0,float z0,float x1,float y1,float z1,float r,float g,float bv,float a){
        b.vertex(m,x0,y0,z0).color(r,g,bv,a);b.vertex(m,x1,y1,z1).color(r,g,bv,a);
    }
}
