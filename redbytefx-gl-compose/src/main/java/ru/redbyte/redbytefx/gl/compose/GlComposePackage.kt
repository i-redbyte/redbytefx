/**
 * Jetpack Compose integration for GLES programs compiled by RedByteFX.
 *
 * Typical flow:
 *
 * 1. compile with `shader(ShaderTarget.Gles30) { ... }` (or GLES 3.1/3.2)
 * 2. [rememberGlController] for the [ru.redbyte.redbytefx.ShaderProgram]
 * 3. [GlSurface] with a [GlMesh] and an [onFrame] callback that uploads uniforms
 * 4. bind static Compose state via [GlController.bindFloat] / [GlController.bindInt], or
 *    [GlController.bindTime] for `uniformTime(...)` handles
 *
 * Uniform writes from composition are queued to the GL thread; imperative updates can use
 * [GlController.set] and [GlController.runOnGl] once [GlSurface] is attached.
 */
package ru.redbyte.redbytefx.gl.compose
