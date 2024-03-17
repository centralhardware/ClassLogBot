plugins {
    kotlin("jvm") version "1.9.22"
    id("org.springframework.boot") version "3.2.3"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.23"
    kotlin("plugin.noarg") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "me.centralhardware.znatoki.telegram.statistic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

allOpen {
    annotation("org.springframework.context.annotation.Configuration")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("me.centralhardware.znatoki.telegram.statistic.Open")
}

noArg {
    annotation("me.centralhardware.znatoki.telegram.statistic.NoArg")
}

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.3")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.github.nsk90:kstatemachine:0.27.0")
    implementation("io.github.nsk90:kstatemachine-coroutines:0.27.0")


    implementation("org.telegram:telegrambots-springboot-longpolling-starter:7.0.0-rc0")
    implementation("org.telegram:telegrambots-client:7.0.0-rc0")

    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.apache.commons:commons-collections4:4.4")

    implementation("io.minio:minio:8.5.9")

    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0-M1")
    implementation("ch.qos.logback:logback-classic:1.5.3")

    implementation("org.springframework.boot:spring-boot-starter:3.2.3")

    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.3")

    implementation("org.postgresql:postgresql:42.7.3")

    implementation("commons-validator:commons-validator:1.8.0")

    implementation("org.hibernate.search:hibernate-search-mapper-orm-orm6:6.2.3.Final")
    implementation("org.hibernate.search:hibernate-search-backend-lucene:6.2.3.Final")

    implementation("org.hibernate.orm:hibernate-core:6.4.4.Final")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.ibm.icu:icu4j:74.2")

    implementation("org.apache.commons:commons-compress:1.26.1")

    implementation("com.github.centralhardware:telegram-bot-commons:fb2bb2a86b")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}