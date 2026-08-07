plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.serializationPlugin)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.kotlinxCoroutines)
            implementation(libs.atomicfu)
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlinxCoroutinesTest)
            implementation(libs.okioFakeFileSystem)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
