package ru.redbyte.redbytefx.sample.model

enum class DemoId {
    Flip,
    Mirror,
    Rotate,
    Scale,
    Offset,
    Wave,
    Pulse,
    Signal,
    Duotone
}

data class DemoInfo(
    val id: DemoId,
    val title: String,
    val subtitle: String,
    val focus: String,
    val snippet: String
)

val DemoCatalog: List<DemoInfo> = listOf(
    DemoInfo(
        id = DemoId.Flip,
        title = "Flip",
        subtitle = "Flip X/Y with simple float uniforms.",
        focus = "Shows the smallest useful RedByteFX effect: uniforms, helper transforms, and bindFloat(...).",
        snippet = """
            val flipX = uniformFloat(0f, "flip_x")
            val flipY = uniformFloat(0f, "flip_y")
            sample(flipY(coord = flipX(amount = flipX), amount = flipY))
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Mirror,
        title = "Mirror",
        subtitle = "Symmetry X/Y using left/right or top/bottom half.",
        focus = "Shows parameterized helpers with multiple uniforms and enum-like values mapped into shader floats.",
        snippet = """
            val enabled = uniformFloat(0f, "mirror_x_enabled")
            val from = uniformFloat(MirrorXFrom.Right.shaderValue, "mirror_x_from")
            sample(mirrorX(amount = enabled, from = from))
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Rotate,
        title = "Rotate",
        subtitle = "Custom rotation written directly in the DSL.",
        focus = "Shows that the DSL is not limited to presets: you can write raw coordinate math yourself.",
        snippet = """
            val angle = uniformFloat(0f, "angle_deg")
            val theta = radians(angle)
            val rotated = pivot + float2(
                c * delta.x - s * delta.y,
                s * delta.x + c * delta.y
            )
            sample(rotated)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Scale,
        title = "Scale",
        subtitle = "Uniform float2 with pivot-aware scaling.",
        focus = "Shows vector uniforms and helper composition around the current content center.",
        snippet = """
            val scale = uniformFloat2(1f, 1f, "scale")
            sample(scale(scale = scale))
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Offset,
        title = "Offset",
        subtitle = "Translate content with a float2 uniform.",
        focus = "Shows the most direct coordinate remap path and bindFloat2(...).",
        snippet = """
            val delta = uniformFloat2(0f, 0f, "offset")
            sample(offset(delta = delta))
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Wave,
        title = "Wave",
        subtitle = "Non-linear sine warp to prove the DSL is not just presets.",
        focus = "Shows locals with let(...), custom math, and a warp built from ordinary shader expressions.",
        snippet = """
            val amp = uniformFloat(0f, "wave_amplitude")
            val freq = uniformFloat(0.08f, "wave_frequency")
            val waveOffset = let(float2(0f, sin(fragCoord.x * freq) * amp), "wave_offset")
            sample(fragCoord + waveOffset)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Pulse,
        title = "Pulse",
        subtitle = "Animated time uniform with floor/ceil/pow and pixel quantization.",
        focus = "Shows bindTime(...), temporal animation, and a richer stack of procedural math helpers.",
        snippet = """
            val time = uniformTime(name = "pulse_time")
            val wave = 0.5f + 0.5f * sin(time * speed + row * 0.7f)
            val glow = pow(wave, 3f)
            mix(base, mix(pixelBase, accent, active * glow), amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Signal,
        title = "Signal",
        subtitle = "Procedural lines with fract/mod/step/smoothstep and bool branches.",
        focus = "Shows fn(...), boolean expressions, procedural masks, and a shader that feels much closer to hand-written AGSL.",
        snippet = """
            val pulseBand = fn(name = "pulse_band", arg1 = FloatType, arg2 = FloatType, returns = FloatType) { phase, threshold ->
                step(threshold, smoothstep(0.08f, 0.92f, fract(phase)))
            }
            ifElse(active, mix(base, mixed, amount), base)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Duotone,
        title = "Duotone",
        subtitle = "Color mixing, luminance and local lets in the shader DSL.",
        focus = "Shows color-first authoring, float3/float4 usage, and reusable palette functions.",
        snippet = """
            val palette = fn(name = "palette_rgb", arg1 = FloatType, arg2 = FloatType, returns = Float3Type) { tone, warmth ->
                float3(...)
            }
            val luma = let(luminance(base), "luma")
            mix(base, mix(mono, lifted, 0.85f), amount)
        """.trimIndent()
    )
)

fun demoInfo(id: DemoId): DemoInfo = DemoCatalog.first { it.id == id }
