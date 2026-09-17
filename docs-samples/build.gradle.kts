// Not published — exists solely so README.md's code samples are checked by the compiler as part
// of `./gradlew build`. If you change a public API these samples use, this module will fail to
// compile; update both the sample here and the corresponding snippet in README.md together.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.github.youndie.sborka.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.github.youndie.sborka.lint")
}

kotlin {
    // NOT A LIBRARY: nothing is published and nothing depends on it, so there is no consumer for a
    // spelled-out public API to be spelled out FOR. The toolchain and warnings as errors come from
    // the convention and are wanted here as much as anywhere.
    explicitApi = null
}

dependencies {
    implementation(projects.core)
    implementation(projects.telegram)
    implementation(projects.ktg)
    implementation(projects.persistence)
    implementation(projects.router)
    implementation(projects.routerTelegram)
    implementation(projects.routerKtg)

    // The README's first example ships a test beside it, and that test is the claim the example
    // exists to make -- a transition is a pure function, so it is checked without Telegram. Here it
    // is compiled AND run by `./gradlew build`, which is a stronger promise than "it compiles".
    testImplementation(kotlin("test"))

    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.kotlinTelegramBot)
    implementation(libs.tgbotapi)
}
