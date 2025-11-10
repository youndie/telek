plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

dependencies {
    runtimeOnly(kotlin("reflect"))

    implementation(projects.core)
    implementation(projects.telegram)
    implementation(libs.kotlinxSerializationProperties)
    implementation(libs.kotlinxCoroutines)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
}
