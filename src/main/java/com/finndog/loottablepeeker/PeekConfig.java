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

    private static PeekMode mode = PeekMode.OFF;

    private PeekConfig() {}

    public static PeekMode getMode() {
        return mode;
    }

    public static void setMode(PeekMode value) {
        mode = value;
        save();
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(CONFIG_PATH), Data.class);
            if (data == null) return;

            if (data.mode() != null) {
                PeekMode parsed = PeekMode.byId(data.mode());
                if (parsed == null) {
                    LootTablePeeker.LOGGER.warn("Unknown mode '{}' in config, falling back to off", data.mode());
                    parsed = PeekMode.OFF;
                }
                mode = parsed;
                return;
            }

            // Pre-mode configs stored a plain boolean; the old "on" behaviour is now the title mode.
            if (data.enabled() != null) {
                mode = data.enabled() ? PeekMode.TITLE : PeekMode.OFF;
                save();
            }
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to load config", e);
        }
    }

    private static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(new Data(mode.id(), null)));
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to save config", e);
        }
    }

    /** {@code enabled} is only read, never written — it exists solely to migrate old config files. */
    private record Data(String mode, Boolean enabled) {}
}
