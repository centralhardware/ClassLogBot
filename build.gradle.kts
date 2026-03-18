plugins {
    kotlin("jvm") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
    id("com.google.cloud.tools.jib") version "3.5.3" apply false
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
extra["luceneVersion"] = "10.4.0"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
