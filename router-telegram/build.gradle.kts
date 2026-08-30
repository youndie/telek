plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.workinprogress.sborka.jvm")
    id("ru.workinprogress.sborka.publish")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.dokkaPlugin)
    id("ru.workinprogress.sborka.lint")
}

dependencies {
    api(projects.router)
    implementation(projects.core)
    implementation(projects.telegram)
    implementation(libs.kotlinTelegramBot)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.kotlinxSerializationJson)
}
