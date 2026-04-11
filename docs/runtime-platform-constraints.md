# Runtime platform constraints (AGSL / Android)

This note lists **stable platform facts** that shape RedByteFX runtime behavior. It is the companion to
[runtime-audit-v0.5.md](runtime-audit-v0.5.md) and [v0.5-runtime-quality.md](v0.5-runtime-quality.md).

## API floor

- **minSdk 33** — the library targets Android **13+** so AGSL-backed `RuntimeShader` and the
  surrounding graphics stack are available without legacy fallbacks.
- Effects are built on **`android.graphics.RuntimeShader`** (AGSL source) and exposed to Compose
  through **`RenderEffect.createRuntimeShaderEffect(...)`**.

## Uniform updates vs `RenderEffect`

`RuntimeShader` uniforms can be updated after the shader is created, but **some `RenderEffect`
instances do not reliably pick up later uniform changes** when the effect object is reused. The
current bridge therefore **recreates the platform `RenderEffect` after each successful runtime
mutation** that should be visible on screen. See `FxInstanceImpl` in `redbytefx-core`
(`Internal.kt`).

Implications:

- redraw cost includes **effect recreation** on real uniform changes, not only uniform writes.
- the Compose layer still **skips redundant invalidation** when values are bitwise-stable; the
  platform refresh applies when something actually changed.
- **`FxInstance.runBatch { }` / `FxController.runBatch { }`** coalesce backing refresh
  notifications so several imperative uniform writes in one block can produce **one** platform
  rebuild and **one** host invalidation (default `FxInstance` implementations simply run the block).

## Shader limits

AGSL / `RuntimeShader` inherits **driver and implementation limits** (uniform count, instruction
complexity, texture bindings). The compiler does not model every GPU limit; very large generated
programs may fail at runtime. When that happens, treat it as a **platform or shader-size** issue
first, then simplify the graph or split effects.

## Related

- [runtime-authoring-checklist.md](runtime-authoring-checklist.md) — one controller per target,
  param ownership.
- [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md) — what the DSL emits vs raw AGSL.
