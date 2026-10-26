import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val ktorVersion: String by rootProject.extra

/** Decided in settings.gradle.kts: the Android target only when an SDK is present. */
val withAndroid = gradle.extra["classlog.android"] as Boolean
if (withAndroid) apply(plugin = "com.android.kotlin.multiplatform.library")

// ClassLog's one UI, Compose Multiplatform: the Telegram Mini App in the browser (Wasm, with a JS fallback for
// webviews without WasmGC) and, as a library, the native Android app in :androidApp. The web distribution is packed
// into the bot jar under /web.
kotlin {
    js {
        outputModuleName = "classlog"
        browser {
            commonWebpackConfig { outputFileName = "classlog.js" }
        }
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "classlog"
        browser {
            commonWebpackConfig { outputFileName = "classlog.js" }
        }
        binaries.executable()
    }
    if (withAndroid) (this as ExtensionAware).extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("android") {
        namespace = "me.centralhardware.znatoki.telegram.statistic.app"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        }
        webMain.dependencies {
            implementation("io.ktor:ktor-client-js:$ktorVersion")
        }
        if (withAndroid) androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
        }
    }
}

compose.resources {
    packageOfResClass = "me.centralhardware.znatoki.telegram.statistic.app.res"
}
