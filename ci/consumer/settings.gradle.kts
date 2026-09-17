// A BOT THAT IS NOT PART OF THIS BUILD.
//
// It shares nothing with telek: not the build, not the version catalogue, not a source set, not a
// project dependency. Those are three of the things publication breaks, and a module inside the
// repository would have all three and could not notice any of them.
//
// What it knows is what a stranger knows: a coordinate and a repository URL.

rootProject.name = "telek-consumer"

pluginManagement {
    repositories {
        gradlePluginPortal()
        // WORKING AROUND B-19, DELIBERATELY NOT FIXED HERE. :telegram and :router-telegram depend
        // on kotlin-telegram-bot, which is on JitPack and not on Maven Central -- and the README's
        // installation block does not say so, so a consumer following it gets "Could not find
        // io.github.kotlin-telegram-bot…" and no reason to suspect telek. Filtered to the group it
        // answers for: an unfiltered repository takes part in resolving everything, and when it is
        // unreachable Gradle disables it and fails artifacts that were perfectly fine.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("io\\.github\\.kotlin-telegram-bot.*") }
        }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // NO mavenLocal and no project dependency. `~/.m2` is shared with every build on this
        // machine, so resolving from it would prove the machine rather than the publication.
        //
        // The URL is a property so the same consumer can be aimed at Maven Central once B-05's
        // question is answered, without changing a line of it. Defaults to the snapshot repository,
        // which is where telek actually is today.
        maven(providers.gradleProperty("telek.repo").getOrElse("https://reposilite.kotlin.website/snapshots")) {
            content { includeGroupAndSubgroups("io.github.youndie") }
        }
        // WORKING AROUND B-19, DELIBERATELY NOT FIXED HERE. :telegram and :router-telegram depend
        // on kotlin-telegram-bot, which is on JitPack and not on Maven Central -- and the README's
        // installation block does not say so, so a consumer following it gets "Could not find
        // io.github.kotlin-telegram-bot…" and no reason to suspect telek. Filtered to the group it
        // answers for: an unfiltered repository takes part in resolving everything, and when it is
        // unreachable Gradle disables it and fails artifacts that were perfectly fine.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("io\\.github\\.kotlin-telegram-bot.*") }
        }
        mavenCentral()
    }
}
