plugins {
    id("com.android.application")
}

/** The web app's host (no scheme), e.g. `classlog.example.com`: `-Phost=…` or env CLASSLOG_HOST. */
val host: String = (findProperty("host") as String?) ?: System.getenv("CLASSLOG_HOST") ?: error("Set -Phost=<web app host> or CLASSLOG_HOST")

android {
    namespace = "me.centralhardware.classlog"
    compileSdk = 35

    defaultConfig {
        applicationId = System.getenv("ANDROID_PACKAGE") ?: "me.centralhardware.classlog"
        minSdk = 21
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        manifestPlaceholders["host"] = host
        resValue(
            "string", "asset_statements",
            """[{ \"relation\": [\"delegate_permission/common.handle_all_urls\"], \"target\": { \"namespace\": \"web\", \"site\": \"https://$host\" } }]""",
        )
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

dependencies {
    implementation("com.google.androidbrowserhelper:androidbrowserhelper:2.5.0")
}
