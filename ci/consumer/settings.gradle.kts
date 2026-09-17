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
        // What the README's installation block now says, copied from it -- which is the point:
        // :telegram and :router-telegram depend on kotlin-telegram-bot, which is on JitPack and not
        // on Maven Central. This consumer is what noticed that the instructions omitted it (B-19),
        // so it is also what keeps them honest: if the README ever loses this line, the two stop
        // matching and somebody has to decide which was right.
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
        // What the README's installation block now says, copied from it -- which is the point:
        // :telegram and :router-telegram depend on kotlin-telegram-bot, which is on JitPack and not
        // on Maven Central. This consumer is what noticed that the instructions omitted it (B-19),
        // so it is also what keeps them honest: if the README ever loses this line, the two stop
        // matching and somebody has to decide which was right.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("io\\.github\\.kotlin-telegram-bot.*") }
        }
        mavenCentral()
    }
}
