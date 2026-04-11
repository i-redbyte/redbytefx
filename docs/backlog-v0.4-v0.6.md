# Backlog: v0.4 Authoring UX, v0.5 Runtime quality, v0.6 Library shaping

This document extends [roadmap.md](roadmap.md) with concrete backlog items. It is **not** a release contract; the library remains pre-publication.

## v0.4 Authoring UX

**Goal:** Writing RedByteFX feels teachable and debuggable for authors coming from raw AGSL or Shadertoy-style shaders.

### Diagnostics

- Improve compiler error messages (unknown symbols, arity, type mismatches) with stable wording and, where feasible, source context.
- Improve runtime/shader bridge errors when uniforms or resolution are inconsistent (without noisy logging in the sample).

Immediate backlog:

- keep same-effect uniform binding errors explicit: param type, debug name when available, and “this handle belongs to another effect” wording
- make “bad literal / unsupported expression” messages point authors toward the next step instead of only rejecting the input
- decide which collision cases deserve surfaced diagnostics versus quiet auto-renaming:
  - user function names
  - local `let(...)` names
  - sanitized uniform names

Nice-to-have later, only if the compiler shape justifies it:

- lightweight source-context hints for generated helper/function names
- grouped diagnostics format instead of one-off exception strings

### Documentation and wording

- Dedicated **“AGSL vs redbytefx”** section: what the DSL guarantees, what remains visible in generated shaders, and how to read `agslSource()`. **Started:** [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md) (expand with cookbook examples as v0.4 progresses).
- **Cookbook:** rewrite patterns from Shadertoy/AGSL into the DSL (coordinates, `fragCoord`/`resolution`, uniforms, time, masks, compositing, `fn` extraction). Draft table: [cookbook-patterns.md](cookbook-patterns.md).
- Pass over public KDoc and parameter names for consistent vocabulary (e.g. “normalized UV”, “feather”, “amount”, `width` vs `thickness`, `progress`).

Priority docs gaps:

- explain `sample(...)` vs `sampleUv(...)` more concretely
- explain the “uniform handles are effect-specific” rule wherever runtime binding is introduced
- add a compact “porting checklist” for AGSL/Shadertoy rewrites
- add 2-3 representative cookbook rewrites, not a giant reference dump:
  - direct wave/warp example
  - mask/compositing example
  - polar or scan/radar example

### Small fixes (when trivial)

- Cross-links from README / debugging section to the cookbook and AGSL comparison doc.
- Any single-message clarity fix that does not require a new diagnostic pipeline.
- Tighten KDoc where the effect/runtime boundary is easy to misunderstand.

### Larger items (explicitly “next system”, not v0.4 in one shot)

- Structured diagnostics API (if/when the compiler grows enough to support it).
- IDE-style tooling inside the sample (out of scope; the sample stays a demo, not an IDE).
- Full parser/source-map level diagnostics. Only worth doing if the DSL stops being expression-first enough for current exception-style errors.

---

## v0.5 Runtime quality

**Goal:** Predictable runtime and sample behavior: performance, lifecycle, and regression confidence without premature micro-optimization.

Current audit snapshot:

- `FxController` already caches scalar/vector uniform writes and skips redundant runtime invalidation for stable values.
- Resolution updates already have a distinct path: `syncResolution(...)` updates shader size during drawing without creating an extra invalidation loop.
- The main open risk is still platform-driven `RenderEffect` refresh frequency after real runtime updates, not obvious accidental churn in the controller layer.
- A reproducible runtime measurement path now exists for `Radar` / `Circuit` via the sample launch extra plus `scripts/measure-sample-runtime.sh`.
- The project now has both an emulator baseline and a first physical-device baseline (`SM-G991B`, Android `15`).
- On that physical-device pass, `Radar` and `Circuit` both looked healthy enough that the current runtime path is no longer the project’s most suspicious area; remaining work is more about breadth of confidence than emergency optimization.

### Performance and invalidation

- Profile or review `FxController` / shader rebuild paths: when AGSL is regenerated and when `RuntimeShader` is updated.
- Avoid redundant invalidation when navigating between demos, switching wide-screen inspection layouts, or when uniforms change.

Immediate runtime review targets:

- verify the current `FxController` value caches and `runtimeInvalidationTick` behavior against real animated demos, not only unit tests
- measure whether recreating `RenderEffect` on each real uniform update is acceptable on representative effects, given the current platform constraint in `FxInstanceImpl`
- confirm the `drawWithCache -> syncResolution(size) -> composeRenderEffect` path stays stable during resize, split-screen, and rapid demo switching

### Lifecycle and leaks

- Audit `rememberFxController`, effect identity, and demo navigation for leaked observers or duplicate controllers.
- Document “one controller per render target” invariants and enforce them in sample patterns where possible.

Immediate lifecycle checks:

- confirm controller replacement is correct when `rememberFxController(effect)` sees a new compiled effect identity
- audit host-view attachment and invalidation behavior so old controllers are not kept alive longer than needed
- review larger demos like `Circuit` for accidental duplicate controllers or effect recreation churn

### Confidence

- Optional smoke or screenshot tests for a **small** set of representative demos (infrastructure-dependent).
- Keep AGSL goldens and stdlib compiler tests as the primary **shape** regressions; add targeted tests when the compiler or stdlib surface changes.

Representative confidence targets:

- `Wave` or another raw-DSL demo for baseline coordinate/runtime sanity
- `Composite` for mask/compositing correctness
- `Radar` for time-driven polar helpers
- `Circuit` for the heaviest current sample flow and navigation/runtime confidence

### AGSL / platform stability

- Track AGSL/runtime quirks (minSdk 33, `RuntimeShader` limits) in docs; fix only when tied to real failures.
- Keep a short list of known platform-driven compromises, such as the current `RenderEffect` refresh behavior, so optimization work stays grounded in real constraints.

Reference note: [runtime-audit-v0.5.md](runtime-audit-v0.5.md)

---

## v0.6 Library shaping

**Goal:** A clear library identity **before** any Maven/publishing conversation: canonical vs experimental surface, examples, and honest limits.

### API surface

- Define what makes a helper **canonical**:
  - it removes recurring boilerplate across multiple demos/cookbook rewrites
  - it preserves the AGSL mental model instead of hiding it behind opaque abstractions
  - its naming/defaults/parameter order feel stable enough to teach
  - it clearly belongs in `stdlib`, not `core`
- Mark or group **canonical** vs **experimental/noisy** helpers through documentation and surface curation first; avoid inventing a heavy classification mechanism unless the library shape truly needs one.
- Final pass on public entry points across `redbytefx-core`, `redbytefx-compose`, and `redbytefx-stdlib` to reduce synonyms and tighten vocabulary.

### Canonical surface backlog

- decide which helper families should be treated as the first canonical teaching surface:
  - coordinate helpers
  - masks/transitions
  - compositing/color recipes
  - signal/polar/routing building blocks
- keep one-off or style-specific helpers out of the “start here” narrative even if they remain in the source tree
- make module/package-level docs point to the canonical path first, then to broader exploratory helpers

First-pass canonical shortlist to validate against docs/sample/cookbook:

- coordinates:
  - `normalizedUv()`
  - `sampleUv(...)`
  - `centeredUv(...)`
  - `aspectCenteredUv(...)`
- masks and reveal:
  - `circleMask(...)`
  - `rectMask(...)`
  - `ringMask(...)`
  - `horizontalReveal(...)`
  - `verticalReveal(...)`
  - `radialReveal(...)`
- compositing and readable color work:
  - `maskedMix(...)`
  - `alphaMask(...)`
  - `maskedScreen(...)`
  - `adjustSaturation(...)`
- shaping / SDF:
  - `sdCircle(...)`
  - `sdRoundedBox(...)`
  - `softFill(...)`
  - `softStroke(...)`
- timing / signal / gradients:
  - `pulse(...)`
  - `bandMask(...)`
  - `linearRamp(...)`
  - `radialRamp(...)`
  - `angularSweep(...)`
- routing / authored scene structure:
  - `segmentMask(...)`
  - `segmentProgress(...)`
  - `segmentPulse(...)`

Helpers that currently look more “keep available, but not first-teaching-surface”:

- heavier stylistic/procedural recipes such as `fbm(...)`, `domainWarp(...)`, `grain(...)`, `vignette(...)`
- special-effect palette/distortion helpers such as `cosinePalette(...)` and `chromaticOffset(...)`
- narrower decorative helpers such as `frameMask(...)`, `cornerMask(...)`, and some frame-edge variants
- scan/pattern variants that are useful, but should not crowd the mental model before the canonical basics feel settled

### Content

- Mature **examples** and cookbook recipes aligned with the canonical helper families instead of showing every available trick equally.
- Keep a small starter set of demos/examples that answer:
  - how to write a raw DSL shader
  - when to reach for stdlib helpers
  - how to inspect the generated AGSL
- Single place for **limitations**:
  - Android-only / `minSdk 33`
  - effect-specific uniform handles
  - normalized UV vs raw `fragCoord` expectations
  - current platform-driven `RenderEffect` refresh compromise

### Explicitly out of scope for v0.6 here

- `maven-publish`, signing, artifact pipelines, and semver-style compatibility promises.

### Exit shape for v0.6

- A new author can tell what the canonical path is without reading the whole codebase.
- The project has a clear boundary between “recommended first surface” and “broader exploratory helper set”.
- Docs, examples, and sample demos reinforce the same mental model instead of competing narratives.
- The library is still pre-publication, but its shape no longer feels accidental.

---

## How this file is used

- **v0.4–v0.6** items are prioritized against the honest roadmap in [roadmap.md](roadmap.md) and milestone status in [v0.2-status.md](v0.2-status.md).
- Completed items should move to [CHANGELOG.md](../CHANGELOG.md) when shipped, not deleted silently.
