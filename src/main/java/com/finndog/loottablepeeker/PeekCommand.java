package com.finndog.loottablepeeker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Shared Brigadier builder for {@code /lootpeek}, used by both loaders so the command tree lives in
 * one place. Fabric registers it from {@code CommandRegistrationCallback.EVENT}; NeoForge from
 * {@code RegisterCommandsEvent}.
 */
public final class PeekCommand {

    /** Vanilla op level required to run a "cheat"-style command. */
    private static final int PERMISSION_LEVEL = 2;

    private PeekCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("lootpeek")
            // 1.21.11 replaced the integer op-level check with a PermissionSet;
            // LEVEL_GAMEMASTERS is the level-2 equivalent.
            //? if >=1.21.11 {
            /*.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            *///?} else {
            .requires(source -> source.hasPermission(PERMISSION_LEVEL))
            //?}
            .executes(PeekCommand::executeStatus);

        for (PeekMode mode : PeekMode.values()) {
            root.then(Commands.literal(mode.id()).executes(ctx -> executeSet(ctx, mode)));
        }
        // Kept from before modes existed; "on" used to mean the title behaviour.
        root.then(Commands.literal("on").executes(ctx -> executeSet(ctx, PeekMode.TITLE)));

        dispatcher.register(root);
    }

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
}
