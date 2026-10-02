// Prints every Maven publication the build registers, one line per module and target:
//
//     PUB<TAB>:core<TAB>linuxX64<TAB>io.github.youndie.telek:core-linuxx64
//
// That set is what `publishAllPublicationsToWipRepository` uploads, read from the build itself
// rather than from a list somebody keeps. `scripts/coordinates_listed.py` compares it with the
// `coordinates:` the snapshot workflow hands to proba:
//
//     ./gradlew -q --no-configuration-cache -I scripts/publications.init.gradle.kts help \
//       | python3 scripts/coordinates_listed.py
//
// `projectsEvaluated`, not earlier: a multiplatform target's artifactId (`core-linuxx64`) is set
// while its project is evaluated. `--no-configuration-cache`, because a reused cache entry skips
// configuration entirely, and this callback with it — the checker then sees no lines and fails.
gradle.projectsEvaluated {
    rootProject.allprojects {
        val publishing = extensions.findByType(PublishingExtension::class.java) ?: return@allprojects
        publishing.publications.withType(MavenPublication::class.java).forEach { publication ->
            println("PUB\t$path\t${publication.name}\t${publication.groupId}:${publication.artifactId}")
        }
    }
}
