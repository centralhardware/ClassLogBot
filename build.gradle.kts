plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    kotlin("multiplatform") version "2.3.0" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("com.google.cloud.tools.jib") version "3.5.2" apply false
    id("com.gradleup.shadow") version "9.3.1" apply false
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
extra["ktgbotapiVersion"] = "30.0.2"
extra["poiVersion"] = "5.5.1"
extra["luceneVersion"] = "10.3.2"
extra["ktorVersion"] = "3.3.3"
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
