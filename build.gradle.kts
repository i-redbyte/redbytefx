import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
}

version = providers.gradleProperty("redbytefx.version").orElse("1.0.0").get()
group = "com.github.i-redbyte"

subprojects {
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

