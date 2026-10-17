package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM entry points that code compiled against 1.1.1 still calls. */
class BinaryCompatibilityTest {
    private val composer = "androidx.compose.runtime.Composer"
    private val linkOverlay = "kotlin.jvm.functions.Function3"

    @Test
    fun glSurfaceKeepsItsOneOneOneSignature() {
        assertHasMethod(
            "ru.redbyte.redbytefx.gl.compose.GlSurfaceKt",
            "GlSurface",
            listOf(
                "ru.redbyte.redbytefx.gl.compose.GlController",
                "ru.redbyte.redbytefx.gl.compose.GlMesh",
                "androidx.compose.ui.Modifier",
                "java.lang.String",
                "ru.redbyte.redbytefx.ShaderProgram",
                "boolean",
                "kotlin.jvm.functions.Function1",
                linkOverlay,
                composer,
                "int",
                "int",
            ),
        )
    }

    @Test
    fun glComputeKeepsItsOneOneOneSignature() {
        assertHasMethod(
            "ru.redbyte.redbytefx.gl.compose.GlComputeKt",
            "GlCompute",
            listOf(
                "ru.redbyte.redbytefx.gl.compose.GlController",
                "androidx.compose.ui.Modifier",
                "java.lang.String",
                linkOverlay,
                composer,
                "int",
                "int",
            ),
        )
    }

    private fun assertHasMethod(owner: String, name: String, parameters: List<String>) {
        val methods = Class.forName(owner).methods.filter { it.name == name }
        val signatures = methods.map { method -> method.parameterTypes.map { it.name } }
        assertTrue("$owner.$name has $signatures", parameters in signatures)
    }
}
