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
    Posterize,
    Film,
    Grade,
    Warp,
    Prism,
    Spotlight,
    Beacon,
    Composite,
    Reveal,
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
        focus = "Shows bindTime(...), temporal animation, and how stdlib helpers like pulse(...) can shrink noisy shader math.",
        snippet = """
            val time = uniformTime(name = "pulse_time")
            val wave = pulse(time, speed, row * 0.7f)
            val glow = pow(wave, 3f)
            mix(base, mix(pixelBase, accent, active * glow), amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Signal,
        title = "Signal",
        subtitle = "Procedural lines with fract/mod/step/smoothstep and bool branches.",
        focus = "Shows how stdlib helpers like gridMask(...) and scanlines(...) can clean up procedural shaders without hiding the generated AGSL.",
        snippet = """
            val grid = let(gridMask(uv, density, lineWidth), "grid")
            val scan = let(scanlines(fragCoord.y, 14f, 3f), "scan")
            ifElse(active, mix(base, mixed, amount), base)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Posterize,
        title = "Posterize",
        subtitle = "Recipe-level stdlib helper layered on top of the core DSL.",
        focus = "Shows the new v0.2 direction: keep core low-level, then build readable authoring helpers like posterize(...) in a separate stdlib module.",
        snippet = """
            val levels by autoUniformFloat(5f)
            val amount by autoUniformFloat(0.85f)
            val base = let(sample(), "base")
            mix(base, posterize(base, levels), amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Film,
        title = "Film",
        subtitle = "Procedural grain, noise drift, and vignette from the stdlib layer.",
        focus = "Shows the second v0.2 stdlib wave: reusable procedural helpers that stay readable in Kotlin while still compiling into ordinary AGSL math.",
        snippet = """
            val noise = let(grain(uv, time, grainScale), "grain")
            val drift = let(remap(valueNoise(uv * 6f + float2(time * 0.08f, 0f)), 0f, 1f, 0.92f, 1.05f), "drift")
            val mask = let(vignette(uv, 0.35f, 1.05f), "mask")
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Grade,
        title = "Grade",
        subtitle = "Color grading helpers and blend modes from the stdlib layer.",
        focus = "Shows the new color-oriented v0.2 stdlib surface: adjustSaturation(...), blendMultiply(...), blendScreen(...), and blendOverlay(...) layered on top of plain DSL math.",
        snippet = """
            val saturated = let(adjustSaturation(base, 1.35f), "saturated")
            val tinted = let(blendMultiply(saturated, tint, 0.25f), "tinted")
            val lifted = let(blendScreen(tinted, tint, glow), "lifted")
            blendOverlay(base, lifted, amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Warp,
        title = "Warp",
        subtitle = "fBm and domain warp helpers for fluid coordinate distortion.",
        focus = "Shows the v0.2 procedural stack growing beyond one-off noise helpers into reusable space-warp building blocks.",
        snippet = """
            val warpedUv = let(domainWarp(uv * scale, time * 0.25f, warpAmount), "warped_uv")
            val drift = let((fbm(warpedUv, octaves = 5) * 2f - 1f) * driftAmount, "drift")
            sample(fragCoord + float2(0f, drift))
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Prism,
        title = "Prism",
        subtitle = "Cosine palette plus chromatic offset in the stdlib layer.",
        focus = "Shows palette authoring and multi-sample color separation without dropping down to hand-written AGSL strings.",
        snippet = """
            val palette = let(cosinePalette(luminance(base) + uv.x * spread), "palette")
            val refracted = let(chromaticOffset(offset = shift, direction = float2(1f, 0.3f), amount = amount), "refracted")
            blendScreen(refracted, color(palette, base.a), amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Spotlight,
        title = "Spotlight",
        subtitle = "Circle, ring, and rect masks from the stdlib layer.",
        focus = "Shows the next v0.2 helper layer: reusable soft masks for focus halos, selective grading, and shape-driven UI emphasis.",
        snippet = """
            val focus = circleMask(uv, center = center, radius = radius, feather = 0.18f)
            val halo = ringMask(uv, center = center, radius = radius + 0.07f, width = 0.10f, feather = 0.05f)
            val panel = rectMask(uv, center = float2(0.78f, 0.5f), size = float2(0.26f, 0.58f), feather = 0.04f)
            blendOverlay(focused, panelTint, panel * amount * 0.35f)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Beacon,
        title = "Beacon",
        subtitle = "Timing and easing helpers layered on top of masks.",
        focus = "Shows pingPong(...), easeInOutSine(...), and easeInOutCubic(...) driving animated focus motion without dropping into noisy hand-written timeline math.",
        snippet = """
            val phase = pingPong(time * speed, 1f)
            val travel = easeInOutSine(phase)
            val glow = easeInOutCubic(pingPong(time * speed + 0.22f, 1f))
            val center = float2(mix(0.18f, 0.82f, travel), 0.5f + sin(time * 0.8f) * 0.12f)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Composite,
        title = "Composite",
        subtitle = "Mask-driven compositing recipes for layered UI treatments.",
        focus = "Shows maskedMix(...), alphaMask(...), maskedScreen(...), and maskedOverlay(...) as reusable high-level authoring blocks instead of one-off blend math.",
        snippet = """
            val glowLayer = alphaMask(color(float3(0.12f, 0.95f, 1f), 1f), halo, amount)
            val screened = maskedScreen(base, glowLayer, halo, amount)
            val overlaid = maskedOverlay(screened, panelTint, panel, amount * 0.6f)
            maskedMix(base, overlaid, focus, amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Reveal,
        title = "Reveal",
        subtitle = "Horizontal, vertical, and radial transitions from the stdlib layer.",
        focus = "Shows horizontalReveal(...), verticalReveal(...), and radialReveal(...) driving stylized content transitions without hand-written threshold math in every shader.",
        snippet = """
            val progress = easeInOutSine(pingPong(time * speed, 1f))
            val horizontal = horizontalReveal(uv, progress, feather = 0.07f)
            val vertical = verticalReveal(uv, progress, feather = 0.07f, fromTop = false)
            val radial = radialReveal(uv, progress, feather = 0.08f, maxRadius = 0.9f)
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
