import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import kotlinx.validation.api.dump
import kotlinx.validation.api.filterOutNonPublic
import kotlinx.validation.api.loadApiFromJvmClasses
import java.util.jar.JarFile
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaExtension

buildscript {
    dependencies {
        classpath(libs.binary.compatibility.validator)
        classpath(libs.kotlin.metadata.jvm)
        classpath(libs.asm.tree)
    }
}

plugins {
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.vanniktech.maven.publish.base) apply false
}

version = providers.gradleProperty("redbytefx.version").orElse("1.1.1").get()
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
    // Every library publishes the same way; a module's gradle.properties names it.
    pluginManager.withPlugin("com.vanniktech.maven.publish.base") {
        configureRedbytePublishing()
        registerPublicApiTasks()
    }

    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        dependencies {
            add("detektPlugins", rootProject.libs.detekt.formatting)
        }
    }

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
            jvmTarget = "17"
            autoCorrect = findProperty("detektAutoCorrect") == "true"
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

/**
 * apiDump records the public JVM API of the compiled classes in api/<module>.api; apiCheck fails
 * when the compiled API no longer matches it, so an incompatible change cannot ship unnoticed.
 * Kotlin `internal` declarations are filtered out through their metadata.
 */
private fun Project.registerPublicApiTasks() {
    // The debug compile jar has the same public API as release and is already built for tests
    // and lint, so the check adds no second compilation.
    val classesJar = layout.buildDirectory.file(
        "intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar",
    )
    val dumpFile = layout.projectDirectory.file("api/$name.api")
    val module = path
    tasks.register("apiDump") {
        group = "verification"
        description = "Writes the public API of this library to api/$name.api."
        dependsOn("bundleLibCompileToJarDebug")
        inputs.file(classesJar)
        outputs.file(dumpFile)
        doLast {
            val file = dumpFile.asFile
            file.parentFile.mkdirs()
            file.writeText(publicApiOf(classesJar.get().asFile))
        }
    }
    tasks.register("apiCheck") {
        group = "verification"
        description = "Fails when the public API differs from api/$name.api."
        dependsOn("bundleLibCompileToJarDebug")
        inputs.file(classesJar)
        // files() instead of file(): a missing dump reaches the check below with its hint.
        inputs.files(dumpFile)
        doLast {
            val recorded = dumpFile.asFile
            check(recorded.isFile) { "$module has no ${recorded.name}; run ./gradlew apiDump" }
            val actual = publicApiOf(classesJar.get().asFile)
            check(recorded.readText() == actual) {
                val removed = recorded.readLines() - actual.lines().toSet()
                val added = actual.lines() - recorded.readLines().toSet()
                buildString {
                    append("Public API of $module changed. If intended, run ./gradlew apiDump.\n")
                    removed.filter { it.isNotBlank() }.forEach { append("- ").append(it).append('\n') }
                    added.filter { it.isNotBlank() }.forEach { append("+ ").append(it).append('\n') }
                }
            }
        }
    }
}

/**
 * Public API dump of a compiled classes jar. ComposableSingletons classes hold compiler-generated
 * lambdas whose names change with the source, so their blocks are dropped.
 */
private fun publicApiOf(jar: java.io.File): String {
    val dump = JarFile(jar).use { classes ->
        buildString { classes.loadApiFromJvmClasses().filterOutNonPublic().dump(this) }
    }
    return dump.split("\n\n")
        .filterNot { block -> "ComposableSingletons$" in block.lineSequence().first() }
        .joinToString("\n\n")
}

private fun Project.configureRedbytePublishing() {
    val pomName = checkNotNull(findProperty("redbytefx.pomName")) { "$path needs redbytefx.pomName" }
    val pomDescription = checkNotNull(findProperty("redbytefx.pomDescription")) {
        "$path needs redbytefx.pomDescription"
    }
    extensions.configure<MavenPublishBaseExtension> {
        publishToMavenCentral(automaticRelease = true)
        if (hasSigningConfiguration) {
            signAllPublications()
        }
        configure(
            AndroidSingleVariantLibrary(
                variant = "release",
                sourcesJar = SourcesJar.Sources(),
                javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            ),
        )
        coordinates(
            groupId = rootProject.group.toString(),
            artifactId = name,
            version = rootProject.version.toString(),
        )
        pom {
            name.set(pomName.toString())
            description.set(pomDescription.toString())
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
}

private data class DocModule(val path: String, val title: String, val blurb: String)

private val docModules = listOf(
    DocModule(":redbytefx-core", "redbytefx-core", "Shader language"),
    DocModule(":redbytefx-gl", "redbytefx-gl", "GLES resources and draws"),
    DocModule(":redbytefx-gl-compose", "redbytefx-gl-compose", "Compose host for a GLES scene"),
    DocModule(":redbytefx-compose", "redbytefx-compose", "AGSL"),
    DocModule(":redbytefx-stdlib", "redbytefx-stdlib", "Effect recipes"),
    DocModule(":redbytefx-3d", "redbytefx-3d", "CPU scene data, no driver"),
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
              <p>Platform: library minSdk 24; AGSL requires API 33+.</p>
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

private fun installGitHooks() {
    val source = rootDir.resolve("githooks")
    val gitDir = rootDir.resolve(".git")
    if (!source.isDirectory || !gitDir.isDirectory) {
        return
    }
    val destDir = gitDir.resolve("hooks")
    destDir.mkdirs()
    source.listFiles()?.filter { it.isFile }?.forEach { hook ->
        val dest = destDir.resolve(hook.name)
        hook.copyTo(dest, overwrite = true)
        dest.setExecutable(true)
    }
}

installGitHooks()

tasks.register("installGitHooks") {
    group = "verification"
    description = "Copy githooks/ into .git/hooks so detekt runs before each commit."
    doLast { installGitHooks() }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Unit tests, sample compilation, Detekt, Android Lint, and the public API check."
    dependsOn(
        ":redbytefx-core:apiCheck",
        ":redbytefx-gl:apiCheck",
        ":redbytefx-gl-compose:apiCheck",
        ":redbytefx-compose:apiCheck",
        ":redbytefx-stdlib:apiCheck",
        ":redbytefx-3d:apiCheck",
        ":redbytefx-core:lintDebug",
        ":redbytefx-gl:lintDebug",
        ":redbytefx-gl-compose:lintDebug",
        ":redbytefx-compose:lintDebug",
        ":redbytefx-stdlib:lintDebug",
        ":redbytefx-3d:lintDebug",
        ":sample:lintDebug",
        ":redbytefx-core:testDebugUnitTest",
        ":redbytefx-gl:testDebugUnitTest",
        ":redbytefx-gl-compose:testDebugUnitTest",
        ":redbytefx-compose:testDebugUnitTest",
        ":redbytefx-stdlib:testDebugUnitTest",
        ":redbytefx-3d:testDebugUnitTest",
        ":sample:testDebugUnitTest",
        ":sample:compileDebugKotlin",
        ":redbytefx-core:detekt",
        ":redbytefx-gl:detekt",
        ":redbytefx-gl-compose:detekt",
        ":redbytefx-compose:detekt",
        ":redbytefx-stdlib:detekt",
        ":redbytefx-3d:detekt",
        ":sample:detekt"
    )
}
