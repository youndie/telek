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

    implementation("io.ktor:ktor-client-core:3.3.1")
    implementation("io.ktor:ktor-client-okhttp:3.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
}
