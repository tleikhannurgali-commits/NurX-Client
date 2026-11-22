package com.nurx.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticlesMode;

/** Client-side module behaviour; no packet spoofing or anti-cheat bypass. */
public final class Gameplay {
    private static Integer oldFov;
    private static Double oldGamma;
    private static ParticlesMode oldParticles;

    private Gameplay() { }

    public static void clearTarget() { CombatLogic.reset(); }

    public static void tick(MinecraftClient mc) {
        CombatLogic.tick(mc);
        if (NurXClient.mod("AutoSprint").enabled && mc.options.forwardKey.isPressed()
                && !mc.player.isSneaking() && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
        updateOptions(mc);
    }

    private static void updateOptions(MinecraftClient mc) {
        boolean zoom = NurXClient.mod("Zoom").enabled;
        if (zoom && oldFov == null) oldFov = mc.options.getFov().getValue();
        if (zoom) {
            int wanted = (int) NurXClient.mod("Zoom").value("FOV");
            if (mc.options.getFov().getValue() != wanted) mc.options.getFov().setValue(wanted);
        } else if (oldFov != null) {
            mc.options.getFov().setValue(oldFov);
            oldFov = null;
        }

        boolean bright = NurXClient.mod("Fullbright").enabled;
        if (bright && oldGamma == null) oldGamma = mc.options.getGamma().getValue();
        if (bright && mc.options.getGamma().getValue() != 1.0d) mc.options.getGamma().setValue(1.0d);
        if (!bright && oldGamma != null) {
            mc.options.getGamma().setValue(oldGamma);
            oldGamma = null;
        }

        boolean particles = NurXClient.mod("LowParticles").enabled;
        if (particles && oldParticles == null) oldParticles = mc.options.getParticles().getValue();
        if (particles) mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
        if (!particles && oldParticles != null) {
            mc.options.getParticles().setValue(oldParticles);
            oldParticles = null;
        }
    }

    public static void restoreOptions(MinecraftClient mc) {
        if (oldFov != null) mc.options.getFov().setValue(oldFov);
        if (oldGamma != null) mc.options.getGamma().setValue(oldGamma);
        if (oldParticles != null) mc.options.getParticles().setValue(oldParticles);
        oldFov = null;
        oldGamma = null;
        oldParticles = null;
    }
}
