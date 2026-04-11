plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs the minimum v0.2 quality gate: unit tests plus sample compilation."
    dependsOn(
        ":redbytefx-core:testDebugUnitTest",
        ":redbytefx-compose:testDebugUnitTest",
        ":redbytefx-stdlib:testDebugUnitTest",
        ":sample:compileDebugKotlin"
    )
}
