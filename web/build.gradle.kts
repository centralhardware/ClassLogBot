plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Jib will be applied later after JVM target is configured
    // id("com.google.cloud.tools.jib")
}

val ktorVersion: String by rootProject.extra
val composeVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xskip-prerelease-check")
                    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
                }
            }
        }
    }
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }
        binaries.executable()
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=org.jetbrains.compose.web.ExperimentalComposeWebApi")
                    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Common dependencies for both JVM and JS
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }

        val jvmMain by getting {
            dependencies {
                // Project dependencies
                implementation(project(":common"))

                // Compose runtime (required by compose plugin but not used)
                implementation(compose.runtime)

                // Ktor Server
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-server-cors:$ktorVersion")
                implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
                implementation("io.ktor:ktor-server-resources:$ktorVersion")
                implementation("io.ktor:ktor-server-call-logging:$ktorVersion")

                // Logging - SLF4J to JUL bridge (KSLog uses JUL on JVM)
                implementation("org.slf4j:slf4j-jdk14:2.0.17")
            }
        }

        val jsMain by getting {
            dependencies {
                // Compose for Web
                implementation(compose.html.core)
                implementation(compose.runtime)

                // Ktor Client for API calls
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

                // Kotlinx DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
    }
}

// Copy JS output to JVM resources
tasks.register<Copy>("copyJsToResources") {
    dependsOn("jsBrowserDistribution")
    from("build/dist/js/productionExecutable")
    into("build/processedResources/jvm/main/static")
}

tasks.named("jvmProcessResources") {
    dependsOn("jsBrowserProductionWebpack")
    finalizedBy("copyJsToResources")
}

tasks.named("jvmJar") {
    dependsOn("copyJsToResources")
}

// Ensure all JavaExec tasks (including IDE run configurations) depend on copyJsToResources
tasks.withType<JavaExec> {
    dependsOn("copyJsToResources")
}

// TODO: Re-enable Jib for Docker builds later
// For now, we'll build locally and run with ./gradlew :web:jvmRun
