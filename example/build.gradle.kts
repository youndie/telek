plugins {
    id("org.jetbrains.kotlin.jvm")
    id("ru.workinprogress.sborka.jvm")
    id("ru.workinprogress.sborka.lint")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

kotlin {
    // NOT A LIBRARY: nothing is published and nothing depends on it, so there is no consumer for a
    // spelled-out public API to be spelled out FOR. The toolchain and warnings as errors come from
    // the convention and are wanted here as much as anywhere.
    explicitApi = null
}

application {
    mainClass.set("ru.workinprogress.telek.example.ApplicationKt")
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

    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
}
