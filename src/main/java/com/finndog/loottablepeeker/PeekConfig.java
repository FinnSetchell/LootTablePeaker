package com.finndog.loottablepeeker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.finndog.loottablepeeker.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PeekConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        Services.PLATFORM.configDir().resolve(LootTablePeeker.MOD_ID + ".json");

    private static PeekMode mode = PeekMode.OFF;
    /** Independent of {@link #mode}: the cue is useful whether or not interactions are intercepted. */
    private static boolean highlight = false;

    private PeekConfig() {}

    public static PeekMode getMode() {
        return mode;
    }

    public static void setMode(PeekMode value) {
        mode = value;
        save();
    }

    public static boolean isHighlightEnabled() {
        return highlight;
    }

    public static void setHighlightEnabled(boolean value) {
        highlight = value;
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
            } else if (data.enabled() != null) {
                // Pre-mode configs stored a plain boolean; the old "on" behaviour is now title mode.
                mode = data.enabled() ? PeekMode.TITLE : PeekMode.OFF;
                save();
            }

            if (data.highlight() != null) {
                highlight = data.highlight();
            }
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to load config", e);
        }
    }

    private static void save() {
        try {
            // A fresh server may not have a config directory yet, and a dedicated server writes this
            // on the very first command.
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(CONFIG_PATH, GSON.toJson(new Data(mode.id(), null, highlight)));
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to save config", e);
        }
    }

    /** {@code enabled} is only read, never written — it exists solely to migrate old config files. */
    private record Data(String mode, Boolean enabled, Boolean highlight) {}
}
