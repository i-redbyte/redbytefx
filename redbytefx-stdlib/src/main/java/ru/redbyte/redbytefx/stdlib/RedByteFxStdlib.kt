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
 * First-pass canonical starter path in `stdlib`:
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
 * Broader helpers such as `fbm(...)`, `domainWarp(...)`, `chromaticOffset(...)`,
 * `frameMask(...)`, and `cornerMask(...)` remain useful, but they are better treated as
 * exploratory helpers than the first teaching surface.
 *
 * Helpers are grouped by topic across source files (mapping, patterns, masks, compositing, …).
 * Prefer composing a few clear helpers over growing the surface with one-off variants. The sample
 * home screen mirrors this same canonical family map so the teaching path stays consistent between
 * docs, package guidance, and live demos.
 */
package ru.redbyte.redbytefx.stdlib
