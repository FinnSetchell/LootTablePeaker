package com.finndog.loottablepeeker.fabric.gui;

//? if fabric {

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu entrypoint, registered under {@code entrypoints.modmenu} in {@code fabric.mod.json}.
 *
 * <p>Mod Menu is a Fabric mod with no NeoForge equivalent, so this side is Fabric-only. It is also
 * a soft dependency: the entrypoint key is read by Mod Menu itself, so with Mod Menu absent nothing
 * ever loads this class and the mod runs unchanged — including on a dedicated server, where no
 * client class is touched at all.</p>
 */
public final class PeekModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PeekConfigScreen::new;
    }
}
//?}
