package com.nurx.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Client-side outlines, limited to entities and block entities the server has already sent. */
public final class WorldOverlay {
    private WorldOverlay() { }

    public static boolean rareLoot(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.DIAMOND || item == Items.NETHERITE_INGOT || item == Items.NETHERITE_SCRAP
                || item == Items.ELYTRA || item == Items.TOTEM_OF_UNDYING || item == Items.NETHER_STAR
                || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.TRIAL_KEY
                || item == Items.OMINOUS_TRIAL_KEY || item == Items.HEAVY_CORE || item == Items.MACE)
            return true;
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("ключ") || name.contains("талисман") || name.contains("сфера")
                || name.contains("key") || name.contains("mystic");
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || ctx.matrixStack() == null || ctx.consumers() == null) return;
        boolean players = NurXClient.mod("PlayerESP").enabled;
        boolean storage = NurXClient.mod("StorageESP").enabled;
        boolean dungeons = NurXClient.mod("DungeonESP").enabled;
        boolean loot = NurXClient.mod("LootESP").enabled;
        if (!players && !storage && !dungeons && !loot) return;

        MatrixStack matrices = ctx.matrixStack();
        Vec3d camera = ctx.camera().getPos();
        VertexConsumer lines = ctx.consumers().getBuffer(RenderLayer.getLines());
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        if (players) {
            double r = NurXClient.mod("PlayerESP").value("Range");
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || !player.isAlive() || mc.player.squaredDistanceTo(player) > r * r) continue;
                Box b = player.getBoundingBox().expand(0.04);
                boolean target = player == CombatLogic.target;
                VertexRendering.drawBox(matrices, lines,
                        b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ,
                        target ? 1.0f : 0.35f, target ? 0.2f : 0.75f,
                        target ? 0.5f : 1.0f, 0.96f);
            }
        }
        if (storage) {
            int r = (int) NurXClient.mod("StorageESP").value("Radius");
            for (BlockPos p : Expedition.STORAGES) {
                if (mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > r * r) continue;
                drawBlock(matrices, lines, p, 0.84f, 0.48f, 1f);
            }
        }
        if (dungeons) {
            int r = (int) NurXClient.mod("DungeonESP").value("Radius");
            for (BlockPos p : Expedition.DUNGEONS) {
                if (mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= r * r)
                    drawBlock(matrices, lines, p, 1f, 0.32f, 0.45f);
            }
            for (BlockPos p : Expedition.VAULTS) {
                if (mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= r * r)
                    drawBlock(matrices, lines, p, 0.25f, 1f, 0.83f);
            }
        }
        if (loot) {
            double range = NurXClient.mod("LootESP").value("Range");
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof ItemEntity item) || mc.player.squaredDistanceTo(entity) > range * range) continue;
                Box box = entity.getBoundingBox().expand(0.13);
                boolean rare = rareLoot(item.getStack());
                VertexRendering.drawBox(matrices, lines, box.minX, box.minY, box.minZ,
                        box.maxX, box.maxY, box.maxZ,
                        rare ? 1f : 0.3f, rare ? 0.76f : 0.68f, rare ? 0.18f : 1f, 0.96f);
            }
        }
        matrices.pop();
    }

    private static void drawBlock(MatrixStack matrices, VertexConsumer lines, BlockPos p, float r, float g, float b) {
        VertexRendering.drawBox(matrices, lines, p.getX() + 0.02, p.getY() + 0.02, p.getZ() + 0.02,
                p.getX() + 0.98, p.getY() + 0.98, p.getZ() + 0.98, r, g, b, 0.96f);
    }
}
