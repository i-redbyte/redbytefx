package ru.redbyte.redbytefx.sample.ui.gl

import kotlin.math.cos
import kotlin.math.sin

internal const val WORD_COUNT = 8
internal const val WORD_CAMERA_Z = 2.5f
internal const val WORD_SCALE = 0.088f
internal const val FALL_FROM = 1.55f

private const val FALL_GAP = 0.18f
private const val FALL_DURATION = 0.85f
private const val YAW = 0.26f

internal fun letterProgress(index: Int, time: Float): Float {
    val elapsed = time - index * FALL_GAP
    return (elapsed / FALL_DURATION).coerceIn(0f, 1f)
}

internal fun wordSettled(time: Float): Boolean = letterProgress(WORD_COUNT - 1, time) >= 1f

internal fun letterLift(index: Int, time: Float): Float = FALL_FROM * (1f - easeOut(letterProgress(index, time)))

internal fun letterWave(index: Int, time: Float): Float {
    if (!wordSettled(time)) return 0f
    val slot = index % 4
    return sin(time * 2.5f - slot * 0.85f) * 0.14f
}

internal class WordModel {
    private val local: FloatArray
    val posed: FloatArray

    init {
        val built = ArrayList<Float>()
        WORD_TEXT.forEachIndexed { index, letter ->
            val line = if (index < 4) 0 else 1
            val slot = index % 4
            wordGlyph(letter).forEachIndexed { row, pattern ->
                pattern.forEachIndexed { column, cell ->
                    if (cell != '#') return@forEachIndexed
                    val x = slot * 6f + column - 11f
                    val y = (3f - row) - line * 9f + 4.5f
                    writeBox(x, y, 0f, 0.4f, 0.4f, 0.5f) { vx, vy, vz, shade ->
                        built += vx
                        built += vy
                        built += vz
                        built += shade
                        built += index.toFloat()
                    }
                }
            }
        }
        local = built.toFloatArray()
        posed = FloatArray(local.size / 5 * 8)
    }

    fun pose(time: Float) {
        val yawCos = cos(YAW)
        val yawSin = sin(YAW)
        var read = 0
        var write = 0
        while (read < local.size) {
            val x = local[read]
            val y = local[read + 1]
            val z = local[read + 2]
            val shade = local[read + 3]
            val id = local[read + 4]
            read += 5
            val index = id.toInt()
            val pitch = (1f - letterProgress(index, time)) * 2.2f
            val pitchCos = cos(pitch)
            val pitchSin = sin(pitch)
            val sy = y * WORD_SCALE
            val sz = z * WORD_SCALE
            val pitchedY = sy * pitchCos - sz * pitchSin
            val pitchedZ = sy * pitchSin + sz * pitchCos
            val sx = x * WORD_SCALE
            posed[write] = sx * yawCos + pitchedZ * yawSin
            posed[write + 1] = pitchedY + letterLift(index, time) + letterWave(index, time)
            posed[write + 2] = -sx * yawSin + pitchedZ * yawCos
            posed[write + 3] = 1f
            posed[write + 4] = id
            posed[write + 5] = shade
            posed[write + 6] = 0f
            posed[write + 7] = 1f
            write += 8
        }
    }
}

private fun easeOut(unit: Float): Float {
    val t = unit - 1f
    val overshoot = 1.15f
    return 1f + t * t * ((overshoot + 1f) * t + overshoot)
}

internal const val WORD_TEXT = "red_byte"

internal fun wordGlyph(letter: Char): Array<String> = glyphs.getValue(letter)

private val glyphs: Map<Char, Array<String>> = mapOf(
    'r' to arrayOf(
        "#    ",
        "#    ",
        "###  ",
        "#  # ",
        "#    ",
        "#    ",
        "#    ",
    ),
    'e' to arrayOf(
        " ### ",
        "#    ",
        "#    ",
        "#### ",
        "#    ",
        "#    ",
        " ### ",
    ),
    'd' to arrayOf(
        "   # ",
        "   # ",
        " ### ",
        "#  # ",
        "#  # ",
        "#  # ",
        " ### ",
    ),
    '_' to arrayOf(
        "     ",
        "     ",
        "     ",
        "     ",
        "     ",
        "     ",
        "#####",
    ),
    'b' to arrayOf(
        "#    ",
        "#    ",
        "###  ",
        "#  # ",
        "#  # ",
        "#  # ",
        "###  ",
    ),
    'y' to arrayOf(
        "#   #",
        "#   #",
        " # # ",
        "  #  ",
        "  #  ",
        " #   ",
        "#    ",
    ),
    't' to arrayOf(
        " ### ",
        "  #  ",
        "  #  ",
        "  #  ",
        "  #  ",
        "  #  ",
        "  #  ",
    ),
)
