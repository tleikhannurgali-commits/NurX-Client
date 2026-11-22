package com.nurx.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.LivingEntity;

/** Responsive custom HUD. All drawings use the standard 1.21.4 DrawContext. */
public final class HudOverlay {
    private static final int WHITE = 0xFFF5F5FA;
    private static final int DIM = 0xFFA2ABC0;
    private static final int PANEL = 0xD90B0E1B;

    private HudOverlay() { }

    public static int accent() {
        double wave = (Math.sin(System.currentTimeMillis() / 800.0) + 1.0) * 0.5;
        int r = (int) (125 + 60 * wave);
        int g = (int) (77 + 40 * (1 - wave));
        int b = 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer f = mc.textRenderer;
        int color = accent();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        if (NurXClient.mod("Watermark").enabled) {
            c.fill(8, 8, 118, 30, PANEL);
            c.fill(8, 8, 11, 30, color);
            c.drawTextWithShadow(f, "NURX", 17, 13, color);
            c.drawTextWithShadow(f, "| " + MinecraftClient.getCurrentFps() + " fps", 54, 13, WHITE);
        }
        if (NurXClient.mod("ExpeditionHUD").enabled) {
            Expedition.Spot nearest = Expedition.nearest(mc);
            int panelY = NurXClient.mod("Watermark").enabled ? 35 : 8;
            int panelH = nearest != null ? 67 : 53;
            c.fill(8, panelY, 224, panelY + panelH, PANEL);
            c.fill(8, panelY, 11, panelY + panelH, color);
            c.drawTextWithShadow(f, "EXPEDITION", 17, panelY + 6, color);
            c.drawTextWithShadow(f, "Storages: " + Expedition.STORAGES.size()
                    + "   Spawners: " + Expedition.DUNGEONS.size(), 17, panelY + 20, WHITE);
            c.drawTextWithShadow(f, "Vaults: " + Expedition.VAULTS.size()
                    + "   Markers: " + Expedition.saved().size(), 17, panelY + 33, DIM);
            if (nearest != null) {
                int distance = (int) Math.round(Math.hypot(mc.player.getX() - nearest.x(), mc.player.getZ() - nearest.z()));
                c.drawTextWithShadow(f, nearest.type() + " " + nearest.x() + " " + nearest.y() + " " + nearest.z()
                        + " | " + distance + "m", 17, panelY + 47, WHITE);
            }
        }
        Expedition.Spot alert = Expedition.alert();
        if (alert != null) {
            String message = alert.type() + " FOUND: " + alert.x() + " " + alert.y() + " " + alert.z()
                    + " (" + alert.containers() + ")";
            int alertW = f.getWidth(message) + 28;
            int x = w / 2 - alertW / 2;
            c.fill(x, 44, x + alertW, 66, PANEL);
            c.fill(x, 44, x + alertW, 46, color);
            c.drawCenteredTextWithShadow(f, message, w / 2, 51, WHITE);
        }
        if (NurXClient.mod("ArrayList").enabled) {
            int y = 8;
            for (Mod m : NurXClient.MODULES) {
                if (!m.enabled || m.name.equals("ArrayList") || m.name.equals("Watermark")) continue;
                int textWidth = f.getWidth(m.name);
                int x = w - textWidth - 17;
                c.fill(x - 5, y, w - 8, y + 15, PANEL);
                c.fill(w - 10, y, w - 8, y + 15, color);
                c.drawTextWithShadow(f, m.name, x, y + 4, WHITE);
                y += 17;
            }
        }
        if (NurXClient.mod("TargetHUD").enabled && CombatLogic.target != null) {
            LivingEntity p = CombatLogic.target;
            String name = p.getName().getString();
            int x = w / 2 - 75, y = h - 90;
            c.fill(x, y, x + 150, y + 49, PANEL);
            c.fill(x, y, x + 150, y + 2, color);
            c.drawTextWithShadow(f, name, x + 9, y + 8, WHITE);
            int hp = (int) Math.ceil(p.getHealth());
            String info = hp + " HP   |   " + String.format(java.util.Locale.ROOT, "%.1fm", mc.player.distanceTo(p));
            c.drawTextWithShadow(f, info, x + 9, y + 22, DIM);
            float fraction = Math.max(0f, Math.min(1f, p.getHealth() / Math.max(1f, p.getMaxHealth())));
            c.fill(x + 9, y + 39, x + 141, y + 43, 0xFF292D40);
            c.fill(x + 9, y + 39, x + 9 + Math.round(132 * fraction), y + 43, color);
        }
        if (NurXClient.mod("Crosshair").enabled) {
            int cx = w / 2, cy = h / 2;
            int s = (int) NurXClient.mod("Crosshair").value("Size");
            int gap = 3;
            c.fill(cx - 1, cy - s - gap, cx + 1, cy - gap, color);
            c.fill(cx - 1, cy + gap, cx + 1, cy + s + gap, color);
            c.fill(cx - s - gap, cy - 1, cx - gap, cy + 1, color);
            c.fill(cx + gap, cy - 1, cx + s + gap, cy + 1, color);
        }
        if (NurXClient.mod("Keystrokes").enabled) {
            int x = 13, y = h - 100;
            key(c, f, "W", x + 24, y, mc.options.forwardKey.isPressed(), color);
            key(c, f, "A", x, y + 24, mc.options.leftKey.isPressed(), color);
            key(c, f, "S", x + 24, y + 24, mc.options.backKey.isPressed(), color);
            key(c, f, "D", x + 48, y + 24, mc.options.rightKey.isPressed(), color);
            c.fill(x, y + 49, x + 70, y + 64, mc.options.jumpKey.isPressed() ? color : PANEL);
            c.drawCenteredTextWithShadow(f, "SPACE", x + 35, y + 52, WHITE);
        }
    }

    private static void key(DrawContext c, TextRenderer f, String name, int x, int y, boolean down, int color) {
        c.fill(x, y, x + 22, y + 22, down ? color : PANEL);
        c.drawCenteredTextWithShadow(f, name, x + 11, y + 7, WHITE);
    }
}
