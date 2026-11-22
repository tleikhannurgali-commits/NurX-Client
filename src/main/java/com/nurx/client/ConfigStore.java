package com.nurx.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Read/write config/nurx-client.json. Uses Gson already bundled with Minecraft. */
public final class ConfigStore {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nurx-client.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigStore() { }

    public static void load() {
        if (!Files.isRegularFile(PATH)) return;
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("guiBind")) NurXClient.guiBind = root.get("guiBind").getAsInt();
            JsonArray waypoints = root.has("waypoints") ? root.getAsJsonArray("waypoints") : null;
            if (waypoints != null) {
                List<Expedition.Spot> stored = new ArrayList<>();
                for (JsonElement item : waypoints) {
                    try {
                        JsonObject obj = item.getAsJsonObject();
                        stored.add(new Expedition.Spot(obj.get("world").getAsString(),
                                obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt(),
                                obj.get("containers").getAsInt(), obj.get("type").getAsString()));
                    } catch (RuntimeException ignored) { /* Skip damaged individual entries. */ }
                }
                Expedition.replaceSpots(stored);
            }
            JsonObject modules = root.getAsJsonObject("modules");
            if (modules == null) return;
            for (Mod mod : NurXClient.MODULES) {
                if (!modules.has(mod.name)) continue;
                JsonObject obj = modules.getAsJsonObject(mod.name);
                if (obj.has("enabled")) mod.enabled = obj.get("enabled").getAsBoolean();
                if (obj.has("bind")) mod.bind = obj.get("bind").getAsInt();
                JsonObject sliders = obj.getAsJsonObject("sliders");
                if (sliders == null) continue;
                for (Mod.Slider setting : mod.settings()) {
                    JsonElement item = sliders.get(setting.name);
                    if (item != null) setting.set(item.getAsDouble());
                }
            }
        } catch (Exception exception) {
            System.err.println("[NurX] Could not load config: " + exception.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("guiBind", NurXClient.guiBind);
        JsonObject modules = new JsonObject();
        for (Mod mod : NurXClient.MODULES) {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", mod.enabled);
            obj.addProperty("bind", mod.bind);
            JsonObject sliders = new JsonObject();
            for (Mod.Slider setting : mod.settings()) sliders.addProperty(setting.name, setting.value);
            obj.add("sliders", sliders);
            modules.add(mod.name, obj);
        }
        root.add("modules", modules);
        JsonArray waypoints = new JsonArray();
        for (Expedition.Spot spot : Expedition.saved()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("world", spot.world());
            obj.addProperty("x", spot.x());
            obj.addProperty("y", spot.y());
            obj.addProperty("z", spot.z());
            obj.addProperty("containers", spot.containers());
            obj.addProperty("type", spot.type());
            waypoints.add(obj);
        }
        root.add("waypoints", waypoints);
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception exception) {
            System.err.println("[NurX] Could not save config: " + exception.getMessage());
        }
    }
}
