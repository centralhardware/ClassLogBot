plugins {
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
    id("com.google.cloud.tools.jib") version "3.4.5" apply false
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// Shared version variables
extra["ktgbotapiVersion"] = "29.0.0"
extra["poiVersion"] = "5.4.1"
extra["kstatemachineVersion"] = "0.34.2"
extra["luceneVersion"] = "10.3.1"
extra["ktorVersion"] = "3.3.1"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
