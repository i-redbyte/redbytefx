import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    id("maven-publish")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = rootProject.group.toString()
                artifactId = "redbytefx-compose"
                version = rootProject.version.toString()
                from(components["release"])
                pom {
                    name.set("RedByteFX Compose")
                    description.set(
                        "Jetpack Compose bindings and runtime controller layer for RedByteFX."
                    )
                    url.set("https://github.com/i-redbyte/redbytefx")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/license/mit")
                        }
                    }
                    developers {
                        developer {
                            id.set("i-redbyte")
                            name.set("i-redbyte")
                        }
                    }
                    scm {
                        url.set("https://github.com/i-redbyte/redbytefx")
                        connection.set("scm:git:git://github.com/i-redbyte/redbytefx.git")
                        developerConnection.set("scm:git:ssh://git@github.com/i-redbyte/redbytefx.git")
                    }
                }
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
