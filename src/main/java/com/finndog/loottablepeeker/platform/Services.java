package com.finndog.loottablepeeker.platform;

/**
 * Bridge between the loader-agnostic code and the per-loader {@link IPlatformHelper}
 * implementation.
 *
 * <p>The implementation is chosen at <b>build time</b> by Stonecutter rather than looked up at
 * runtime. Both loaders' sources live in this one tree, so the inactive loader's helper is
 * comment-neutralised and never compiled; fully-qualified names below avoid needing conditional
 * imports.</p>
 *
 * <p>A {@link java.util.ServiceLoader} lookup would not work here: under a single shared source
 * tree the two {@code META-INF/services/...} files would collide on one resource path.</p>
 */
public final class Services {

    /** The platform helper for the loader this jar is built for; never {@code null}. */
    public static final IPlatformHelper PLATFORM =
            /*? if fabric {*/ new com.finndog.loottablepeeker.fabric.platform.FabricPlatformHelper();
            /*?} elif neoforge *///new com.finndog.loottablepeeker.neoforge.platform.NeoForgePlatformHelper();

    private Services() {}
}
