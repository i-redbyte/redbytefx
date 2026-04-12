**English** · [Русский](README.ru.md)

# RedByteFX

**RedByteFX** is a Kotlin-first DSL for Android AGSL that lets you write shader logic as real Kotlin-shaped code instead of long hand-written shader strings.

AGSL itself is powerful, but from Kotlin it is usually awkward:

- shader code lives inside strings
- uniforms are manual and easy to mistype
- even small effects grow a lot of boilerplate
- Compose wiring often needs extra runtime plumbing
- debugging starts with “which string did I just break?”

RedByteFX keeps the actual Android runtime path intact:

`Kotlin DSL -> generated AGSL -> RuntimeShader / RenderEffect`

So the runtime stays native and predictable, while authoring becomes much nicer: typed expressions, readable locals through `let(...)`, reusable helpers through `fn(...)`, effect-specific uniform handles, Compose bindings, and generated AGSL you can inspect with `agslSource()`.

**Platform requirement:** RedByteFX targets **Android API 33+**. The library is built around the modern AGSL / `RuntimeShader` / `RenderEffect` stack and is not intended for lower API levels.

## Why this library exists

RedByteFX is for the common case where raw AGSL is technically the right runtime, but ergonomically the wrong authoring surface.

It helps when you want:

- shader code that feels native to Kotlin
- typed uniforms and expressions instead of stringly-typed glue
- readable ports of AGSL or Shadertoy-style math
- a direct path from authored code to inspectable generated AGSL
- Compose integration without building a separate rendering abstraction
- no performance tax for choosing a friendlier API

## Raw AGSL vs RedByteFX

Same idea, same math, much better ergonomics.

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

The shader is still explicit, but now it is real code: types, IDE completion, safer refactors, and runtime params you can bind without managing raw AGSL strings by hand.

## Example from the sample app

This is the kind of effect that gets noisy quickly in raw AGSL, but stays readable when expressions, locals, and compositing helpers are first-class Kotlin constructs:

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

## Install

RedByteFX is published to **Maven Central**, so for consumers the normal repository setup is enough:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

Add the modules you need:

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

### Version Catalog / TOML

`gradle/libs.versions.toml`:

```toml
[versions]
redbytefx = "1.0.0"

[libraries]
redbytefx-core = { module = "io.github.i-redbyte:redbytefx-core", version.ref = "redbytefx" }
redbytefx-compose = { module = "io.github.i-redbyte:redbytefx-compose", version.ref = "redbytefx" }
redbytefx-stdlib = { module = "io.github.i-redbyte:redbytefx-stdlib", version.ref = "redbytefx" }
```

Module `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.redbytefx.core)
    implementation(libs.redbytefx.compose)
    implementation(libs.redbytefx.stdlib)
}
```

## Modules

| Artifact | What it gives you |
|----------|-------------------|
| `redbytefx-core` | Authoring DSL, compiler, uniforms, runtime bridge, `redbytefx { }` |
| `redbytefx-compose` | `FxController`, `rememberFxController`, `Modifier.redbyteFx`, Compose bindings |
| `redbytefx-stdlib` | Higher-level shader helpers for coordinates, masks, compositing, routing, lighting, SDF, and more |

## Quick start

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

Typical flow:

1. Compile an effect once with `redbytefx { ... }`.
2. Inspect `effect.agslSource()` if you want to verify the generated AGSL shape.
3. Keep the returned `FxParam` handles for runtime binding.
4. Create an `FxInstance` or `FxController`.
5. Bind values and apply the effect.

## DSL guide

### Coordinates and sampling

- `fragCoord` is the current fragment coordinate in pixel space.
- `resolution` is the current render size in pixels.
- `sample()` reads the input content in pixel space and defaults to `fragCoord`.
- `sample(coord)` reads the input at another pixel-space coordinate.
- `sampleUnclamped(...)` skips coordinate clamping and is mainly for deliberate edge experiments.
- In `stdlib`, `normalizedUv()` converts from pixel space to normalized UV `[0, 1]²`.
- In `stdlib`, `sampleUv(uv)` converts normalized UV back into a sampled read.
- In `stdlib`, `centeredUv(...)` and `aspectCenteredUv(...)` are good starting points for masks, lighting, SDF, and local scene math.

### Uniforms and runtime params

- `uniformFloat`, `uniformFloat2`, `uniformFloat3`, `uniformFloat4` declare effect-owned uniforms.
- `uniformTime()` is just a specialized float uniform intended for elapsed time.
- `autoUniformFloat`, `autoUniformTime`, `autoUniformFloat2/3/4` derive readable debug names from the delegated Kotlin property name.
- The returned `FxParam.*` handles are both DSL expressions and runtime handles.
- Uniform handles are effect-specific: a param from one `redbytefx { ... }` block must not be reused with another compiled effect.

Example:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(0.5f)
    val shift = uniformFloat2(0f, 0f, "shift")
    sample(fragCoord + shift * amount)
}
```

### Expression types

- `FloatExpr` is a scalar float expression.
- `BoolExpr` is a boolean expression.
- `Float2Expr`, `Float3Expr`, `Float4Expr` model vector expressions.
- `ColorExpr` models color output and maps to AGSL `half4`.
- These are not runtime values. They are typed nodes in the shader expression tree that the compiler turns into AGSL.

### Constructors

- Use `float(...)` for scalar literals when you need an explicit scalar expression.
- Use `float2(...)`, `float3(...)`, `float4(...)` to build vectors.
- Use `color(r, g, b, a)` or `color(rgb, a)` to build color output.

Example:

```kotlin
val uv = fragCoord / resolution
val tint = color(0.2f, 0.8f, 1f, 1f)
val offset = float2(0f, sin(uv.x * 12f) * 6f)
sample(fragCoord + offset) * tint.a
```

### Operators and conditionals

- Scalar expressions support `+`, `-`, `*`, `/`.
- Vector expressions support vector-vector and vector-scalar math where it makes sense.
- Whole-number literals like `1 - amount` and `2 * uv` are supported to keep AGSL-style ports readable.
- Comparisons use `lt`, `lte`, `gt`, `gte`, `eq`, `neq`.
- `ifElse(condition, ifTrue, ifFalse)` selects between expressions inside the shader.
- Boolean expressions support `and`, `or`, and `!`.

Example:

```kotlin
val mask = ifElse(amount gt 0.5f, 1f, 0f)
```

### Built-in math and color helpers

Core covers the usual AGSL-style building blocks such as:

- `mix`, `clamp`, `smoothstep`, `step`, `saturate`
- `sin`, `cos`, `atan`, `pow`, `sqrt`, `abs`, `floor`, `ceil`, `fract`
- `min`, `max`, `mod`
- `dot`, `length`, `distance`, `normalize`
- `luminance`, `grayscale`

The intent is to stay close to AGSL vocabulary while keeping the call sites typed and Kotlin-friendly.

### Locals with `let(...)`

- `let(...)` stores an expression into a named local variable in the generated AGSL.
- This is one of the main tools for keeping large effects readable.
- If you pass a name, RedByteFX normalizes it into a safe AGSL identifier.

Example:

```kotlin
val base = let(sample(), "base")
val luma = let(luminance(base), "luma")
val mono = let(grayscale(base), "mono")
mix(base, mono, luma)
```

### Reusable helpers with `fn(...)` and `fnN(...)`

- `fn(...)` creates reusable AGSL helper functions with arity 0 to 4.
- `fnN(...)` handles helpers with five or more parameters.
- Use this when you want generated AGSL to keep a visible function boundary instead of becoming one very long expression tree.

Example:

```kotlin
val palette = fn(
    name = "palette_rgb",
    arg1 = FloatType,
    arg2 = FloatType,
    returns = Float3Type
) { tone, warmth ->
    val phase = let(tone * 6.2831855f, "phase")
    float3(
        0.24f + 0.45f * sin(phase + warmth * 0.90f + 0.10f),
        0.30f + 0.42f * sin(phase + warmth * 1.50f + 2.10f),
        0.42f + 0.36f * sin(phase + warmth * 2.10f + 4.20f)
    )
}
```

### Core transforms in `core`

Core already includes useful authoring helpers such as:

- `flipX`, `flipY`
- `mirrorX`, `mirrorY`
- basic coordinate transforms that stay close to the underlying AGSL model

These are good for direct ports before you move into richer `stdlib` helpers.

### Higher-level helpers in `stdlib`

`redbytefx-stdlib` adds reusable recipes built on top of the core DSL. Common first-stop helpers include:

- coordinates: `normalizedUv`, `sampleUv`, `centeredUv`, `aspectCenteredUv`
- masks and reveal: `circleMask`, `rectMask`, `ringMask`, `horizontalReveal`, `verticalReveal`, `radialReveal`
- compositing: `maskedMix`, `alphaMask`, `maskedScreen`, `maskedOverlay`, `blendMultiply`, `blendScreen`, `blendOverlay`
- lighting and shaping: `rimLight`, `centerGlow`, SDF helpers, routing helpers, noise and warp helpers

Good rule of thumb:

1. Stay in `core` when you are porting raw AGSL directly.
2. Move into `stdlib` when repeated coordinate, mask, compositing, or shaping patterns start appearing.

### Runtime surfaces

- `FxEffect` is the immutable compiled shader definition.
- `FxEffect.agslSource()` returns the generated AGSL for debugging, docs, or tests.
- `FxInstance` is the low-level mutable runtime API for imperative hosts.
- `FxController` in `redbytefx-compose` wraps one mutable runtime instance for Compose-friendly binding and invalidation.
- Use one controller or runtime instance per render target.

## Contributing

The project is open to external contributions.

Please keep the existing project style intact:

- preserve the Kotlin-first DSL feel
- keep generated AGSL readable
- prefer explicit naming and clear local variables over clever compression
- stay consistent with the current API naming, docs tone, and sample style

### PR flow

1. Fork the repository and create a focused branch.
2. Make the smallest complete change that solves one clear problem.
3. Run `./gradlew qualityCheck` and any relevant module tests.
4. Update docs or samples if the public API or behavior changed.
5. Open a PR with a short summary: problem, change, and how you verified it.

## License

Released under the [MIT](LICENSE) license.
