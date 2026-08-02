package com.finndog.loottablepeeker.fabric.platform;

//? if fabric {

import com.finndog.loottablepeeker.platform.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
//?}
