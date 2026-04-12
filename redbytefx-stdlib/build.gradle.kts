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
    namespace = "ru.redbyte.redbytefx.stdlib"
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
    api(project(":redbytefx-core"))

    testImplementation(libs.junit4)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = rootProject.group.toString()
                artifactId = "redbytefx-stdlib"
                version = rootProject.version.toString()
                from(components["release"])
                pom {
                    name.set("RedByteFX Stdlib")
                    description.set(
                        "Reusable shader recipes and helpers built on top of the RedByteFX DSL."
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
