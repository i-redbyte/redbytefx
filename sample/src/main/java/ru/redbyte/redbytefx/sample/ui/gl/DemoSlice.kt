package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

internal const val SLICE_QUADS: Int = 3

internal val sliceDsl = """
shader(ShaderTarget.Gles30) {
    val clipX = varyingFloat("x")
    vertex {
        val position = attributeVec3("position")
        clipX.set(position.x)
        glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
    }
    fragment {
        val left = clipX.expr lt -0.3f
        val right = clipX.expr gt 0.3f
        val color = ifElse(left, vec3(0.25f.lit, 0.45f.lit, 0.95f.lit), ifElse(right, vec3(0.95f.lit, 0.42f.lit, 0.16f.lit), vec3(0.2f.lit, 0.75f.lit, 0.38f.lit)))
        vec4(color.x, color.y, color.z, 1f.lit)
    }
}
""".trimIndent()

internal fun sliceScene(): ShaderProgram = shader(ShaderTarget.Gles30) {
    val clipX = varyingFloat("x")
    vertex {
        val position = attributeVec3("position")
        clipX.set(position.x)
        glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
    }
    fragment {
        val left = clipX.expr lt (-0.3f).lit
        val right = clipX.expr gt 0.3f.lit
        val color = ifElse(
            left,
            vec3(0.25f.lit, 0.45f.lit, 0.95f.lit),
            ifElse(right, vec3(0.95f.lit, 0.42f.lit, 0.16f.lit), vec3(0.2f.lit, 0.75f.lit, 0.38f.lit)),
        )
        vec4(color.x, color.y, color.z, 1f.lit)
    }
}

internal fun sliceIndexCount(quads: Int): Int = quads.coerceIn(1, SLICE_QUADS) * 6

internal fun sliceMesh(): GlMesh {
    val vertices = FloatArray(SLICE_QUADS * 4 * 3)
    val indices = IntArray(SLICE_QUADS * 6)
    val edges = floatArrayOf(-0.92f, -0.38f, -0.22f, 0.22f, 0.38f, 0.92f)
    var vertex = 0
    var cursor = 0
    for (quad in 0 until SLICE_QUADS) {
        val x0 = edges[quad * 2]
        val x1 = edges[quad * 2 + 1]
        val base = vertex / 3
        floatArrayOf(x0, -0.45f, 0f, x1, -0.45f, 0f, x1, 0.45f, 0f, x0, 0.45f, 0f).copyInto(vertices, vertex)
        vertex += 12
        for (offset in intArrayOf(0, 1, 2, 0, 2, 3)) indices[cursor++] = base + offset
    }
    return GlMesh(
        vertices = vertices,
        stride = 3,
        attribs = listOf(GlAttrib("a_position", 3, 0)),
        clearR = 0.02f,
        clearG = 0.025f,
        clearB = 0.04f,
        indices = indices,
    )
}

@Composable
internal fun DemoSlice() {
    val program = remember { sliceScene() }
    val mesh = remember { sliceMesh() }
    val requested = remember { AtomicInteger(SLICE_QUADS) }
    var shown by remember { mutableIntStateOf(SLICE_QUADS) }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            GlCanvas(program, mesh, dsl = sliceDsl) { frame ->
                frame.draw(first = 0, count = sliceIndexCount(requested.get()))
            }
        }
        CyberPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = say("Quads $shown", "Прямоугольников: $shown"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Slider(
                value = shown.toFloat(),
                onValueChange = { value ->
                    val next = value.roundToInt().coerceIn(1, SLICE_QUADS)
                    shown = next
                    requested.set(next)
                },
                valueRange = 1f..SLICE_QUADS.toFloat(),
                steps = SLICE_QUADS - 2,
            )
        }
    }
}
