package com.finndog.loottablepeeker.neoforge.gametest;

//? if >=1.21.5 && neoforge {
/*import com.finndog.loottablepeeker.fabric.gametest.PeekGameTests;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

*///?}

/**
 * Registers the shared {@link com.finndog.loottablepeeker.fabric.gametest.PeekGameTests} bodies with
 * NeoForge on 1.21.5 and above, where the annotation-driven framework no longer exists.
 *
 * <p>1.21.5 deleted vanilla's {@code @GameTest} and {@code GameTestRegistry}. Fabric shipped a
 * replacement annotation under the same simple name, so that side needed only an import swap;
 * NeoForge instead builds tests out of two registries — a {@code Consumer<GameTestHelper>} in
 * {@code Registries.TEST_FUNCTION} holding the body, and a {@code FunctionGameTestInstance} in
 * {@code Registries.TEST_INSTANCE} pairing that body with its structure, environment and timeout.
 * This class does both, so the test bodies themselves stay loader-agnostic and are written once.</p>
 *
 * <p>Below 1.21.5 NeoForge still uses {@code @GameTestHolder} on the test class itself and this
 * file compiles to nothing.</p>
 */
//? if >=1.21.5 && neoforge {
/*@Mod(NeoForgeGameTests.NAMESPACE)
public final class NeoForgeGameTests {

    /^ The test-only mod id; also the namespace of both the test functions and the structure. ^/
    public static final String NAMESPACE = "loot_table_peeker_gametest";

    /^ 8x8x8 all-air structure shipped in this source set's resources. ^/
    private static final String STRUCTURE = "empty";

    /^ Generous: these settle well inside a second, and a stall should fail loudly. ^/
    private static final int MAX_TICKS = 400;

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, NAMESPACE);

    /^ Names registered above, replayed when the instances are built. ^/
    private static final List<String> NAMES = new ArrayList<>();

    static {
        // One shared instance: the bodies hold no per-test state.
        PeekGameTests tests = new PeekGameTests();

        add("loot_table_access_round_trips_id_and_seed", tests::lootTableAccessRoundTripsIdAndSeed);
        add("plain_chest_has_no_loot_table_id", tests::plainChestHasNoLootTableId);
        add("known_table_resolves", tests::knownTableResolves);
        add("unknown_table_does_not_resolve", tests::unknownTableDoesNotResolve);
        add("highlight_check_does_not_resolve_loot_table", tests::highlightCheckDoesNotResolveLootTable);
        add("highlight_check_ignores_plain_chests", tests::highlightCheckIgnoresPlainChests);
        add("preview_mode_leaves_loot_table_unresolved", tests::previewModeLeavesLootTableUnresolved);
        add("title_mode_leaves_loot_table_unresolved", tests::titleModeLeavesLootTableUnresolved);
        add("off_mode_resolves_loot_table_as_vanilla", tests::offModeResolvesLootTableAsVanilla);
        add("plain_chest_is_not_intercepted", tests::plainChestIsNotIntercepted);
        add("preview_opens_a_menu_without_resolving", tests::previewOpensAMenuWithoutResolving);
    }

    private static void add(String name, Consumer<GameTestHelper> body) {
        FUNCTIONS.register(name, () -> body);
        NAMES.add(name);
    }

    public NeoForgeGameTests(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(NeoForgeGameTests::registerTests);
    }

    // Pairs every registered function with a test instance. The environment is registered with no
    // definitions, which yields an AllOf of nothing - a no-op environment, matching what the
    // annotation-driven side got by default.
    private static void registerTests(RegisterGameTestsEvent event) {
        // TestEnvironmentDefinition gained a type parameter in 26.1, which ripples through both the
        // Holder and the TestData that carries it. Only the declarations differ.
        //? if >=26.1 {
        /^Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        ^///?} else {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(id("default"));
        //?}
        for (String name : NAMES) {
            ResourceKey<Consumer<GameTestHelper>> function =
                    ResourceKey.create(Registries.TEST_FUNCTION, id(name));
            //? if >=26.1 {
            /^TestData<Holder<TestEnvironmentDefinition<?>>> data =
                    new TestData<>(environment, id(STRUCTURE), MAX_TICKS, 0, true);
            ^///?} else {
            TestData<Holder<TestEnvironmentDefinition>> data =
                    new TestData<>(environment, id(STRUCTURE), MAX_TICKS, 0, true);
            //?}
            event.registerTest(id(name), new FunctionGameTestInstance(function, data));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }
}
*///?}
