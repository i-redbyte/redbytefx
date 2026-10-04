package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30

/** Scene. Blend factor passed to `glBlendFunc`. */
public enum class BlendFactor {
    Zero,
    One,
    SrcColor,
    OneMinusSrcColor,
    DstColor,
    OneMinusDstColor,
    SrcAlpha,
    OneMinusSrcAlpha,
    DstAlpha,
    OneMinusDstAlpha,
}

/** Scene. Blend equation passed to `glBlendEquation`. */
public enum class BlendEquation {
    Add,
    Subtract,
    ReverseSubtract,
}

/**
 * Scene. Extra pass state on top of the depth test.
 *
 * The default matches the driver: blend off, scissor off, all color channels written, depth writes
 * on. Each draw applies its own [GlPipeline], so a pass does not leave blend, scissor, the color
 * mask, or the depth mask for the next one. Face culling is not enabled.
 */
public class GlPipeline(
    public val blend: Boolean = false,
    public val srcFactor: BlendFactor = BlendFactor.One,
    public val dstFactor: BlendFactor = BlendFactor.Zero,
    public val equation: BlendEquation = BlendEquation.Add,
    public val scissor: Boolean = false,
    public val x: Int = 0,
    public val y: Int = 0,
    public val width: Int = 0,
    public val height: Int = 0,
    public val writeRed: Boolean = true,
    public val writeGreen: Boolean = true,
    public val writeBlue: Boolean = true,
    public val writeAlpha: Boolean = true,
    public val depthMask: Boolean = true,
) {
    public companion object {
        public val Default: GlPipeline = GlPipeline()
    }
}

internal interface PipelineOps {
    fun blend(enabled: Boolean)
    fun blendFunc(src: Int, dst: Int)
    fun blendEquation(equation: Int)
    fun scissorTest(enabled: Boolean)
    fun scissor(x: Int, y: Int, width: Int, height: Int)
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
    fun depthMask(enabled: Boolean)
}

internal fun applyPipeline(pipeline: GlPipeline, ops: PipelineOps) {
    if (pipeline.blend) {
        ops.blend(true)
        ops.blendFunc(blendFactor(pipeline.srcFactor), blendFactor(pipeline.dstFactor))
        ops.blendEquation(blendEquation(pipeline.equation))
    } else {
        ops.blend(false)
    }
    if (pipeline.scissor) {
        require(pipeline.width > 0 && pipeline.height > 0) {
            "Scissor size must be positive, was ${pipeline.width}x${pipeline.height}"
        }
        ops.scissorTest(true)
        ops.scissor(pipeline.x, pipeline.y, pipeline.width, pipeline.height)
    } else {
        ops.scissorTest(false)
    }
    ops.colorMask(pipeline.writeRed, pipeline.writeGreen, pipeline.writeBlue, pipeline.writeAlpha)
    ops.depthMask(pipeline.depthMask)
}

internal fun blendFactor(factor: BlendFactor): Int = when (factor) {
    BlendFactor.Zero -> GLES30.GL_ZERO
    BlendFactor.One -> GLES30.GL_ONE
    BlendFactor.SrcColor -> GLES30.GL_SRC_COLOR
    BlendFactor.OneMinusSrcColor -> GLES30.GL_ONE_MINUS_SRC_COLOR
    BlendFactor.DstColor -> GLES30.GL_DST_COLOR
    BlendFactor.OneMinusDstColor -> GLES30.GL_ONE_MINUS_DST_COLOR
    BlendFactor.SrcAlpha -> GLES30.GL_SRC_ALPHA
    BlendFactor.OneMinusSrcAlpha -> GLES30.GL_ONE_MINUS_SRC_ALPHA
    BlendFactor.DstAlpha -> GLES30.GL_DST_ALPHA
    BlendFactor.OneMinusDstAlpha -> GLES30.GL_ONE_MINUS_DST_ALPHA
}

internal fun blendEquation(equation: BlendEquation): Int = when (equation) {
    BlendEquation.Add -> GLES30.GL_FUNC_ADD
    BlendEquation.Subtract -> GLES30.GL_FUNC_SUBTRACT
    BlendEquation.ReverseSubtract -> GLES30.GL_FUNC_REVERSE_SUBTRACT
}

internal object GlesPipelineOps : PipelineOps {
    override fun blend(enabled: Boolean) {
        if (enabled) GLES30.glEnable(GLES30.GL_BLEND) else GLES30.glDisable(GLES30.GL_BLEND)
    }

    override fun blendFunc(src: Int, dst: Int) {
        GLES30.glBlendFunc(src, dst)
    }

    override fun blendEquation(equation: Int) {
        GLES30.glBlendEquation(equation)
    }

    override fun scissorTest(enabled: Boolean) {
        if (enabled) GLES30.glEnable(GLES30.GL_SCISSOR_TEST) else GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        GLES30.glScissor(x, y, width, height)
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        GLES30.glColorMask(red, green, blue, alpha)
    }

    override fun depthMask(enabled: Boolean) {
        GLES30.glDepthMask(enabled)
    }
}
