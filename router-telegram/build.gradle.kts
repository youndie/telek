plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.github.youndie.sborka.jvm")
    id("io.github.youndie.sborka.publish")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.dokkaPlugin)
    id("io.github.youndie.sborka.lint")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}
}

dependencies {
    api(projects.router)
    implementation(projects.core)
    // `api`: the one function here is an extension on `:telegram`'s `RowBuilder`.
    api(projects.telegram)
    implementation(libs.kotlinTelegramBot)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.kotlinxSerializationJson)
}
