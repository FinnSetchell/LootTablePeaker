package com.finndog.loottablepeeker.neoforge;

//? if neoforge {

/*import com.finndog.loottablepeeker.ContainerInterceptHandler;
import com.finndog.loottablepeeker.LootHighlighter;
import com.finndog.loottablepeeker.LootTablePeeker;
import com.finndog.loottablepeeker.PeekCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/^*
 * NeoForge entrypoint. Interaction and command events live on the game bus, not the mod bus, so
 * they are registered against NeoForge.EVENT_BUS rather than the bus handed to the constructor.
 ^/
@Mod(LootTablePeeker.MOD_ID)
public final class LootTablePeekerNeoForge {

    public LootTablePeekerNeoForge(IEventBus modEventBus) {
        LootTablePeeker.init();
        NeoForge.EVENT_BUS.addListener(LootTablePeekerNeoForge::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(LootTablePeekerNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(LootTablePeekerNeoForge::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        LootHighlighter.tick(event.getServer());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ContainerInterceptHandler.tryPeek(event.getEntity(), event.getLevel(), event.getPos())) {
            // Cancelling here stops vanilla consulting the block's own use handler, which is what
            // would otherwise open the container and resolve its loot table.
            event.setCanceled(true);
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        PeekCommand.register(event.getDispatcher());
    }
}
*///?}
