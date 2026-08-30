plugins {
    alias(libs.plugins.dokkaPlugin)
    // DECLARED HERE, APPLIED IN THE MODULES, and the versions named once.
    //
    // Not a style choice. Each module's own `plugins { }` block gets a classloader of its own, so
    // eight modules each asking for the Kotlin plugin by version load it eight times — and the
    // Kotlin/Native build service, which is shared across the build, then refuses: "cannot set the
    // value ... loaded with InstrumentingVisitableURLClassLoader(... project-docs-samples) using a
    // provider ... loaded with (... project-core)". `buildSrc` used to hold that classpath for
    // everyone, which is why it never came up before it was deleted.
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.serializationPlugin) apply false
    alias(libs.plugins.sborkaKmp) apply false
    alias(libs.plugins.sborkaJvm) apply false
    alias(libs.plugins.sborkaLint) apply false
    alias(libs.plugins.sborkaPublish) apply false
}

// The version used to be set here from `telek.version` and the GROUP was set inside the publishing
// block of each convention, on the publication rather than on the project. `sborka.base` sets both
// on every module, from `sborka.group` and `version` in `gradle.properties`.
//
// That was not tidiness. The archive tasks take their file names from the PROJECT version, so a
// publication carrying `-PVERSION` while the project kept the head shipped files named after a
// version that was never released — resolving correctly, from the right url, under the wrong name.

dependencies {
    dokka(projects.core)
    dokka(projects.telegram)
    dokka(projects.ktg)
}
