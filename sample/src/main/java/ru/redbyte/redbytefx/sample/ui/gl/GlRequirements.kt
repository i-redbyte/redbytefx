package ru.redbyte.redbytefx.sample.ui.gl

import java.util.Locale
import ru.redbyte.redbytefx.sample.model.Phrase

private val GLES_30_REQUIREMENT = Phrase(
    en = "OpenGL ES 3.0 is required for this scene.",
    ru = "Для этой сцены нужен OpenGL ES 3.0.",
)

private val GLES_32_REQUIREMENT = Phrase(
    en = "OpenGL ES 3.2 is required for geometry and tessellation.",
    ru = "Для geometry и tessellation нужен OpenGL ES 3.2.",
)

internal fun glEs30LinkRequirement(): String = GLES_30_REQUIREMENT.localized()

internal fun glEs32LinkRequirement(): String = GLES_32_REQUIREMENT.localized()

private fun Phrase.localized(): String =
    if (Locale.getDefault().language == "ru") ru else en
