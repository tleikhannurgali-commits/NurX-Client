package com.nurx.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;

/**
 * Original combat implementation for Fabric 1.21.4. This uses ordinary
 * client input/interaction calls; it does not copy Night DLC, spoof network
 * packets, inspect server-private data or claim to evade server anti-cheats.
 */
public final class CombatLogic {
    public static LivingEntity target;
    private static LivingEntity locked;
    private static int delayTicks;

    private CombatLogic() { }

    public static void reset() {
        target = null;
        locked = null;
        delayTicks = 0;
    }

    public static void tick(MinecraftClient mc) {
        if (delayTicks > 0) delayTicks--;
        target = null;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }
        // Do not act when typing in GUIs (inventory, chat, etc.).
        if (mc.currentScreen != null) return;

        Mod aura = NurXClient.mod("KillAura");
        Mod aim = NurXClient.mod("AimAssist");
        Mod trigger = NurXClient.mod("TriggerBot");

        if (aura.enabled) {
            if (aura.value("Sticky target") < 0.5) locked = null;
            LivingEntity selected = pick(mc, aura, locked);
            locked = selected;
            if (selected != null) {
                target = selected;
                if (aura.value("Rotate") >= 0.5) {
                    rotate(mc, selected, (float) aura.value("Turn speed"),
                            aura.value("Rotation mode") >= 0.5);
                }
                if (ready(mc, aura.value("Cooldown")) && delayTicks == 0
                        && (!mc.player.isUsingItem() || aura.value("Attack while using") >= 0.5)
                        && aligned(mc, selected, aura.value("Hit angle"))
                        && visibleAndReachable(mc, selected, aura.value("Range"))) {
                    swingAndAttack(mc, selected);
                    delayTicks = 1;
                }
            }
        } else {
            locked = null;
        }

        // AimAssist only turns the player's camera; it never attacks on its own.
        if (!aura.enabled && aim.enabled && mc.options.attackKey.isPressed()) {
            LivingEntity selected = pick(mc, aim, null);
            if (selected != null) {
                target = selected;
                rotate(mc, selected, (float) aim.value("Strength"), false);
            }
        }

        if (!aura.enabled && trigger.enabled
                && mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity selected
                && allowedType(mc, selected, trigger)
                && visibleAndReachable(mc, selected, trigger.value("Range"))) {
            target = selected;
            if (ready(mc, trigger.value("Cooldown")) && delayTicks == 0) {
                swingAndAttack(mc, selected);
                delayTicks = 1;
            }
        }
    }

    private static LivingEntity pick(MinecraftClient mc, Mod mod, LivingEntity previous) {
        double range = mod.value("Range");
        double maxSquare = range * range;
        double fov = mod.value("FOV");
        if (previous != null && allowedType(mc, previous, mod)
                && visibleAndReachable(mc, previous, range)
                && inFov(mc, previous, fov)) return previous;

        LivingEntity result = null;
        double best = Double.POSITIVE_INFINITY;
        Box around = mc.player.getBoundingBox().expand(range);
        int priority = (int) mod.value("Priority");
        for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class, around,
                e -> e.isAlive() && e.isAttackable())) {
            if (!allowedType(mc, entity, mod) || !mc.player.canSee(entity)) continue;
            double distance = mc.player.squaredDistanceTo(entity);
            if (distance > maxSquare || !inFov(mc, entity, fov)) continue;
            float bearing = angles(mc, entity)[0];
            double score = switch (priority) {
                case 1 -> entity.getHealth() + distance * 0.0001; // lowest health
                case 2 -> Math.abs(AuraMath.wrap(bearing - mc.player.getYaw()))
                        + distance * 0.001;  // closest to crosshair
                default -> distance; // nearest entity
            };
            if (score < best) {
                best = score;
                result = entity;
            }
        }
        return result;
    }

    private static boolean allowedType(MinecraftClient mc, LivingEntity e, Mod mod) {
        if (e == mc.player || !e.isAlive() || e.isSpectator() || e.isRemoved()
                || !e.isAttackable() || mc.player.isTeammate(e)) return false;
        if (e instanceof PlayerEntity player) {
            return mod.value("Players") >= 0.5 && !player.isCreative();
        }
        if (e instanceof HostileEntity || e instanceof SlimeEntity) {
            return mod.value("Hostile mobs") >= 0.5;
        }
        // Does not target villagers, pets/wolves, armour stands or NPCs.
        if (e instanceof AnimalEntity) return mod.value("Animals") >= 0.5;
        return false;
    }

    private static float[] angles(MinecraftClient mc, LivingEntity e) {
        // Slightly below the eyes: more reliable on mobs with unusual heights.
        double aimY = e.getBoundingBox().minY + e.getHeight() * 0.78;
        return new float[]{
                AuraMath.yaw(mc.player.getX(), mc.player.getZ(), e.getX(), e.getZ()),
                AuraMath.pitch(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(),
                        e.getX(), aimY, e.getZ())
        };
    }

    private static boolean inFov(MinecraftClient mc, LivingEntity e, double fov) {
        return AuraMath.withinFov(mc.player.getYaw(), angles(mc, e)[0], fov);
    }

    private static boolean aligned(MinecraftClient mc, LivingEntity e, double angle) {
        float[] desired = angles(mc, e);
        return Math.abs(AuraMath.wrap(desired[0] - mc.player.getYaw())) <= angle
                && Math.abs(desired[1] - mc.player.getPitch()) <= angle;
    }

    private static void rotate(MinecraftClient mc, LivingEntity selected, float turnSpeed, boolean snap) {
        float[] desired = angles(mc, selected);
        float step = snap ? 180.0f : turnSpeed;
        mc.player.setYaw(AuraMath.approachYaw(mc.player.getYaw(), desired[0], step));
        mc.player.setPitch(AuraMath.approachPitch(mc.player.getPitch(), desired[1], step));
    }

    private static boolean visibleAndReachable(MinecraftClient mc, LivingEntity entity, double range) {
        return entity.isAlive() && mc.player.canSee(entity)
                && mc.player.squaredDistanceTo(entity) <= range * range;
    }

    private static boolean ready(MinecraftClient mc, double cooldown) {
        return mc.player.getAttackCooldownProgress(0.0f) >= cooldown;
    }

    private static void swingAndAttack(MinecraftClient mc, LivingEntity selected) {
        mc.interactionManager.attackEntity(mc.player, selected);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
