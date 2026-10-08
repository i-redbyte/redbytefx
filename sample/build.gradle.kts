plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

android {
    namespace = "ru.redbyte.redbytefx.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.redbyte.redbytefx.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.1"
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

dependencies {
    implementation(project(":redbytefx-compose"))
    implementation(project(":redbytefx-stdlib"))
    implementation(project(":redbytefx-gl"))
    implementation(project(":redbytefx-gl-compose"))
    implementation(project(":redbytefx-3d"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
}
