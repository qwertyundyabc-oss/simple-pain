plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
}

version = "1.0.1+1.20.4"
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
    minecraft("com.mojang:minecraft:1.20.4")
    mappings("net.fabricmc:yarn:1.20.4+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.19.3")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.97.3+1.20.4")

    modCompileOnly("com.terraformersmc:modmenu:9.2.0") { isTransitive = false }
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:13.0.138")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}


