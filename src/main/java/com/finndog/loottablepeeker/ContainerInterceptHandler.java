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
//? if >=1.21 {
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
//?}

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

        if (be instanceof RandomizableContainerBlockEntity container) {
            // A null id means the loot has already been generated, so the container is an ordinary
            // chest now and should open normally.
            String tableId = LootTableAccess.idOf(container);
            if (tableId == null) return false;
            if (mode == PeekMode.PREVIEW) {
                LootPreviewMenu.open(serverPlayer, serverLevel, pos, container,
                    container.getContainerSize(), tableId);
            } else {
                sendPeekTitle(serverPlayer, tableId);
            }
            return true;
        }

        //? if >=1.21 {
        // Decorated pots have no unresolved-loot-table state to key on, so only intercept when
        // the player is sneaking — plain right-click still opens the pot normally.
        if (be instanceof DecoratedPotBlockEntity pot && player.isShiftKeyDown()) {
            String tableId = LootTableAccess.idOf(pot);
            if (tableId != null) {
                // Pot has an unresolved loot table — show the same loot preview as a chest.
                if (mode == PeekMode.PREVIEW) {
                    LootPreviewMenu.open(serverPlayer, serverLevel, pos, pot, 1, tableId);
                } else {
                    sendPeekTitle(serverPlayer, tableId);
                }
            } else {
                // Loot already resolved (or was never present) — show the stored item.
                if (mode == PeekMode.PREVIEW) {
                    PotPeekMenu.open(serverPlayer, pot);
                } else {
                    sendPotTitle(serverPlayer, pot);
                }
            }
            return true;
        }
        //?}

        return false;
    }

    private static void sendPeekTitle(ServerPlayer player, String tableId) {
        Component title = Component.literal("§6⚠ Loot Table");
        Component subtitle = Component.literal("§b" + tableId);

        // Timing: 10 ticks fade in (~0.5s), 70 ticks stay (~3.5s), 20 ticks fade out (~1s).
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    //? if >=1.21 {
    private static void sendPotTitle(ServerPlayer player, DecoratedPotBlockEntity pot) {
        ItemStack item = pot.getItem(0);
        Component title = Component.literal("§6⚠ Decorated Pot");
        Component subtitle = item.isEmpty()
            ? Component.literal("§7Empty")
            : item.getHoverName().copy().withStyle(ChatFormatting.AQUA);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
    //?}
}
