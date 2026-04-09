plugins {
    alias(libs.plugins.android.library)
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
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":redbytefx-core"))

    testImplementation(libs.junit4)
}
