package com.finndog.loottablepeeker;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

/**
 * Loader-neutral interception logic. Each loader calls {@link #tryPeek} from its own
 * right-click-block event and cancels the interaction when it returns {@code true}: Fabric from
 * {@code UseBlockCallback}, NeoForge from {@code PlayerInteractEvent.RightClickBlock}.
 */
public final class ContainerInterceptHandler {

    private ContainerInterceptHandler() {}

    /**
     * @return {@code true} if the interaction was handled and the caller should cancel it, leaving
     *         the container's loot table unresolved.
     */
    public static boolean tryPeek(Player player, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;

        PeekMode mode = PeekConfig.getMode();
        if (mode == PeekMode.OFF) return false;
        if (player.isSpectator()) return false;
        if (!(player instanceof ServerPlayer serverPlayer)) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity container)) return false;

        // A null id means the loot has already been generated, so the container is an ordinary
        // chest now and should open normally.
        String tableId = LootTableAccess.idOf(container);
        if (tableId == null) return false;

        if (mode == PeekMode.PREVIEW) {
            LootPreviewMenu.open(serverPlayer, serverLevel, pos, container, tableId);
        } else {
            sendPeekTitle(serverPlayer, tableId);
        }
        return true;
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
