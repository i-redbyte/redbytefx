package ru.redbyte.redbytefx.sample.ui.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import ru.redbyte.redbytefx.sample.ui.demos.glesTriangle

class GlScenesTest {
    @Test
    fun everySceneShowsTheDslBesideTheGlslItCompilesTo() {
        val pairs = listOf(
            glesTriangle().program to triangleDsl,
            ballProgram().program to ballsDsl,
            flagScene().program to flagDsl,
            floorScene().program to floorDsl,
            lampScene().program to lampDsl,
            cityScene().program to cityDsl,
            orbScene().program to orbDsl,
            bandsScene().program to bandsDsl,
            paletteScene().program to paletteDsl,
            hedgehogScene().program to hedgehogDsl,
            oceanScene().program to oceanDsl,
            wireScene().program to wireDsl,
            stormScene().program to stormDsl,
            wordScene(floatArrayOf(0f, 0f, 0f, 1f, 0f, 1f, 0f, 1f)).program to wordDsl,
            playfield(0.01f, 0.02f, 0.04f).program to gameDsl("tunnel"),
            playfield(0.03f, 0.02f, 0.02f).program to gameDsl("maze"),
            playfield(0.02f, 0.02f, 0.04f).program to gameDsl("breakout"),
            playfield(0.01f, 0.01f, 0.03f).program to gameDsl("raid"),
            playfield(0.02f, 0.03f, 0.06f).program to gameDsl("descent"),
        )
        assertEquals(GlExample.entries.size, pairs.size)
        pairs.forEach { (program, dsl) ->
            val glsl = glesListing(program)
            assertTrue(dsl.contains("shader("))
            assertTrue(glsl.contains("// vertex"))
            assertTrue(glsl.contains("// fragment"))
            assertTrue(glsl.contains("void main"))
            if (dsl.contains("geometry(")) assertTrue(glsl.contains("// geometry"))
            if (dsl.contains("tessEval(")) assertTrue(glsl.contains("// tessellation evaluation"))
        }
    }

    @Test
    fun everySceneBuildsALinkedProgram() {
        val sources = listOf(
            flagScene().program.fragmentSource(),
            floorScene().program.fragmentSource(),
            lampScene().program.fragmentSource(),
            cityScene().program.fragmentSource(),
            orbScene().program.fragmentSource(),
            bandsScene().program.fragmentSource(),
            paletteScene().program.fragmentSource(),
            hedgehogScene().program.geometrySource(),
            oceanScene().program.tessEvalSource(),
            wireScene().program.geometrySource(),
            stormScene().program.fragmentSource(),
            wordScene(floatArrayOf(0f, 0f, 0f, 1f, 0f, 1f, 0f, 1f)).program.fragmentSource(),
        )
        sources.forEach { source ->
            assertTrue(source.contains("void main"))
        }
        assertTrue(cityScene().program.fragmentSource().contains("layout(std140)"))
        assertTrue(oceanScene().program.tessEvalSource().contains("gl_TessCoord"))
        assertTrue(hedgehogScene().program.geometrySource().contains("EmitVertex"))
        assertTrue(paletteScene().program.vertexSource().contains("ink"))
        assertTrue(paletteScene().program.fragmentSource().contains("ink"))
        assertEquals(8, paletteScene().mesh.stride)
        assertTrue(paletteScene().mesh.vertices.size > 500)
        val storm = stormScene().program.fragmentSource()
        assertTrue(storm.contains("floor"))
        assertTrue(!storm.contains("atan"))
        val flag = flagScene().program.fragmentSource()
        assertTrue(!flag.contains("fract"))
    }

    @Test
    fun theWordGlyphsSpellRedByte() {
        assertEquals(WORD_TEXT, "red_byte")
        val stem = wordGlyph('r')
        assertEquals('#', stem[0][0])
        assertEquals(' ', stem[0][3])
        assertTrue(stem[5].drop(1).all { it == ' ' })
        assertTrue(stem[6].drop(1).all { it == ' ' })
        assertEquals('#', wordGlyph('e')[0][1])
        assertEquals(' ', wordGlyph('e')[1][4])
        assertEquals('#', wordGlyph('_')[6][0])
        assertEquals(' ', wordGlyph('_')[0][0])
    }

    @Test
    fun facetedNormalsPointOutward() {
        val vertices = solidShaded(1f).vertices
        var index = 0
        while (index < vertices.size) {
            val dot = vertices[index] * vertices[index + 4] +
                vertices[index + 1] * vertices[index + 5] +
                vertices[index + 2] * vertices[index + 6]
            assertTrue(dot > 0.2f)
            index += 8
        }
    }

    @Test
    fun lettersFallThenWaveAndStayOnAPortraitScreen() {
        assertTrue(letterLift(0, 0f) > 1.4f)
        assertEquals(0f, letterLift(0, 10f), 0.02f)
        assertTrue(!wordSettled(0.2f))
        assertTrue(wordSettled(10f))
        assertEquals(0f, letterWave(1, 0.2f), 0.0001f)
        assertTrue(abs(letterWave(0, 10f)) > 0.01f)
        assertTrue(abs(letterWave(0, 10f) - letterWave(1, 10f)) > 0.01f)

        val model = WordModel()
        model.pose(10f)
        var maxX = 0f
        var maxY = 0f
        var index = 0
        while (index < model.posed.size) {
            maxX = maxOf(maxX, abs(model.posed[index]))
            maxY = maxOf(maxY, abs(model.posed[index + 1]))
            index += 8
        }
        assertTrue(maxX / 0.45f / WORD_CAMERA_Z < 0.98f)
        assertTrue(maxY / WORD_CAMERA_Z > 0.2f)
        assertTrue(maxY / WORD_CAMERA_Z < 0.75f)
        assertTrue(model.posed.size > 100)
    }
}
