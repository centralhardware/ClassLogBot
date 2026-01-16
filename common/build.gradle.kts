plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val poiVersion: String by rootProject.extra
val luceneVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra

val ktgbotapiVersion: String by rootProject.extra

dependencies {
    // Database
    api("com.github.seratch:kotliquery:1.9.1")
    api("org.postgresql:postgresql:42.7.9")
    api("com.zaxxer:HikariCP:7.0.2")
    api("org.flywaydb:flyway-core:11.20.1")
    api("org.flywaydb:flyway-database-postgresql:11.20.1")

    // Serialization
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor Client for Firefly III
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-client-logging:$ktorVersion")

    // Utilities
    api("io.arrow-kt:arrow-core:2.2.1.1")
    api("io.minio:minio:8.6.0")

    // Excel
    api("org.apache.poi:poi:$poiVersion")
    api("org.apache.poi:poi-ooxml:$poiVersion")

    // Search
    api("org.apache.lucene:lucene-core:$luceneVersion")
    api("org.apache.lucene:lucene-queryparser:$luceneVersion")
    api("org.apache.lucene:lucene-backward-codecs:$luceneVersion")
    api("org.apache.lucene:lucene-codecs:$luceneVersion")

    // Scheduling
    api("dev.inmo:krontab:2.7.2")

    // Diff library
    api("de.danielbechler:java-object-diff:0.95")

    // TgBotAPI (minimal - only needed for entity Tutor - includes kslog transitively)
    api("dev.inmo:tgbotapi:$ktgbotapiVersion")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.2")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
