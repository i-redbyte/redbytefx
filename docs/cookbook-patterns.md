# Cookbook patterns (draft)

Companion to [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md). Expand this as **v0.4 Authoring UX** matures; it is not a complete migration guide yet.

## Coordinates

| Typical source | In redbytefx |
|----------------|--------------|
| Shadertoy `fragCoord` / `iResolution.xy` | `fragCoord`, `resolution`; normalized UV often `normalizedUv()` or `fragCoord / resolution` |
| Centered UV | `let(fragCoord / resolution - float2(0.5f, 0.5f), "uv")` or stdlib `centeredUv` |
| Sample again from normalized UV | `sampleUv(uv)` or `sample(uv * resolution)` |
| Use UV only for masks/gradients | keep `sample()` for the base content read; `normalizedUv()` only drives the mask math |

## Sampling-space rule of thumb

- If the content re-read coordinate is still expressed in pixels, stay on `sample(...)`.
- If the content re-read coordinate has moved into normalized `[0,1]` UV space, switch to `sampleUv(...)`.
- Using `normalizedUv()` for masks, gradients, or polar math does **not** automatically mean the shader should use `sampleUv(...)`.
- A good first port keeps one sampling space all the way through, then introduces stdlib helpers once the generated AGSL still reads predictably.

## Time

| Typical source | In redbytefx |
|----------------|--------------|
| `iTime` | `uniformTime(...)` or `autoUniformTime()` + `FxController.bindTime` in Compose |

## Uniforms

- Named uniforms: `uniformFloat(0f, "name")` or `val x by autoUniformFloat(0f)` so the Kotlin name becomes the shader uniform.
- Anonymous: `uniformFloat(0f)` when you do not care about the string name.

## Masks and mixing

- Prefer stdlib masks (`circleMask`, `rectMask`, …) when they match the intent; otherwise build `smoothstep` chains in the DSL the same way as in AGSL.

## User functions

- AGSL `float foo(float x) { ... }` maps to `fn(name = "foo", ...) { x -> ... }` in the DSL (see core tests and sample demos).

## End-to-end example: extracted AGSL helper function

Typical AGSL shape:

```glsl
float pulse_band(float phase, float threshold) {
  return step(threshold, smoothstep(0.08, 0.92, fract(phase)));
}

half4 main(float2 fragCoord) {
  float2 uv = fragCoord / rb_resolution;
  float mask = pulse_band(uv.y * density + time, 0.55);
  half4 base = rb_sample(fragCoord);
  half4 accent = half4(0.05, 0.95, 0.82, base.a);
  return mix(base, accent, mask * amount);
}
```

Equivalent RedByteFX port:

```kotlin
val effect = redbytefx {
    val time by autoUniformTime()
    val density by autoUniformFloat(8f)
    val amount by autoUniformFloat(0.7f)
    val pulseBand = fn(
        name = "pulse_band",
        arg1 = FloatType,
        arg2 = FloatType,
        returns = FloatType
    ) { phase, threshold ->
        step(threshold, smoothstep(0.08f, 0.92f, fract(phase)))
    }

    val base = let(sample(), "base")
    val uv = let(normalizedUv(), "uv")
    val mask = let(pulseBand(uv.y * density + time, 0.55f), "mask")
    val accent = let(color(float3(0.05f, 0.95f, 0.82f), base.a), "accent")

    mix(base, accent, mask * amount)
}
```

Why this is a good rewrite:

- `fn(...)` is the direct DSL analogue of a hand-written AGSL helper function; it keeps the reusable math named and visible in generated source.
- The extracted helper still operates on typed expressions, so the port stays close to the original shader shape instead of becoming a separate abstraction layer.
- This is a good fit for repeated recipe math. It is less useful for one-off expressions that read fine inline.

## End-to-end example: wave warp

Minimal AGSL-style source:

```glsl
uniform float2 rb_resolution;
uniform float u_amp;
uniform float u_freq;

half4 main(float2 fragCoord) {
  float2 offset = float2(0.0, sin(fragCoord.x * u_freq) * u_amp);
  return rb_sample(fragCoord + offset);
}
```

Equivalent RedByteFX port:

```kotlin
val effect = redbytefx {
    val amp = uniformFloat(12f, "amp")
    val freq = uniformFloat(0.08f, "freq")
    val offset = let(float2(0f, sin(fragCoord.x * freq) * amp), "offset")

    sample(fragCoord + offset)
}
```

What changed:

- `fragCoord` and `resolution` still mean the same thing.
- Uniforms become typed handles instead of string lookups.
- `let(...)` gives you readable locals that survive into generated AGSL.
- This stays on `sample(...)` because the offset is still measured in pixel/sample space.
- Add `normalizedUv()` only when the source shader actually moves into normalized space; otherwise stay closer to the AGSL source.

## End-to-end example: Shadertoy-style UV resample

Typical AGSL shape:

```glsl
float2 uv = fragCoord / rb_resolution;
uv.x += sin(uv.y * 18.0 + time * 1.2) * amp;
return rb_sample(uv * rb_resolution);
```

Equivalent RedByteFX port:

```kotlin
val effect = redbytefx {
    val time by autoUniformTime()
    val amp by autoUniformFloat(0.02f)
    val uv = let(normalizedUv(), "uv")
    val warpedUv = let(
        uv + float2(sin(uv.y * 18f + time * 1.2f) * amp, 0f),
        "warped_uv"
    )

    sampleUv(warpedUv)
}
```

Why this is a good rewrite:

- This is the canonical place for `sampleUv(...)`: the shader is authored in normalized UV space and only converts back to sample space at the final resample.
- `normalizedUv()` plus `sampleUv(...)` keeps the Kotlin version close to common Shadertoy mental models without forcing every author to rewrite `uv * resolution`.
- If the shader never re-samples from UV space, prefer plain `sample(...)` and keep the port closer to pixel coordinates.

## End-to-end example: UV-authored mask, pixel-space base sample

Typical AGSL shape:

```glsl
float2 uv = fragCoord / rb_resolution;
float focus = 1.0 - smoothstep(radius, radius + feather, length(uv - center));
half4 base = rb_sample(fragCoord);
half4 tint = half4(0.10, 0.95, 0.86, base.a);
return mix(base, tint, focus * amount);
```

Equivalent RedByteFX port:

```kotlin
val effect = redbytefx {
    val radius by autoUniformFloat(0.22f)
    val amount by autoUniformFloat(0.8f)
    val base = let(sample(), "base")
    val uv = let(normalizedUv(), "uv")
    val focus = let(
        circleMask(
            uv = uv,
            center = float2(0.36f, 0.52f),
            radius = radius,
            feather = 0.12f
        ),
        "focus"
    )
    val tint = let(color(float3(0.10f, 0.95f, 0.86f), base.a), "tint")

    maskedMix(base, tint, focus, amount)
}
```

Why this is a useful contrast:

- The shader clearly uses normalized UV for the authored mask, but it never warps the content lookup itself.
- `sample()` stays correct here because the base content is still read at `fragCoord`.
- Replacing `sample()` with `sampleUv(uv)` would not help readability; it would only obscure the fact that no UV-space resampling is happening.

## End-to-end example: masked compositing

Typical AGSL shape:

```glsl
float2 uv = fragCoord / rb_resolution;
float focus = 1.0 - smoothstep(radius, radius + 0.16, length(uv - float2(0.34, 0.5)));
float halo = 1.0 - smoothstep(0.05, 0.10, abs(length(uv - float2(0.34, 0.5)) - (radius + 0.05)));
half4 glow = half4(0.12, 0.95, 1.0, 1.0) * halo * amount;
half4 screened = blend_screen(rb_sample(fragCoord), glow, halo * amount);
return mix(rb_sample(fragCoord), screened, focus * amount);
```

Equivalent RedByteFX port:

```kotlin
val effect = redbytefx {
    val radius by autoUniformFloat(0.2f)
    val amount by autoUniformFloat(0.82f)
    val base = let(sample(), "base")
    val uv = let(normalizedUv(), "uv")
    val focus = let(circleMask(uv, center = float2(0.34f, 0.5f), radius = radius, feather = 0.16f), "focus")
    val halo = let(ringMask(uv, center = float2(0.34f, 0.5f), radius = radius + 0.05f, width = 0.1f, feather = 0.05f), "halo")
    val glowLayer = let(alphaMask(color(float3(0.12f, 0.95f, 1f), 1f), halo, amount), "glow_layer")
    val screened = let(maskedScreen(base, glowLayer, halo, amount), "screened")

    maskedMix(base, screened, focus, amount)
}
```

Why this is a good stdlib rewrite:

- The AGSL version is still conceptually the same shader: normalized UV, one soft circle, one soft ring, then masked compositing.
- `circleMask(...)`, `ringMask(...)`, `alphaMask(...)`, `maskedScreen(...)`, and `maskedMix(...)` remove noisy edge math without hiding the generated AGSL shape too much.
- This is the kind of rewrite that belongs in `stdlib`; the mental model still maps cleanly back to masks and blends.

## End-to-end example: polar radar sweep

Typical AGSL shape:

```glsl
float2 uv = fragCoord / rb_resolution;
float2 delta = uv - float2(0.5);
float angle01 = fract(atan(delta.y, delta.x) / 6.2831855 + 1.0);
float sweep = 1.0 - smoothstep(0.06, 0.09, abs(fract(angle01 - sweepAngle + 0.5) - 0.5));
float ring = 1.0 - smoothstep(0.045, 0.06, abs(length(delta) - radius));
float arc = ring * sweep;
```

Equivalent RedByteFX port:

```kotlin
val effect = redbytefx {
    val time by autoUniformTime()
    val speed by autoUniformFloat(0.72f)
    val radius by autoUniformFloat(0.34f)
    val amount by autoUniformFloat(0.86f)
    val base = let(sample(), "base")
    val uv = let(normalizedUv(), "uv")
    val polar = let(polarCoordinates(uv), "polar")
    val sweepAngle = let(fract(time * speed * 0.08f), "sweep_angle")
    val sweep = let(angularSweep(uv = uv, angle = sweepAngle, width = 0.12f, feather = 0.03f), "sweep")
    val arc = let(arcMask(uv = uv, radius = radius, ringWidth = 0.09f, angle = sweepAngle, arcWidth = 0.18f, feather = 0.03f), "arc")
    val tint = let(color(float3(0.2f, 1f, 0.78f), base.a), "tint")

    maskedScreen(base, tint, sweep + arc + (1f - smoothstep(radius, radius + 0.18f, polar.x)) * 0.25f, amount)
}
```

Why this is a good stdlib rewrite:

- The first pass still matches AGSL thinking: normalize coordinates, derive angle/radius, animate a sweep angle.
- `polarCoordinates(...)`, `angularSweep(...)`, and `arcMask(...)` turn repetitive polar math into named authoring blocks that stay easy to reason about.
- Keeping `polar.x` in the final expression is still useful when you want one direct radial term alongside the higher-level helpers.

## Next examples to add

- signal/distortion rewrite using `scanWarp(...)` or `signalBars(...)`
- one compact `fn(...)` extraction example from hand-written AGSL helper functions

## Next steps

- Add 2–3 end-to-end examples: “port this minimal Shadertoy shader line by line.”
- Cross-link from README once examples are stable.
