package com.hackmod.common.seed;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * WorldSeedManager
 * ─────────────────
 * Reads the current world seed (solo only — servers don't send it to clients)
 * and uses Minecraft's own structure locator to find nearby structures.
 *
 * This is identical in approach to Amidst / Chunkbase:
 * pure maths on the seed, no server communication.
 */
public class WorldSeedManager {

    public static long cachedSeed    = Long.MIN_VALUE;
    public static boolean seedKnown  = false;

    // List of found structures, refreshed on demand
    public static final List<StructureHit> nearbyStructures = new ArrayList<>();
    public static long lastScanTime = 0;
    public static final long SCAN_INTERVAL_MS = 5000; // rescan every 5 seconds

    // ── Seed reading ──────────────────────────────────────────────────────────

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Read seed — works in singleplayer and on servers that expose it
        long seed = mc.world.getSeed();
        if (seed != 0) {
            cachedSeed = seed;
            seedKnown  = true;
        }

        // Periodic structure scan
        long now = System.currentTimeMillis();
        if (now - lastScanTime > SCAN_INTERVAL_MS) {
            lastScanTime = now;
            scanStructures(mc);
        }
    }

    // ── Structure scanning ────────────────────────────────────────────────────

    private static void scanStructures(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        nearbyStructures.clear();

        BlockPos playerPos = mc.player.getBlockPos();

        // Use Minecraft's own StructureLocator through the server chunk manager
        // This works in singleplayer perfectly; on servers it depends on loaded chunks
        var chunkManager = mc.world.getChunkManager();

        // List of structure tags to search for
        List<net.minecraft.registry.tag.TagKey<Structure>> tags = List.of(
            net.minecraft.registry.tag.StructureTags.VILLAGE,
            net.minecraft.registry.tag.StructureTags.DESERT_PYRAMID,
            net.minecraft.registry.tag.StructureTags.JUNGLE_TEMPLE,
            net.minecraft.registry.tag.StructureTags.IGLOO,
            net.minecraft.registry.tag.StructureTags.SWAMP_HUT,
            net.minecraft.registry.tag.StructureTags.SHIPWRECK,
            net.minecraft.registry.tag.StructureTags.OCEAN_RUIN,
            net.minecraft.registry.tag.StructureTags.STRONGHOLD,
            net.minecraft.registry.tag.StructureTags.MINESHAFT,
            net.minecraft.registry.tag.StructureTags.RUINED_PORTAL,
            net.minecraft.registry.tag.StructureTags.NETHER_FOSSIL,
            net.minecraft.registry.tag.StructureTags.BASTION_REMNANT,
            net.minecraft.registry.tag.StructureTags.FORTRESS
        );

        String[] names = {
            "Village", "Desert Temple", "Jungle Temple", "Igloo",
            "Witch Hut", "Shipwreck", "Ocean Ruins", "Stronghold",
            "Mineshaft", "Ruined Portal", "Nether Fossil",
            "Bastion Remnant", "Nether Fortress"
        };

        for (int i = 0; i < tags.size(); i++) {
            try {
                BlockPos found = chunkManager.getStructureAccessor()
                    .findNearestStructurePosition(
                        mc.world.getRegistryManager(),
                        chunkManager.getNoiseConfig(),
                        chunkManager.getStructurePlacementCalculator(),
                        tags.get(i),
                        playerPos,
                        100,   // search radius in chunks
                        false  // skipExistingChunks=false → include unloaded chunks
                    );

                if (found != null) {
                    int dist = (int) Math.sqrt(playerPos.getSquaredDistance(found));
                    nearbyStructures.add(new StructureHit(names[i], found, dist));
                }
            } catch (Exception ignored) {
                // Some structures may not exist in current dimension — skip silently
            }
        }

        // Sort by distance
        nearbyStructures.sort((a, b) -> Integer.compare(a.distanceBlocks, b.distanceBlocks));
    }

    // ── StructureHit record ───────────────────────────────────────────────────

    public record StructureHit(String name, BlockPos pos, int distanceBlocks) {
        public String toJson() {
            return String.format(
                "{\"name\":\"%s\",\"x\":%d,\"y\":%d,\"z\":%d,\"dist\":%d}",
                name, pos.getX(), pos.getY(), pos.getZ(), distanceBlocks
            );
        }

        /** Compass direction from origin to this structure */
        public String direction(BlockPos origin) {
            int dx = pos.getX() - origin.getX();
            int dz = pos.getZ() - origin.getZ();
            double angle = Math.toDegrees(Math.atan2(dz, dx));
            if (angle < 0) angle += 360;
            // Convert to N/NE/E/SE/S/SW/W/NW
            String[] dirs = {"E","NE","N","NW","W","SW","S","SE"};
            int idx = (int)((angle + 22.5) / 45) % 8;
            return dirs[idx];
        }
    }
}
