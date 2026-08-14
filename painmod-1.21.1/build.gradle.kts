plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
}

version = "1.0.1+1.21.1"
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
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.19.3")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.13+1.21.1")
    modCompileOnly("com.terraformersmc:modmenu:11.0.4")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:15.0.140")
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


