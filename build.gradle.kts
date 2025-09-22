plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.cloud.tools.jib") version "3.4.5"
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val ktgbotapiVersion = "28.0.2"
var poiVersion = "5.4.1"
var kstatemachineVersion = "0.34.2"
var luceneVersion = "10.3.0"

dependencies {
    implementation("io.arrow-kt:arrow-core:2.1.2")
    implementation("com.github.seratch:kotliquery:1.9.1")

    implementation("io.github.nsk90:kstatemachine:$kstatemachineVersion")
    implementation("io.github.nsk90:kstatemachine-coroutines:$kstatemachineVersion")

    implementation("dev.inmo:tgbotapi:$ktgbotapiVersion")
    implementation("com.github.centralhardware:ktgbotapi-commons:$ktgbotapiVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-restrict-access-middleware:$ktgbotapiVersion")

    implementation("io.minio:minio:8.5.17")

    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.postgresql:postgresql:42.7.8")

    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-codecs:$luceneVersion")

    implementation("dev.inmo:krontab:2.7.2")
}

jib {
    from {
        image = System.getenv("JIB_FROM_IMAGE") ?: "eclipse-temurin:25-jre"
    }
    to {
    }
    container {
        mainClass = "me.centralhardware.znatoki.telegram.statistic.MainKt"
        jvmFlags = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-modules=jdk.incubator.vector"
        )
        creationTime = "USE_CURRENT_TIMESTAMP"
        labels = mapOf(
            "org.opencontainers.image.source" to (System.getenv("GITHUB_SERVER_URL")?.let { server ->
                val repo = System.getenv("GITHUB_REPOSITORY")
                if (repo != null) "$server/$repo" else ""
            } ?: ""),
            "org.opencontainers.image.revision" to (System.getenv("GITHUB_SHA") ?: "")
        )
    }
}
