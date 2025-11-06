plugins {
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.dokkaPlugin)
}

version = findProperty("telek.version").toString()

dependencies {
    dokka(projects.core)
    dokka(projects.telegram)
}
