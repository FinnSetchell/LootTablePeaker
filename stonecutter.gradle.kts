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

    // No `replacements` block on purpose. 1.21.11's ResourceLocation -> Identifier rename does not
    // reach this mod: the only place a loot table id is named as a type is the 1.20.1 branch of
    // LootTableRef, which is below the rename. Everywhere else the id is handled as
    // ResourceKey<LootTable> and only ever stringified. See the warning in Moog's Wear and Tear's
    // stonecutter.gradle.kts about regex replacements breaking the IntelliJ project model.
}
