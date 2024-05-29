plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

var poiVersion = "5.2.5"
var kstatemachineVersion = "0.30.0"

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("com.github.seratch:kotliquery:1.9.0")

    implementation("io.github.nsk90:kstatemachine:$kstatemachineVersion")
    implementation("io.github.nsk90:kstatemachine-coroutines:$kstatemachineVersion")

    implementation("dev.inmo:tgbotapi:13.0.0")

    implementation("io.minio:minio:8.5.10")

    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.postgresql:postgresql:42.7.3")

    implementation("org.apache.lucene:lucene-core:9.10.0")
    implementation("org.apache.lucene:lucene-queryparser:9.10.0")

    implementation("com.github.centralhardware:telegram-bot-commons:2a55dd22e2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
}