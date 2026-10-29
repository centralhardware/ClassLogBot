// The Android app: a Trusted Web Activity over the web frontend. Built on its own, not part of the root build.
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "classlog-android"
include(":app")
