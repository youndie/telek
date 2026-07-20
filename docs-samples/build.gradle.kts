// Not published — exists solely so README.md's code samples are checked by the compiler as part
// of `./gradlew build`. If you change a public API these samples use, this module will fail to
// compile; update both the sample here and the corresponding snippet in README.md together.
plugins {
    kotlin("jvm")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.ktlintPlugin)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.core)
    implementation(projects.telegram)
    implementation(projects.persistence)
    implementation(projects.router)
    implementation(projects.routerTelegram)

    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.kotlinTelegramBot)
}
