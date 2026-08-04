import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.11.1"
}

group = "com.arkapp"
version = "1.1.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    // Core icon set only (~50 icons); the extended artifact is 75 MB of classes
    implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.arkapp.MainKt"
        // Without -Xmx the JVM claims up to 25% of physical RAM (8 GB on a 32 GB
        // machine) and never gives it back. SerialGC + free ratios shrink the
        // committed heap after each GC.
        jvmArgs += listOf(
            "-Xms32m",
            "-Xmx256m",
            "-XX:+UseSerialGC",
            "-XX:MaxHeapFreeRatio=40",
            "-XX:MinHeapFreeRatio=15",
            "-XX:ReservedCodeCacheSize=48m",
            "-Dapp.version=$version",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Prodigiosos App"
            packageVersion = "1.1.0"
            modules("java.instrument", "java.net.http", "jdk.unsupported")
            description = "Steam server favorites & INI profile manager for ARK: Survival Evolved"
            vendor = "Aimar"
            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menu = true
                menuGroup = "Prodigiosos App"
                shortcut = true
                perUserInstall = true
                dirChooser = false
                upgradeUuid = "B4960FFF-AD6A-45C4-A1B9-D575DB7DF413"
            }
        }
    }
}
