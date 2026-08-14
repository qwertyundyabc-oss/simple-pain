plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.143"
}

version = "1.0.0+1.21.1"
group = "com.painmechanic"

base {
    archivesName = "Pain-Mechanic-NeoForge"
}

repositories {
    maven("https://maven.shedaniel.me/")
}

neoForge {
    version = "21.1.248"

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create("pain_mechanic") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    // Cloth Config API（运行时由用户自行安装，不打包进 jar）
    compileOnly("me.shedaniel.cloth:cloth-config-neoforge:15.0.140")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}