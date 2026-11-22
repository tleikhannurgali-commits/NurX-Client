package com.nurx.client;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scans only server-provided, loaded block entities. No server packet tricks, no invisible chunks. */
public final class Expedition {
    public static final List<BlockPos> STORAGES = new ArrayList<>();
    public static final List<BlockPos> DUNGEONS = new ArrayList<>();
    public static final List<BlockPos> VAULTS = new ArrayList<>();
    private static final List<Spot> SPOTS = new ArrayList<>();
    private static String currentWorld = "";
    private static Spot lastAlert;
    private static long alertUntil;

    public record Spot(String world, int x, int y, int z, int containers, String type) { }
    private Expedition() { }

    public static void clearVisible() {
        STORAGES.clear(); DUNGEONS.clear(); VAULTS.clear();
    }

    public static void replaceSpots(List<Spot> stored) {
        SPOTS.clear();
        SPOTS.addAll(stored.subList(0, Math.min(96, stored.size())));
    }
    public static List<Spot> saved() { return Collections.unmodifiableList(SPOTS); }
    public static String currentWorld() { return currentWorld; }
    public static Spot alert() { return System.currentTimeMillis() < alertUntil ? lastAlert : null; }

    public static void markHere(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        updateWorld(mc);
        BlockPos p = mc.player.getBlockPos();
        addSpot(new Spot(currentWorld, p.getX(), p.getY(), p.getZ(), 0, "MANUAL"), true);
    }

    private static void updateWorld(MinecraftClient mc) {
        String server = mc.getCurrentServerEntry() == null ? "singleplayer" : mc.getCurrentServerEntry().address;
        currentWorld = server + "@" + mc.world.getRegistryKey().getValue();
    }

    private static void addSpot(Spot spot, boolean manual) {
        if (!manual) {
            for (Spot old : SPOTS) {
                if (!old.world().equals(spot.world()) || !old.type().equals(spot.type())) continue;
                long dx = (long) old.x() - spot.x(), dy = (long) old.y() - spot.y(), dz = (long) old.z() - spot.z();
                if (dx * dx + dy * dy + dz * dz < 24L * 24L) return;
            }
        }
        SPOTS.add(0, spot);
        if (SPOTS.size() > 96) SPOTS.remove(SPOTS.size() - 1);
        lastAlert = spot;
        alertUntil = System.currentTimeMillis() + 6000L;
        ConfigStore.save();
    }

    public static Spot nearest(MinecraftClient mc) {
        if (mc.player == null) return null;
        Spot closest = null;
        double best = Double.MAX_VALUE;
        for (Spot spot : SPOTS) {
            if (!spot.world().equals(currentWorld)) continue;
            double dx = spot.x() - mc.player.getX(), dz = spot.z() - mc.player.getZ();
            double d = dx * dx + dz * dz;
            if (d < best) { best = d; closest = spot; }
        }
        return closest;
    }

    private static boolean storage(Block block) {
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock
                || block instanceof EnderChestBlock || block instanceof HopperBlock
                || block instanceof DispenserBlock || block instanceof DropperBlock
                || block instanceof FurnaceBlock || block instanceof BrewingStandBlock;
    }

    public static void scan(MinecraftClient mc) {
        clearVisible();
        if (mc.world == null || mc.player == null) return;
        updateWorld(mc);
        boolean stash = NurXClient.mod("StashFinder").enabled;
        boolean storageEsp = NurXClient.mod("StorageESP").enabled;
        boolean dungeon = NurXClient.mod("DungeonESP").enabled;
        boolean hud = NurXClient.mod("ExpeditionHUD").enabled;
        if (!stash && !storageEsp && !dungeon && !hud) return;
        int radius = 16;
        if (storageEsp) radius = Math.max(radius, (int) NurXClient.mod("StorageESP").value("Radius"));
        if (stash) radius = Math.max(radius, (int) NurXClient.mod("StashFinder").value("Radius"));
        if (dungeon || hud) radius = Math.max(radius, (int) NurXClient.mod("DungeonESP").value("Radius"));
        radius = Math.min(radius, 96);  // scan bound; never enumerate unloaded chunks
        int px = mc.player.getBlockX(), py = mc.player.getBlockY(), pz = mc.player.getBlockZ();
        List<ClusterFinder.Point> potentialStashes = new ArrayList<>();
        for (int cx = (px - radius) >> 4; cx <= (px + radius) >> 4; cx++) {
            for (int cz = (pz - radius) >> 4; cz <= (pz + radius) >> 4; cz++) {
                if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                var chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (Math.abs(pos.getX() - px) > radius || Math.abs(pos.getZ() - pz) > radius) continue;
                    if (Math.abs(pos.getY() - py) > 128) continue;
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (storage(block)) {
                        if (STORAGES.size() < 450) STORAGES.add(pos.toImmutable());
                        // Hoppers, ender chests, furnaces etc are useful for ESP,
                        // but should not independently trigger an alleged base discovery.
                        if (stash && (block instanceof ChestBlock || block instanceof BarrelBlock
                                || block instanceof ShulkerBoxBlock)) {
                            if (potentialStashes.size() < 400)
                                potentialStashes.add(new ClusterFinder.Point(pos.getX(), pos.getY(), pos.getZ()));
                        }
                    }
                    if (block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER) {
                        if (DUNGEONS.size() < 160) DUNGEONS.add(pos.toImmutable());
                    }
                    if (block == Blocks.VAULT) {
                        if (VAULTS.size() < 160) VAULTS.add(pos.toImmutable());
                    }
                }
            }
        }
        if (stash && !potentialStashes.isEmpty()) {
            int minimum = (int) NurXClient.mod("StashFinder").value("Min containers");
            for (ClusterFinder.Cluster cluster : ClusterFinder.find(potentialStashes, 12, minimum)) {
                addSpot(new Spot(currentWorld, cluster.x(), cluster.y(), cluster.z(), cluster.size(), "STASH"), false);
            }
        }
    }
}
