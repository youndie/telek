plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.workinprogress.sborka.jvm")
    id("ru.workinprogress.sborka.publish")
    alias(libs.plugins.dokkaPlugin)
    id("ru.workinprogress.sborka.lint")
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
