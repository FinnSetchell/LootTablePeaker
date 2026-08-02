plugins {
    id("net.neoforged.moddev") version "2.0.140"
    id("neoforge-mutex")
}

// Fleet artifact convention: {ModName}-{loader}-{mc}-{version}.jar — see build.fabric.gradle.kts.
version = sc.properties.get<String>("mod.version")
base.archivesName = "${sc.properties.get<String>("mod.archive_name")}-neoforge-${sc.current.version}"

// Declared per version in stonecutter.properties.toml (1.21.x -> 21, 26.x -> 25).
val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java"))

// The in-world tests build as a second, never-shipped mod, mirroring the Fabric side. The `jar`
// task only packages `main`, so nothing here reaches the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

// NeoForge checks the EULA at the very top of the server's main and returns early if it is not
// accepted — the process then exits 0 having run NO tests, which reads as a green build. Writing
// the file up front removes that silent-pass trap.
val writeGameTestEula = tasks.register("writeGameTestEula") {
    val eula = layout.buildDirectory.file("gametest/eula.txt")
    outputs.file(eula)
    doLast {
        eula.get().asFile.apply {
            parentFile.mkdirs()
            writeText("eula=true\n")
        }
    }
}

neoForge {
    version = sc.properties.get<String>("deps.neo_loader")

    // Without this the gametest source set has no Minecraft on its classpath.
    addModdingDependenciesTo(gametest)

    mods {
        register(sc.properties.get<String>("mod.id")) {
            sourceSet(sourceSets.main.get())
        }
        register("${sc.properties.get<String>("mod.id")}_gametest") {
            sourceSet(gametest)
        }
    }

    runs {
        // Per-node game directory — see the matching comment in build.fabric.gradle.kts.
        register("client") {
            client()
            gameDirectory = rootProject.file("run/${project.name}")
        }
        register("server") {
            server()
            gameDirectory = rootProject.file("run/${project.name}")
        }
        // Headless in-world tests: `./gradlew :<node>:runGameTest`. GameTestServer exits with the
        // number of failed required tests, so a failure fails the Gradle build.
        register("gameTest") {
            type = "gameTestServer"
            sourceSet = gametest
            gameDirectory = layout.buildDirectory.dir("gametest").get().asFile
            disableIdeRun()
            taskBefore(writeGameTestEula)
            systemProperty("neoforge.enabledGameTestNamespaces", "${sc.properties.get<String>("mod.id")}_gametest")
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
            "neo_loader" to sc.properties.get<String>("deps.neo_loader"),
            "pack_format" to sc.properties.get<String>("mod.pack_format"),
            "java" to requiredJava.majorVersion,
        )
        props.forEach { (k, v) -> inputs.property(k, v) }

        filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) { expand(props) }

        // Fabric-only metadata must not ship in the NeoForge jar.
        exclude("fabric.mod.json")
    }

    // The gametest source set carries both loaders' manifests; keep Fabric's out of this side.
    named<ProcessResources>("processGametestResources") {
        exclude("fabric.mod.json")
    }

    // Required: moddev's Minecraft artifacts must not be created before Stonecutter has written
    // the processed sources, or Gradle fails with an implicit-dependency validation error.
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // The compile tasks need the same ordering, and for a nastier reason: without it they can
    // snapshot their inputs before the generator has written the processed sources, so an edit
    // lands in build/generated but never reaches the class files — while the build still reports
    // SUCCESS, having compiled the previous version's code. Loom wires this up itself, so it is
    // only needed here.
    withType<JavaCompile>().configureEach {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${sc.properties.get<String>("mod.version")}"))
    }
}
