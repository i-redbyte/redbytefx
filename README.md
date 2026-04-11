# redbytefx

`redbytefx` is an Android shader-effect library built around a Kotlin-first DSL that compiles to AGSL and plugs into `RuntimeShader` / `RenderEffect` workflows.

## Status

- current level: `v0.2 closed / early v0.3`
- current platform target: Android `minSdk 33+`
- current roadmap: `v0.2 Shader stdlib` -> `v0.3 Tooling` -> `v0.4 Authoring UX` -> `v0.5 Runtime quality` -> `v0.6 Library shaping`
- publication is intentionally out of scope for now; the library is still raw
- current design rule: keep runtime integration tiny, keep generated AGSL predictable, grow reusable helpers in `stdlib` instead of bloating `core`

## Modules

- `:redbytefx-core`: the DSL, compiler, uniform model, and runtime shader bridge
- `:redbytefx-compose`: Compose adapter with `Modifier.redbyteFx(...)`
- `:redbytefx-stdlib`: recipe-level authoring helpers that build on top of the core DSL
- `:sample`: searchable demo app for exploring the DSL, testing effects visually, and inspecting generated AGSL

## Current Capabilities

- low-level shader authoring through typed expressions, uniforms, functions, local variables, and AGSL-like math/color primitives
- low-level shader authoring stays close to AGSL, including direct left-hand whole-number math such as `1 - amount` and `2 * uv`
- Compose runtime integration through `rememberFxController(...)`, `Modifier.redbyteFx(...)`, and direct state/time binding helpers
- a broad `stdlib` surface covering procedural, color, masks, timing, compositing, transitions, gradients, signal, polar, lighting, SDF, and routing helpers
- a strong sample catalog with search, section jumps, starter routes, a canonical family map, canonical/start-here filters, per-demo focus metadata/tags, related-demo recommendations, live controls, copyable DSL snippet, generated AGSL inspection, and a phone-friendly preview-first layout
- an early but real canonical teaching surface across package docs, sample family maps, demo path guidance, and follow-up routing

## Current Limitations

- Android-only for now, built around AGSL-backed `RuntimeShader`
- `minSdk 33` is currently required
- the library surface is still raw and not API-frozen
- `stdlib` is already broad, but it still needs a naming and curation pass before it feels deliberately shaped
- authoring UX is still early: diagnostics, migration docs, and cookbook material are not at the right level yet
- `v0.6` shaping has started in docs/sample guidance, but the canonical surface is still being formalized rather than treated as stable

## Project Docs

- `CHANGELOG.md` tracks milestone-level changes.
- `docs/milestone-snapshot.md` gives a short cross-version status snapshot of what is solid, what is still raw, and what should happen next.
- `docs/v0.2-status.md` explains how the shader-stdlib milestone was closed and what kind of surface work still remains after that closure.
- `docs/roadmap.md` describes the current application/tooling state and the roadmap from `v0.3` to `v0.6`.
- `docs/agsl-vs-redbytefx.md` explains the authoring mental model and how to read generated AGSL.
- `docs/cookbook-patterns.md` captures the first translation patterns from raw AGSL/Shadertoy-style code into the DSL, including a few end-to-end rewrite examples.
- `docs/runtime-audit-v0.5.md` records the current runtime audit baseline, representative demo flows, and the reproducible `Radar` / `Circuit` runtime measurement path.
- `docs/backlog-v0.4-v0.6.md` turns the later roadmap stages into a concrete pre-publication backlog.

## Quickstart

1. Define an effect with `redbytefx { ... }` and keep typed uniform handles when you need runtime updates.
   Names are optional: use `uniformFloat(0f)` for anonymous generated uniforms, or `val waveAmplitude by autoUniformFloat(0f)` when you want the Kotlin property name to become a readable snake_case shader name automatically.
2. In Compose, create one controller per render target with `rememberFxController(effect)`.
3. Bind ordinary Compose state with `bindFloat(...)` / `bindFloat2(...)` or drive time with `bindTime(...)`.
4. Apply the effect with `Modifier.redbyteFx(fx)`.

```kotlin
val grayscaleSetup = run {
    var amountParam: FxParam.Float? = null
    val effect = redbytefx {
        val amount by autoUniformFloat(0f)
        amountParam = amount
        val base = let(sample(), "base")
        mix(base, grayscale(base), amount)
    }
    effect to amountParam!!
}

@Composable
fun Example(amount: Float) {
    val fx = rememberFxController(grayscaleSetup.first)
    fx.bindFloat(grayscaleSetup.second, amount)

    Text(
        text = "RedByteFX",
        modifier = Modifier.redbyteFx(fx)
    )
}
```

## Example

```kotlin
var amplitude: FxParam.Float? = null
var frequency: FxParam.Float? = null

val effect = redbytefx {
    val amp = uniformFloat(0f, "wave_amplitude")
    val freq = uniformFloat(0.08f, "wave_frequency")
    amplitude = amp
    frequency = freq

    val waveOffset = float2(
        0f,
        sin(fragCoord.x * freq) * amp
    )

    sample(fragCoord + waveOffset)
}
```

The DSL is intentionally expression-oriented:

- create uniforms with `uniformFloat(...)` or `uniformFloat2(...)`
- skip manual string labels when you want to with `autoUniformFloat(...)`, `autoUniformTime(...)`, `autoUniformFloat2(...)`, `autoUniformFloat3(...)`, and `autoUniformFloat4(...)`
- animate effects over time with `uniformTime(...)` and `FxController.bindTime(...)` in Compose, with pause/resume preserving the current phase
- bind Compose state directly to uniforms with `bindFloat(...)`, `bindFloat2(...)`, `bindFloat3(...)`, and `bindFloat4(...)`
- cache intermediate expressions with `let(...)` when you want real local variables in generated AGSL
- define reusable AGSL helpers with `fn(...)` and call them from the main shader body
- compose math with operators and shader-like helpers such as `sin`, `cos`, `mix`, `clamp`, `length`, `floor`, `ceil`, `pow`, `fract`, `mod`, `step`, `smoothstep`
- branch with `ifElse(...)` and boolean expressions like `(value gt 0.5f) or other`
- work with `float2`, `float3`, `float4` and colors through `color(...)`, `withAlpha(...)`, `luminance(...)`, `grayscale(...)`, `mix(...)`
- return a final color via `sample(...)` or `sampleUnclamped(...)`

## Stdlib Example

`redbytefx-core` stays close to raw shader authoring. `:redbytefx-stdlib` is where reusable recipes live:

```kotlin
import ru.redbyte.redbytefx.stdlib.gridMask
import ru.redbyte.redbytefx.stdlib.pulse

val effect = redbytefx {
    val time by autoUniformTime()
    val base = let(sample(), "base")
    val uv = let(fragCoord / resolution, "uv")
    val grid = let(gridMask(uv, density = 10f, lineWidth = 0.06f), "grid")
    val drive = let(pulse(time, speed = 1.35f, phase = uv.x * 0.35f), "drive")
    val tint = color(float3(0.08f, 0.95f, 0.82f), base.a)

    mix(base, tint, grid * drive * 0.85f)
}
```

## DSL vs AGSL

The goal is not to hide shaders. The goal is to keep shader authoring readable, typed, and easier to wire into Android UI.

DSL:

```kotlin
val effect = redbytefx {
    val amp = uniformFloat(12f, "amp")
    val freq = uniformFloat(0.08f, "freq")
    val waveOffset = float2(0f, sin(fragCoord.x * freq) * amp)
    sample(fragCoord + waveOffset)
}
```

Generated AGSL shape:

```glsl
uniform shader rb_input;
uniform float2 rb_resolution;
uniform float u_amp;
uniform float u_freq;

half4 main(float2 fragCoord) {
  float2 waveOffset = float2(0.0, sin(fragCoord.x * u_freq) * u_amp);
  return rb_sample(fragCoord + waveOffset);
}
```

`redbytefx` is meant to stay close enough to AGSL that the generated shader is still predictable.

If you're learning or comparing approaches, start with [docs/agsl-vs-redbytefx.md](docs/agsl-vs-redbytefx.md) and [docs/cookbook-patterns.md](docs/cookbook-patterns.md), then use the sample app to inspect the DSL and generated AGSL side by side.

## v0.1 Core Stdlib

For `v0.1`, `redbytefx-core` is intentionally limited to the minimum shader-authoring surface we expect most effects to need:

- components and channels: `.x/.y/.z/.w`, `.r/.g/.b/.a`
- constructors and reinterpretation: `float2`, `float3`, `float4`, `color`
- control flow: `ifElse(...)`
- operators: scalar/vector/color math plus boolean comparisons
- fundamental math: `sin`, `cos`, `abs`, `min`, `max`, `clamp`, `saturate`, `floor`, `ceil`, `fract`, `mod`, `pow`, `step`, `smoothstep`, `mix`, `length`, `radians`
- fundamental color helpers: `withAlpha`, `luminance`, `grayscale`

What is deliberately *not* part of the `v0.1` core stdlib:

- ready-made visual styles
- noise, hash, palette, scanline, vignette, posterize, or other recipe-level helpers
- large convenience layers that hide the generated AGSL too much

Those belong in separate layers once the core language feels settled.

## v0.2 Shader Stdlib

`v0.2` is the shader-stdlib milestone.

- `:redbytefx-core` stays focused on the language, compiler, uniforms, and essential math/color primitives
- `:redbytefx-compose` keeps runtime integration explicit and small
- `:redbytefx-stdlib` carries the reusable authoring layer without hiding the generated shader too much

The current `stdlib` coverage includes:

- mapping and quantization: `inverseLerp(...)`, `remap(...)`, `posterize(...)`
- procedural and pattern helpers: `pulse(...)`, `gridMask(...)`, `scanlines(...)`, `hash21(...)`, `valueNoise(...)`, `grain(...)`, `vignette(...)`, `fbm(...)`, `domainWarp(...)`
- color and compositing: `adjustSaturation(...)`, `blendMultiply(...)`, `blendScreen(...)`, `blendOverlay(...)`, `maskedMix(...)`, `alphaMask(...)`, `maskedScreen(...)`, `maskedOverlay(...)`
- masks and transitions: `circleMask(...)`, `ringMask(...)`, `rectMask(...)`, `horizontalReveal(...)`, `verticalReveal(...)`, `radialReveal(...)`
- signal, gradients, polar, and lighting helpers: `bandMask(...)`, `signalBars(...)`, `scanWarp(...)`, `linearRamp(...)`, `radialRamp(...)`, `directionalSweep(...)`, `radialDistance(...)`, `polarAngle01(...)`, `polarCoordinates(...)`, `angularSweep(...)`, `arcMask(...)`, `centeredUv(...)`, `aspectCenteredUv(...)`, `radialDirection(...)`, `centerGlow(...)`, `rimLight(...)`
- SDF, frame, and routing helpers: `sdCircle(...)`, `sdBox(...)`, `sdRoundedBox(...)`, `sdSegment(...)`, `fill(...)`, `softFill(...)`, `stroke(...)`, `softStroke(...)`, `segmentMask(...)`, `segmentProgress(...)`, `segmentPulse(...)`, `edgeDistance(...)`, `edgeFade(...)`, `frameMask(...)`, `cornerMask(...)`

The main remaining work for `v0.2` is no longer raw helper volume. It is curation:

- audit which helpers are truly canonical and which still feel noisy
- normalize naming, defaults, and parameter ordering
- improve KotlinDoc and module-level guidance around `core` vs `stdlib`
- tighten compiler/golden coverage for representative helpers in each family

The current first-pass “teach this first” shortlist looks like:

- coordinates: `normalizedUv(...)`, `sampleUv(...)`, `centeredUv(...)`, `aspectCenteredUv(...)`
- masking/reveal: `circleMask(...)`, `rectMask(...)`, `ringMask(...)`, `horizontalReveal(...)`, `verticalReveal(...)`, `radialReveal(...)`
- compositing/shaping: `maskedMix(...)`, `alphaMask(...)`, `maskedScreen(...)`, `sdCircle(...)`, `sdRoundedBox(...)`, `softFill(...)`, `softStroke(...)`
- timing/signal/gradients: `pulse(...)`, `bandMask(...)`, `linearRamp(...)`, `radialRamp(...)`, `angularSweep(...)`
- routing/scene structure: `segmentMask(...)`, `segmentProgress(...)`, `segmentPulse(...)`

Broader helpers like `fbm(...)`, `domainWarp(...)`, `chromaticOffset(...)`, `frameMask(...)`, and `cornerMask(...)` remain useful, but they should not define the first mental model for the library.

They currently make the most sense as a secondary exploratory layer:

- `fbm(...)` and `domainWarp(...)` for heavier procedural space-warp recipes
- `chromaticOffset(...)` for stylized sampling/distortion passes
- `frameMask(...)` and `cornerMask(...)` for decorative panel chrome after the simpler mask vocabulary is already in place

The sample home screen now mirrors the same family map so the recommended teaching surface stays aligned between README, package docs, and live demos.

## Procedural Example

```kotlin
val effect = redbytefx {
    val density = uniformFloat(8f, "signal_density")
    val width = uniformFloat(0.08f, "signal_width")
    val pulseBand = fn(
        name = "pulse_band",
        arg1 = FloatType,
        arg2 = FloatType,
        returns = FloatType
    ) { phase, threshold ->
        step(threshold, smoothstep(0.08f, 0.92f, fract(phase)))
    }

    val base = let(sample(), "base")
    val uv = let(fragCoord / resolution, "uv")
    val cell = let(fract(uv * density), "cell")
    val edgeX = let(min(cell.x, 1f - cell.x), "edge_x")
    val edgeY = let(min(cell.y, 1f - cell.y), "edge_y")
    val grid = let(
        max(
            1f - smoothstep(0f, width, edgeX),
            1f - smoothstep(0f, width, edgeY)
        ),
        "grid"
    )
    val scan = let(1f - smoothstep(0f, 3f, mod(fragCoord.y, 14f)), "scan")
    val pulse = let(pulseBand(uv.y * density * 0.5f + grid * 0.35f, 0.55f), "pulse")
    val active = let((grid gt 0.05f) or (step(0.45f, scan * pulse) gt 0.5f), "active")

    ifElse(active, mix(base, color(float3(0.05f, 0.95f, 0.82f), base.a), 0.85f), base)
}
```

In Compose you can drive time like this:

```kotlin
val fx = rememberFxController(effect)
fx.bindTime(timeParam)

Text(
    text = "RedByteFX",
    modifier = Modifier.redbyteFx(fx)
)
```

You can also bind ordinary Compose state directly to uniforms:

```kotlin
val fx = rememberFxController(effect)
fx.bindFloat(amountParam, amount)
fx.bindFloat2(offsetParam, dx, dy)

Text(
    text = "RedByteFX",
    modifier = Modifier.redbyteFx(fx)
)
```

## Debugging

- Call `effect.agslSource()` whenever you want to inspect the generated shader source.
- Keep one `FxController` per render target so resolution and runtime state stay unambiguous.
- Use `:sample` as a cookbook: each demo now keeps preview and live controls near the top on phones, collapses the heavier DSL/AGSL inspection below, and still shows a few sensible follow-up demos.
- Use `ru.redbyte.redbytefx.sample.extra.START_DEMO` when launching `:sample` from `adb`; the app now retargets the running activity instead of only honoring the extra on cold start.
- Reach for `:redbytefx-stdlib` when a helper reads like a reusable recipe instead of a fundamental shader primitive.

## Current Focus

The sample app already contains a meaningful slice of `v0.3 Tooling`, but the library itself still needs a proper `v0.2` curation pass. The next steps are to finish the shader-stdlib milestone cleanly, then lean into tooling and authoring UX before any publication conversation starts.
