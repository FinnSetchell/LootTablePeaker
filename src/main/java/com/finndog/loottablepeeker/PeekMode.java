package com.finndog.loottablepeeker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PeekMode {

    /** Containers behave normally. */
    OFF("off", ChatFormatting.RED, "containers open normally"),

    /** Interaction is cancelled and the loot table id is shown as a title. */
    TITLE("title", ChatFormatting.GOLD, "shows the loot table id on screen"),

    /** Interaction is cancelled and a rerollable preview of the rolled loot is opened. */
    PREVIEW("preview", ChatFormatting.AQUA, "opens a rerollable preview of the loot");

    private final String id;
    private final ChatFormatting colour;
    private final String description;

    PeekMode(String id, ChatFormatting colour, String description) {
        this.id = id;
        this.colour = colour;
        this.description = description;
    }

    public String id() {
        return this.id;
    }

    public String description() {
        return this.description;
    }

    public Component displayName() {
        return Component.literal(this.id).withStyle(this.colour);
    }

    /** Returns null for an unrecognised id rather than throwing, so bad config values can be reported. */
    public static PeekMode byId(String id) {
        for (PeekMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) return mode;
        }
        return null;
    }
}
