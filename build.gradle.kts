import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.ncorti.ktfmt.gradle") version "0.20.1"
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

var poiVersion = "5.3.0"
var kstatemachineVersion = "0.32.0"
var luceneVersion = "10.0.0"

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("com.github.seratch:kotliquery:1.9.0")

    implementation("io.github.nsk90:kstatemachine:$kstatemachineVersion")
    implementation("io.github.nsk90:kstatemachine-coroutines:$kstatemachineVersion")

    implementation("dev.inmo:tgbotapi:18.2.3")
    implementation("com.github.centralhardware:telegram-bot-commons:af385ce6fe")

    implementation("io.minio:minio:8.5.13")


    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.postgresql:postgresql:42.7.4")

    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-codecs:$luceneVersion")

    implementation("dev.inmo:krontab:2.5.0")
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

ktfmt {
    kotlinLangStyle()
}