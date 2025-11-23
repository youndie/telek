plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.serializationPlugin)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.core)
    implementation(projects.telegram)
    implementation(projects.persistence)
    implementation(projects.router)

    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.kotlinTelegramBot)

    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
}
