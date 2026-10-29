plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/** The API's host (no scheme), e.g. `classlog.example.com`: `-Phost=…` or env CLASSLOG_HOST. */
val host: String = (findProperty("host") as String?) ?: System.getenv("CLASSLOG_HOST") ?: error("Set -Phost=<API host> or CLASSLOG_HOST")

/** The web frontend's production build (`./gradlew :web-frontend:jsBrowserDistribution` in the repo root). */
val webDist = rootDir.resolve("../web-frontend/build/dist/js/productionExecutable")

/**
 * The frontend bundled into the APK's assets. index.html loses Telegram's Mini App script and the web manifest,
 * and gains the API's address, so the app loads nothing from the network but API calls.
 */
val bundleWeb by tasks.registering(Sync::class) {
    doFirst { check(webDist.resolve("index.html").exists()) { "No web build at $webDist: run ./gradlew :web-frontend:jsBrowserDistribution in the repo root first" } }
    inputs.property("host", host)
    from(webDist) {
        exclude("**/*.map", "manifest.json")
        filesMatching("index.html") {
            filter { line ->
                when {
                    "telegram-web-app.js" in line -> """<script>window.CLASSLOG_API="https://$host"</script>"""
                    "rel=\"manifest\"" in line -> ""
                    else -> line
                }
            }
        }
    }
    into(layout.buildDirectory.dir("generated/web"))
}

android {
    namespace = "me.centralhardware.classlog"
    compileSdk = 35

    defaultConfig {
        applicationId = System.getenv("ANDROID_PACKAGE") ?: "me.centralhardware.classlog"
        minSdk = 21
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "API_HOST", "\"$host\"")
    }
    buildFeatures { buildConfig = true }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/web"))
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Release signing comes from env (CI decodes the keystore from a secret); without it the APK is debug-signed.
    val keystore = System.getenv("KEYSTORE_FILE")?.let(::file)?.takeIf { it.exists() }
    signingConfigs {
        if (keystore != null) create("release") {
            storeFile = keystore
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "classlog"
            keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}

tasks.named("preBuild") { dependsOn(bundleWeb) }

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
