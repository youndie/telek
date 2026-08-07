plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // No kotlin("reflect") any more: @RouteContext is read off the generated
            // SerialDescriptor instead of via KClass.annotations — see RouteUtils.getRouteContext.
            implementation(projects.core)
            implementation(libs.kotlinxSerializationProperties)
            implementation(libs.kotlinxCoroutines)
            implementation(libs.atomicfu)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
