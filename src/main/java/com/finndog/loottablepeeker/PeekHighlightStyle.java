package com.finndog.loottablepeeker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** How a player wants loot containers marked. Personal, like the on/off toggle itself. */
public enum PeekHighlightStyle {

    /** A sparse box on every loot container in range — ambient awareness, easy to build around. */
    FAINT("faint", ChatFormatting.GREEN, "a sparse box on every loot container nearby"),

    /** A dense box on the one container you are looking at — quiet until you point at something. */
    CROSSHAIR("crosshair", ChatFormatting.AQUA, "a solid box on the container you are looking at");

    private final String id;
    private final ChatFormatting colour;
    private final String description;

    PeekHighlightStyle(String id, ChatFormatting colour, String description) {
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

    /** Returns null for an unrecognised id rather than throwing, so bad config can be reported. */
    public static PeekHighlightStyle byId(String id) {
        for (PeekHighlightStyle style : values()) {
            if (style.id.equalsIgnoreCase(id)) return style;
        }
        return null;
    }
}
