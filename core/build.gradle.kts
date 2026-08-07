plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.ktlintPlugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinxCoroutines)
            implementation(libs.atomicfu)
        }
        commonTest.dependencies {
            implementation(libs.kotlinxCoroutinesTest)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}
