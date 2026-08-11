plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
}

version = "1.0.0+1.20.1"
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
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10:v2")
    modImplementation("net.fabricmc:fabric-loader:0.19.3")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.92.9+1.20.1")

    modCompileOnly("com.terraformersmc:modmenu:7.2.2")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:11.1.136")
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}


