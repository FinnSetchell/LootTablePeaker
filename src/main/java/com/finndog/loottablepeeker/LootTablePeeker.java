package com.finndog.loottablepeeker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loader-neutral constants and bootstrap. Each loader's entrypoint calls {@link #init()} once,
 * then wires its own interaction and command events to {@link ContainerInterceptHandler} and
 * {@link PeekCommand}.
 */
public final class LootTablePeeker {

    public static final String MOD_ID = "loot_table_peeker";
    public static final String MOD_NAME = "Loot Table Peeker";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private LootTablePeeker() {}

    public static void init() {
        PeekConfig.load();
        LOGGER.info("{} loaded in {} mode", MOD_NAME, PeekConfig.getMode().id());
    }
}
