enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.3.2")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":core")
include(":telegram")
include(":persistence")
include(":router")
include(":example")

rootProject.name = "telek"
