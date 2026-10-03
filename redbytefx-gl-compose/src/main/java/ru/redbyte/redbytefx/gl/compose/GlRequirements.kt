package ru.redbyte.redbytefx.gl.compose

import java.util.Locale
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget

private const val ES_30_EN = "OpenGL ES 3.0 is required for this scene."
private const val ES_30_RU = "Для этой сцены нужен OpenGL ES 3.0."
private const val ES_31_EN = "OpenGL ES 3.1 is required for compute."
private const val ES_31_RU = "Для compute нужен OpenGL ES 3.1."
private const val ES_32_EN = "OpenGL ES 3.2 is required for this scene."
private const val ES_32_RU = "Для этой сцены нужен OpenGL ES 3.2."

/** User-visible hint when GLES 3.0 link fails. */
public fun glEs30LinkRequirement(): String =
    if (Locale.getDefault().language == "ru") ES_30_RU else ES_30_EN

/** User-visible hint when a GLES 3.1 compute program fails to link. */
public fun glEs31LinkRequirement(): String =
    if (Locale.getDefault().language == "ru") ES_31_RU else ES_31_EN

/** User-visible hint when GLES 3.2 link fails. */
public fun glEs32LinkRequirement(): String =
    if (Locale.getDefault().language == "ru") ES_32_RU else ES_32_EN

/**
 * Picks a link-failure hint from [program]'s target.
 *
 * GLES 3.2 programs are spelled as `#version 320 es` even without geometry or tessellation, and
 * GLES 3.1 programs are `#version 310 es`.
 */
public fun ShaderProgram.glLinkRequirementHint(): String? = when (target) {
    ShaderTarget.Gles32 -> glEs32LinkRequirement()
    ShaderTarget.Gles31 -> glEs31LinkRequirement()
    ShaderTarget.Gles30 -> glEs30LinkRequirement()
    ShaderTarget.Agsl -> null
}
