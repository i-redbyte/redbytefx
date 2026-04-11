# Library limitations and supported patterns

One index for **honest limits** (v0.6 shaping). This is **not** a semver contract or publication
promise — see [milestone-snapshot.md](milestone-snapshot.md).

## Platform

- **Android-only**, **minSdk 33** — AGSL `RuntimeShader` and the chosen integration path assume a
  modern graphics stack. Details: [runtime-platform-constraints.md](runtime-platform-constraints.md).

## Runtime and uniforms

- **Effect-specific `FxParam` handles** — bindings must come from the same compiled `redbytefx { }`
  effect as the controller. Rules: [runtime-authoring-checklist.md](runtime-authoring-checklist.md).
- **`RenderEffect` refresh** — after real uniform updates, the implementation may rebuild the
  platform effect so the GPU sees changes; redundant writes are skipped when values are unchanged.
  Rationale: [runtime-platform-constraints.md](runtime-platform-constraints.md). Multiple imperative
  writes in one block can use **`runBatch`** on **`FxInstance`** / **`FxController`** to coalesce rebuilds.

## Coordinates and sampling

- **`fragCoord` / `resolution` vs normalized UV** — mixing spaces without a clear port plan is a
  common source of “wrong picture” bugs. Start with [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md)
  (`sample` vs `sampleUv`) and [cookbook-patterns.md](cookbook-patterns.md) (porting checklist).

## Tooling and API shape

- **Diagnostics** — compile-time and uniform-binding failures throw **`FxDiagnosticException`**
  (subtype of **`IllegalStateException`**) with structured **`FxDiagnostic`** entries
  (**`FxDiagnosticCode`**, message, optional hint). There is no separate IDE-style diagnostic pipeline
  yet; callers can inspect **`FxDiagnosticException.diagnostics`** instead of parsing **`message`**
  strings only.
- **Canonical vs exploratory** stdlib helpers are documented and signaled in sample/docs; the surface
  can still evolve ([v0.6-library-shaping.md](v0.6-library-shaping.md)).

## Related

- [README.md](../README.md) — Current limitations (short list).
- [roadmap.md](roadmap.md) — what is explicitly not promised yet (publishing, semver).
