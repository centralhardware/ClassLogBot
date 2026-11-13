plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.cloud.tools.jib")
}

val ktorVersion: String by rootProject.extra

dependencies {
    implementation(project(":common"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-conditional-headers:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

jib {
    from {
        image = System.getenv("JIB_FROM_IMAGE") ?: "eclipse-temurin:25-jre"
    }
    to {
        val repoLower = System.getenv("GITHUB_REPOSITORY")?.lowercase() ?: "local/classlogbot"
        image = "ghcr.io/${repoLower}:web-latest"
        tags = setOf("web-${System.getenv("GITHUB_SHA")?.take(7) ?: "dev"}")
    }
    container {
        mainClass = "me.centralhardware.znatoki.telegram.statistic.WebMainKt"
        // The Mini App and its API (WEB_PORT); /health is served on the same port
        ports = listOf("8080")
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
            "org.opencontainers.image.revision" to (System.getenv("GITHUB_SHA") ?: ""),
            "org.opencontainers.image.description" to "Web app (Telegram Mini App) module"
        )
    }
}
// The Mini App frontend is built from :web-frontend and served by the web server from /web.
tasks.named<ProcessResources>("processResources") {
    from(project(":web-frontend").tasks.named("jsBrowserDistribution")) { into("web") }
}
