package com.finndog.loottablepeeker;

import net.minecraft.ChatFormatting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * Shared Brigadier builder for {@code /lootpeek}, used by both loaders so the command tree lives in
 * one place. Fabric registers it from {@code CommandRegistrationCallback.EVENT}; NeoForge from
 * {@code RegisterCommandsEvent}.
 *
 * <p>Permissions are applied per subcommand rather than on the root, because the tree splits in two:
 * changing the <em>mode</em> alters how containers behave for everyone and is op-gated, while the
 * highlight is a personal display preference any player sets for themselves. A {@code requires} on
 * the root would gate the whole subtree and lock ordinary players out of their own setting.</p>
 */
public final class PeekCommand {

    /** Vanilla op level required to run a "cheat"-style command. */
    private static final int PERMISSION_LEVEL = 2;

    private PeekCommand() {}

    /** The op check, in whichever form this version spells it. */
    private static Predicate<CommandSourceStack> opOnly() {
        // 1.21.11 replaced the integer op-level check with a PermissionSet;
        // LEVEL_GAMEMASTERS is the level-2 equivalent.
        //? if >=1.21.11 {
        /*return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        *///?} else {
        return source -> source.hasPermission(PERMISSION_LEVEL);
        //?}
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Readable by anyone: it only reports what the server is already doing to them.
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("lootpeek")
            .executes(PeekCommand::executeStatus);

        for (PeekMode mode : PeekMode.values()) {
            root.then(Commands.literal(mode.id()).requires(opOnly())
                .executes(ctx -> executeSet(ctx, mode)));
        }
        // Kept from before modes existed; "on" used to mean the title behaviour.
        root.then(Commands.literal("on").requires(opOnly())
            .executes(ctx -> executeSet(ctx, PeekMode.TITLE)));

        root.then(Commands.literal("highlight")
            // No permission: this is the caller's own display preference.
            .executes(PeekCommand::executeHighlightStatus)
            .then(Commands.literal("on").executes(ctx -> executeSetHighlight(ctx, true)))
            .then(Commands.literal("off").executes(ctx -> executeSetHighlight(ctx, false)))
            .then(Commands.literal("reset").executes(PeekCommand::executeResetHighlight))
            // Only ops may move the default, since it applies to everyone who has not chosen.
            .then(Commands.literal("default").requires(opOnly())
                .then(Commands.literal("on").executes(ctx -> executeSetDefault(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> executeSetDefault(ctx, false)))));

        dispatcher.register(root);
    }

    // ------------------------------------------------------------------ mode (op-gated)

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        PeekMode mode = PeekConfig.getMode();
        ctx.getSource().sendSuccess(
            () -> Component.literal("Loot Table Peeker mode: ")
                .append(mode.displayName())
                .append(Component.literal(" — " + mode.description())),
            false
        );
        return 1;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx, PeekMode mode) {
        PeekConfig.setMode(mode);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Loot Table Peeker mode set to ")
                .append(mode.displayName())
                .append(Component.literal(" (server-wide) — " + mode.description())),
            true
        );
        return 1;
    }

    // ------------------------------------------------------------------ highlight (per player)

    /** The calling player, or null with a message already sent if the source is not a player. */
    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal(
                "The loot highlight is a per-player setting, so it has to be set by a player. "
                    + "Use /lootpeek highlight default on|off to change the server default."));
        }
        return player;
    }

    private static int executeHighlightStatus(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;

        boolean on = PeekConfig.isHighlightEnabledFor(player.getUUID());
        boolean own = PeekConfig.hasHighlightOverride(player.getUUID());
        ctx.getSource().sendSuccess(
            () -> Component.literal("Your loot highlight is ")
                .append(state(on))
                .append(Component.literal(own ? "" : " (following the server default)")
                    .withStyle(ChatFormatting.GRAY)),
            false
        );
        return 1;
    }

    private static int executeSetHighlight(CommandContext<CommandSourceStack> ctx, boolean enable) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;

        PeekConfig.setHighlightEnabledFor(player.getUUID(), enable);
        // Not broadcast to ops: it changes nothing for anyone else.
        ctx.getSource().sendSuccess(
            () -> Component.literal("Your loot highlight is now ")
                .append(state(enable))
                .append(Component.literal(" — marks containers that still hold loot")),
            false
        );
        return 1;
    }

    private static int executeResetHighlight(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = requirePlayer(ctx);
        if (player == null) return 0;

        PeekConfig.clearHighlightFor(player.getUUID());
        boolean on = PeekConfig.isHighlightEnabledFor(player.getUUID());
        ctx.getSource().sendSuccess(
            () -> Component.literal("Your loot highlight now follows the server default: ")
                .append(state(on)),
            false
        );
        return 1;
    }

    private static int executeSetDefault(CommandContext<CommandSourceStack> ctx, boolean enable) {
        PeekConfig.setHighlightEnabled(enable);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Default loot highlight for players who have not chosen: ")
                .append(state(enable)),
            true
        );
        return 1;
    }

    private static Component state(boolean on) {
        return Component.literal(on ? "on" : "off")
            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
