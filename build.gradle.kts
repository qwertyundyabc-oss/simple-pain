plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
}

version = "1.0.0+1.21.11"
group = "com.painmechanic"

base {
    archivesName = "Pain-Mechanic"
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.shedaniel.me/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings("net.fabricmc:yarn:1.21.11+build.6:v2")
    modImplementation("net.fabricmc:fabric-loader:0.19.3")

    modImplementation(platform("net.fabricmc.fabric-api:fabric-api-bom:0.141.6+1.21.11"))
    modImplementation("net.fabricmc.fabric-api:fabric-lifecycle-events-v1")
    modImplementation("net.fabricmc.fabric-api:fabric-entity-events-v1")
    modImplementation("net.fabricmc.fabric-api:fabric-networking-api-v1")
    modImplementation("net.fabricmc.fabric-api:fabric-rendering-v1")
    modImplementation("net.fabricmc.fabric-api:fabric-events-interaction-v0")
    modImplementation("net.fabricmc.fabric-api:fabric-resource-loader-v1")
    modImplementation("net.fabricmc.fabric-api:fabric-registry-sync-v0")
    modImplementation("net.fabricmc.fabric-api:fabric-item-api-v1")
    modImplementation("net.fabricmc.fabric-api:fabric-item-group-api-v1")

    // Optional integrations: compiled against, but not required at runtime.
    modCompileOnly("com.terraformersmc:modmenu:17.0.1-beta.1")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:21.11.153")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
