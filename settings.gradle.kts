pluginManagement {
    repositories { gradlePluginPortal(); google(); mavenCentral() }
}

rootProject.name = "classlogbot"

include("common")
include("bot")
include("app")

/**
 * The Android app needs an SDK; without one (a plain bot build, the Jib image) the shared module stays web-only
 * and :androidApp is left out. Found the usual way: ANDROID_HOME / ANDROID_SDK_ROOT, or sdk.dir in local.properties.
 */
val androidSdk = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    ?: file("local.properties").takeIf { it.exists() }?.readLines()?.firstOrNull { it.startsWith("sdk.dir=") }?.substringAfter("=")
val withAndroid = androidSdk != null && providers.gradleProperty("classlogAndroid").orNull != "false"
gradle.extra["classlog.android"] = withAndroid
if (withAndroid) include("androidApp")