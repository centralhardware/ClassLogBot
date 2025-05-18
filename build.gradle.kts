plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.21"
    application
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("me.centralhardware.znatoki.telegram.statistic.MainKt")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

var poiVersion = "5.4.1"
var luceneVersion = "10.2.0"
val ktorVersion = "3.1.2"

dependencies {
    implementation("io.arrow-kt:arrow-core:2.1.0")
    implementation("com.github.seratch:kotliquery:1.9.1")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("dev.inmo:tgbotapi:24.0.2")
    implementation("com.github.centralhardware:ktgbotapi-commons:6ef1dde4fe")

    implementation("io.minio:minio:8.5.17")

    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.postgresql:postgresql:42.7.5")

    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-codecs:$luceneVersion")

    implementation("dev.inmo:krontab:2.7.2")
}