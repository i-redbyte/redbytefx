package ru.redbyte.redbytefx.gl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.Uniform

/**
 * Remembers a [GlController] for [program]. Pair with [GlSurface] in the same composition.
 */
@Composable
public fun rememberGlController(
    program: ShaderProgram,
    config: GlSurfaceConfig = GlSurfaceConfig(),
): GlController = remember(program, config) { GlController(program, config) }

@Composable
public fun GlController.bindFloat(param: Uniform<Flt<High>>, value: Float) {
    SideEffect {
        set(param, value)
    }
}

@Composable
public fun GlController.bindInt(param: Uniform<IntS>, value: Int) {
    SideEffect {
        set(param, value)
    }
}
