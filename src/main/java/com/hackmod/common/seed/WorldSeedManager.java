package com.hackmod.common.seed;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * WorldSeedManager - reads world seed and scans for nearby structures
 * using Minecraft's built-in structure locator (same approach as Chunkbase/Amidst).
 * Compatible with Minecraft 1.21.4 client API.
 */
public class WorldSeedManager {

    public static long    cachedSeed  = Long.MIN_VALUE;
    public static boolean seedKnown   = false;
    public static final List<StructureHit> nearbyStructures = new ArrayList<>();
    public static long lastScanTime = 0;
    public static final long SCAN_INTERVAL_MS = 5000;

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Read seed via level properties (works in singleplayer)
        try {
            long seed = mc.world.getSeed();
            if (seed != 0) { cachedSeed = seed; seedKnown = true; }
        } catch (Exception ignored) {}

        long now = System.currentTimeMillis();
        if (now - lastScanTime > SCAN_INTERVAL_MS) {
            lastScanTime = now;
            scanStructures(mc);
        }
    }

    private static void scanStructures(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        nearbyStructures.clear();

        BlockPos playerPos = mc.player.getBlockPos();

        // Structure tag keys available in 1.21.4
        String[][] structureDefs = {
            {"minecraft:village",          "Village"},
            {"minecraft:desert_pyramid",   "Desert Temple"},
            {"minecraft:jungle_pyramid",   "Jungle Temple"},
            {"minecraft:igloo",            "Igloo"},
            {"minecraft:swamp_hut",        "Witch Hut"},
            {"minecraft:shipwreck",        "Shipwreck"},
            {"minecraft:ocean_ruin",       "Ocean Ruins"},
            {"minecraft:stronghold",       "Stronghold"},
            {"minecraft:mineshaft",        "Mineshaft"},
            {"minecraft:ruined_portal",    "Ruined Portal"},
            {"minecraft:bastion_remnant",  "Bastion Remnant"},
            {"minecraft:fortress",         "Nether Fortress"},
            {"minecraft:ancient_city",     "Ancient City"},
            {"minecraft:trail_ruins",      "Trail Ruins"},
        };

        for (String[] def : structureDefs) {
            try {
                var registryKey = net.minecraft.registry.RegistryKey.of(
                    net.minecraft.registry.RegistryKeys.STRUCTURE,
                    new net.minecraft.util.Identifier(def[0])
                );

                var structureRegistry = mc.world.getRegistryManager()
                    .get(net.minecraft.registry.RegistryKeys.STRUCTURE);

                var structure = structureRegistry.get(registryKey);
                if (structure == null) continue;

                BlockPos found = mc.world.getChunkManager()
                    .getStructureAccessor()
                    .findNearestStructurePosition(
                        mc.world.getRegistryManager(),
                        mc.world.getChunkManager().getNoiseConfig(),
                        mc.world.getChunkManager().getStructurePlacementCalculator(),
                        net.minecraft.registry.tag.TagKey.of(
                            net.minecraft.registry.RegistryKeys.STRUCTURE,
                            new net.minecraft.util.Identifier(def[0])
                        ),
                        playerPos,
                        100,
                        false
                    );

                if (found != null) {
                    int dist = (int) Math.sqrt(playerPos.getSquaredDistance(found));
                    nearbyStructures.add(new StructureHit(def[1], found, dist));
                }
            } catch (Exception ignored) {}
        }

        nearbyStructures.sort((a, b) -> Integer.compare(a.distanceBlocks(), b.distanceBlocks()));
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
