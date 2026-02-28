package ru.redbyte.redbytefx

internal object FxCompiler {

    fun compile(graph: FxGraph): FxProgram {
        val layout = UniformLayout()
        graph.ops.forEach { it.collect(layout) }
        val agsl = buildAglsl(layout, graph.ops)
        return FxProgram(agsl = agsl, layout = layout, defaults = graph.defaults)
    }

    private fun buildAglsl(layout: UniformLayout, ops: List<Op>): String {
        val sb = StringBuilder()

        sb.append("uniform shader ").append(RB_INPUT_UNIFORM).append(";\n")
        sb.append("uniform float2 ").append(RB_RESOLUTION_UNIFORM).append(";\n")

        for (name in layout.floatUniforms.values) {
            sb.append("uniform float ").append(name).append(";\n")
        }
        for (name in layout.float2Uniforms.values) {
            sb.append("uniform float2 ").append(name).append(";\n")
        }

        sb.append("\n")
        sb.append("float rb_lerp(float a, float b, float t) { return a + (b - a) * t; }\n")
        sb.append("float2 rb_maxCoord(float2 res) {\n")
        sb.append("  float2 e = float2(0.001);\n")
        sb.append("  return max(res - e, float2(0.0));\n")
        sb.append("}\n")
        sb.append("\n")
        sb.append("half4 main(float2 fragCoord) {\n")
        sb.append("  float2 p = fragCoord;\n")

        val ctx = EmitContext(layout)
        for (op in ops) {
            op.emit(ctx, sb)
        }

        sb.append("  if (").append(RB_RESOLUTION_UNIFORM).append(".x > 2.0 && ")
            .append(RB_RESOLUTION_UNIFORM).append(".y > 2.0) {\n")
        sb.append("    p = clamp(p, float2(0.0), rb_maxCoord(").append(RB_RESOLUTION_UNIFORM).append("));\n")
        sb.append("  }\n")
        sb.append("  return ").append(RB_INPUT_UNIFORM).append(".eval(p);\n")
        sb.append("}\n")

        return sb.toString()
    }
}

internal class EmitContext(
    private val layout: UniformLayout
) {
    private var tmpIndex = 0

    fun tmp(prefix: String): String = prefix + (tmpIndex++)

    fun u(p: FxParam.Float): String = layout.floatUniforms[p] ?: error("Missing uniform")
    fun u(p: FxParam.Float2): String = layout.float2Uniforms[p] ?: error("Missing uniform")

    fun pivotExpr(pivot: Pivot): String =
        when (pivot) {
            Pivot.Center -> "${RB_RESOLUTION_UNIFORM} * 0.5"
            is Pivot.Fraction -> "${RB_RESOLUTION_UNIFORM} * float2(${pivot.x}, ${pivot.y})"
        }
}