import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    id("maven-publish")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
}

dependencies {
    testImplementation(libs.junit4)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = rootProject.group.toString()
                artifactId = "redbytefx-core"
                version = rootProject.version.toString()
                from(components["release"])
            }
        }
        repositories {
            mavenLocal()
            if (System.getenv("GITHUB_TOKEN") != null) {
                maven {
                    name = "GitHubPackages"
                    url = uri(
                        "https://maven.pkg.github.com/" +
                            (System.getenv("GITHUB_REPOSITORY") ?: "i-redbyte/redbytefx")
                    )
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: ""
                        password = System.getenv("GITHUB_TOKEN") ?: ""
                    }
                }
            }
        }
    }
}
