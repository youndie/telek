package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

/**
 * KMP counterpart of [buildsrc.convention.kotlin-jvm] for the transport-agnostic modules.
 *
 * Target set is capped by ktgbotapi (`dev.inmo:tgbotapi`), which publishes jvm / js / linuxX64 /
 * linuxArm64 / mingwX64 and **no Apple targets**. JS is deliberately left out project-wide: it
 * would force an `expect/actual` for `Dispatchers.IO` (which lives in coroutines' `concurrent`
 * source set, not `common`) with nothing asking for it. `:telegram` and `:router-telegram` stay
 * plain JVM — kotlin-telegram-bot is JVM-only.
 *
 * Unlike the JVM convention this does **not** create a `MavenPublication` by hand: the
 * multiplatform plugin already registers one publication per target plus the root `kotlinMultiplatform`
 * one, and a hand-rolled `artifactId = project.name` would collide with their `-jvm`/`-linuxx64`
 * suffixes.
 */
plugins {
    kotlin("multiplatform")
    `maven-publish`
}

version = findProperty("telek.version").toString()

kotlin {
    jvmToolchain(21)

    jvm {
        withSourcesJar()
    }
    linuxX64()
    linuxArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        version = findProperty("VERSION")?.toString() ?: project.version.toString()
        groupId = "ru.workinprogress.telek"
    }

    repositories {
        maven {
            name = "wip"
            url = uri("https://reposilite.kotlin.website/snapshots")
            credentials {
                username = findProperty("REPOSILITE_USER")?.toString()
                password = findProperty("REPOSILITE_SECRET")?.toString()
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
        )
    }
}
