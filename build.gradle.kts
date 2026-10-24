plugins {
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.serialization") version "2.4.21" apply false
    kotlin("multiplatform") version "2.4.21" apply false
    kotlin("plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    id("com.google.cloud.tools.jib") version "3.5.4" apply false
    id("com.android.application") version "9.4.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.4.1" apply false
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
}

// Shared version variables
extra["poiVersion"] = "5.5.1"
extra["luceneVersion"] = "10.5.2"
extra["ktorVersion"] = "3.6.0"

// JVM modules; app is Kotlin Multiplatform (Compose: web and Android)
configure(listOf(project(":common"), project(":bot"))) {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // Show each test's outcome in CI logs, so a skipped Testcontainers test is visible.
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
