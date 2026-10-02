plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.youndie.sborka.kmp")
    id("io.github.youndie.sborka.publish")
    id("org.jetbrains.kotlin.plugin.serialization")
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
            // `api`: `StateStorage<S : State>` and `UserStateStore` are `:core` types, and every
            // `State` a consumer stores must be `@Serializable`.
            api(projects.core)
            api(libs.kotlinxSerializationCore)
            // `api` as well, and not by taste: `FileStateStorage.json` is public and its type is
            // `kotlinx.serialization.json.Json`, so a consumer touching it needs the format on its
            // compile classpath. B-18 kept the format `implementation` on the premise that no
            // public signature names it; this one does. proba's first run against telek reported
            // it as `api-unreachable` (see B-18, 2026-10-02).
            api(libs.kotlinxSerializationJson)
            implementation(libs.kotlinxCoroutines)
            implementation(libs.atomicfu)
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlinxCoroutinesTest)
            implementation(libs.okioFakeFileSystem)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
