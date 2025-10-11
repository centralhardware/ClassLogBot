plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.cloud.tools.jib")
}

val ktorVersion: String by rootProject.extra

dependencies {
    implementation(project(":common"))

    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")

    // Logging - SLF4J to JUL bridge (KSLog uses JUL on JVM)
    implementation("org.slf4j:slf4j-jdk14:2.0.17")
}

jib {
    from {
        image = System.getenv("JIB_FROM_IMAGE") ?: "eclipse-temurin:24-jre"
    }
    to {
        val repoLower = System.getenv("GITHUB_REPOSITORY")?.lowercase() ?: "local/classlogbot"
        image = "ghcr.io/${repoLower}:web-latest"
        tags = setOf("web-${System.getenv("GITHUB_SHA")?.take(7) ?: "dev"}")
    }
    container {
        mainClass = "me.centralhardware.znatoki.telegram.statistic.web.WebMainKt"
        jvmFlags = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-modules=jdk.incubator.vector",
            "--enable-native-access=ALL-UNNAMED"
        )
        creationTime = "USE_CURRENT_TIMESTAMP"
        labels = mapOf(
            "org.opencontainers.image.source" to (System.getenv("GITHUB_SERVER_URL")?.let { server ->
                val repo = System.getenv("GITHUB_REPOSITORY")
                if (repo != null) "$server/$repo" else ""
            } ?: ""),
            "org.opencontainers.image.revision" to (System.getenv("GITHUB_SHA") ?: ""),
            "org.opencontainers.image.description" to "Web application module"
        )
        ports = listOf("8080")
    }
}
