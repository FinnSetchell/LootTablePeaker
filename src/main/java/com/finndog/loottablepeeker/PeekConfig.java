package com.finndog.loottablepeeker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PeekConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("loot_table_peeker.json");

    private static boolean enabled = false;

    private PeekConfig() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            String json = Files.readString(CONFIG_PATH);
            Data data = GSON.fromJson(json, Data.class);
            if (data != null) {
                enabled = data.enabled;
            }
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to load config", e);
        }
    }

    private static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(new Data(enabled)));
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to save config", e);
        }
    }

    private record Data(boolean enabled) {}
}
