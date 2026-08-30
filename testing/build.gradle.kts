plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.workinprogress.sborka.kmp")
    id("ru.workinprogress.sborka.publish")
    alias(libs.plugins.dokkaPlugin)
    id("ru.workinprogress.sborka.lint")
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
            api(projects.core)
        }
    }
}
