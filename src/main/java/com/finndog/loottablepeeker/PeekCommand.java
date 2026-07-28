package com.finndog.loottablepeeker;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public final class PeekCommand {

    private PeekCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> root = literal("lootpeek")
                .requires(source -> source.hasPermission(2))
                .executes(PeekCommand::executeStatus);

            for (PeekMode mode : PeekMode.values()) {
                root.then(literal(mode.id()).executes(ctx -> executeSet(ctx, mode)));
            }
            // Kept from before modes existed; "on" used to mean the title behaviour.
            root.then(literal("on").executes(ctx -> executeSet(ctx, PeekMode.TITLE)));

            dispatcher.register(root);
        });
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
