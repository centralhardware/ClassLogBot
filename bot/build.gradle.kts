plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.cloud.tools.jib")
}

val ktgbotapiVersion: String by rootProject.extra

dependencies {
    implementation(project(":common"))

    // Telegram Bot API
    implementation("dev.inmo:tgbotapi:$ktgbotapiVersion")
    implementation("com.github.centralhardware:ktgbotapi-commons:$ktgbotapiVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-restrict-access-middleware:$ktgbotapiVersion")
}

jib {
    from {
        image = System.getenv("JIB_FROM_IMAGE") ?: "eclipse-temurin:24-jre"
    }
    to {
        val repoLower = System.getenv("GITHUB_REPOSITORY")?.lowercase() ?: "local/classlogbot"
        image = "ghcr.io/${repoLower}:bot-latest"
        tags = setOf("bot-${System.getenv("GITHUB_SHA")?.take(7) ?: "dev"}")
    }
    container {
        mainClass = "me.centralhardware.znatoki.telegram.statistic.bot.BotMainKt"
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
            "org.opencontainers.image.description" to "Telegram bot module"
        )
    }
}
