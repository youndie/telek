plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

dependencies {
    api(projects.core)
}
