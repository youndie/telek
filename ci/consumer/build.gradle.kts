plugins {
    // Pinned to what the published metadata says telek was compiled with, because Kotlin metadata
    // compatibility is one-directional: an older compiler cannot read a newer one's classes. A
    // consumer discovering that is the point, so the version is written here rather than inherited.
    kotlin("multiplatform") version "2.4.20"

    // A consumer always applies the serialization compiler plugin itself -- telek cannot apply it
    // on their behalf, and that part is not a defect. What IS a defect is that the annotation the
    // plugin processes is not on the compile classpath: see the dependency note below.
    kotlin("plugin.serialization") version "2.4.20"
}

val telekVersion: String = providers.gradleProperty("telek.version").get()

kotlin {
    jvmToolchain(21)

    jvm {
        binaries {
            // A real executable. The point of this project is to be run, not to type-check.
            executable { mainClass.set("MainKt") }
        }
    }
    linuxX64 {
        // LINKED, not merely compiled. A klib that compiles and cannot be linked into a binary is a
        // library nobody can ship, and compiling is where a consumer usually stops looking.
        binaries.executable { entryPoint = "main" }
    }

    sourceSets {
        commonMain.dependencies {
            // Every multiplatform module telek publishes, so a module that resolves on the JVM and
            // not on native is a failure here rather than a surprise later.
            implementation("io.github.youndie.telek:core:$telekVersion")
            implementation("io.github.youndie.telek:ktg:$telekVersion")
            implementation("io.github.youndie.telek:router:$telekVersion")
            implementation("io.github.youndie.telek:router-ktg:$telekVersion")
            implementation("io.github.youndie.telek:persistence:$telekVersion")
            implementation("io.github.youndie.telek:testing:$telekVersion")
        }
        jvmMain.dependencies {
            // JVM-only, because kotlin-telegram-bot is. In maintenance, and still published --
            // which is a claim the README makes and this is where it is checked.
            implementation("io.github.youndie.telek:telegram:$telekVersion")
            implementation("io.github.youndie.telek:router-telegram:$telekVersion")
        }
    }
}
