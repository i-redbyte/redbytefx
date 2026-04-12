**English** · [Русский](README.ru.md)

# RedByteFX

[![JitPack](https://jitpack.io/v/i-redbyte/redbytefx.svg)](https://jitpack.io/#i-redbyte/redbytefx)

**RedByteFX** is a Kotlin-first DSL for authoring Android AGSL effects without living inside giant shader strings.

AGSL is powerful, but from Kotlin it often feels like the wrong shape of tool:

- shader logic lives in strings instead of code
- uniforms are manual and easy to mistype
- even simple effects collect noisy boilerplate
- Compose integration usually needs extra glue
- debugging starts with "where did I break this string?"

RedByteFX keeps the real Android pipeline exactly where it belongs:

`Kotlin DSL -> generated AGSL -> RuntimeShader / RenderEffect`

So you still get the native AGSL runtime path, but the authoring experience becomes much nicer: typed expressions, readable locals via `let(...)`, reusable helpers via `fn(...)`, Compose bindings, and generated AGSL you can inspect anytime with `agslSource()`.

In other words: **less stringly-typed pain, less glue code, no second runtime, no need to give up performance or flexibility just to make AGSL pleasant.**

## Raw AGSL vs RedByteFX

Same idea, same math, very different ergonomics.

### Hand-written AGSL

```glsl
uniform shader content;
uniform float wave_amplitude;
uniform float wave_frequency;

half4 main(float2 fragCoord) {
  float2 offset = float2(0.0, sin(fragCoord.x * wave_frequency) * wave_amplitude);
  return content.eval(fragCoord + offset);
}
```

### RedByteFX

```kotlin
val wave = redbytefx {
    val amplitude = uniformFloat(0f, "wave_amplitude")
    val frequency = uniformFloat(0.08f, "wave_frequency")
    val offset = float2(0f, sin(fragCoord.x * frequency) * amplitude)
    sample(fragCoord + offset)
}
```

The shader is now ordinary Kotlin-shaped code: typed expressions, IDE completion, refactor-friendly names, and runtime params you bind without juggling raw AGSL strings.

## Sample-sized example

Here is a real slice of the sample app. This is the kind of effect that gets bulky fast in raw AGSL, but stays readable when the building blocks are Kotlin functions and typed shader expressions:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(0.82f)
    val warmth by autoUniformFloat(0.58f)
    val glow by autoUniformFloat(0.38f)

    val base = let(sample(), "base")
    val saturated = let(
        adjustSaturation(base, mix(0.9f, 1.55f, amount)),
        "saturated"
    )
    val tint = let(
        color(
            mix(0.26f, 0.94f, warmth),
            mix(0.48f, 0.72f, warmth),
            mix(0.92f, 0.38f, warmth),
            base.a
        ),
        "tint"
    )
    val multiplied = let(blendMultiply(saturated, tint, 0.25f), "multiplied")
    val screened = let(blendScreen(multiplied, tint, glow), "screened")

    blendOverlay(base, screened, amount)
}
```

You still control the shader logic directly, but the code finally looks like something Kotlin developers can read without bargaining with a long string literal.

## Install

Public releases are distributed through JitPack.

Add the repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content {
                includeGroup("com.github.i-redbyte.redbytefx")
            }
        }
    }
}
```

Then add the modules you need:

```kotlin
dependencies {
    implementation("com.github.i-redbyte.redbytefx:redbytefx-core:1.0.0")
    implementation("com.github.i-redbyte.redbytefx:redbytefx-compose:1.0.0")
    implementation("com.github.i-redbyte.redbytefx:redbytefx-stdlib:1.0.0")
}
```

| Artifact | What it gives you |
|----------|-------------------|
| `redbytefx-core` | DSL, compiler, uniforms, runtime bridge, `redbytefx { }` |
| `redbytefx-compose` | `FxController`, `rememberFxController`, `Modifier.redbyteFx`, Compose bindings |
| `redbytefx-stdlib` | Reusable shader helpers: masks, ramps, blends, noise, SDF, routing, lighting, and more |

**Requirements:** Android **API 33+**. Add `redbytefx-compose` only when you want the Compose runtime bindings.

## Typical workflow

1. Write an effect with `redbytefx { ... }`.
2. Inspect the generated shader with `effect.agslSource()` whenever you want to verify the real AGSL shape.
3. Create or remember a runtime instance/controller.
4. Bind uniforms from state, animation, or time.
5. Apply it via `Modifier.redbyteFx(...)` or the lower-level runtime API.

Example with Compose:

```kotlin
val waveEffect = run {
    var amplitude: FxParam.Float? = null
    var frequency: FxParam.Float? = null
    val effect = redbytefx {
        val amp = uniformFloat(0f, "wave_amplitude")
        val freq = uniformFloat(0.08f, "wave_frequency")
        amplitude = amp
        frequency = freq
        val offset = float2(0f, sin(fragCoord.x * freq) * amp)
        sample(fragCoord + offset)
    }
    Triple(effect, amplitude!!, frequency!!)
}

@Composable
fun WaveLabel(amp: Float, freq: Float) {
    val fx = rememberFxController(waveEffect.first)
    fx.bindFloat(waveEffect.second, amp)
    fx.bindFloat(waveEffect.third, freq)
    Text("RedByteFX", modifier = Modifier.redbyteFx(fx))
}
```

## How the library thinks

- **Kotlin-first authoring.** Shader code should feel native to Kotlin, not like a string templating exercise.
- **Typed expressions and uniforms.** The DSL helps you catch mistakes earlier and makes refactors far less scary.
- **Readable generated AGSL.** `let(...)`, `fn(...)`, and `agslSource()` keep the final shader inspectable instead of mysterious.
- **No custom rendering engine.** The result still goes through the standard Android `RuntimeShader` / `RenderEffect` path.
- **Composable layers.** `core` gives you the language, `compose` gives you runtime bindings, `stdlib` gives you reusable higher-level recipes.

## Repository modules

| Module | Role |
|--------|------|
| `:redbytefx-core` | Authoring DSL, compiler, runtime bridge |
| `:redbytefx-compose` | Compose integration |
| `:redbytefx-stdlib` | Reusable higher-level shader helpers |
| `:sample` | Demo application with real effects and generated AGSL previews |

## Contributing

Issues, ideas, PRs, experiments, and "I made AGSL slightly less grumpy" improvements are welcome.

Small focused changes are easiest to review, but larger ideas are welcome too. If you plan something big, opening an issue first is a great way to align on direction before anyone writes a heroic amount of shader code at 2 a.m.

## License

Released under the [MIT](LICENSE) license.
