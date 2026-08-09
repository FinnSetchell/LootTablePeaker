package com.finndog.loottablepeeker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.finndog.loottablepeeker.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PeekConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        Services.PLATFORM.configDir().resolve(LootTablePeeker.MOD_ID + ".json");

    private static PeekMode mode = PeekMode.OFF;
    /**
     * Server-wide default for the container cue, used for any player who has not chosen for
     * themselves. Independent of {@link #mode}: the cue is useful whether or not interactions are
     * intercepted.
     */
    private static boolean highlight = false;
    /**
     * Per-player overrides of {@link #highlight}, by player UUID. The cue is a personal display
     * preference — it changes nothing about the world — so any player may set their own without
     * needing permissions, and one player turning it on must not put particles on everyone's screen.
     */
    private static final Map<UUID, Boolean> highlightOverrides = new HashMap<>();
    /** Per-player marker style; also personal, and independent of whether the cue is on. */
    private static final Map<UUID, PeekHighlightStyle> highlightStyles = new HashMap<>();
    private static PeekHighlightStyle defaultStyle = PeekHighlightStyle.FAINT;

    private PeekConfig() {}

    public static PeekMode getMode() {
        return mode;
    }

    public static void setMode(PeekMode value) {
        mode = value;
        save();
    }

    /** The server-wide default, used for players who have not set their own preference. */
    public static boolean isHighlightEnabled() {
        return highlight;
    }

    public static void setHighlightEnabled(boolean value) {
        highlight = value;
        save();
    }

    /** Whether this specific player should see the cue: their own choice, else the default. */
    public static boolean isHighlightEnabledFor(UUID player) {
        Boolean own = highlightOverrides.get(player);
        return own != null ? own : highlight;
    }

    public static void setHighlightEnabledFor(UUID player, boolean value) {
        highlightOverrides.put(player, value);
        save();
    }

    /** Drops a player's override so they follow the server default again. */
    public static void clearHighlightFor(UUID player) {
        if (highlightOverrides.remove(player) != null) save();
    }

    /** True when this player has chosen for themselves rather than following the default. */
    public static boolean hasHighlightOverride(UUID player) {
        return highlightOverrides.containsKey(player);
    }

    public static PeekHighlightStyle highlightStyleFor(UUID player) {
        return highlightStyles.getOrDefault(player, defaultStyle);
    }

    public static void setHighlightStyleFor(UUID player, PeekHighlightStyle style) {
        highlightStyles.put(player, style);
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

            highlightStyles.clear();
            if (data.highlightStyles() != null) {
                data.highlightStyles().forEach((id, styleId) -> {
                    PeekHighlightStyle style = PeekHighlightStyle.byId(styleId);
                    if (style == null) {
                        LootTablePeeker.LOGGER.warn("Ignoring unknown highlight style '{}' in config", styleId);
                        return;
                    }
                    try {
                        highlightStyles.put(UUID.fromString(id), style);
                    } catch (IllegalArgumentException e) {
                        LootTablePeeker.LOGGER.warn("Ignoring malformed player UUID in config: {}", id);
                    }
                });
            }

            highlightOverrides.clear();
            if (data.highlightPlayers() != null) {
                data.highlightPlayers().forEach((id, value) -> {
                    if (value == null) return;
                    try {
                        highlightOverrides.put(UUID.fromString(id), value);
                    } catch (IllegalArgumentException e) {
                        // A hand-edited config should not take the mod down over one bad key.
                        LootTablePeeker.LOGGER.warn("Ignoring malformed player UUID in config: {}", id);
                    }
                });
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
            Map<String, Boolean> overrides = new LinkedHashMap<>();
            highlightOverrides.forEach((id, value) -> overrides.put(id.toString(), value));
            Map<String, String> styles = new LinkedHashMap<>();
            highlightStyles.forEach((id, style) -> styles.put(id.toString(), style.id()));
            Files.writeString(CONFIG_PATH,
                GSON.toJson(new Data(mode.id(), null, highlight, overrides, styles)));
        } catch (IOException e) {
            LootTablePeeker.LOGGER.error("Failed to save config", e);
        }
    }

    /** {@code enabled} is only read, never written — it exists solely to migrate old config files. */
    private record Data(String mode, Boolean enabled, Boolean highlight,
                        Map<String, Boolean> highlightPlayers,
                        Map<String, String> highlightStyles) {}
}
