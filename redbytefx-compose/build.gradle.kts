import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.vanniktech.maven.publish.base)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

android {
    namespace = "ru.redbyte.redbytefx.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":redbytefx-core"))

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
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
        artifactId = "redbytefx-compose",
        version = rootProject.version.toString()
    )
    pom {
        name.set("RedByteFX Compose")
        description.set(
            "Jetpack Compose bindings and runtime controller layer for RedByteFX."
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
