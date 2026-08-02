plugins {
    id("dev.kikugie.stonecutter")
}

// Must be a string literal — Stonecutter rewrites this line when switching versions, and an
// assignment from a variable would compile but silently never update.
stonecutter active "1.21.1-fabric"

// Registers each node's buildAndCollect with Stonecutter so it sequences them across the tree.
// `./gradlew buildAndCollect` (unqualified, so Gradle runs it in every node project) then builds
// the whole matrix in one invocation — what .github/workflows/release.yml uses. The older
// `registerChiseled` / `stonecutter.chiseled` API this replaces does not exist in 0.9.
stonecutter tasks {
    named("buildAndCollect")
}

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Lets `[fabric."1.21.1"]`-style tables in stonecutter.properties.toml resolve for this node.
    properties {
        tags(version, loader)
    }

    // Provides the `//? if fabric { ... //?}` / `//? if neoforge { ... //?}` conditionals that
    // keep both loaders' entrypoints in one source tree.
    constants {
        match(loader, "fabric", "neoforge")
    }

    // 1.21.5 removed vanilla's @GameTest annotation and the FabricGameTest interface with it;
    // Fabric replaced them with its own @GameTest, whose `structure` already defaults to the empty
    // structure the old code named explicitly. Defined once here rather than at every test method.
    // NeoForge has no equivalent of Fabric's bundled empty structure, so it uses the 8x8x8 all-air
    // one this repo ships.
    swaps["gametest"] = when {
        // NeoForge from 1.21.5 has no test annotation: NeoForgeGameTests registers these bodies
        // through RegisterGameTestsEvent instead. The slot still has to hold a real annotation —
        // swapping it to a bare comment consumed the following line, the method declaration, when
        // switching back — so it takes an inert one that exists on every version.
        current.parsed >= "1.21.5" && loader == "neoforge" -> "@SuppressWarnings(\"unused\")"
        current.parsed >= "1.21.5" -> "@GameTest"
        // Unqualified: @GameTestHolder already supplies the namespace, and naming it again here
        // yields "loot_table_peeker_gametest:loot_table_peeker_gametest:empty".
        loader == "neoforge" -> "@GameTest(template = \"empty\")"
        else -> "@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)"
    }

    // No `replacements` block on purpose. 1.21.11's ResourceLocation -> Identifier rename does not
    // reach this mod: the only place a loot table id is named as a type is the 1.20.1 branch of
    // LootTableRef, which is below the rename. Everywhere else the id is handled as
    // ResourceKey<LootTable> and only ever stringified. See the warning in Moog's Wear and Tear's
    // stonecutter.gradle.kts about regex replacements breaking the IntelliJ project model.
}
