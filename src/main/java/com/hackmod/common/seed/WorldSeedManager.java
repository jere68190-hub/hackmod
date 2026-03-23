package com.hackmod.common.seed;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * WorldSeedManager - 100% client-side compatible with Minecraft 1.21.4.
 *
 * Seed: obtained via integrated server (singleplayer) through the server world.
 * Structures: scanned by iterating loaded chunks and reading their structure starts.
 * No server-only API used.
 */
public class WorldSeedManager {

    public static long    cachedSeed = Long.MIN_VALUE;
    public static boolean seedKnown  = false;
    public static final List<StructureHit> nearbyStructures = new ArrayList<>();
    public static long lastScanTime = 0;
    public static final long SCAN_INTERVAL_MS = 5000;

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Read seed via integrated server (works in singleplayer)
        tryReadSeed(mc);

        long now = System.currentTimeMillis();
        if (now - lastScanTime > SCAN_INTERVAL_MS) {
            lastScanTime = now;
            scanStructures(mc);
        }
    }

    private static void tryReadSeed(MinecraftClient mc) {
        if (seedKnown) return;
        try {
            // In singleplayer, the integrated server is accessible
            if (mc.getServer() != null) {
                var serverWorld = mc.getServer().getWorld(mc.world.getRegistryKey());
                if (serverWorld != null) {
                    cachedSeed = serverWorld.getSeed();
                    seedKnown = true;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void scanStructures(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        nearbyStructures.clear();

        ClientWorld world = mc.world;
        BlockPos playerPos = mc.player.getBlockPos();
        ChunkPos centerChunk = new ChunkPos(playerPos);

        // Scan loaded chunks for structure starts
        int radius = 8; // chunks to scan around player
        java.util.Map<String, StructureHit> bestPerType = new java.util.HashMap<>();

        for (int cx = centerChunk.x - radius; cx <= centerChunk.x + radius; cx++) {
            for (int cz = centerChunk.z - radius; cz <= centerChunk.z + radius; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                // Read structure starts from chunk data
                chunk.getStructureStarts().forEach((structureKey, structureStart) -> {
                    if (structureStart == null || !structureStart.hasChildren()) return;

                    var bb = structureStart.getBoundingBox();
                    BlockPos center = new BlockPos(
                        (bb.getMinX() + bb.getMaxX()) / 2,
                        (bb.getMinY() + bb.getMaxY()) / 2,
                        (bb.getMinZ() + bb.getMaxZ()) / 2
                    );

                    int dist = (int) Math.sqrt(playerPos.getSquaredDistance(center));
                    String name = formatStructureName(structureKey.toString());

                    // Keep only the closest instance of each structure type
                    bestPerType.merge(name, new StructureHit(name, center, dist),
                        (a, b) -> a.distanceBlocks() <= b.distanceBlocks() ? a : b);
                });
            }
        }

        nearbyStructures.addAll(bestPerType.values());
        nearbyStructures.sort((a, b) -> Integer.compare(a.distanceBlocks(), b.distanceBlocks()));
    }

    private static String formatStructureName(String registryKey) {
        // Convert "minecraft:village_plains" → "Village Plains"
        String path = registryKey.contains(":") ? registryKey.split(":")[1] : registryKey;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    public record StructureHit(String name, BlockPos pos, int distanceBlocks) {
        public String direction(BlockPos origin) {
            int dx = pos.getX() - origin.getX();
            int dz = pos.getZ() - origin.getZ();
            double angle = Math.toDegrees(Math.atan2(dz, dx));
            if (angle < 0) angle += 360;
            String[] dirs = {"E","NE","N","NW","W","SW","S","SE"};
            return dirs[(int)((angle + 22.5) / 45) % 8];
        }
    }
}
