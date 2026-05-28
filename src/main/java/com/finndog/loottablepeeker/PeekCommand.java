package com.finndog.loottablepeeker;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public final class PeekCommand {

    private PeekCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                literal("lootpeek")
                    .requires(source -> source.hasPermission(2))
                    .executes(PeekCommand::executeStatus)
                    .then(literal("on").executes(ctx -> executeSet(ctx, true)))
                    .then(literal("off").executes(ctx -> executeSet(ctx, false)))
            )
        );
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        boolean state = PeekConfig.isEnabled();
        ctx.getSource().sendSuccess(
            () -> Component.literal("Loot Table Peeker is " + (state ? "§aON" : "§cOFF")),
            false
        );
        return 1;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx, boolean enable) {
        PeekConfig.setEnabled(enable);
        ctx.getSource().sendSuccess(
            () -> Component.literal("Loot Table Peeker " + (enable ? "§aenabled" : "§cdisabled") + "§r (server-wide)"),
            true
        );
        return 1;
    }
}
