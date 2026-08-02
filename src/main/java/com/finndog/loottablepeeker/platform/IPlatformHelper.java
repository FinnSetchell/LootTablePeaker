package com.finndog.loottablepeeker.platform;

import java.nio.file.Path;

/** The small slice of loader-specific behaviour this mod needs. */
public interface IPlatformHelper {

    /** The instance's config directory, where {@code loot_table_peeker.json} lives. */
    Path configDir();
}
