plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.router)
            implementation(projects.core)
            implementation(projects.ktg)
            implementation(libs.tgbotapi)
        }
        commonTest.dependencies {
            implementation(libs.kotlinxSerializationJson)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
