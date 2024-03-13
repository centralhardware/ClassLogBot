plugins {
    kotlin("jvm") version "1.9.22"
    id("org.springframework.boot") version "3.2.3"
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

dependencies {
    implementation("org.telegram:telegrambots-springboot-longpolling-starter:7.0.0-rc0")
    implementation("org.telegram:telegrambots-client:7.0.0-rc0")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("one.util:streamex:0.8.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.2")
    implementation("io.minio:minio:8.5.9")

    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0-M1")
    implementation("ch.qos.logback:logback-classic:1.5.3")

    implementation("io.vavr:vavr:0.10.4")

    implementation("org.springframework.boot:spring-boot-starter:3.2.3")
    implementation("com.clickhouse:clickhouse-jdbc:0.6.0-patch1")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("org.mybatis:mybatis:3.5.15")
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.3")
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.3")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:3.2.3")

    implementation("org.postgresql:postgresql:42.7.2")

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
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}