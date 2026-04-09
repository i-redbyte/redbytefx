# redbytefx

`redbytefx` is an Android shader-effect library built around a Kotlin-like DSL that compiles to AGSL.

The current direction is:

- keep the runtime integration tiny
- make the shader language expressive enough for real custom effects
- ship common transforms like `flip`, `mirror`, `rotate`, `scale`, `offset` as helpers, not as hardcoded engine limits

## Modules

- `:redbytefx-core`: the DSL, compiler, uniform model, and runtime shader bridge
- `:redbytefx-compose`: Compose adapter with `Modifier.redbyteFx(...)`
- `:sample`: manual demo app for exploring the DSL and testing effects visually

## Quickstart

1. Define an effect with `redbytefx { ... }` and keep typed uniform handles when you need runtime updates.
2. In Compose, create one controller per render target with `rememberFxController(effect)`.
3. Bind ordinary Compose state with `bindFloat(...)` / `bindFloat2(...)` or drive time with `bindTime(...)`.
4. Apply the effect with `Modifier.redbyteFx(fx)`.

```kotlin
val grayscaleSetup = run {
    var amountParam: FxParam.Float? = null
    val effect = redbytefx {
        val amount = uniformFloat(0f, "amount")
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
- animate effects over time with `uniformTime(...)` and `FxController.bindTime(...)` in Compose, with pause/resume preserving the current phase
- bind Compose state directly to uniforms with `bindFloat(...)`, `bindFloat2(...)`, `bindFloat3(...)`, and `bindFloat4(...)`
- cache intermediate expressions with `let(...)` when you want real local variables in generated AGSL
- define reusable AGSL helpers with `fn(...)` and call them from the main shader body
- compose math with operators and shader-like helpers such as `sin`, `cos`, `mix`, `clamp`, `length`, `floor`, `ceil`, `pow`, `fract`, `mod`, `step`, `smoothstep`
- branch with `ifElse(...)` and boolean expressions like `(value gt 0.5f) or other`
- work with `float2`, `float3`, `float4` and colors through `color(...)`, `withAlpha(...)`, `luminance(...)`, `grayscale(...)`, `mix(...)`
- return a final color via `sample(...)` or `sampleUnclamped(...)`

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

Those can come later as a separate stdlib/effects layer once the core language feels settled.

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
- Use `:sample` as a cookbook: each demo now shows what part of the DSL it is exercising, plus a preview of the generated AGSL.

## Current focus

The project is in active redesign. The goal is not just to wrap a few stock transforms, but to grow a real Kotlin-first AGSL authoring experience.
