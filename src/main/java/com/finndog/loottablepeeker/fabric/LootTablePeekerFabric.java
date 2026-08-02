package com.finndog.loottablepeeker.fabric;

//? if fabric {

import com.finndog.loottablepeeker.ContainerInterceptHandler;
import com.finndog.loottablepeeker.LootTablePeeker;
import com.finndog.loottablepeeker.PeekCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

/** Fabric entrypoint (registered under {@code entrypoints.main} in {@code fabric.mod.json}). */
public final class LootTablePeekerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LootTablePeeker.init();

        // UseBlockCallback fires server-side before the block's own use() runs, so returning FAIL
        // cancels the interaction without a swing animation or screen-open packet.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
            ContainerInterceptHandler.tryPeek(player, world, hitResult.getBlockPos())
                ? InteractionResult.FAIL
                : InteractionResult.PASS);

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> PeekCommand.register(dispatcher));
    }
}
//?}
