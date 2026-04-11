# Cookbook patterns (draft)

Companion to [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md). Expand this as **v0.4 Authoring UX** matures; it is not a complete migration guide yet.

## Coordinates

| Typical source | In redbytefx |
|----------------|--------------|
| Shadertoy `fragCoord` / `iResolution.xy` | `fragCoord`, `resolution`; normalized UV often `normalizedUv()` or `fragCoord / resolution` |
| Centered UV | `let(fragCoord / resolution - float2(0.5f, 0.5f), "uv")` or stdlib `centeredUv` |
| Sample again from normalized UV | `sampleUv(uv)` or `sample(uv * resolution)` |

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
