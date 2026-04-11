/**
 * Compose integration package for RedByteFX.
 *
 * This is the runtime-facing "apply and drive it" surface:
 *
 * - create one controller per render target with [rememberFxController]
 * - bind Compose state or time through [FxController.bindFloat], [FxController.bindFloat2],
 *   [FxController.bindFloat3], [FxController.bindFloat4], and [FxController.bindTime]
 * - apply the effect with [redbyteFx]
 *
 * Important runtime rules:
 *
 * - a controller owns one mutable runtime shader instance, so do not share one controller across
 *   unrelated render targets
 * - uniform handles still belong to the compiled effect that created the controller; matching
 *   names do not make params interchangeable across effects
 * - resolution is synchronized from the draw target automatically, so most Compose callers should
 *   bind author-controlled uniforms only and let [redbyteFx] handle the render size
 *
 * A good Compose flow is:
 *
 * 1. compile an effect once with `redbytefx { ... }`
 * 2. remember one controller for that compiled effect in each place that renders it
 * 3. drive time and ordinary state through the controller bind helpers
 * 4. inspect `agslSource()` from the compiled effect whenever the authored shader shape is in
 *    doubt
 */
package ru.redbyte.redbytefx.compose
