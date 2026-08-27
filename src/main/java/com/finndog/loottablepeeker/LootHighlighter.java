package com.finndog.loottablepeeker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
//? if >=1.21 {
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
//?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Draws a box of particles around containers that still hold an unresolved loot table.
 *
 * <p>Particles rather than a rendered outline because they are the only spatial cue a server can push
 * to an unmodified client — every player sees this, with or without the mod installed. 26.2 also
 * removed the immediate-mode box helpers entirely (there is no {@code ShapeRenderer}; outlines are
 * submitted as render states to a deferred pipeline), so a rendered outline would have meant three
 * separate implementations across the matrix. A particle box needs none.</p>
 *
 * <p>Two styles, chosen per player — see {@link PeekHighlightStyle}. {@code FAINT} marks every
 * container nearby with just its eight corners, which stays legible in a room full of chests and is
 * cheap enough to repeat once a second. {@code CROSSHAIR} draws a full wireframe, but only on the
 * container the player is actually looking at, so it costs one box no matter how much loot is
 * around.</p>
 */
public final class LootHighlighter {

    /** Faint sweeps are ambient, so once a second is plenty. */
    private static final int FAINT_INTERVAL_TICKS = 20;
    /** The crosshair box tracks where you look, so it needs to keep up. */
    private static final int CROSSHAIR_INTERVAL_TICKS = 5;
    /** Chunks either side of a player's own; 3 is a little under a 64-block box. */
    private static final int CHUNK_RADIUS = 3;
    /**
     * Containers marked per faint sweep. Lower than a single-particle cue would need, because each
     * container now costs eight particle packets rather than one.
     */
    private static final int MAX_PER_CYCLE = 24;
    /** How far the crosshair ray reaches; a little beyond creative reach. */
    private static final double CROSSHAIR_REACH = 6.0;
    /** Spacing along each edge of the crosshair box. Small enough to read as a solid line. */
    private static final double DENSE_STEP = 0.25;
    /** Keeps the box just clear of the block face so it does not sit inside the texture. */
    private static final double PADDING = 0.02;

    private static int ticks;

    private LootHighlighter() {}

    /** Called from each loader's server tick event. */
    public static void tick(MinecraftServer server) {
        // Wrapped rather than left to grow, so the modulo stays meaningful indefinitely.
        ticks = (ticks + 1) % (FAINT_INTERVAL_TICKS * CROSSHAIR_INTERVAL_TICKS);
        boolean faintDue = ticks % FAINT_INTERVAL_TICKS == 0;
        boolean crosshairDue = ticks % CROSSHAIR_INTERVAL_TICKS == 0;
        if (!faintDue && !crosshairDue) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                // Checked per player: the cue is a personal preference, so one player enabling it
                // must not put particles on anyone else's screen.
                if (!PeekConfig.isHighlightEnabledFor(player.getUUID())) continue;

                if (PeekConfig.highlightStyleFor(player.getUUID()) == PeekHighlightStyle.CROSSHAIR) {
                    if (crosshairDue) markLookedAt(level, player);
                } else if (faintDue) {
                    markNearby(level, player);
                }
            }
        }
    }

    /** Corners only, on every loot container in range of this player. */
    private static void markNearby(ServerLevel level, ServerPlayer player) {
        Set<Long> visited = new HashSet<>();
        int spawned = 0;

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
                    BlockEntity be = entry.getValue();
                    if (be instanceof RandomizableContainerBlockEntity rcbe) {
                        if (!LootTableAccess.hasLootTable(rcbe)) continue;
                    //? if >=1.21 {
                    } else if (be instanceof DecoratedPotBlockEntity pot) {
                        if (!LootTableAccess.hasLootTable(pot)) continue;
                    //?}
                    } else {
                        continue;
                    }
                    drawCorners(level, player, boxOf(level, entry.getKey()));
                    if (++spawned >= MAX_PER_CYCLE) return;
                }
            }
        }
    }

    /** A full wireframe, but only on the container under the player's crosshair. */
    private static void markLookedAt(ServerLevel level, ServerPlayer player) {
        HitResult hit = player.pick(CROSSHAIR_REACH, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RandomizableContainerBlockEntity rcbe) {
            if (!LootTableAccess.hasLootTable(rcbe)) return;
        //? if >=1.21 {
        } else if (be instanceof DecoratedPotBlockEntity pot) {
            if (!LootTableAccess.hasLootTable(pot)) return;
        //?}
        } else {
            return;
        }

        drawWireframe(level, player, boxOf(level, pos));
    }

    /**
     * The block's own outline shape rather than a full cube, so the box hugs a chest instead of
     * floating around it. Padded slightly to sit clear of the block face.
     */
    private static AABB boxOf(ServerLevel level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        AABB local = shape.isEmpty() ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : shape.bounds();
        return local.move(pos.getX(), pos.getY(), pos.getZ()).inflate(PADDING);
    }

    private static void drawCorners(ServerLevel level, ServerPlayer player, AABB box) {
        for (double x : new double[]{box.minX, box.maxX}) {
            for (double y : new double[]{box.minY, box.maxY}) {
                for (double z : new double[]{box.minZ, box.maxZ}) {
                    mark(level, player, x, y, z);
                }
            }
        }
    }

    /** All twelve edges, stepped finely enough to read as a continuous line. */
    private static void drawWireframe(ServerLevel level, ServerPlayer player, AABB box) {
        for (double y : new double[]{box.minY, box.maxY}) {
            for (double z : new double[]{box.minZ, box.maxZ}) {
                for (double x = box.minX; x <= box.maxX; x += DENSE_STEP) mark(level, player, x, y, z);
            }
            for (double x : new double[]{box.minX, box.maxX}) {
                for (double z = box.minZ; z <= box.maxZ; z += DENSE_STEP) mark(level, player, x, y, z);
            }
        }
        for (double x : new double[]{box.minX, box.maxX}) {
            for (double z : new double[]{box.minZ, box.maxZ}) {
                for (double y = box.minY; y <= box.maxY; y += DENSE_STEP) mark(level, player, x, y, z);
            }
        }
    }

    /**
     * Packs a chunk coordinate pair into a set key.
     *
     * <p>Done by hand rather than with {@code ChunkPos}: 26.1 turned that class into a record, so the
     * {@code x}/{@code z} fields became private accessors and {@code asLong} was renamed to
     * {@code pack}. Shifting and packing the pair here is plain arithmetic, identical on every
     * supported version, and keeps this file free of that churn.</p>
     */
    private static long packChunk(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Sends one particle to a single player.
     *
     * <p>Both boolean flags are false on purpose. {@code longDistance} is unnecessary — nothing is
     * marked beyond a few chunks anyway — and forcing visibility would override the player's own
     * particle setting, which an opt-in cue has no business doing. Zero offset and zero speed keep
     * each particle exactly where it is put, which is what makes the box read as an edge rather than
     * a cloud.</p>
     */
    private static void mark(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        // 1.21.10 added an "always visible" flag to the targeted overload.
        //? if >=1.21.10 {
        /*level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false,
                x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        *///?} else {
        level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false,
                x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        //?}
    }
}
