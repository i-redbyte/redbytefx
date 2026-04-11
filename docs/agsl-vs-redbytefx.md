# AGSL vs redbytefx

This note is for **authors** moving between hand-written AGSL and the Kotlin DSL. It is not a compatibility contract: the library is still pre-publication.

## Mental model

- **AGSL** is what `RuntimeShader` executes on device. You can always inspect the exact string with `FxEffect.agslSource()` (or the **Generated AGSL** panel in `:sample`).
- **redbytefx** is a thin authoring layer: typed expressions, uniforms, and composable control flow that **compile to** AGSL-shaped source. The goal is predictability, not hiding GPU code.

## What stays visible

- Math and logic you write (`sin`, `mix`, `smoothstep`, `ifElse`, …) appear in generated shaders in essentially the same shape you would hand-write.
- Uniforms become `uniform float …` / `uniform float2 …` with stable names when you pass explicit labels, or generated names when you use anonymous/auto uniforms.
- Kotlin-style names are normalized into readable AGSL identifiers: `waveAmplitude` becomes `u_wave_amplitude`, `let(..., "waveOffset")` becomes `l_wave_offset`, and `fn(name = "PulseBand", ...)` becomes `pulse_band`.
- Built-in AGSL function names are reserved for user helpers: if you ask for `fn(name = "mix", ...)` or `fn(name = "sin", ...)`, RedByteFX suffixes the generated helper name instead of shadowing the built-in.
- Marker interfaces such as `FloatExpr` and `ColorExpr` are authoring types, not extension points to implement directly. If a value flows through `let(...)` or `fn(...)`, return a composed DSL expression instead of a custom marker-interface object.
- Input sampling goes through the runtime bridge (`rb_sample` / content shader); that wrapper is generated, not something you hand-edit per effect.
- Common whole-number math also stays readable when porting line by line: expressions such as `1 - amount` and `2 * uv` are accepted directly instead of forcing `1f` on the left-hand side before the first translation is working.

## One important invariant

- Uniform handles are **effect-specific**. A `FxParam` created inside one `redbytefx { ... }` block is only valid for runtime instances/controllers created from that same compiled effect.
- Matching debug names do **not** make handles interchangeable. If you compile two separate effects that both declare `"amount"`, those are still different params.

## Coordinates and resolution

- **`fragCoord`** in the DSL matches AGSL `main(float2 fragCoord)` pixel coordinates.
- **`resolution`** is the logical content size in pixels (see core docs). Normalized UV is typically `fragCoord / resolution` when you want `[0,1]`-style space.
- When you want that normalized path frequently, `:redbytefx-stdlib` now provides `normalizedUv()` and `sampleUv(uv)` as small coordinate conveniences instead of rewriting the conversion each time.

## Sampling quick rule

- Stay on **`sample(...)`** when the authored coordinate is still in pixel/sample space: `fragCoord`, `fragCoord + offsetPx`, mirrored pixel coordinates, or anything else already measured against `resolution` in pixels.
- Switch to **`normalizedUv()` + `sampleUv(...)`** when the authored logic has clearly moved into `[0,1]` UV space and your resampling point is expressed as `uv`, `uv + drift`, centered UV, polar UV-derived coordinates, or other normalized-space math.
- Do not mix them accidentally: `sample(uv)` is wrong because `sample(...)` expects sample-space pixels, while `sampleUv(fragCoord)` is wrong because `sampleUv(...)` expects normalized UV.
- When porting AGSL or Shadertoy-style code, keep the first translation in one space only, inspect `agslSource()`, then introduce `sampleUv(...)` only if the normalized-space intent becomes clearer.

## When to use stdlib

- **`redbytefx-core`:** language primitives and essential math/color helpers.
- **`redbytefx-stdlib`:** reusable recipes (masks, noise, compositing helpers). Prefer a few clear stdlib calls over copying large blocks—then adjust in AGSL terms mentally via `agslSource()`.

## Debugging workflow

1. Get the effect compiling in Kotlin.
2. Check your coordinate space first: raw pixel space (`fragCoord`), normalized UV (`fragCoord / resolution` or `normalizedUv()`), or centered UV helpers from `stdlib`.
3. Read `agslSource()` and verify uniforms, locals, sampling calls, and the `main` body shape.
4. If runtime binding looks wrong, confirm the uniform handle comes from the same compiled effect/controller.
5. If something still looks wrong, bisect: simplify the DSL body, or compare with a minimal AGSL snippet in the same coordinate system.

## Translation checklist

1. Mark the original shader inputs: `fragCoord`, `resolution`, time, user uniforms, and resampling points.
2. Keep the first port close to AGSL shape: use `let(...)` for obvious locals and do not introduce stdlib helpers too early.
3. Once the generated AGSL looks sane, replace repeated recipe math with stdlib helpers only where the intent becomes clearer.
4. Re-check the generated shader after each helper swap so the authoring model stays predictable.

## Cookbook (next)

- Draft translation table plus explicit pixel-space vs UV-space sampling rewrites, `fn(...)` extraction, compositing, and radar examples: [cookbook-patterns.md](cookbook-patterns.md).
- Larger narrative and more examples: [backlog-v0.4-v0.6.md](backlog-v0.4-v0.6.md) under **v0.4 Authoring UX**.
