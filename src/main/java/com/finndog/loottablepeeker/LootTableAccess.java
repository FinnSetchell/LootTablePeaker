package com.finndog.loottablepeeker;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

//? if >=1.21 {
import net.minecraft.core.registries.Registries;
//?}

/**
 * Every place the loot table API differs across the supported Minecraft versions, in one file.
 *
 * <p>Only version-stable types cross this boundary — {@link String}, {@link LootTable},
 * {@code long} — so callers need no conditionals of their own. In particular the id is never
 * handed out as a {@code ResourceLocation}: that type is renamed to {@code Identifier} in 1.21.11,
 * and keeping it inside this file means the rename never reaches the rest of the mod.</p>
 *
 * <p>The two shapes being bridged: 1.21+ exposes a {@code ResourceKey<LootTable>} through public
 * getters and resolves it against {@code MinecraftServer#reloadableRegistries()}, while on 1.20.1
 * the block entity's {@code lootTable}/{@code lootTableSeed} fields are {@code protected} with no
 * accessors at all. Rather than add an access widener for one version, 1.20.1 reads them back out
 * of the block entity's own serialised form — {@code saveWithoutMetadata()} writes both tags
 * exactly when the loot table is still unresolved — and resolves through
 * {@code MinecraftServer#getLootData()}.</p>
 */
public final class LootTableAccess {

    /** Seed sentinel meaning "roll with a fresh random seed". */
    //? if >=1.21 {
    public static final long RANDOMIZE_SEED = LootTable.RANDOMIZE_SEED;
    //?} else {
    /*public static final long RANDOMIZE_SEED = 0L;
    *///?}

    private LootTableAccess() {}

    /**
     * The container's unresolved loot table id, or {@code null} if it has none — which is also how
     * a container reads once its loot has already been generated.
     */
    public static String idOf(RandomizableContainerBlockEntity container) {
        // 1.21.11 renamed ResourceKey#location to #identifier, alongside ResourceLocation itself.
        //? if >=1.21.11 {
        /*var key = container.getLootTable();
        return key == null ? null : key.identifier().toString();
        *///?} elif >=1.21 {
        var key = container.getLootTable();
        return key == null ? null : key.location().toString();
        //?} else {
        /*net.minecraft.nbt.CompoundTag tag = container.saveWithoutMetadata();
        String id = tag.getString(RandomizableContainerBlockEntity.LOOT_TABLE_TAG);
        return id.isEmpty() ? null : id;
        *///?}
    }

    /**
     * Whether the container still holds an unresolved loot table, without building its id.
     *
     * <p>Used by {@link LootHighlighter}, which asks this of every container near a player once a
     * second. Note that the obvious cheap pre-filter — skipping non-empty containers — would be a
     * disaster here: {@code RandomizableContainerBlockEntity} overrides {@code isEmpty()},
     * {@code getItem()} and the rest to call {@code unpackLootTable} first, so probing a container
     * through the {@code Container} interface generates its loot. Everything on this path has to go
     * through the loot table fields directly.</p>
     */
    public static boolean hasLootTable(RandomizableContainerBlockEntity container) {
        //? if >=1.21 {
        return container.getLootTable() != null;
        //?} else {
        /*// 1.20.1 has no getter, so this costs a block entity serialisation per call. That is
        // cheap for a container that still has its loot table (vanilla writes the two loot tags and
        // skips the item list entirely), and bounded by the scan caps in LootHighlighter otherwise.
        return container.saveWithoutMetadata().contains(RandomizableContainerBlockEntity.LOOT_TABLE_TAG);
        *///?}
    }

    /**
     * Resolves the container's loot table, or returns {@code null} when nothing is registered under
     * that id. Distinguishing "not registered" from "registered but rolls nothing" is what lets the
     * preview explain an empty result instead of just showing a bare chest.
     */
    public static LootTable tableOf(MinecraftServer server, RandomizableContainerBlockEntity container) {
        //? if >=1.21 {
        var key = container.getLootTable();
        if (key == null) return null;
        // Membership is asked of the registry directly rather than by comparing the result against
        // LootTable.EMPTY: a table can be legitimately empty (minecraft:empty is a real, registered
        // table that containers do use), and that must not read as missing.
        var registry = server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
        if (registry.isEmpty() || registry.get().get(key).isEmpty()) return null;
        return server.reloadableRegistries().getLootTable(key);
        //?} else {
        /*String id = idOf(container);
        if (id == null) return null;
        // getElementOptional is the 1.20.1 equivalent of the registry membership check above:
        // absent means unregistered, which getLootTable would have flattened into LootTable.EMPTY.
        return server.getLootData().getElementOptional(
                net.minecraft.world.level.storage.loot.LootDataType.TABLE,
                new net.minecraft.resources.ResourceLocation(id)).orElse(null);
        *///?}
    }

    /** The container's stored loot table seed; {@link #RANDOMIZE_SEED} means "roll fresh". */
    public static long seedOf(RandomizableContainerBlockEntity container) {
        //? if >=1.21 {
        return container.getLootTableSeed();
        //?} else {
        /*// Absent tag reads as 0, which is RANDOMIZE_SEED — the same thing it means on 1.21+.
        return container.saveWithoutMetadata().getLong(RandomizableContainerBlockEntity.LOOT_TABLE_SEED_TAG);
        *///?}
    }
}
