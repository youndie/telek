plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.youndie.sborka.kmp")
    id("io.github.youndie.sborka.publish")
    alias(libs.plugins.dokkaPlugin)
    id("io.github.youndie.sborka.lint")
}

// THE TARGETS STAY HERE. `sborka.kmp` gives the mechanics — explicit API, the toolchain, warnings as
// errors, `kotlin("test")` in `commonTest`, the jvm target compiled to `sborka.jvmFloor` and that
// floor advertised in the metadata — and declares no target of its own.
//
// The set is capped by ktgbotapi (`dev.inmo:tgbotapi`), which publishes jvm / js / linuxX64 /
// linuxArm64 / mingwX64 and no Apple targets. JS is deliberately left out project-wide: it would
// force an `expect/actual` for `Dispatchers.IO`, which lives in coroutines' `concurrent` source set
// rather than in `common`, with nothing asking for it.
kotlin {
    // THE SHAPE, NOT ONLY THE VISIBILITY. `explicitApi()` makes the compiler ask what is public;
    // it cannot see that a `val` became a function, a parameter was renamed, or a default was
    // dropped. All three compile here and break a consumer at link time. The dump turns each of
    // them into a diff somebody has to approve. Explained here once, for the eight published
    // modules that carry the same two lines.
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}

    jvm {
        withSourcesJar()
    }
    linuxX64()
    linuxArm64()

    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinxCoroutines)
            implementation(libs.atomicfu)
        }
        commonTest.dependencies {
            implementation(libs.kotlinxCoroutinesTest)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
