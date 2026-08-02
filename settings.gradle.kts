pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    // Applies the Loom variant matching each node's Minecraft version, which the matrix needs
    // once it straddles the 26.1 un-obfuscation boundary.
    id("dev.kikugie.loom-back-compat") version "0.4"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * Registers one node per loader for a Minecraft version, as `versions/{project}-{loader}`
         * built by `build.{loader}.gradle.kts`.
         *
         * The two-argument [version] call is load-bearing: passing `"1.21.1-fabric"` as a single
         * argument would make SemVer read `fabric` as a pre-release tag, ordering it
         * `1.21 < 1.21.1-fabric < 1.21.1` and silently breaking `//? if >=1.21.1` conditions.
         */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) {
                version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
            }
        }

        match("1.21.1", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.1.2", "fabric", "neoforge")
        match("26.2", "fabric", "neoforge")

        // 1.20.1 is Fabric-only. NeoForge had not split from Forge at 1.20.1, so there is no
        // `net.neoforged:neoforge` artifact for it — it ships as `net.neoforged:forge` and needs
        // the `moddev.legacyforge` plugin, which cannot share a buildscript with the modern
        // NeoForge nodes. Mirrors the same call in Moog's Wear and Tear.
        version("1.20.1-fabric", "1.20.1").buildscript("build.fabric.gradle.kts")

        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "LootTablePeeker"
