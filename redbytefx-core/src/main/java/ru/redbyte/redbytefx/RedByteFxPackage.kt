/**
 * Typed Kotlin DSL and compiler for Android GPU shaders.
 *
 * Author programs with [shader] and [ShaderDsl], then target AGSL ([ShaderTarget.Agsl]) or GLES
 * ([ShaderTarget.Gles30], [ShaderTarget.Gles31], [ShaderTarget.Gles32]). Platform floors live in
 * [RedByteFxApis]; AGSL entry points call [RedByteFxPlatform.requireAgslRuntime] at runtime.
 *
 * Playback:
 *
 * - AGSL: [newAgslInstance] or [ru.redbyte.redbytefx.compose.rememberFxController] +
 *   [ru.redbyte.redbytefx.compose.redbyteFx]
 * - GLES: [ru.redbyte.redbytefx.gl.GlProgramRuntime] or
 *   [ru.redbyte.redbytefx.gl.compose.GlSurface]
 */
package ru.redbyte.redbytefx
