plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.tgbotapi)
            implementation(libs.kotlinxCoroutines)
        }
        // MockK is JVM-only, and so are the sealed ktgbotapi message types these tests fake — see
        // CLAUDE.md. The code under test is entirely in commonMain and compiles for every target.
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
            implementation(libs.mockk)
            implementation(libs.kotlinxCoroutines)
        }
    }
}
