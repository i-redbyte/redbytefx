package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.gl.compose.GL_LINK_FALLBACK
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlLinkState
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.GlSurface
import ru.redbyte.redbytefx.gl.compose.rememberGlController
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.sample.ui.gl.GlCodeCompare
import ru.redbyte.redbytefx.sample.ui.gl.triangleDsl
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y

internal class GlesTriangle(
    val program: ShaderProgram,
    val time: Uniform<Flt<High>>,
)

internal fun glesTriangle(): GlesTriangle {
    lateinit var time: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        vertex {
            val position = attributeVec2("position")
            glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
        }
        fragment {
            val wave = sin(time.expr)
            vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
        }
    }
    return GlesTriangle(program, time)
}

@Composable
fun DemoGles() {
    val triangle = remember { glesTriangle() }
    val mesh = remember {
        GlMesh(
            vertices = floatArrayOf(-0.6f, -0.5f, 0.6f, -0.5f, 0f, 0.7f),
            stride = 2,
            attribs = listOf(GlAttrib("a_position", 2, 0)),
            clearR = 0.05f,
            clearG = 0.06f,
            clearB = 0.09f,
        )
    }
    val controller = rememberGlController(triangle.program)
    val linkState by controller.linkState
    Box(modifier = Modifier.fillMaxSize()) {
        key(triangle.program) {
            GlSurface(
                controller = controller,
                mesh = mesh,
                modifier = Modifier.fillMaxSize(),
                onFrame = { frame -> frame.runtime.set(triangle.time, frame.seconds) },
            )
        }
        if (linkState is GlLinkState.Failed) {
            val message = (linkState as GlLinkState.Failed).message
            Text(
                text = if (message == GL_LINK_FALLBACK) {
                    say(GL_LINK_FALLBACK, "Это устройство не может собрать шейдер.")
                } else {
                    message
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        }
        GlCodeCompare(
            program = triangle.program,
            dsl = triangleDsl,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
