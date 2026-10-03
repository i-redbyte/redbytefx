package ru.redbyte.redbytefx.sample.ui.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import ru.redbyte.redbytefx.gl.compose.MESH_STRIDE
import ru.redbyte.redbytefx.gl.compose.sphere
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
            crateScene().program to crateDsl,
            planetScene().program to planetDsl,
            sliceScene() to sliceDsl,
            stampScene().program to stampDsl,
            skyScene().program to skyDsl,
            mirrorPrograms().present to mirrorDsl,
            mipPrograms().program to mipsDsl,
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
        val city = cityScene()
        val cityFragment = city.program.fragmentSource()
        assertTrue(cityFragment.contains("layout(std140)"))
        assertTrue(cityFragment.contains("texture(u_ground"))
        assertTrue(cityFragment.contains("texture(u_facade"))
        assertTrue(cityFragment.indexOf("texture(u_ground") < cityFragment.indexOf("texture(u_facade"))
        assertTrue(city.program.vertexSource().contains("a_uv"))
        assertEquals(MESH_STRIDE, city.mesh.stride)
        assertEquals(6 * 24 * MESH_STRIDE, city.mesh.vertices.size)
        assertEquals(6 * 36, city.mesh.indices?.size)
        val crate = crateScene()
        val crateFragment = crate.program.fragmentSource()
        assertTrue(crateFragment.contains("texture(u_ground"))
        assertTrue(crateFragment.contains("texture(u_facade"))
        assertTrue(crate.program.vertexSource().contains("a_uv"))
        assertTrue(crate.program.vertexSource().contains("a_position"))
        assertEquals(2 * 24 * MESH_STRIDE, crate.mesh.vertices.size)
        assertEquals(2 * 36, crate.mesh.indices?.size)
        assertTrue(crateDsl.contains("attributeVec3(\"position\")"))
        assertTrue(crateDsl.contains("attributeVec2(\"uv\")"))
        assertTrue(crateDsl.contains("sampler2D(\"facade\")"))
        assertTrue(crateDsl.contains("sampler2D(\"ground\")"))
        assertTrue(crateDsl.contains("lambert("))
        val planet = planetScene()
        val planetVertex = planet.program.vertexSource()
        val planetFragment = planet.program.fragmentSource()
        assertTrue(planetVertex.contains("a_position"))
        assertTrue(planetVertex.contains("a_uv"))
        assertTrue(planetVertex.contains("a_model0"))
        assertTrue(planetFragment.contains("texture(u_albedo"))
        assertTrue(planetFragment.contains("u_heat"))
        assertTrue(planetFragment.contains("u_mode"))
        assertTrue(planet.mesh.depth)
        val planetIndices = planet.mesh.indices
        assertTrue(planetIndices != null && planetIndices.isNotEmpty())
        assertTrue(planetDsl.contains("attributeVec3(\"position\")"))
        assertTrue(planetDsl.contains("attributeVec2(\"uv\")"))
        assertTrue(planetDsl.contains("attributeVec4(\"model0\")"))
        assertTrue(planetDsl.contains("sampler2D(\"albedo\")"))
        assertTrue(planetDsl.contains("lambert("))
        assertEquals(18, sliceMesh().indices?.size)
        assertEquals(6, sliceIndexCount(1))
        assertEquals(18, sliceIndexCount(3))
        assertEquals(0 to 0, stampCell(-0.9f, -0.9f))
        assertEquals(STAMP_CELLS - 1 to STAMP_CELLS - 1, stampCell(0.9f, 0.9f))
        assertEquals(MIP_SIZE * MIP_SIZE * 4, checkerRgba(MIP_SIZE, 1).size)
        assertTrue(checkerRgba(4, 1)[0] != checkerRgba(4, 1)[4])
        val sky = skyScene().program
        assertTrue(sky.fragmentSource().contains("u_sky"))
        assertTrue(skyDsl.contains("samplerCube(\"sky\")"))
        val mirror = mirrorPrograms()
        assertTrue(!mirror.program.fragmentSource().contains("texture("))
        assertTrue(mirror.present.fragmentSource().contains("texture(u_image"))
        assertTrue(mirrorDsl.contains("sampler2D(\"image\")"))
        assertTrue(mipsDsl.contains("sampler2D(\"image\")"))
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
    fun draggingThePlanetHeatsItAndMovesEachMoon() {
        assertTrue(nextPlanetHeat(0.2f, 0.4f, 0.016f) > 0.2f)
        assertTrue(nextPlanetHeat(0.5f, 0f, 0.5f) < 0.5f)
        val cool = planetLight(0.2f, 0.1f, 0.1f)
        val turned = planetLight(1.4f, 0.1f, 0.1f)
        val hot = planetLight(0.2f, 0.1f, 1f)
        assertTrue(abs(cool[0] - turned[0]) + abs(cool[2] - turned[2]) > 0.1f)
        assertTrue(hot[1] > cool[1])
        assertTrue(planetTapped(0.05f, -0.08f, 6f))
        assertTrue(!planetTapped(0.9f, 0.2f, 6f))
        assertTrue(!planetTapped(0.05f, 0f, 80f))

        val rest = planetBodies(PLANET_MOON_LIMIT, 0.2f, 0.1f)
        val spun = planetBodies(PLANET_MOON_LIMIT, 0.2f, 1.3f)
        assertEquals(PLANET_MOON_LIMIT + 1, rest.size)
        assertEquals(1, planetBodies(0, 1f, 1f).size)
        assertEquals(0f, rest[0].x, 0.0001f)
        assertEquals(0f, rest[0].y, 0.0001f)
        assertEquals(0f, rest[0].z, 0.0001f)
        assertEquals(PLANET_BODY, rest[0].kind, 0.0001f)
        for (index in 1..PLANET_MOON_LIMIT) {
            val moon = rest[index]
            val previous = rest[index - 1]
            val moved = abs(moon.x - spun[index].x) + abs(moon.y - spun[index].y) + abs(moon.z - spun[index].z)
            val apart = abs(moon.x - previous.x) + abs(moon.y - previous.y) + abs(moon.z - previous.z)
            assertEquals(PLANET_MOON, moon.kind, 0.0001f)
            assertTrue(apart > 0.2f)
            assertTrue(moved > 0.05f)
        }
        val packed = planetInstances(3, 0.4f, 0.2f)
        assertEquals(4 * 16, packed.size)
        assertEquals(1f, packed[0], 0.0001f)
        assertEquals(PLANET_BODY, packed[3], 0.0001f)
        assertEquals(1f, packed[15], 0.0001f)
        assertEquals(PLANET_MOON_SCALE, packed[16], 0.0001f)
        assertEquals(PLANET_MOON, packed[19], 0.0001f)
        assertEquals(0, planetOrbitInstances(0, 0.2f).size)
        val orbits = planetOrbitInstances(2, 0.4f)
        assertEquals(2 * 16, orbits.size)
        assertEquals(PLANET_RING, orbits[3], 0.0001f)
        val orbit = planetOrbit(1, 0.4f)
        val matrix = planetOrbitMatrix(1, 0.4f)
        val ahead = orbitPoint(orbit, 1f, 0f)
        val side = orbitPoint(orbit, 0f, 1f)
        assertEquals(ahead[0], matrix[0], 0.0001f)
        assertEquals(ahead[1], matrix[1], 0.0001f)
        assertEquals(ahead[2], matrix[2], 0.0001f)
        assertEquals(side[0], matrix[8], 0.0001f)
        assertEquals(side[1], matrix[9], 0.0001f)
        assertEquals(side[2], matrix[10], 0.0001f)
    }

    @Test
    fun sphereNormalsPointOutward() {
        val vertices = sphere(1f).vertices
        var index = 0
        while (index < vertices.size) {
            val dot = vertices[index] * vertices[index + 3] +
                vertices[index + 1] * vertices[index + 4] +
                vertices[index + 2] * vertices[index + 5]
            assertTrue(dot > 0.2f)
            index += MESH_STRIDE
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
