package com.nurx.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NurXClient implements ClientModInitializer {
    public static final List<Mod> MODULES = new ArrayList<>();
    public static int guiBind = GLFW.GLFW_KEY_RIGHT_SHIFT;
    private static boolean guiWasDown;
    private static int ticks;

    @Override
    public void onInitializeClient() {
        registerModules();
        ConfigStore.load();
        ClientTickEvents.END_CLIENT_TICK.register(NurXClient::onTick);
        HudRenderCallback.EVENT.register((context, counter) -> HudOverlay.render(context));
        WorldRenderEvents.AFTER_ENTITIES.register(WorldOverlay::render);
    }

    private static void registerModules() {
        add(new Mod("KillAura", "Configurable PvE/PvP aura: smooth aim, target lock, cooldown", Mod.Category.COMBAT, false, GLFW.GLFW_KEY_R)
                .slider("Range", 3.0, 2.0, 3.2, 0.1)
                .slider("FOV", 110, 30, 180, 5)
                .slider("Turn speed", 24, 2, 90, 2)
                .slider("Rotation mode", 0, 0, 1, 1)  // 0 smooth, 1 snap
                .slider("Rotate", 1, 0, 1, 1)
                .slider("Hit angle", 16, 5, 50, 1)
                .slider("Cooldown", 0.95, 0.6, 1.0, 0.05)
                .slider("Priority", 0, 0, 2, 1)       // 0 near, 1 low HP, 2 angle
                .slider("Sticky target", 1, 0, 1, 1)
                .slider("Players", 0, 0, 1, 1)
                .slider("Hostile mobs", 1, 0, 1, 1)
                .slider("Animals", 0, 0, 1, 1)
                .slider("Attack while using", 0, 0, 1, 1));
        add(new Mod("AimAssist", "Gentle camera assist while attacking, for mobs or players", Mod.Category.COMBAT, false, GLFW.GLFW_KEY_V)
                .slider("Range", 4.0, 2.0, 5.0, 0.1)
                .slider("FOV", 65, 5, 120, 5)
                .slider("Strength", 12, 1, 35, 1)
                .slider("Priority", 2, 0, 2, 1)
                .slider("Players", 0, 0, 1, 1)
                .slider("Hostile mobs", 1, 0, 1, 1)
                .slider("Animals", 0, 0, 1, 1));
        add(new Mod("TriggerBot", "Cooldown-aware hits on the mob/player under the crosshair", Mod.Category.COMBAT, false, GLFW.GLFW_KEY_G)
                .slider("Range", 3.0, 2.0, 3.2, 0.1)
                .slider("Cooldown", 0.95, 0.6, 1.0, 0.05)
                .slider("Players", 0, 0, 1, 1)
                .slider("Hostile mobs", 1, 0, 1, 1)
                .slider("Animals", 0, 0, 1, 1));
        add(new Mod("StashFinder", "Locates clusters of already loaded containers; saves coordinates", Mod.Category.EXPLORATION, false, GLFW.GLFW_KEY_J)
                .slider("Radius", 64, 16, 96, 16)
                .slider("Min containers", 5, 2, 20, 1));
        add(new Mod("DungeonESP", "Highlights loaded spawners, trial spawners and vaults", Mod.Category.EXPLORATION, false, GLFW.GLFW_KEY_N)
                .slider("Radius", 48, 16, 96, 16));
        add(new Mod("LootESP", "Outlines dropped items, with a brighter rare-loot color", Mod.Category.EXPLORATION, false, GLFW.GLFW_KEY_L)
                .slider("Range", 32, 8, 64, 8));
        add(new Mod("QuickLoot", "Hold Shift in an open chest/shulker to shift-click its loot", Mod.Category.EXPLORATION, false, GLFW.GLFW_KEY_K)
                .slider("Delay ticks", 5, 2, 12, 1));
        add(new Mod("ExpeditionHUD", "Shows visible storages, dungeon blocks and saved markers", Mod.Category.EXPLORATION, true, GLFW.GLFW_KEY_H));
        add(new Mod("MarkSpot", "Action: save your current coordinates as a waypoint", Mod.Category.EXPLORATION, false, GLFW.GLFW_KEY_M));
        add(new Mod("TargetHUD", "Displays target name, health and distance", Mod.Category.VISUALS, true, -1));
        add(new Mod("PlayerESP", "Draws 3D outlines around nearby players", Mod.Category.VISUALS, false, GLFW.GLFW_KEY_B)
                .slider("Range", 32, 8, 64, 4));
        add(new Mod("StorageESP", "Highlights loaded nearby storage containers", Mod.Category.VISUALS, false, -1)
                .slider("Radius", 32, 8, 64, 8));
        add(new Mod("ArrayList", "Active modules on the right of the HUD", Mod.Category.VISUALS, true, -1));
        add(new Mod("Watermark", "Client branding and FPS on screen", Mod.Category.VISUALS, true, -1));
        add(new Mod("Crosshair", "A crisp custom center crosshair", Mod.Category.VISUALS, false, -1)
                .slider("Size", 5, 2, 12, 1));
        add(new Mod("Keystrokes", "Live WASD and jump indicators", Mod.Category.VISUALS, false, -1));
        add(new Mod("AutoSprint", "Sprints while you hold forward", Mod.Category.MOVEMENT, false, -1));
        add(new Mod("Zoom", "Switches to a narrow field of view", Mod.Category.MISC, false, GLFW.GLFW_KEY_C)
                .slider("FOV", 30, 20, 60, 5));
        add(new Mod("LowParticles", "Reduces particle density until disabled", Mod.Category.MISC, false, -1));
        add(new Mod("Fullbright", "Sets in-game brightness to maximum", Mod.Category.MISC, false, -1));
    }

    private static void add(Mod module) { MODULES.add(module); }

    public static Mod mod(String name) {
        for (Mod m : MODULES) if (m.name.equals(name)) return m;
        throw new IllegalArgumentException("Unknown module " + name);
    }

    public static List<Mod> all() { return Collections.unmodifiableList(MODULES); }

    public static void toggle(Mod module) {
        if ("MarkSpot".equals(module.name)) {
            Expedition.markHere(MinecraftClient.getInstance());
            return;
        }
        module.enabled = !module.enabled;
        ConfigStore.save();
    }

    private static void onTick(MinecraftClient mc) {
        long handle = mc.getWindow().getHandle();
        if (mc.currentScreen == null) {
            boolean down = GLFW.glfwGetKey(handle, guiBind) == GLFW.GLFW_PRESS;
            if (down && !guiWasDown) mc.setScreen(new ClickGui());
            guiWasDown = down;
            for (Mod m : MODULES) {
                if (m.bind < 0 || m.bind == guiBind) continue;
                boolean pressed = GLFW.glfwGetKey(handle, m.bind) == GLFW.GLFW_PRESS;
                if (pressed && !m.lastKeyDown) toggle(m);
                m.lastKeyDown = pressed;
            }
        } else {
            guiWasDown = false;
            for (Mod m : MODULES) m.lastKeyDown = false;
        }
        if (mc.player == null || mc.world == null) {
            Gameplay.clearTarget();
            Expedition.clearVisible();
            Gameplay.restoreOptions(mc);
            return;
        }
        ticks++;
        Gameplay.tick(mc);
        QuickLoot.tick(mc);
        if (ticks % 40 == 0) Expedition.scan(mc);
    }
}
