enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Written out by hand, and it has to be: `pluginManagement` is evaluated before any settings
        // plugin is applied — including the sborka one, which is fetched through it.
        maven("https://reposilite.kotlin.website/snapshots") {
            name = "wip-snapshots"
            content {
                // One group, and it is the only one there can be. The portfolio's move to
                // `io.github.youndie` is finished: nothing this build resolves is under
                // `ru.workinprogress` any more, and a filter naming a group the server is never asked
                // about reads as a dependency that is still there.
                includeGroupByRegex("io\\.github\\.youndie.*")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // mavenCentral() and google() with their content filters, the shared `wip` catalog, and the check
    // that this repository's `.editorconfig` is the one the rest of them use.
    id("io.github.youndie.sborka.settings") version "0.4.0.43"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        // JitPack, filtered. An unfiltered repository takes part in resolving EVERY dependency, and
        // when it is unreachable Gradle disables it and fails everything that had not resolved
        // earlier in the list — artifacts that are perfectly fine included.
        // JitPack, filtered to the groups it actually answers for. An unfiltered repository takes
        // part in resolving EVERY dependency, and when it is unreachable Gradle disables it and fails
        // everything that had not resolved earlier in the list — artifacts that are perfectly fine
        // included.
        //
        // BOTH prefixes, and the second is the one that matters here: kotlin-telegram-bot is served
        // as `io.github.kotlin-telegram-bot.kotlin-telegram-bot`, not `com.github.…`. A filter on
        // `com.github` alone looks right and resolves nothing.
        maven("https://jitpack.io") {
            name = "jitpack"
            content {
                includeGroupByRegex("com\\.github\\..*")
                includeGroupByRegex("io\\.github\\..*")
            }
        }
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.5.2")
        }
    }
}

include(":core")
include(":telegram")
include(":ktg")
include(":persistence")
include(":router")
include(":router-telegram")
include(":router-ktg")
include(":testing")
include(":example")
include(":docs-samples")

rootProject.name = "telek"
