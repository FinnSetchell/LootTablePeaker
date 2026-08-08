package com.finndog.loottablepeeker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Marks containers that still hold an unresolved loot table with a small particle, so they can be
 * picked out at a glance without opening anything.
 *
 * <p>Particles are used rather than a glow or outline because they are the only spatial cue a
 * server can push to an unmodified client. Nothing here requires the mod on the client side.</p>
 *
 * <p>Tuned to stay out of the way: one particle per container per second, and only for containers
 * near a player. {@link #MAX_PER_CYCLE} keeps a room full of loot chests from turning into a green
 * haze — and, more importantly, bounds the work done per cycle.</p>
 */
public final class LootHighlighter {

    /** One sweep a second. Frequent enough to feel live, sparse enough not to nag. */
    private static final int INTERVAL_TICKS = 20;
    /** Chunks either side of a player's own; 3 is a little under a 64-block box. */
    private static final int CHUNK_RADIUS = 3;
    /** Per level, per sweep. A dense loot room stops being useful as a cue well before this. */
    private static final int MAX_PER_CYCLE = 64;

    private static int ticks;

    private LootHighlighter() {}

    /** Called from each loader's server tick event. */
    public static void tick(MinecraftServer server) {
        if (!PeekConfig.isHighlightEnabled()) return;
        if (++ticks < INTERVAL_TICKS) return;
        ticks = 0;

        for (ServerLevel level : server.getAllLevels()) {
            highlight(level);
        }
    }

    private static void highlight(ServerLevel level) {
        if (level.players().isEmpty()) return;

        // Players standing together share chunks; visiting each chunk once keeps a crowd from
        // multiplying both the scan cost and the particle count.
        Set<Long> visited = new HashSet<>();
        int spawned = 0;

        for (ServerPlayer player : level.players()) {
            int centreX = player.getBlockX() >> 4;
            int centreZ = player.getBlockZ() >> 4;
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    int chunkX = centreX + dx;
                    int chunkZ = centreZ + dz;
                    if (!visited.add(packChunk(chunkX, chunkZ))) continue;

                    // getChunkNow rather than getChunk: a cosmetic cue must never force a load.
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) continue;

                    for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                        if (!(entry.getValue() instanceof RandomizableContainerBlockEntity container)) continue;
                        if (!LootTableAccess.hasLootTable(container)) continue;

                        mark(level, entry.getKey());
                        if (++spawned >= MAX_PER_CYCLE) return;
                    }
                }
            }
        }
    }

    /**
     * Packs a chunk coordinate pair into a set key.
     *
     * <p>Done by hand rather than with {@code ChunkPos}: 26.1 turned that class into a record, so
     * the {@code x}/{@code z} fields became private accessors and {@code asLong} was renamed to
     * {@code pack}. Deriving the chunk coordinate by shifting and packing the pair here is plain
     * arithmetic, identical on every supported version, and keeps this file conditional-free.</p>
     */
    private static long packChunk(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static void mark(ServerLevel level, BlockPos pos) {
        // Just above the block, jittered slightly so a row of chests does not read as a straight
        // line of identical dots. Speed 0 keeps the particle where it is put.
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                1, 0.15, 0.05, 0.15, 0.0);
    }
}
