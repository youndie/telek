plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.github.youndie.sborka.jvm")
    id("io.github.youndie.sborka.publish")
    alias(libs.plugins.dokkaPlugin)
    id("io.github.youndie.sborka.lint")
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
