/**
 * Standard shader library for RedByteFX: reusable recipes built with the `redbytefx { }` DSL.
 *
 * **Core vs stdlib:** `ru.redbyte.redbytefx` (`:redbytefx-core`) owns the
 * language surface (types, uniforms, compiler, essential math/color). This module adds higher-level
 * helpers—mapping, noise, masks, compositing, SDF, routing—without hiding the generated AGSL.
 *
 * Canonical parameter vocabulary in this module:
 *
 * - `uv`: normalized `[0, 1]` coordinates unless the helper explicitly says it works in sample or
 *   pixel space
 * - `amount`: blend or modulation intensity
 * - `feather`: edge softness / falloff distance
 * - `width` / `thickness`: visible band or stroke size
 * - `progress`: normalized reveal / timeline position
 *
 * ### Semantics (read before mixing helpers)
 *
 * - **Masked compositing** (`maskedMix`, `maskedScreen`, `maskedOverlay`, `alphaMask`, …): the
 *   effective weight is always `saturate(mask * amount)` (see `maskedAmount` in
 *   `FxStdlibCompositing.kt`). Author [mask] in roughly `[0, 1]`; use [amount] to scale how strong
 *   the effect is, not as a second unrelated alpha.
 * - **Mapping** (`inverseLerp`, `remap`): [inverseLerp] returns the unclamped normalized `t`
 *   between two input endpoints; [remap] is exactly `mix(outputStart, outputEnd, inverseLerp(...))` —
 *   do not duplicate that math with ad-hoc helpers.
 *
 * ### Canonical (first teaching surface)
 *
 * First-pass starter path in `stdlib`:
 *
 * - coordinates: [normalizedUv], [sampleUv], [centeredUv], [aspectCenteredUv]
 * - masks / reveal: [circleMask], [rectMask], [ringMask], [horizontalReveal],
 *   [verticalReveal], [radialReveal]
 * - compositing: [maskedMix], [alphaMask], [maskedScreen]
 * - shaping / SDF: [sdCircle], [sdRoundedBox], [softFill], [softStroke]
 * - timing / signal / gradients / polar: [pulse], [bandMask], [linearRamp], [radialRamp],
 *   [angularSweep]
 * - routing / scene structure: [segmentMask], [segmentProgress], [segmentPulse]
 *
 * A useful teaching order is:
 *
 * 1. move from sample space into normalized or local space with [normalizedUv], [centeredUv], and
 *    [aspectCenteredUv]
 * 2. build readable masks with [bandMask], [circleMask], [rectMask], and the reveal helpers
 * 3. turn those masks into layers with [maskedMix], [alphaMask], and [maskedScreen]
 * 4. switch into local SDF authoring with [sdCircle], [sdRoundedBox], [softFill], and [softStroke]
 * 5. add richer motion or scene structure through [pulse], [linearRamp], [angularSweep], and the
 *    routing helpers
 *
 * ### Exploratory (supporting / advanced)
 *
 * Prefer these **after** the canonical path is comfortable: distortion and noise
 * (`fbm`, `domainWarp`, `chromaticOffset`), frame/crop masks (`frameMask`, `cornerMask`),
 * procedural detail, palette remixing, and similar “extra visual” helpers.
 *
 * ### Supporting utilities (not the teaching spine)
 *
 * Mapping helpers such as [inverseLerp] and [remap] support composition without being the main
 * story; quantization, heavy procedural noise, palette, frame, and distortion helpers usually
 * belong in exploratory or polish passes once masks/composites/SDF are clear.
 *
 * Helpers are grouped by topic across source files (mapping, patterns, masks, compositing, …).
 * Prefer composing a few clear helpers over growing the surface with one-off variants. The sample
 * home screen mirrors this same canonical family map so the teaching path stays consistent between
 * docs, package guidance, and live demos.
 */
package ru.redbyte.redbytefx.stdlib
