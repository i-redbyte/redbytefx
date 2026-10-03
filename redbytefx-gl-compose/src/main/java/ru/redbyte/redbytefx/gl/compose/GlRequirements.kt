package ru.redbyte.redbytefx.gl.compose

import java.util.Locale
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget

private const val ES_30_EN = "OpenGL ES 3.0 is required for this scene."
private const val ES_30_RU = "Для этой сцены нужен OpenGL ES 3.0."
private const val ES_32_EN = "OpenGL ES 3.2 is required for geometry and tessellation."
private const val ES_32_RU = "Для geometry и tessellation нужен OpenGL ES 3.2."

/** User-visible hint when GLES 3.0 link fails. */
public fun glEs30LinkRequirement(): String =
    if (Locale.getDefault().language == "ru") ES_30_RU else ES_30_EN

/** User-visible hint when GLES 3.2 link fails. */
public fun glEs32LinkRequirement(): String =
    if (Locale.getDefault().language == "ru") ES_32_RU else ES_32_EN

/**
 * Picks a link-failure hint from [program] stages, or null when no extra hint is needed.
 */
public fun ShaderProgram.glLinkRequirementHint(): String? = when {
    hasGeometry() || hasTessellation() -> glEs32LinkRequirement()
    target == ShaderTarget.Gles31 -> glEs30LinkRequirement()
    target == ShaderTarget.Gles30 || target == ShaderTarget.Gles32 -> glEs30LinkRequirement()
    else -> null
}
