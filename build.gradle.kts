plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

application {
    mainClass.set("me.centralhardware.znatoki.telegram.statistic.MainKt")
    applicationDefaultJvmArgs = listof("--add-modules jdk.incubator.vector")
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

var poiVersion = "5.4.1"
var kstatemachineVersion = "0.33.0"
var luceneVersion = "10.2.2"
val ktorVersion = "3.2.3"

dependencies {
    implementation("io.arrow-kt:arrow-core:2.1.2")
    implementation("com.github.seratch:kotliquery:1.9.1")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("io.github.nsk90:kstatemachine:$kstatemachineVersion")
    implementation("io.github.nsk90:kstatemachine-coroutines:$kstatemachineVersion")

    implementation("dev.inmo:tgbotapi:27.1.0")
    implementation("com.github.centralhardware:ktgbotapi-commons:beafbfc9a8")

    implementation("io.minio:minio:8.5.17")

    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.postgresql:postgresql:42.7.5")

    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-codecs:$luceneVersion")

    implementation("dev.inmo:krontab:2.7.2")
}

