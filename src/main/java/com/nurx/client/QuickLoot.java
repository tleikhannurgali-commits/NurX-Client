package com.nurx.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

/** One user-confirmed shift-click every N ticks in a standard chest/shulker UI. */
public final class QuickLoot {
    private static int openSyncId = -1;
    private static int nextSlot = 0;
    private static int clock = 0;
    private QuickLoot() { }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null || !NurXClient.mod("QuickLoot").enabled
                || !(mc.currentScreen instanceof GenericContainerScreen || mc.currentScreen instanceof ShulkerBoxScreen)) {
            openSyncId = -1; nextSlot = 0; clock = 0;
            return;
        }
        long window = mc.getWindow().getHandle();
        boolean shiftHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shiftHeld) return; // Must consciously hold Shift while chest is open.
        // FunTime uses container-shaped custom menus; avoid blindly moving shop/menu items.
        String title = mc.currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)
                && !(title.contains("chest") || title.contains("сундук") || title.contains("barrel")
                        || title.contains("бочка") || title.contains("loot") || title.contains("добыч")
                        || title.contains("сокровищ"))) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler.syncId != openSyncId) {
            openSyncId = handler.syncId;
            nextSlot = 0;
            clock = 0;
        }
        if (++clock < NurXClient.mod("QuickLoot").value("Delay ticks")) return;
        clock = 0;
        while (nextSlot < handler.slots.size()) {
            Slot slot = handler.slots.get(nextSlot++);
            if (slot.inventory == mc.player.getInventory() || !slot.hasStack()) continue;
            // Standard, server-visible shift-click. No packet spoofing or GUI bypass.
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            break;
        }
    }
}
