plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val ktorVersion: String by rootProject.extra

// The Telegram Mini App frontend (Compose HTML). Its browser distribution is packed into the bot
// jar under /web, so the app ships in the same image as the bot.
kotlin {
    js {
        outputModuleName = "classlog"
        browser {
            commonWebpackConfig { outputFileName = "classlog.js" }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.html.core)
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-js:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        }
    }
}
