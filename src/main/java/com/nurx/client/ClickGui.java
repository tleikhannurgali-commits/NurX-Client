package com.nurx.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** A custom dark-violet ClickGUI: click toggle, right-click settings, middle-click bind. */
public final class ClickGui extends Screen {
    private static final int BG = 0xEB090C18;
    private static final int PANEL = 0xF4181D31;
    private static final int CELL = 0xCC22283F;
    private static final int OFF = 0xFF7B849B;
    private static final int WHITE = 0xFFF4F4FA;
    private static final int GAP = 10;
    private static final int HEADER_Y = 68;
    private static final int ROW_H = 25;
    private static final int SETTING_H = 27;
    private static final int HEADER_H = 30;
    private Mod bindCapture;
    private Mod.Slider dragging;
    private int draggingX;
    private int draggingWidth;
    private String tooltip;
    private int selectedCategory = 1;
    private final int[] scrollOffsets = new int[Mod.Category.values().length];

    private int visibleBodyHeight() {
        return Math.max(1, height - 29 - (HEADER_Y + HEADER_H));
    }

    private int maxScroll(Mod.Category category) {
        int needed = 0;
        for (Mod m : NurXClient.all()) {
            if (m.category != category) continue;
            needed += ROW_H + (m.expanded ? m.settings().size() * SETTING_H : 0);
        }
        return Math.max(0, needed - visibleBodyHeight());
    }

    private int scroll(int categoryIndex, Mod.Category category) {
        scrollOffsets[categoryIndex] = Math.max(0,
                Math.min(scrollOffsets[categoryIndex], maxScroll(category)));
        return scrollOffsets[categoryIndex];
    }

    private boolean compact() { return width < 610; }

    public ClickGui() {
        super(Text.literal("NurX ClickGUI"));
    }

    private int panelWidth() { return compact() ? Math.min(310, width - 24) : Math.min(165, Math.max(92, (width - 70) / 5)); }
    private int startX() { return compact() ? (width - panelWidth()) / 2 : (width - (panelWidth() * 5 + GAP * 4)) / 2; }
    private int panelX(int index) { return compact() ? startX() : startX() + index * (panelWidth() + GAP); }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        int accent = HudOverlay.accent();
        c.fill(0, 0, width, height, BG);
        c.fill(0, 0, width, 3, accent);
        c.drawCenteredTextWithShadow(textRenderer, "N U R X   C L I E N T", width / 2, 18, WHITE);
        c.drawCenteredTextWithShadow(textRenderer, "FABRIC 1.21.4  |  MODULE EDITOR", width / 2, 34, OFF);
        tooltip = null;
        if (compact()) c.drawCenteredTextWithShadow(textRenderer, "<  " + Mod.Category.values()[selectedCategory].name() + "  >   (LEFT / RIGHT)", width / 2, 51, WHITE);
        int idx = 0;
        for (Mod.Category category : Mod.Category.values()) {
            int categoryIndex = idx++;
            if (compact() && categoryIndex != selectedCategory) continue;
            int x = panelX(categoryIndex);
            int panelW = panelWidth();
            int end = height - 27;
            c.fill(x, HEADER_Y, x + panelW, end, PANEL);
            c.fill(x, HEADER_Y, x + panelW, HEADER_Y + 2, accent);
            c.drawCenteredTextWithShadow(textRenderer, category.name(), x + panelW / 2, HEADER_Y + 10, WHITE);
            int bodyTop = HEADER_Y + HEADER_H;
            c.enableScissor(x + 1, bodyTop, x + panelW - 1, end - 2);
            int y = bodyTop - scroll(categoryIndex, category);
            for (Mod m : NurXClient.all()) {
                if (m.category != category) continue;
                boolean hovered = mouseY >= bodyTop && mouseY < end - 2
                        && inside(mouseX, mouseY, x + 3, y, panelW - 6, ROW_H - 2);
                if (hovered) tooltip = m.description;
                c.fill(x + 3, y, x + panelW - 3, y + ROW_H - 2, hovered ? 0xEE353B58 : CELL);
                c.fill(x + 3, y, x + 5, y + ROW_H - 2, m.enabled ? accent : OFF);
                c.drawTextWithShadow(textRenderer, m.name, x + 10, y + 8, m.enabled ? WHITE : OFF);
                String b = m.bind >= 0 ? keyName(m.bind) : "-";
                int bindX = x + panelW - 9 - textRenderer.getWidth(b);
                c.drawTextWithShadow(textRenderer, b, bindX, y + 8,
                        bindCapture == m ? 0xFFFFFF6A : (m.enabled ? accent : OFF));
                y += ROW_H;
                if (m.expanded) {
                    for (Mod.Slider s : m.settings()) {
                        int sx = x + 8;
                        int sw = panelW - 16;
                        c.fill(x + 3, y, x + panelW - 3, y + SETTING_H - 1, 0xDD101425);
                        c.drawTextWithShadow(textRenderer, s.name, sx, y + 3, OFF);
                        String number = s.display();
                        c.drawTextWithShadow(textRenderer, number,
                                x + panelW - textRenderer.getWidth(number) - 8, y + 3, WHITE);
                        c.fill(sx, y + 19, sx + sw, y + 22, 0xFF333950);
                        int fillWidth = (int) Math.round(sw * ((s.value - s.min) / (s.max - s.min)));
                        c.fill(sx, y + 19, sx + fillWidth, y + 22, accent);
                        y += SETTING_H;
                    }
                }
            }
            c.disableScissor();
            if (maxScroll(category) > 0) {
                int bottom = height - 30;
                c.drawCenteredTextWithShadow(textRenderer, "SCROLL", x + panelW / 2, bottom - 3, OFF);
            }
        }
        if (bindCapture != null) {
            int bw = 310, bh = 35;
            int x = (width - bw) / 2, y = height - 55;
            c.fill(x, y, x + bw, y + bh, 0xFF232943);
            c.fill(x, y, x + bw, y + 2, accent);
            c.drawCenteredTextWithShadow(textRenderer, "Press a key for " + bindCapture.name, width / 2, y + 7, WHITE);
            c.drawCenteredTextWithShadow(textRenderer, "BACKSPACE = unbind   |   ESC = cancel", width / 2, y + 20, OFF);
        } else {
            c.drawCenteredTextWithShadow(textRenderer,
                    "LMB: toggle   |   RMB: settings   |   MMB: bind   |   RSHIFT: open", width / 2,
                    height - 24, OFF);
            if (tooltip != null) {
                c.drawCenteredTextWithShadow(textRenderer, tooltip, width / 2, height - 40, WHITE);
            }
        }
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindCapture != null) return true;
        int index = 0;
        for (Mod.Category category : Mod.Category.values()) {
            int categoryIndex = index++;
            if (compact() && categoryIndex != selectedCategory) continue;
            int x = panelX(categoryIndex);
            int y = HEADER_Y + HEADER_H - scroll(categoryIndex, category);
            int w = panelWidth();
            for (Mod mod : NurXClient.all()) {
                if (mod.category != category) continue;
                if (mouseY >= HEADER_Y + HEADER_H && mouseY < height - 29
                        && inside(mouseX, mouseY, x + 3, y, w - 6, ROW_H - 2)) {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) NurXClient.toggle(mod);
                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) mod.expanded = !mod.expanded;
                    if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) bindCapture = mod;
                    return true;
                }
                y += ROW_H;
                if (!mod.expanded) continue;
                for (Mod.Slider slider : mod.settings()) {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                            && mouseY >= HEADER_Y + HEADER_H && mouseY < height - 29
                            && inside(mouseX, mouseY, x + 6, y, w - 12, SETTING_H)) {
                        dragging = slider;
                        draggingX = x + 8;
                        draggingWidth = w - 16;
                        updateSlider(mouseX);
                        return true;
                    }
                    y += SETTING_H;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int idx = 0;
        for (Mod.Category category : Mod.Category.values()) {
            int categoryIndex = idx++;
            if (compact() && categoryIndex != selectedCategory) continue;
            if (!inside(mouseX, mouseY, panelX(categoryIndex), HEADER_Y, panelWidth(), height - HEADER_Y - 27)) continue;
            int amount = (int) Math.round(verticalAmount * 34);
            if (amount == 0 && verticalAmount != 0) amount = verticalAmount > 0 ? 16 : -16;
            scrollOffsets[categoryIndex] = Math.max(0,
                    Math.min(maxScroll(category), scroll(categoryIndex, category) - amount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateSlider(double mouseX) {
        if (dragging == null) return;
        double portion = Math.max(0.0, Math.min(1.0, (mouseX - draggingX) / draggingWidth));
        dragging.set(dragging.min + (dragging.max - dragging.min) * portion);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            ConfigStore.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindCapture != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) bindCapture.bind = -1;
            else if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != NurXClient.guiBind) bindCapture.bind = keyCode;
            bindCapture = null;
            ConfigStore.save();
            return true;
        }
        if (compact() && keyCode == GLFW.GLFW_KEY_LEFT) {
            selectedCategory = (selectedCategory + Mod.Category.values().length - 1) % Mod.Category.values().length;
            return true;
        }
        if (compact() && keyCode == GLFW.GLFW_KEY_RIGHT) {
            selectedCategory = (selectedCategory + 1) % Mod.Category.values().length;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        ConfigStore.save();
        super.close();
    }

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
        if (key == GLFW.GLFW_KEY_SPACE) return "SPACE";
        String text = GLFW.glfwGetKeyName(key, 0);
        return text == null ? Integer.toString(key) : text.toUpperCase(Locale.ROOT);
    }
}
