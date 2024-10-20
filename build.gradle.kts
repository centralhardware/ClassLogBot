import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://nexus.inmo.dev/repository/maven-releases/")

}

var poiVersion = "5.3.0"
var kstatemachineVersion = "0.31.1"
var luceneVersion = "9.12.0"

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("com.github.seratch:kotliquery:1.9.0")

    implementation("io.github.nsk90:kstatemachine:$kstatemachineVersion")
    implementation("io.github.nsk90:kstatemachine-coroutines:$kstatemachineVersion")

    implementation("dev.inmo:tgbotapi:18.2.2-branch_18.2.2-build2465")
    implementation("com.github.centralhardware:telegram-bot-commons:1e503cc156")

    implementation("io.minio:minio:8.5.12")


    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.postgresql:postgresql:42.7.4")

    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-codecs:$luceneVersion")

    implementation("dev.inmo:krontab:2.5.0")

    testImplementation("org.mockito:mockito-core:5.13.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "me.centralhardware.znatoki.telegram.statistic.MainKt"))
        }
    }
}