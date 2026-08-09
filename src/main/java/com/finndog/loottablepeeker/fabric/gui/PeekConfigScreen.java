package com.finndog.loottablepeeker.fabric.gui;

//? if fabric {

import com.finndog.loottablepeeker.PeekConfig;
import com.finndog.loottablepeeker.PeekMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The screen behind Mod Menu's config button.
 *
 * <p>Deliberately built from nothing but widgets, with no {@code render} override. 26.1 replaced the
 * whole screen render pipeline ({@code GuiGraphics} became {@code GuiGraphicsExtractor} and
 * {@code render} became {@code extractRenderState...}), but widgets draw themselves and
 * {@code init}/{@code addRenderableWidget}/{@code Button.builder} are unchanged from 1.20 through
 * 26.2. Keeping every label inside a button rather than drawing text is what lets this one class
 * serve every version with a single conditional.</p>
 *
 * <p>The settings are <b>server-side</b>: they live in the server's config and are applied there.
 * A client connected to a remote server cannot read or change them — there is no networking here by
 * design — so the buttons are disabled in that case and say so, rather than silently editing a local
 * file that would have no effect. In singleplayer the integrated server is this process, so editing
 * works normally.</p>
 */
public final class PeekConfigScreen extends Screen {

    private final Screen parent;
    private Button modeButton;
    private Button highlightButton;
    private Button styleButton;

    public PeekConfigScreen(Screen parent) {
        super(Component.literal("Loot Table Peeker"));
        this.parent = parent;
    }

    /**
     * True when this client is its own server <em>and</em> has a player, i.e. the config it edits is
     * the one actually in effect. Opened from the title screen there is no player and no server, so
     * the controls stay disabled.
     */
    private boolean editable() {
        return this.minecraft != null
                && this.minecraft.hasSingleplayerServer()
                && this.minecraft.player != null;
    }

    /** The local player's id, used for their personal highlight preference. Only valid if editable. */
    private java.util.UUID self() {
        return this.minecraft.player.getUUID();
    }

    @Override
    protected void init() {
        boolean editable = editable();
        int left = this.width / 2 - 110;
        int top = this.height / 3;

        this.modeButton = addRenderableWidget(Button.builder(modeLabel(editable), button -> {
            PeekMode[] modes = PeekMode.values();
            PeekConfig.setMode(modes[(PeekConfig.getMode().ordinal() + 1) % modes.length]);
            this.modeButton.setMessage(modeLabel(true));
        }).bounds(left, top, 220, 20).build());

        this.highlightButton = addRenderableWidget(Button.builder(highlightLabel(editable), button -> {
            // The highlight is per player, so this sets the local player's own preference rather
            // than the server default — matching what /lootpeek highlight does in game.
            PeekConfig.setHighlightEnabledFor(self(), !PeekConfig.isHighlightEnabledFor(self()));
            this.highlightButton.setMessage(highlightLabel(true));
        }).bounds(left, top + 24, 220, 20).build());

        this.styleButton = addRenderableWidget(Button.builder(styleLabel(editable), button -> {
            com.finndog.loottablepeeker.PeekHighlightStyle[] styles =
                    com.finndog.loottablepeeker.PeekHighlightStyle.values();
            int next = (PeekConfig.highlightStyleFor(self()).ordinal() + 1) % styles.length;
            PeekConfig.setHighlightStyleFor(self(), styles[next]);
            this.styleButton.setMessage(styleLabel(true));
        }).bounds(left, top + 48, 220, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + 60, top + 84, 100, 20).build());

        this.modeButton.active = editable;
        this.highlightButton.active = editable;
        this.styleButton.active = editable;
    }

    private static Component modeLabel(boolean editable) {
        if (!editable) return serverSideNotice();
        PeekMode mode = PeekConfig.getMode();
        return Component.literal("Mode: ").append(mode.displayName());
    }

    private Component highlightLabel(boolean editable) {
        if (!editable) return serverSideNotice();
        boolean on = PeekConfig.isHighlightEnabledFor(self());
        return Component.literal("Highlight loot containers: ")
                .append(Component.literal(on ? "on" : "off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private Component styleLabel(boolean editable) {
        if (!editable) return serverSideNotice();
        return Component.literal("Marker style: ")
                .append(PeekConfig.highlightStyleFor(self()).displayName());
    }

    /** Shown instead of a control when the settings in effect belong to a remote server. */
    private static Component serverSideNotice() {
        return Component.literal("Server-side — use /lootpeek").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) return;
        // 26.2 dropped Minecraft#setScreen in favour of setScreenAndShow; 26.1 and earlier have both.
        //? if >=26.2 {
        /*this.minecraft.setScreenAndShow(this.parent);
        *///?} else {
        this.minecraft.setScreen(this.parent);
        //?}
    }
}
//?}
