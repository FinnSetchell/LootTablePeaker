package com.finndog.loottablepeeker.fabric.gametest;

import com.finndog.loottablepeeker.LootTableAccess;
import com.finndog.loottablepeeker.PeekConfig;
import com.finndog.loottablepeeker.PeekMode;
//? if <1.21.5 && fabric {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
//?}
//? if <1.21.5 && neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}
// 1.21.5 removed vanilla's annotation; Fabric supplies the replacement under the same simple name.
// NeoForge supplies nothing — from 1.21.5 it registers tests through RegisterGameTestsEvent, so on
// that branch there is no annotation to import and the `gametest` swap blanks the annotation line.
//? if <1.21.5 {
import net.minecraft.gametest.framework.GameTest;
//?} elif fabric {
/*import net.fabricmc.fabric.api.gametest.v1.GameTest;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-world GameTests for Loot Table Peeker, run headless via {@code ./gradlew :<node>:runGameTest}.
 *
 * <p>The bodies are loader-agnostic and shared: Fabric registers them through the
 * {@code fabric-gametest} entrypoint of the {@code loot_table_peeker_gametest} test mod, and from
 * 1.21.5 NeoForge registers the same methods through
 * {@link com.finndog.loottablepeeker.neoforge.gametest.NeoForgeGameTests}.</p>
 *
 * <p>The point of running these on every node is {@link LootTableAccess}, which is where the loot
 * table API differs across versions. A real server is running here, so the tests drive the actual
 * server-side right-click path (see {@link #rightClick}) rather than calling the mod's internals —
 * which means they also prove each loader's event wiring cancels the interaction for real.</p>
 */
//? if <1.21.5 && fabric {
public class PeekGameTests implements FabricGameTest {
//?} elif <1.21.5 && neoforge {
/*@GameTestHolder("loot_table_peeker_gametest")
@PrefixGameTestTemplate(false)
public class PeekGameTests {
*///?} else {
/*public class PeekGameTests {
*///?}

    /** A vanilla table that exists on every supported version. */
    private static final String KNOWN_TABLE = "minecraft:chests/simple_dungeon";
    private static final String UNKNOWN_TABLE = "loot_table_peeker_gametest:does_not_exist";
    private static final long FIXED_SEED = 123456789L;

    /** Somewhere inside the 8x8x8 test structure, clear of the floor. */
    private static final BlockPos CHEST = new BlockPos(1, 1, 1);

    // ------------------------------------------------------------------ helpers

    /** Places a chest and points it at {@code tableId} without generating its loot. */
    private static RandomizableContainerBlockEntity chestWithTable(GameTestHelper helper, String tableId, long seed) {
        helper.setBlock(CHEST, Blocks.CHEST);
        RandomizableContainerBlockEntity container =
                (RandomizableContainerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CHEST));
        setLootTable(container, tableId, seed);
        return container;
    }

    /**
     * Right-clicks the block bare-handed the way the server does for a real player.
     *
     * <p>Deliberately not {@code GameTestHelper#useBlock}: that calls the block state's own use
     * method directly, bypassing {@link net.minecraft.server.level.ServerPlayerGameMode} — and with
     * it both Fabric's {@code UseBlockCallback} and NeoForge's {@code RightClickBlock}, the very
     * hooks these tests exist to cover. Going through {@code gameMode.useItemOn} fires them, and
     * that signature is unchanged from 1.20.1 through 26.2.</p>
     */
    private static void rightClick(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(),
                player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    /**
     * 1.20.1 sets the id and seed together and names tables with a bare ResourceLocation; 1.21 split
     * the setters and moved to ResourceKey; 1.21.11 renamed ResourceLocation to Identifier.
     */
    private static void setLootTable(RandomizableContainerBlockEntity container, String tableId, long seed) {
        //? if >=1.21.11 {
        /*container.setLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.Identifier.parse(tableId)));
        container.setLootTableSeed(seed);
        *///?} elif >=1.21 {
        container.setLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.parse(tableId)));
        container.setLootTableSeed(seed);
        //?} else {
        /*container.setLootTable(new net.minecraft.resources.ResourceLocation(tableId), seed);
        *///?}
    }

    // ------------------------------------------------------------------ LootTableAccess

    /**
     * The version-conditional accessors must round-trip what was just written. This is the test that
     * actually earns its keep on 1.20.1, where the id and seed are read back out of the block
     * entity's serialised form because the fields have no public getters.
     */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void lootTableAccessRoundTripsIdAndSeed(GameTestHelper helper) {
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);

        helper.assertTrue(KNOWN_TABLE.equals(LootTableAccess.idOf(container)),
                "idOf must return the id that was just set, got " + LootTableAccess.idOf(container));
        helper.assertTrue(LootTableAccess.seedOf(container) == FIXED_SEED,
                "seedOf must return the seed that was just set, got " + LootTableAccess.seedOf(container));
        helper.succeed();
    }

    /** A chest with no loot table at all must report no id, rather than an empty string. */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void plainChestHasNoLootTableId(GameTestHelper helper) {
        helper.setBlock(CHEST, Blocks.CHEST);
        RandomizableContainerBlockEntity container =
                (RandomizableContainerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CHEST));

        helper.assertTrue(LootTableAccess.idOf(container) == null,
                "a chest with no loot table must report a null id");
        helper.succeed();
    }

    /** A real vanilla table must resolve, or the preview would wrongly report it as missing. */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void knownTableResolves(GameTestHelper helper) {
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);
        LootTable table = LootTableAccess.tableOf(helper.getLevel().getServer(), container);

        helper.assertTrue(table != null, KNOWN_TABLE + " must resolve to a real loot table");
        helper.succeed();
    }

    /**
     * An unregistered id must resolve to null, which is what drives the preview's "Loot table not
     * found" marker. Getting this wrong turns a typo into a chest that merely looks empty.
     */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void unknownTableDoesNotResolve(GameTestHelper helper) {
        RandomizableContainerBlockEntity container = chestWithTable(helper, UNKNOWN_TABLE, FIXED_SEED);
        LootTable table = LootTableAccess.tableOf(helper.getLevel().getServer(), container);

        helper.assertTrue(table == null, UNKNOWN_TABLE + " must not resolve to a loot table");
        helper.succeed();
    }

    /**
     * The highlighter asks this of every nearby container once a second, so it must both answer
     * correctly and — critically — leave the loot table alone. Probing a container through the
     * {@code Container} interface ({@code isEmpty}, {@code getItem}) would unpack its loot instead,
     * which would have this cosmetic feature quietly destroying exactly what the mod protects.
     */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void highlightCheckDoesNotResolveLootTable(GameTestHelper helper) {
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);

        helper.assertTrue(LootTableAccess.hasLootTable(container),
                "a container with an unresolved loot table must report having one");
        // Ask repeatedly: the highlighter runs every second for as long as a player is nearby.
        for (int i = 0; i < 5; i++) {
            LootTableAccess.hasLootTable(container);
        }

        helper.assertTrue(KNOWN_TABLE.equals(LootTableAccess.idOf(container)),
                "the highlight check must not resolve the loot table, but the container reported "
                        + LootTableAccess.idOf(container));
        helper.succeed();
    }

    /** The cue must not appear on ordinary storage. */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void highlightCheckIgnoresPlainChests(GameTestHelper helper) {
        helper.setBlock(CHEST, Blocks.CHEST);
        RandomizableContainerBlockEntity container =
                (RandomizableContainerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CHEST));

        helper.assertFalse(LootTableAccess.hasLootTable(container),
                "a chest with no loot table must not be highlighted");
        helper.succeed();
    }

    // ------------------------------------------------------------------ interception

    /**
     * The whole point of the mod: with peeking on, using a loot chest must leave its loot table
     * unresolved, so it can be opened again and again while testing.
     */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void previewModeLeavesLootTableUnresolved(GameTestHelper helper) {
        PeekConfig.setMode(PeekMode.PREVIEW);
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        rightClick(helper, player, CHEST);

        helper.assertTrue(KNOWN_TABLE.equals(LootTableAccess.idOf(container)),
                "the loot table must survive a peek, but the container reported "
                        + LootTableAccess.idOf(container));
        helper.succeed();
    }

    /** Title mode cancels the interaction the same way, just without the GUI. */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void titleModeLeavesLootTableUnresolved(GameTestHelper helper) {
        PeekConfig.setMode(PeekMode.TITLE);
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        rightClick(helper, player, CHEST);

        helper.assertTrue(KNOWN_TABLE.equals(LootTableAccess.idOf(container)),
                "the loot table must survive a peek, but the container reported "
                        + LootTableAccess.idOf(container));
        helper.succeed();
    }

    /**
     * The counterpart that proves the tests above are measuring something: with peeking off, the
     * same interaction must resolve the loot table exactly as vanilla does.
     */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void offModeResolvesLootTableAsVanilla(GameTestHelper helper) {
        PeekConfig.setMode(PeekMode.OFF);
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        rightClick(helper, player, CHEST);

        helper.assertTrue(LootTableAccess.idOf(container) == null,
                "with peeking off the container must generate its loot and clear the table");
        helper.succeed();
    }

    /**
     * Peeking must not interfere with ordinary containers — only ones still holding an unresolved
     * loot table.
     */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void plainChestIsNotIntercepted(GameTestHelper helper) {
        PeekConfig.setMode(PeekMode.PREVIEW);
        helper.setBlock(CHEST, Blocks.CHEST);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        rightClick(helper, player, CHEST);

        helper.assertTrue(player.containerMenu != player.inventoryMenu,
                "a chest with no loot table must still open normally while peeking");
        helper.succeed();
    }

    /** The preview must open a menu, and still leave the container's loot table alone. */
    //$ gametest
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void previewOpensAMenuWithoutResolving(GameTestHelper helper) {
        PeekConfig.setMode(PeekMode.PREVIEW);
        RandomizableContainerBlockEntity container = chestWithTable(helper, KNOWN_TABLE, FIXED_SEED);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        rightClick(helper, player, CHEST);

        helper.assertTrue(player.containerMenu != player.inventoryMenu,
                "preview mode must open a menu for the player");
        helper.assertTrue(KNOWN_TABLE.equals(LootTableAccess.idOf(container)),
                "opening the preview must not resolve the loot table");
        helper.succeed();
    }
}
