package com.finndog.loottablepeeker;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootTablePeeker implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("loot_table_peeker");

    @Override
    public void onInitialize() {
        PeekConfig.load();
        PeekCommand.register();
        ContainerInterceptHandler.register();
    }
}
