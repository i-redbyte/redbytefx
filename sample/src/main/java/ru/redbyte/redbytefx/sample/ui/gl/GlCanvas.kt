package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.gl.compose.GL_LINK_FALLBACK
import ru.redbyte.redbytefx.gl.compose.GlFrame
import ru.redbyte.redbytefx.gl.compose.GlLinkState
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.GlSurface
import ru.redbyte.redbytefx.gl.compose.GlSurfaceConfig
import ru.redbyte.redbytefx.gl.compose.glPointerInput
import ru.redbyte.redbytefx.gl.compose.rememberGlController
import ru.redbyte.redbytefx.sample.ui.say

internal class PointerState {
    @Volatile var x: Float = 0f

    @Volatile var y: Float = 0.2f
    private val ticks = java.util.concurrent.atomic.AtomicInteger(0)

    fun mark(x: Float, y: Float) {
        this.x = x
        this.y = y
        ticks.incrementAndGet()
    }

    fun serial(): Int = ticks.get()
}

@Composable
internal fun GlCanvas(
    program: ShaderProgram,
    mesh: GlMesh,
    requirement: String? = null,
    pointer: PointerState? = null,
    caption: String? = null,
    dsl: String? = null,
    present: ShaderProgram? = null,
    renderToTexture: Boolean = false,
    onFrame: (GlFrame) -> Unit,
) {
    val controller = rememberGlController(
        program = program,
        config = GlSurfaceConfig(depth = mesh.depth),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        key(program) {
            val baseModifier = if (pointer != null) {
                Modifier
                    .fillMaxSize()
                    .glPointerInput { x, y -> pointer.mark(x, y) }
            } else {
                Modifier.fillMaxSize()
            }
            GlSurface(
                controller = controller,
                mesh = mesh,
                modifier = baseModifier,
                requirement = requirement,
                present = present,
                renderToTexture = renderToTexture,
                onFrame = onFrame,
                overlay = { state ->
                    if (state is GlLinkState.Failed) {
                        val text = if (state.message == GL_LINK_FALLBACK) {
                            say(GL_LINK_FALLBACK, "Это устройство не может собрать шейдер.")
                        } else {
                            state.message
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                },
            )
        }
        if (caption != null) {
            val linkState by controller.linkState
            if (linkState !is GlLinkState.Failed) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp),
                )
            }
        }
        if (dsl != null) {
            GlCodeCompare(
                program = program,
                dsl = dsl,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
