plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

dependencies {
    implementation(projects.core)
    implementation(libs.kotlinTelegramBot)
    implementation(libs.retrofitCore)
    implementation(libs.kotlinxCoroutines)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutines)
}
