plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

// The native Android app: the shared Compose UI from :app in an activity. Included only when an Android SDK is present.

/** The API's host (no scheme), e.g. `classlog.example.com`: `-Phost=…` or env CLASSLOG_HOST. */
val host: String? = (findProperty("host") as String?) ?: System.getenv("CLASSLOG_HOST")

android {
    namespace = "me.centralhardware.classlog"
    compileSdk = 37

    defaultConfig {
        applicationId = System.getenv("ANDROID_PACKAGE") ?: "me.centralhardware.classlog"
        minSdk = 24
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "API_HOST", "\"${host.orEmpty()}\"")
    }
    buildFeatures { buildConfig = true }
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

tasks.named("preBuild") {
    doFirst { checkNotNull(host) { "Set -Phost=<API host> or CLASSLOG_HOST for the Android build" } }
}

dependencies {
    implementation(project(":app"))
    implementation("androidx.activity:activity-compose:1.11.0")
}
