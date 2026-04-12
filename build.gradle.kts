import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.vanniktech.maven.publish.base) apply false
}

version = providers.gradleProperty("redbytefx.version").orElse("1.0.0").get()
group = "io.github.i-redbyte"

val hasMavenCentralCredentials =
    (
        providers.gradleProperty("mavenCentralUsername").isPresent &&
            providers.gradleProperty("mavenCentralPassword").isPresent
        ) || (
        providers.environmentVariable("ORG_GRADLE_PROJECT_mavenCentralUsername").isPresent &&
            providers.environmentVariable("ORG_GRADLE_PROJECT_mavenCentralPassword").isPresent
        )

val hasInMemoryOrLegacySigningConfiguration =
    providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.secretKeyRingFile").isPresent

val hasGpgCmdSigningConfiguration =
    providers.gradleProperty("signing.gnupg.keyName").isPresent ||
        providers.gradleProperty("signing.gnupg.passphrase").isPresent ||
        providers.gradleProperty("signing.gnupg.homeDir").isPresent ||
        providers.gradleProperty("signing.gnupg.executable").isPresent

val hasSigningConfiguration =
    hasInMemoryOrLegacySigningConfiguration || hasGpgCmdSigningConfiguration

subprojects {
    plugins.withId("signing") {
        if (hasGpgCmdSigningConfiguration) {
            extensions.configure<SigningExtension> {
                useGpgCmd()
            }
        }
    }

    afterEvaluate {
        tasks.withType<Detekt>().configureEach {
            val mainRoots = listOf("src/main/java", "src/main/kotlin")
                .map { project.layout.projectDirectory.file(it).asFile }
                .filter { it.exists() }
            if (mainRoots.isNotEmpty()) {
                setSource(mainRoots)
            }
        }
    }

    tasks.matching { task -> task.name.contains("MavenCentral") }.configureEach {
        doFirst {
            check(hasMavenCentralCredentials) {
                "Publishing to Maven Central requires mavenCentralUsername and " +
                    "mavenCentralPassword from a Sonatype Central Portal user token."
            }
            check(hasSigningConfiguration) {
                "Publishing to Maven Central requires a configured GPG key. " +
                    "Set signingInMemoryKey, signing.secretKeyRingFile, or signing.gnupg.* " +
                    "in Gradle properties."
            }
        }
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Unit tests, sample compilation, and Detekt on main sources."
    dependsOn(
        ":redbytefx-core:testDebugUnitTest",
        ":redbytefx-compose:testDebugUnitTest",
        ":redbytefx-stdlib:testDebugUnitTest",
        ":sample:compileDebugKotlin",
        ":redbytefx-core:detekt",
        ":redbytefx-compose:detekt",
        ":redbytefx-stdlib:detekt",
        ":sample:detekt"
    )
}
