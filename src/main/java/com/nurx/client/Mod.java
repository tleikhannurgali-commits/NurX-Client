package com.nurx.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One configurable, bindable feature. No dummy modules are registered. */
public final class Mod {
    public enum Category { COMBAT, EXPLORATION, VISUALS, MOVEMENT, MISC }

    public final String name;
    public final String description;
    public final Category category;
    public boolean enabled;
    public boolean expanded;
    public int bind;
    public boolean lastKeyDown;
    private final List<Slider> settings = new ArrayList<>();

    public Mod(String name, String description, Category category, boolean enabled, int bind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabled;
        this.bind = bind;
    }

    public Mod slider(String name, double value, double min, double max, double step) {
        settings.add(new Slider(name, value, min, max, step));
        return this;
    }

    public double value(String settingName) {
        for (Slider slider : settings) {
            if (slider.name.equals(settingName)) return slider.value;
        }
        throw new IllegalArgumentException("No setting " + settingName + " in " + name);
    }

    public List<Slider> settings() {
        return Collections.unmodifiableList(settings);
    }

    public static final class Slider {
        public final String name;
        public final double min, max, step;
        public double value;

        public Slider(String name, double value, double min, double max, double step) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.step = step;
            set(value);
        }

        public void set(double newValue) {
            double clamped = Math.max(min, Math.min(max, newValue));
            double snapped = Math.round((clamped - min) / step) * step + min;
            value = Math.max(min, Math.min(max, Math.round(snapped * 10000.0) / 10000.0));
        }

        public String display() {
            if (name.equals("Rotation mode")) return value < 0.5 ? "Smooth" : "Snap";
            if (name.equals("Priority")) {
                return switch ((int) value) {
                    case 1 -> "Low HP";
                    case 2 -> "Angle";
                    default -> "Nearest";
                };
            }
            if (name.equals("Rotate") || name.equals("Sticky target")
                    || name.equals("Players") || name.equals("Hostile mobs")
                    || name.equals("Animals") || name.equals("Attack while using")) {
                return value >= 0.5 ? "ON" : "OFF";
            }
            return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
    }
}
