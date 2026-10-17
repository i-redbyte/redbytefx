package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShaderIdentifierTest {

    @Test
    fun cyrillicNamesAreTransliteratedToAscii() {
        assertEquals("yarkost", sanitizeSuggestedIdentifier("яркость", leadingDigitPrefix = "u_"))
        assertEquals("tsvet_fona", sanitizeSuggestedIdentifier("ЦветФона", leadingDigitPrefix = "u_"))
        assertEquals("shchuka", sanitizeSuggestedIdentifier("щука", leadingDigitPrefix = "u_"))
    }

    @Test
    fun otherNonAsciiLettersFallBackToAValidName() {
        val name = sanitizeSuggestedIdentifier("光", leadingDigitPrefix = "u_")
        assertTrue(name, name.isValidShaderIdentifier())
    }

    @Test
    fun reservedWordsAndPrefixesNeverSurviveSanitizing() {
        for (raw in listOf("smooth", "sample", "precision", "vec3", "half4", "gl_Pos", "sk_x", "a__b")) {
            val name = sanitizeSuggestedIdentifier(raw, leadingDigitPrefix = "x")
            assertTrue("$raw -> $name", name.isValidShaderIdentifier())
            assertFalse("$raw -> $name", isReservedShaderWord(name))
        }
    }

    @Test
    fun glslSourceStaysAsciiAndAvoidsKeywords() {
        val source = shader(ShaderTarget.Gles30) {
            val bright = uniform("яркость", 1f)
            fragment {
                val smooth = let(fragCoord.x, "smooth")
                val precision = let(fragCoord.y, "precision")
                val reserved = let(fragCoord.x, "gl_value")
                val color = let(bright.expr, "цвет")
                vec4(smooth + reserved, precision, color, 1f.lit)
            }
        }.fragmentSource()

        assertTrue(source.all { it.code < 128 })
        assertTrue(source.contains("uniform highp float u_yarkost;"))
        assertFalse(source.contains("float smooth ="))
        assertFalse(source.contains("float precision ="))
        assertFalse(Regex("(?<![A-Za-z0-9_])gl_value").containsMatchIn(source))
        assertTrue(source.contains("float tsvet ="))
    }

    @Test
    fun agslSourceAvoidsAgslTypeNames() {
        val source = shader(ShaderTarget.Agsl) {
            fragment {
                val half = let(fragCoord.x, "half4")
                val skia = let(fragCoord.y, "sk_FragCoord")
                vec4(half, skia, 0f.lit, 1f.lit)
            }
        }.agslSource()

        assertFalse(source.contains("float half4 ="))
        assertFalse(source.contains("sk_FragCoord"))
    }

    @Test
    fun blockMembersAndOutputsAvoidKeywords() {
        val source = shader(ShaderTarget.Gles30) {
            lateinit var tint: Expr<Vec4<Flt<High>>>
            uniformBlock("lights") { tint = vec4("sample") }
            fragment {
                val out = outVec4("smooth", 0)
                out.set(tint)
                vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }.fragmentSource()

        assertFalse(source.contains(" sample;"))
        assertFalse(source.contains(" smooth;"))
    }

    private fun String.isValidShaderIdentifier(): Boolean =
        isNotEmpty() && first().let { it in 'a'..'z' || it in 'A'..'Z' || it == '_' } &&
            all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' } &&
            !startsWith("gl_") && !startsWith("sk_") && "__" !in this
}
