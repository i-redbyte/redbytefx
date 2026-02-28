package ru.redbyte.redbytefx

internal sealed interface Op {
    fun collect(layout: UniformLayout)
    fun emit(ctx: EmitContext, sb: StringBuilder)
}

internal data class OpFlipX(val enabled: FxParam.Float) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float(enabled)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        val t = ctx.tmp("t")
        sb.append("  float ").append(t).append(" = clamp(").append(ctx.u(enabled)).append(", 0.0, 1.0);\n")
        sb.append("  p.x = rb_lerp(p.x, uResolution.x - p.x, ").append(t).append(");\n")
    }
}

internal data class OpFlipY(val enabled: FxParam.Float) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float(enabled)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        val t = ctx.tmp("t")
        sb.append("  float ").append(t).append(" = clamp(").append(ctx.u(enabled)).append(", 0.0, 1.0);\n")
        sb.append("  p.y = rb_lerp(p.y, uResolution.y - p.y, ").append(t).append(");\n")
    }
}

internal data class OpMirrorX(
    val enabled: FxParam.Float,
    val from: MirrorXFrom
) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float(enabled)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        val t = ctx.tmp("t")
        val cx = ctx.tmp("cx")
        val mx = ctx.tmp("mx")

        sb.append("  float ").append(t).append(" = clamp(").append(ctx.u(enabled)).append(", 0.0, 1.0);\n")
        sb.append("  float ").append(cx).append(" = ").append(RB_RESOLUTION_UNIFORM).append(".x * 0.5;\n")

        when (from) {
            MirrorXFrom.Right -> {
                sb.append("  float ").append(mx).append(" = ").append(cx).append(" + abs(p.x - ").append(cx).append(");\n")
            }
            MirrorXFrom.Left -> {
                sb.append("  float ").append(mx).append(" = ").append(cx).append(" - abs(p.x - ").append(cx).append(");\n")
            }
        }

        sb.append("  p.x = rb_lerp(p.x, ").append(mx).append(", ").append(t).append(");\n")
    }
}

internal data class OpMirrorY(
    val enabled: FxParam.Float,
    val from: MirrorYFrom
) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float(enabled)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        val t = ctx.tmp("t")
        val cy = ctx.tmp("cy")
        val my = ctx.tmp("my")

        sb.append("  float ").append(t).append(" = clamp(").append(ctx.u(enabled)).append(", 0.0, 1.0);\n")
        sb.append("  float ").append(cy).append(" = ").append(RB_RESOLUTION_UNIFORM).append(".y * 0.5;\n")

        when (from) {
            MirrorYFrom.Bottom -> {
                sb.append("  float ").append(my).append(" = ").append(cy).append(" + abs(p.y - ").append(cy).append(");\n")
            }
            MirrorYFrom.Top -> {
                sb.append("  float ").append(my).append(" = ").append(cy).append(" - abs(p.y - ").append(cy).append(");\n")
            }
        }

        sb.append("  p.y = rb_lerp(p.y, ").append(my).append(", ").append(t).append(");\n")
    }
}

internal data class OpRotate(val deg: FxParam.Float, val pivot: Pivot) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float(deg)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        val pv = ctx.tmp("pv")
        val a = ctx.tmp("a")
        val s = ctx.tmp("s")
        val c = ctx.tmp("c")
        val v = ctx.tmp("v")

        sb.append("  float2 ").append(pv).append(" = ").append(ctx.pivotExpr(pivot)).append(";\n")
        sb.append("  float ").append(a).append(" = ").append(ctx.u(deg)).append(" * 0.01745329252;\n")
        sb.append("  float ").append(s).append(" = sin(").append(a).append(");\n")
        sb.append("  float ").append(c).append(" = cos(").append(a).append(");\n")
        sb.append("  float2 ").append(v).append(" = p - ").append(pv).append(";\n")
        sb.append("  p = ").append(pv).append(" + float2(")
            .append(c).append("*").append(v).append(".x - ").append(s).append("*").append(v).append(".y, ")
            .append(s).append("*").append(v).append(".x + ").append(c).append("*").append(v).append(".y")
            .append(");\n")
    }
}

internal data class OpScale(val scale: FxParam.Float2, val pivot: Pivot) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float2(scale)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        val pv = ctx.tmp("pv")
        val sc = ctx.tmp("sc")
        val s = ctx.tmp("s")

        sb.append("  float2 ").append(pv).append(" = ").append(ctx.pivotExpr(pivot)).append(";\n")
        sb.append("  float2 ").append(sc).append(" = ").append(ctx.u(scale)).append(";\n")
        sb.append("  float2 ").append(s).append(" = float2(max(").append(sc).append(".x, 0.0001), max(").append(sc).append(".y, 0.0001));\n")
        sb.append("  p = ").append(pv).append(" + (p - ").append(pv).append(") / ").append(s).append(";\n")
    }
}

internal data class OpOffset(val delta: FxParam.Float2) : Op {
    override fun collect(layout: UniformLayout) {
        layout.float2(delta)
    }

    override fun emit(ctx: EmitContext, sb: StringBuilder) {
        sb.append("  p = p - ").append(ctx.u(delta)).append(";\n")
    }
}