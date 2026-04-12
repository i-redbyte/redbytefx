**English** · [Русский](README.ru.md)

# RedByteFX

Write **Android shaders** in **Kotlin**, compile to **AGSL**, and drive them with **Jetpack Compose** — with typed uniforms, readable locals, and `effect.agslSource()` whenever you want to see exactly what ships to the GPU.

---

## Why it’s fun

- **Shader code that still feels like math** — `sin`, `mix`, `float2`, `sample()`, `let(...)`, `fn(...)`, without hand-maintaining string shaders.
- **AGSL you can read** — the DSL stays close to generated AGSL so debugging stays grounded.
- **Compose-friendly** — `rememberFxController`, `bindFloat` / `bindTime`, and `Modifier.redbyteFx(...)` for live UI.
- **Pick your depth** — stay in **core** for full control, or layer **stdlib** helpers (masks, ramps, noise, SDF recipes, and more) when you want speed.

---

## Requirements

- **Android API 33+** (the runtime path used here targets modern AGSL / `RuntimeShader` usage).
- **Kotlin** and a recent **Android Gradle Plugin** (see the repo’s `gradle/libs.versions.toml`).
- **Jetpack Compose** if you use `:redbytefx-compose` (the Compose BOM is exposed from that artifact so versions stay aligned).

---

## Add RedByteFX **1.0.0** to your app

### 1. GitHub Packages Maven repository

[Create a token](https://github.com/settings/tokens) with the **`read:packages`** scope. Never commit it — use `~/.gradle/gradle.properties` or CI secrets.

In **`settings.gradle.kts`** (inside `dependencyResolutionManagement { repositories { ... } }`):

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/i-redbyte/redbytefx")
    credentials {
        username = providers.gradleProperty("gpr.user").orNull
            ?: System.getenv("GITHUB_ACTOR")
            ?: ""
        password = providers.gradleProperty("gpr.token").orNull
            ?: System.getenv("GITHUB_TOKEN")
            ?: ""
    }
}
```

In **`gradle.properties`** (your machine only):

```properties
gpr.user=YOUR_GITHUB_LOGIN
gpr.token=YOUR_TOKEN
```

*(On GitHub Actions, `GITHUB_TOKEN` is injected automatically for workflows in this repository.)*

### 2. Dependencies

```kotlin
dependencies {
    implementation("com.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

| Artifact | What it gives you |
|----------|-------------------|
| `redbytefx-core` | DSL, compiler, uniforms, `redbytefx { }`, AGSL output |
| `redbytefx-compose` | `FxController`, `Modifier.redbyteFx`, bindings |
| `redbytefx-stdlib` | Reusable shader recipes (optional but handy) |

You can also depend only on **`redbytefx-compose`** and **`redbytefx-stdlib`** — **`redbytefx-core`** is pulled in transitively. The three-line block above is explicit and easy to copy.

---

## Tiny end-to-end snippet

Add the usual Compose imports plus `ru.redbyte.redbytefx.*` (and `grayscale` / `sample` / `mix` from the DSL).

```kotlin
val grayscaleDemo = run {
    var amount: FxParam.Float? = null
    val effect = redbytefx {
        val amountUniform by autoUniformFloat(0f)
        amount = amountUniform
        val base = let(sample(), "base")
        mix(base, grayscale(base), amountUniform)
    }
    effect to amount!!
}

@Composable
fun HelloFx(amount: Float) {
    val fx = rememberFxController(grayscaleDemo.first)
    fx.bindFloat(grayscaleDemo.second, amount)
    Text(
        text = "RedByteFX",
        modifier = Modifier.redbyteFx(fx)
    )
}
```

Use **one controller per render target**, bind only uniforms that belong to **that** compiled effect, and call `agslSource()` when something looks off.

---

## Modules in this repo

| Gradle module | Role |
|---------------|------|
| `:redbytefx-core` | Language, compiler, runtime bridge |
| `:redbytefx-compose` | Compose integration |
| `:redbytefx-stdlib` | Higher-level helpers |
| `:sample` | Demo app (source only — not published as a library) |

---

## Build & verify locally

```bash
./gradlew qualityCheck
```

Publish the three libraries to **Maven Local** (no GitHub token needed):

```bash
./gradlew :redbytefx-core:publishAllPublicationsToMavenLocalRepository \
  :redbytefx-compose:publishAllPublicationsToMavenLocalRepository \
  :redbytefx-stdlib:publishAllPublicationsToMavenLocalRepository
```

---

## Contributing — you’re welcome here

Whether you fix a typo, tighten an error message, add a test, or propose a small stdlib helper — **thank you** for showing up.

- **Talk first** for large changes — open an issue so we can align before a huge PR.
- **Keep PRs focused** — small, reviewable steps land faster.
- Run **`./gradlew qualityCheck`** before you push.
- **Public API** changes should carry **KotlinDoc** updates in the same commit.

Maintainers care about clarity, kindness, and code that future readers will understand. If that sounds like you, we’d love to collaborate.

---

## License

Released under the **MIT License** — see [LICENSE](LICENSE).
