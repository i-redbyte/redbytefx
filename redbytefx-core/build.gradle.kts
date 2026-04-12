import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.vanniktech.maven.publish.base)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

android {
    namespace = "ru.redbyte.redbytefx"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }

}

kotlin {
    explicitApi()
}

dependencies {
    testImplementation(libs.junit4)
}

val hasSigningConfiguration =
    providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.secretKeyRingFile").isPresent ||
        providers.gradleProperty("signing.gnupg.keyName").isPresent ||
        providers.gradleProperty("signing.gnupg.passphrase").isPresent ||
        providers.gradleProperty("signing.gnupg.homeDir").isPresent ||
        providers.gradleProperty("signing.gnupg.executable").isPresent

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (hasSigningConfiguration) {
        signAllPublications()
    }
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = SourcesJar.Sources(),
            javadocJar = JavadocJar.Empty()
        )
    )
    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = "redbytefx-core",
        version = rootProject.version.toString()
    )
    pom {
        name.set("RedByteFX Core")
        description.set(
            "Typed Kotlin DSL and compiler for authoring Android AGSL effects."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/i-redbyte/redbytefx")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("i-redbyte")
                name.set("Ilya Sokolov")
                url.set("https://github.com/i-redbyte")
            }
        }
        scm {
            url.set("https://github.com/i-redbyte/redbytefx")
            connection.set("scm:git:git://github.com/i-redbyte/redbytefx.git")
            developerConnection.set("scm:git:ssh://git@github.com/i-redbyte/redbytefx.git")
        }
    }
}
