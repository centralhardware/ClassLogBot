plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    kotlin("multiplatform") version "2.2.21" apply false
    id("org.jetbrains.compose") version "1.9.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("com.google.cloud.tools.jib") version "3.4.5" apply false
    id("com.gradleup.shadow") version "9.2.2" apply false
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
extra["ktgbotapiVersion"] = "29.0.0"
extra["poiVersion"] = "5.4.1"
extra["luceneVersion"] = "10.3.1"
extra["ktorVersion"] = "3.3.2"
extra["composeVersion"] = "1.8.0"

subprojects {
    // Don't apply JVM plugin to web module as it uses multiplatform
    if (name != "web") {
        apply(plugin = "org.jetbrains.kotlin.jvm")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
