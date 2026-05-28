package com.finndog.loottablepeeker;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

public final class ContainerInterceptHandler {

    private ContainerInterceptHandler() {}

    public static void register() {
        // UseBlockCallback fires server-side before the block's use() method runs,
        // giving us a chance to cancel the interaction entirely.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(world instanceof net.minecraft.server.level.ServerLevel)) return InteractionResult.PASS;
            if (!PeekConfig.isEnabled()) return InteractionResult.PASS;
            if (player.isSpectator()) return InteractionResult.PASS;

            BlockEntity be = world.getBlockEntity(hitResult.getBlockPos());
            if (!(be instanceof RandomizableContainerBlockEntity container)) return InteractionResult.PASS;

            // getLootTable() returns null once loot has been generated,
            // so this only triggers on containers still holding an ungenerated loot table.
            ResourceKey<LootTable> lootTableKey = container.getLootTable();
            if (lootTableKey == null) return InteractionResult.PASS;

            String tableId = lootTableKey.location().toString();
            sendPeekTitle((ServerPlayer) player, tableId);

            // FAIL cancels the interaction without a swing animation or screen-open packet.
            return InteractionResult.FAIL;
        });
    }

    private static void sendPeekTitle(ServerPlayer player, String tableId) {
        Component title = Component.literal("§6⚠ Loot Table");
        Component subtitle = Component.literal("§b" + tableId);

        // Timing: 10 ticks fade in (~0.5s), 70 ticks stay (~3.5s), 20 ticks fade out (~1s).
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}
