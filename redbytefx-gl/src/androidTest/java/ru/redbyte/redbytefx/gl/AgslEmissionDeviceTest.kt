package ru.redbyte.redbytefx.gl

import android.os.Build
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.redbyte.redbytefx.RedByteFxApis
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.newAgslInstance
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y

@RunWith(AndroidJUnit4::class)
class AgslEmissionDeviceTest {
    @Test
    fun letSharedByConditionalAndResultCompilesAsRuntimeShader() {
        assumeTrue(Build.VERSION.SDK_INT >= RedByteFxApis.AGSL_MIN_SDK)

        val program = shader(ShaderTarget.Agsl) {
            fragment {
                val board = let(fragCoord / resolution, "board")
                val boardMask = let(board.x + board.y, "board_mask")
                val active = local(0f.lit, "active")
                whenTrue(fragCoord.x gt 0f.lit) {
                    active.set(boardMask)
                }
                val result = let(active.expr + boardMask, "result")
                vec4(result, result, result, 1f.lit)
            }
        }

        assertNotNull(program.newAgslInstance().renderEffect())
    }
}
