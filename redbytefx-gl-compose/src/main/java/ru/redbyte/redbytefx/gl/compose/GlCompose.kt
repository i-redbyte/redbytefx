package ru.redbyte.redbytefx.gl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.Uniform

/**
 * Remembers a [GlController] for [program].
 *
 * Use one controller per [GlSurface] or [GlCompute]. A new [program] or [config] creates a new controller,
 * which does not carry over values set on the previous one.
 */
@Composable
public fun rememberGlController(
    program: ShaderProgram,
    config: GlSurfaceConfig = GlSurfaceConfig(),
): GlController = remember(program, config) { GlController(program, config) }

/**
 * Binds a scalar float uniform after successful recomposition (queued to the GL thread).
 *
 * For animation time prefer [bindTime] or set the uniform from [GlFrame.seconds] inside [GlSurface]'s
 * `onFrame` callback.
 */
@Composable
public fun GlController.bindFloat(param: Uniform<Flt<High>>, value: Float) {
    SideEffect {
        set(param, value)
    }
}

/** Binds an int uniform after successful recomposition (queued to the GL thread). */
@Composable
public fun GlController.bindInt(param: Uniform<IntS>, value: Int) {
    SideEffect {
        set(param, value)
    }
}

/**
 * Drives [param] with elapsed time in seconds, similar to [ru.redbyte.redbytefx.compose.FxController.bindTime].
 *
 * Intended for uniforms from `uniformTime(...)`. Updates are queued to the GL thread; when [isPlaying]
 * is `false`, the current value is held. [offsetSeconds] shifts the reported time without resetting
 * accumulated phase.
 */
@Composable
public fun GlController.bindTime(
    param: Uniform<Flt<High>>,
    isPlaying: Boolean = true,
    offsetSeconds: Float = 0f,
) {
    val state = remember(this, param) { GlTimeBindingState() }
    LaunchedEffect(this, param, isPlaying, offsetSeconds) {
        if (!isPlaying) {
            state.lastFrameNanos = NO_FRAME
            set(param, offsetSeconds + state.elapsedSeconds)
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { frameNanos ->
                val lastFrameNanos = state.lastFrameNanos
                if (lastFrameNanos != NO_FRAME) {
                    state.elapsedSeconds += (frameNanos - lastFrameNanos) / 1_000_000_000f
                }
                state.lastFrameNanos = frameNanos
                set(param, offsetSeconds + state.elapsedSeconds)
            }
        }
    }
}

private const val NO_FRAME = Long.MIN_VALUE

private class GlTimeBindingState {
    var elapsedSeconds: Float = 0f
    var lastFrameNanos: Long = NO_FRAME
}

/**
 * GLES effect host. Same short path as AGSL [ru.redbyte.redbytefx.compose.redbyteFx]:
 * fragment-only `shader(ShaderTarget.Gles30) { fragment { … } }`, [rememberGlController], then this
 * composable. It draws a fullscreen triangle ([screenMesh]) and writes `resolution` from the view
 * size. Unlike the AGSL modifier, it does not sample Compose content (`sample()` / `sampleUv()` stay
 * AGSL-only). The two `redbyteFx` symbols live in different packages; import one per file.
 */
@Composable
public fun redbyteFx(
    controller: GlController,
    modifier: Modifier = Modifier,
    onFrame: (GlFrame) -> Unit = {},
) {
    val mesh = remember { screenMesh() }
    GlSurface(
        controller = controller,
        mesh = mesh,
        modifier = modifier,
        onFrame = onFrame,
    )
}
