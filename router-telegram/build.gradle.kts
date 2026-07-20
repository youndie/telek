plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
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
