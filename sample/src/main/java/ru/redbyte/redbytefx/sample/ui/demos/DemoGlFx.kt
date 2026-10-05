package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.gl.compose.bindTime
import ru.redbyte.redbytefx.gl.compose.redbyteFx
import ru.redbyte.redbytefx.gl.compose.rememberGlController
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.cosinePalette
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.rotate2d
import ru.redbyte.redbytefx.stdlib.sdHexagon
import ru.redbyte.redbytefx.stdlib.softFill
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

@Composable
fun DemoGlFx() {
    val setup = remember { glFxEffect() }
    val fx = rememberGlController(setup.program)
    fx.bindTime(setup.time)
    redbyteFx(fx, Modifier.fillMaxSize())
}

private class GlFxSetup(
    val program: ru.redbyte.redbytefx.ShaderProgram,
    val time: Uniform<Flt<High>>,
)

private fun glFxEffect(): GlFxSetup {
    lateinit var time: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        fragment {
            val uv = aspectCenteredUv(normalizedUv(), resolution)
            val spun = rotate2d(uv, time.expr)
            val fill = softFill(sdHexagon(spun, 0.28f), 0.02f)
            val rgb = cosinePalette(time.expr * 0.12f)
            vec4(rgb.x * fill, rgb.y * fill, rgb.z * fill, 1f.lit)
        }
    }
    return GlFxSetup(program, time)
}
