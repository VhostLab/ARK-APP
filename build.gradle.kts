import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.11.1"
}

group = "com.arkapp"
version = "1.0.3"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
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
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "ARK-APP"
            packageVersion = "1.0.3"
            modules("java.instrument", "jdk.unsupported")
            description = "Steam server favorites & INI profile manager for ARK: Survival Evolved"
            vendor = "Aimar"
            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menu = true
                menuGroup = "ARK-APP"
                shortcut = true
                perUserInstall = true
                dirChooser = false
                upgradeUuid = "B4960FFF-AD6A-45C4-A1B9-D575DB7DF413"
            }
        }
    }
}
