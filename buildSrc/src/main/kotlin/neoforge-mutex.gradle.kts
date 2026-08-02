import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

// Serialises NeoForm's Minecraft setup across nodes. Without this, building several NeoForge
// versions at once starts a full Minecraft decompile per node in parallel and will bring the
// machine to its knees.
interface NeoForgeMutex : BuildService<BuildServiceParameters.None>

val mutex = gradle.sharedServices.registerIfAbsent("createMinecraftArtifactsMutex", NeoForgeMutex::class.java) {
    maxParallelUsages.set(1)
}

tasks.named { it == "createMinecraftArtifacts" }.configureEach {
    usesService(mutex)
}
