plugins {
    kotlin("jvm") version "2.0.0"
    id("org.springframework.boot") version "3.2.3"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.0"
    kotlin("plugin.noarg") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
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

val tgVersoin = "7.2.1"
var springBootVersion = "3.2.5"
var poiVersion = "5.2.5"
var kstatemachineVersion = "0.30.0"

dependencies {
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("io.github.nsk90:kstatemachine:$kstatemachineVersion")
    implementation("io.github.nsk90:kstatemachine-coroutines:$kstatemachineVersion")

    implementation("org.telegram:telegrambots-springboot-longpolling-starter:$tgVersoin")
    implementation("org.telegram:telegrambots-client:$tgVersoin")

    implementation("io.minio:minio:8.5.10")

    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0-M1")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")

    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")

    implementation("org.postgresql:postgresql:42.7.3")

    implementation("org.hibernate.search:hibernate-search-mapper-orm-orm6:6.2.3.Final")
    implementation("org.hibernate.search:hibernate-search-backend-lucene:6.2.3.Final")

    implementation("org.hibernate.orm:hibernate-core:6.4.4.Final")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.ibm.icu:icu4j:75.1")

    implementation("org.apache.commons:commons-compress:1.26.1")

    implementation("com.github.centralhardware:telegram-bot-commons:2a55dd22e2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}