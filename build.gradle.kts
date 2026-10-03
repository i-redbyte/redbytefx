import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaExtension

plugins {
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.vanniktech.maven.publish.base) apply false
}

version = providers.gradleProperty("redbytefx.version").orElse("1.1.0").get()
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
        extensions.findByType<DokkaExtension>()?.let { dokka ->
            dokka.moduleName.set(project.name)
            dokka.modulePath.set(".")
            val isAndroidLibrary = plugins.hasPlugin("com.android.library")
            if (isAndroidLibrary) {
                dependencies.add("dokkaPlugin", rootProject.libs.dokka.android.doc)
            }
            val mainJava = layout.projectDirectory.dir("src/main/java")
            if (mainJava.asFile.isDirectory) {
                val modulePath = path.removePrefix(":").replace(':', '/')
                val docsGitRef = providers.environmentVariable("GITHUB_REF_NAME")
                    .orElse(providers.gradleProperty("redbytefx.docsGitRef"))
                    .orElse("master")
                    .get()
                dokka.dokkaSourceSets.configureEach {
                    sourceLink {
                        localDirectory.set(mainJava)
                        remoteUrl(
                            "https://github.com/i-redbyte/redbytefx/blob/$docsGitRef/" +
                                "$modulePath/src/main/java",
                        )
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
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

private data class DocModule(val path: String, val title: String, val blurb: String)

private val docModules = listOf(
    DocModule(":redbytefx-core", "redbytefx-core", "Shader DSL, compiler, and AGSL instance"),
    DocModule(":redbytefx-gl", "redbytefx-gl", "OpenGL ES 3.x program runtime"),
    DocModule(":redbytefx-gl-compose", "redbytefx-gl-compose", "Compose GlSurface and GlController"),
    DocModule(":redbytefx-compose", "redbytefx-compose", "AGSL FxController and redbyteFx"),
    DocModule(":redbytefx-stdlib", "redbytefx-stdlib", "Fragment helpers and SDF recipes"),
)

tasks.register("dokkaHtmlAll") {
    group = "documentation"
    description = "Generate HTML API reference for all library modules."
    dependsOn(docModules.map { "${it.path}:dokkaGeneratePublicationHtml" })
}

tasks.register("dokkaHtmlSite") {
    group = "documentation"
    description = "Assemble a GitHub Pages site with an index and per-module Dokka output."
    dependsOn("dokkaHtmlAll")
    val siteDir = layout.buildDirectory.dir("docs/site")
    outputs.dir(siteDir)
    doLast {
        val site = siteDir.get().asFile
        if (site.exists()) {
            site.deleteRecursively()
        }
        site.mkdirs()
        val entries = docModules.map { module ->
            val projectDir = project(module.path).layout.buildDirectory.get().asFile
            val source = projectDir.resolve("dokka/html")
            val targetName = module.path.removePrefix(":")
            val target = site.resolve(targetName)
            target.mkdirs()
            check(source.resolve("index.html").isFile && source.resolve("styles/style.css").isFile) {
                "Dokka HTML for ${module.path} is missing at ${source}. " +
                    "The Android library plugin must be applied before Dokka so source sets are registered."
            }
            source.copyRecursively(target, overwrite = true)
            // Dokka's cover page only names the package. Declarations, including functions,
            // live on the package page and the Functions tab is hidden until clicked.
            target.resolve("styles/style.css").appendText(
                "\n.tabs-section-body > [data-togglable] { display: block !important; }\n",
            )
            val packagePage = dokkaPackagePage(target.resolve("index.html"))
            if (packagePage != null) {
                target.resolve("index.html").writeText(dokkaEntryRedirect(module.title, packagePage))
            }
            module to (packagePage ?: "index.html")
        }
        val docsDir = rootProject.file("docs")
        if (docsDir.isDirectory) {
            val siteDocs = site.resolve("docs")
            siteDocs.mkdirs()
            docsDir.listFiles()?.filter { it.extension == "md" }?.forEach { file ->
                file.copyTo(siteDocs.resolve(file.name), overwrite = true)
            }
        }
        val links = entries.joinToString("\n") { (module, page) ->
            val slug = module.path.removePrefix(":")
            """        <li><a href="$slug/$page">${module.title}</a> - ${module.blurb}</li>"""
        }
        site.resolve("index.html").writeText(
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8"/>
              <title>RedByteFX API reference</title>
              <style>
                body { font-family: system-ui, sans-serif; max-width: 42rem; margin: 2rem auto; padding: 0 1rem; }
                a { color: #0b57d0; }
              </style>
            </head>
            <body>
              <h1>RedByteFX</h1>
              <p>Typed Kotlin shader DSL for Android AGSL and OpenGL ES 3.x.</p>
              <ul>
            $links
              </ul>
              <p><a href="docs/language-reference.md">Language reference</a> |
              <a href="docs/error-codes.md">Error codes</a></p>
              <p>Platform: library minSdk 24; AGSL requires API 31+.</p>
            </body>
            </html>
            """.trimIndent(),
        )
    }
}

private val dokkaPackageLink = Regex("""href="([^"]+/index\.html)"""")

private fun dokkaPackagePage(cover: java.io.File): String? {
    val pages = dokkaPackageLink.findAll(cover.readText())
        .map { it.groupValues[1] }
        .filter { !it.startsWith("http") && it != "index.html" }
        .distinct()
        .toList()
    return pages.singleOrNull()
}

private fun dokkaEntryRedirect(title: String, page: String): String = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="utf-8"/>
      <meta http-equiv="refresh" content="0; url=$page"/>
      <link rel="canonical" href="$page"/>
      <title>$title</title>
      <script>location.replace("$page")</script>
    </head>
    <body>
      <p><a href="$page">$title API reference</a></p>
    </body>
    </html>
""".trimIndent()

tasks.register("qualityCheck") {
    group = "verification"
    description = "Unit tests, sample compilation, and Detekt on main sources."
    dependsOn(
        ":redbytefx-core:testDebugUnitTest",
        ":redbytefx-gl:testDebugUnitTest",
        ":redbytefx-gl-compose:testDebugUnitTest",
        ":redbytefx-compose:testDebugUnitTest",
        ":redbytefx-stdlib:testDebugUnitTest",
        ":sample:compileDebugKotlin",
        ":redbytefx-core:detekt",
        ":redbytefx-gl:detekt",
        ":redbytefx-gl-compose:detekt",
        ":redbytefx-compose:detekt",
        ":redbytefx-stdlib:detekt",
        ":sample:detekt"
    )
}
