plugins {
    // Applies the Loom variant matching this node's Minecraft version.
    id("dev.kikugie.loom-back-compat")
}

// Do not set `group` — Loom derives coordinates itself.
// Fleet artifact convention: {ModName}-{loader}-{mc}-{version}.jar. Setting it via archivesName
// (rather than the jar task's archiveFileName) keeps the sources jar on the same stem.
version = sc.properties.get<String>("mod.version")
base.archivesName = "${sc.properties.get<String>("mod.archive_name")}-fabric-${sc.current.version}"

// Declared per version in stonecutter.properties.toml (1.20.1 -> 17, 1.21.x -> 21, 26.x -> 25).
val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java"))

// The gametest sources build a second, never-shipped mod (`loot_table_peeker_gametest`) so nothing
// test-only ends up in the released jar. It sees the mod's own classes via main's output.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // No-op on the un-obfuscated versions; applies Mojang mappings on the obfuscated ones.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

loom {
    runs {
        // Per-node game directory. A shared one would put every version's worlds, options and
        // config in the same folder, so opening a 1.21.1 world under 26.2 would upgrade it
        // irreversibly. `project.name` is the node name, e.g. "1.21.1-fabric".
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("../../run/${project.name}")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("../../run/${project.name}")
        }
        // Headless in-world tests: boots a dedicated server, runs every test in an empty structure,
        // writes a JUnit XML report and exits non-zero on failure.
        register("gameTest") {
            server()
            configName = "Fabric Game Test"
            ideConfigGenerated(true)
            source(gametest)
            runDir("build/gametest")
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.get().asFile}/gametest/report.xml")
        }
    }

    mods {
        register(sc.properties.get<String>("mod.id")) {
            sourceSet(sourceSets.main.get())
        }
        register("${sc.properties.get<String>("mod.id")}_gametest") {
            sourceSet(gametest)
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to sc.properties.get<String>("mod.version"),
            "description" to sc.properties.get<String>("mod.description"),
            "author" to sc.properties.get<String>("mod.author"),
            "license" to sc.properties.get<String>("mod.license"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to sc.properties.get<String>("deps.fabric_loader"),
            "pack_format" to sc.properties.get<String>("mod.pack_format"),
            "java" to requiredJava.majorVersion,
        )
        props.forEach { (k, v) -> inputs.property(k, v) }

        filesMatching(listOf("fabric.mod.json", "pack.mcmeta")) { expand(props) }

        // NeoForge-only metadata must not ship in the Fabric jar.
        exclude("META-INF/neoforge.mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${sc.properties.get<String>("mod.version")}"))
    }
}
