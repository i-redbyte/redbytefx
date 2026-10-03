/**
 * OpenGL ES 3.x runtime for shaders compiled with [ru.redbyte.redbytefx.ShaderTarget.Gles30],
 * [ru.redbyte.redbytefx.ShaderTarget.Gles31], or [ru.redbyte.redbytefx.ShaderTarget.Gles32].
 *
 * [GlProgramRuntime] owns program link, uniform upload, and UBO/SSBO binding on the thread that
 * created it. Compose apps typically use [ru.redbyte.redbytefx.gl.compose.GlSurface] instead of
 * calling the runtime directly.
 *
 * Failures throw [GlException] with a [GlCode]; see [docs/error-codes.md](https://github.com/i-redbyte/redbytefx/blob/main/docs/error-codes.md).
 */
package ru.redbyte.redbytefx.gl
