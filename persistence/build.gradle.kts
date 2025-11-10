plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

dependencies {
    implementation(projects.core)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.kotlinxCoroutines)
}
