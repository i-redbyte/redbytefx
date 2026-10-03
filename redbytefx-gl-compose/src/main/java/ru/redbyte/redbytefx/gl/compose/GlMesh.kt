package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30

/**
 * Vertex layout uploaded on the GL thread when a [GlSurface] is created.
 */
public class GlMesh(
    public val vertices: FloatArray,
    public val stride: Int,
    public val attribs: List<GlAttrib>,
    public val mode: Int = GLES30.GL_TRIANGLES,
    public val patchVertices: Int = 0,
    public val depth: Boolean = false,
    public val clearR: Float = 0.02f,
    public val clearG: Float = 0.025f,
    public val clearB: Float = 0.045f,
)

public class GlAttrib(
    public val name: String,
    public val size: Int,
    public val offset: Int,
)

/** Full-screen triangle for fragment-style GLES scenes. */
public fun screenMesh(
    clearR: Float = 0.02f,
    clearG: Float = 0.025f,
    clearB: Float = 0.045f,
): GlMesh = GlMesh(
    vertices = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f),
    stride = 2,
    attribs = listOf(GlAttrib("a_corner", 2, 0)),
    clearR = clearR,
    clearG = clearG,
    clearB = clearB,
)
