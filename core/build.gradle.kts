plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

dependencies {
    implementation(libs.kotlinxCoroutines)
    testImplementation(kotlin("test"))
}
