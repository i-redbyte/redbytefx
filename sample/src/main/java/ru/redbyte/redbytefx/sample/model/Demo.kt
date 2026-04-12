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
    Frame,
    Corner,
    Reveal,
    Sweep,
    Glitch,
    Radar,
    Halo,
    Circuit,
    Sigil,
    Duotone,
    Aurora,
    LiquidGlass,
    AnimatedGradient,
    TouchRipple,
    Metaballs,
    CrtTerminal
}

enum class DemoSection(
    val title: String,
    val subtitle: String
) {
    Foundations(
        title = "Foundations",
        subtitle = "Core transforms, direct coordinate math, and the smallest useful shader building blocks."
    ),
    Motion(
        title = "Motion",
        subtitle = "Time-driven effects, eased timelines, and animated runtime bindings."
    ),
    Procedural(
        title = "Procedural",
        subtitle = "Noise, signals, warp fields, and pattern helpers layered on top of the DSL."
    ),
    Color(
        title = "Color",
        subtitle = "Palettes, grading, quantization, and color-first shader authoring."
    ),
    Compositing(
        title = "Compositing",
        subtitle = "Masks, reveals, and layered UI treatments built from reusable recipe helpers."
    )
}

enum class DemoLayer(val label: String) {
    Core("CORE DSL"),
    Stdlib("STDLIB")
}

data class DemoInfo(
    val id: DemoId,
    val title: String,
    val subtitle: String,
    val focus: String,
    val snippet: String
)

data class DemoFollowUp(
    val demo: DemoInfo,
    val label: String,
    val description: String
)

enum class DemoPathKind {
    StartHere,
    Canonical,
    Exploratory,
    Foundation
}

data class DemoPathSignal(
    val kind: DemoPathKind,
    val badge: String,
    val title: String,
    val body: String,
    val family: String? = null
)

data class CanonicalGuide(
    val label: String,
    val title: String,
    val summary: String,
    val helperPreview: String,
    val demoIds: List<DemoId>
)

val CanonicalGuideCatalog: List<CanonicalGuide> = listOf(
    CanonicalGuide(
        label = "COORDINATES",
        title = "Coordinate space and sampling",
        summary = "Normalize coordinates once, keep sampling-space decisions explicit, then build local UV/light space from there.",
        helperPreview = "normalizedUv(...), sampleUv(...), centeredUv(...), aspectCenteredUv(...)",
        demoIds = listOf(DemoId.Halo)
    ),
    CanonicalGuide(
        label = "MASKS / REVEAL",
        title = "Readable masks before style chrome",
        summary = "Start from named masks and reveal helpers instead of re-writing edge and falloff math inside every shader.",
        helperPreview = "circleMask(...), rectMask(...), ringMask(...), horizontalReveal(...), verticalReveal(...), radialReveal(...)",
        demoIds = listOf(DemoId.Spotlight, DemoId.Reveal)
    ),
    CanonicalGuide(
        label = "COMPOSITING",
        title = "Layered color work with explicit intent",
        summary = "Keep base, blend, mask, and amount readable so the generated AGSL still matches the authored compositing story.",
        helperPreview = "maskedMix(...), alphaMask(...), maskedScreen(...)",
        demoIds = listOf(DemoId.Composite)
    ),
    CanonicalGuide(
        label = "SHAPING / SDF",
        title = "Signed distance to fill/stroke",
        summary = "Turn distance fields into fills and strokes first, then build larger authored scenes from those stable shape pieces.",
        helperPreview = "sdCircle(...), sdRoundedBox(...), softFill(...), softStroke(...)",
        demoIds = listOf(DemoId.Sigil)
    ),
    CanonicalGuide(
        label = "SIGNAL / GRADIENTS / POLAR",
        title = "Time-driven ramps and scan logic",
        summary = "Treat time, sweeps, ramps, and polar scans as named authored signals instead of one-off procedural fragments.",
        helperPreview = "pulse(...), bandMask(...), linearRamp(...), radialRamp(...), angularSweep(...)",
        demoIds = listOf(DemoId.Signal, DemoId.Sweep, DemoId.Radar)
    ),
    CanonicalGuide(
        label = "ROUTING",
        title = "Scene structure from reusable segments",
        summary = "Build authored route logic from segment helpers before reaching for heavier scene-specific math.",
        helperPreview = "segmentMask(...), segmentProgress(...), segmentPulse(...)",
        demoIds = listOf(DemoId.Circuit)
    )
)

val DemoInfo.isStartHere: Boolean
    get() = when (id) {
        DemoId.Wave,
        DemoId.Signal,
        DemoId.Composite,
        DemoId.Circuit -> true

        else -> false
    }

val DemoInfo.canonicalFamily: String?
    get() = when (id) {
        DemoId.Wave -> "FOUNDATIONS"
        DemoId.Offset -> "COORDINATES"
        DemoId.Pulse -> "TIMING"
        DemoId.Signal -> "PATTERNS"
        DemoId.Spotlight -> "MASKS"
        DemoId.Composite -> "COMPOSITING"
        DemoId.Reveal -> "REVEALS"
        DemoId.Sweep -> "GRADIENTS"
        DemoId.Radar -> "POLAR"
        DemoId.Halo -> "UV + LIGHT"
        DemoId.Sigil -> "SDF"
        DemoId.Circuit -> "ROUTING"
        DemoId.Aurora -> "SHOWCASE"
        DemoId.LiquidGlass -> "SHOWCASE"
        else -> null
    }

val DemoInfo.isCanonicalDemo: Boolean
    get() = canonicalFamily != null

val DemoInfo.pathSignal: DemoPathSignal
    get() = when {
        isStartHere && layer == DemoLayer.Core -> DemoPathSignal(
            kind = DemoPathKind.StartHere,
            badge = "START HERE",
            title = "Recommended first raw DSL route",
            body = "Use this demo as a first mental model for coordinates, locals, uniforms, and AGSL-shaped sampling before layering on stdlib recipes.",
            family = canonicalFamily
        )

        isStartHere -> DemoPathSignal(
            kind = DemoPathKind.StartHere,
            badge = "START HERE",
            title = "Recommended first stdlib route",
            body = "This demo is part of the curated starter path. Read it as a preferred entry into named helpers before exploring more stylized variants.",
            family = canonicalFamily
        )

        isCanonicalDemo -> DemoPathSignal(
            kind = DemoPathKind.Canonical,
            badge = "CANONICAL",
            title = "Curated helper family",
            body = "This sits on the recommended first teaching surface. Learn this helper family before jumping into broader stylistic or recipe-heavy demos.",
            family = canonicalFamily
        )

        layer == DemoLayer.Stdlib -> DemoPathSignal(
            kind = DemoPathKind.Exploratory,
            badge = "EXPLORATORY",
            title = "Secondary stdlib territory",
            body = "Useful helpers live here, but this is intentionally beyond the first teaching surface. Map it back to the canonical families first, then use it for richer style work."
        )

        else -> DemoPathSignal(
            kind = DemoPathKind.Foundation,
            badge = "FOUNDATION",
            title = "Supportive raw DSL building block",
            body = "This demo stays close to AGSL shape and helps reinforce the core language model, even though it is not one of the primary starter stops."
        )
    }

val DemoInfo.focusTags: List<String>
    get() = when (id) {
        DemoId.Flip -> listOf("transform", "uniforms", "runtime binding")
        DemoId.Mirror -> listOf("symmetry", "branching", "direction")
        DemoId.Rotate -> listOf("raw DSL", "trig", "pivot")
        DemoId.Scale -> listOf("float2", "scaling", "pivot")
        DemoId.Offset -> listOf("float2", "coordinates", "translation")
        DemoId.Wave -> listOf("locals", "sine warp", "coordinate math")
        DemoId.Pulse -> listOf("time", "modulation", "pixel grid")
        DemoId.Signal -> listOf("grid", "scanlines", "mask logic")
        DemoId.Posterize -> listOf("quantization", "remap", "tone shaping")
        DemoId.Film -> listOf("grain", "vignette", "noise")
        DemoId.Grade -> listOf("saturation", "blend", "tint")
        DemoId.Warp -> listOf("fbm", "domain warp", "noise")
        DemoId.Prism -> listOf("palette", "chromatic", "sampling")
        DemoId.Spotlight -> listOf("masks", "radial", "lighting")
        DemoId.Beacon -> listOf("polar", "sweep", "animation")
        DemoId.Composite -> listOf("masked mix", "overlay", "screen")
        DemoId.Frame -> listOf("frame mask", "edge fade", "sweep")
        DemoId.Corner -> listOf("corner mask", "HUD", "frame")
        DemoId.Reveal -> listOf("transitions", "progress", "masked mix")
        DemoId.Sweep -> listOf("gradients", "directional", "feather")
        DemoId.Glitch -> listOf("scan warp", "signal", "distortion")
        DemoId.Radar -> listOf("arc mask", "polar", "scan")
        DemoId.Halo -> listOf("rim light", "center glow", "lighting")
        DemoId.Circuit -> listOf("routing", "SDF", "Compose scene")
        DemoId.Sigil -> listOf("SDF", "soft stroke", "shape composition")
        DemoId.Duotone -> listOf("palette fn", "luminance", "color mix")
        DemoId.Aurora -> listOf("hero", "iridescent", "chromatic", "rim", "polar sweep")
        DemoId.LiquidGlass -> listOf("glass", "refraction", "fresnel", "domain warp", "liquid")
        DemoId.AnimatedGradient -> listOf("gradient", "sin", "rgb", "time", "uv", "port", "agsl")
        DemoId.TouchRipple -> listOf("pointer", "float2", "touch", "compose", "ripple", "time")
        DemoId.Metaballs -> listOf("sdf", "metaballs", "smoothmin", "procedural", "animation")
        DemoId.CrtTerminal -> listOf("crt", "scanlines", "barrel", "chromatic", "retro", "terminal")
    }

val DemoInfo.catalogSearchText: String
    get() = buildString {
        append(id.name)
        append(' ')
        append(title)
        append(' ')
        append(subtitle)
        append(' ')
        append(focus)
        append(' ')
        append(section.title)
        append(' ')
        append(section.subtitle)
        append(' ')
        append(layer.label)
        append(' ')
        append(if (isAnimated) "animated" else "static")
        append(' ')
        if (isStartHere) {
            append("start here ")
        }
        if (isCanonicalDemo) {
            append("canonical ")
            append(canonicalFamily)
            append(' ')
        }
        append(focusTags.joinToString(separator = " "))
    }.lowercase()

val DemoInfo.section: DemoSection
    get() = when (id) {
        DemoId.Flip,
        DemoId.Mirror,
        DemoId.Rotate,
        DemoId.Scale,
        DemoId.Offset,
        DemoId.Wave -> DemoSection.Foundations

        DemoId.Pulse,
        DemoId.Beacon,
        DemoId.Sweep,
        DemoId.AnimatedGradient -> DemoSection.Motion

        DemoId.Signal,
        DemoId.Film,
        DemoId.Warp,
        DemoId.Glitch,
        DemoId.Radar,
        DemoId.Circuit,
        DemoId.Metaballs,
        DemoId.CrtTerminal -> DemoSection.Procedural

        DemoId.Posterize,
        DemoId.Grade,
        DemoId.Prism,
        DemoId.Duotone -> DemoSection.Color

        DemoId.Spotlight,
        DemoId.Composite,
        DemoId.Frame,
        DemoId.Corner,
        DemoId.Halo,
        DemoId.Sigil,
        DemoId.Reveal,
        DemoId.Aurora,
        DemoId.LiquidGlass,
        DemoId.TouchRipple -> DemoSection.Compositing
    }

val DemoInfo.layer: DemoLayer
    get() = when (id) {
        DemoId.Flip,
        DemoId.Mirror,
        DemoId.Rotate,
        DemoId.Scale,
        DemoId.Offset,
        DemoId.Wave,
        DemoId.Duotone,
        DemoId.AnimatedGradient,
        DemoId.TouchRipple -> DemoLayer.Core

        DemoId.Pulse,
        DemoId.Signal,
        DemoId.Posterize,
        DemoId.Film,
        DemoId.Grade,
        DemoId.Warp,
        DemoId.Prism,
        DemoId.Spotlight,
        DemoId.Beacon,
        DemoId.Composite,
        DemoId.Frame,
        DemoId.Corner,
        DemoId.Reveal,
        DemoId.Sweep,
        DemoId.Glitch,
        DemoId.Radar,
        DemoId.Halo,
        DemoId.Circuit,
        DemoId.Sigil,
        DemoId.Aurora,
        DemoId.LiquidGlass,
        DemoId.Metaballs,
        DemoId.CrtTerminal -> DemoLayer.Stdlib
    }

val DemoInfo.isAnimated: Boolean
    get() = when (id) {
        DemoId.Pulse,
        DemoId.Film,
        DemoId.Warp,
        DemoId.Beacon,
        DemoId.Frame,
        DemoId.Corner,
        DemoId.Reveal,
        DemoId.Sweep,
        DemoId.Glitch,
        DemoId.Radar,
        DemoId.Halo,
        DemoId.Circuit,
        DemoId.Sigil,
        DemoId.Aurora,
        DemoId.LiquidGlass,
        DemoId.AnimatedGradient,
        DemoId.TouchRipple,
        DemoId.Metaballs,
        DemoId.CrtTerminal -> true

        DemoId.Flip,
        DemoId.Mirror,
        DemoId.Rotate,
        DemoId.Scale,
        DemoId.Offset,
        DemoId.Wave,
        DemoId.Signal,
        DemoId.Posterize,
        DemoId.Grade,
        DemoId.Prism,
        DemoId.Spotlight,
        DemoId.Composite,
        DemoId.Duotone -> false
    }

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
        subtitle = "A secondary color recipe layered on top of the core DSL path.",
        focus = "Extends the canonical core sampling/color path with posterize(...) and remap-style quantization once the base DSL mental model already feels comfortable.",
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
        subtitle = "A secondary signal/style pass built on the canonical procedural path.",
        focus = "Extends the canonical signal and ramp vocabulary with grain(...), valueNoise(...), and vignette(...) once the simpler procedural helpers already read clearly.",
        snippet = """
            val noise = let(grain(uv, time, grainScale), "grain")
            val drift = let(remap(valueNoise(uv * 6f + float2(time * 0.08f, 0f)), 0f, 1f, 0.92f, 1.05f), "drift")
            val mask = let(vignette(uv, 0.35f, 1.05f), "mask")
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Grade,
        title = "Grade",
        subtitle = "A secondary color pass extending canonical compositing and tinting.",
        focus = "Extends the canonical compositing/color path with adjustSaturation(...), blendMultiply(...), blendScreen(...), and blendOverlay(...) after mask and mix logic are already familiar.",
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
        subtitle = "A secondary procedural pass extending canonical coordinate work.",
        focus = "Extends the canonical coordinate and signal path into heavier space-warp helpers like fbm(...) and domainWarp(...), which are best read after the simpler UV and mask model is settled.",
        snippet = """
            val warpedUv = let(domainWarp(uv * scale, time * 0.25f, warpAmount), "warped_uv")
            val drift = let((fbm(warpedUv, octaves = 5) * 2f - 1f) * driftAmount, "drift")
            sample(fragCoord + float2(0f, drift))
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Prism,
        title = "Prism",
        subtitle = "A secondary color/sampling pass built on the canonical sampling path.",
        focus = "Extends the canonical sampling and compositing path with cosinePalette(...) and chromaticOffset(...) for stylized color separation once the base sample(...) vs sampleUv(...) model is already clear.",
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
        subtitle = "A secondary motion pass extending the canonical mask path with easing.",
        focus = "Extends the canonical masks and lighting route with pingPong(...), easeInOutSine(...), and easeInOutCubic(...) so animated focus motion stays readable after the base mask vocabulary is familiar.",
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
        id = DemoId.Frame,
        title = "Frame",
        subtitle = "A decorative extension of the canonical mask/reveal path.",
        focus = "Extends the canonical mask and reveal surface with edgeDistance(...), edgeFade(...), and frameMask(...) to build panel chrome after the simpler focus and reveal helpers are already understood.",
        snippet = """
            val frame = frameMask(uv, thickness, feather = 0.03f)
            val interior = edgeFade(uv, thickness + 0.08f)
            val sweep = directionalSweep(uv, direction = float2(1f, -0.24f), center = center, width = 0.20f, feather = 0.08f)
            maskedOverlay(screened, shellTint, frame + (1f - interior) * 0.28f, amount * 0.45f)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Corner,
        title = "Corner",
        subtitle = "A decorative extension of the canonical frame/mask path.",
        focus = "Extends the canonical frame and mask path with cornerMask(...) and directionalSweep(...) so bracket-style HUD chrome stays readable without devolving into per-corner AGSL math.",
        snippet = """
            val corners = cornerMask(uv, size = size, thickness = thickness, feather = 0.03f)
            val sweep = directionalSweep(uv, direction = float2(1f, -0.20f), center = center, width = 0.16f, feather = 0.08f)
            maskedScreen(base, accent, corners * sweep, amount)
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
        id = DemoId.Sweep,
        title = "Sweep",
        subtitle = "Directional ramps and moving sweep bands for UI lighting.",
        focus = "Shows linearRamp(...), radialRamp(...), and directionalSweep(...) as reusable helpers for panel light passes, scanning beams, and stylized UI highlights.",
        snippet = """
            val center = 0.16f + pingPong(time * 0.18f, 1f) * 0.68f
            val ramp = linearRamp(uv, direction = float2(1f, -0.35f), start = 0.08f, end = 0.92f)
            val sweep = directionalSweep(uv, direction = float2(1f, -0.35f), center = center, width = 0.22f, feather = 0.08f)
            val vignette = radialRamp(uv, innerRadius = 0.12f, outerRadius = 0.68f)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Glitch,
        title = "Glitch",
        subtitle = "A stylized extension of the canonical signal/gradient path.",
        focus = "Extends the canonical signal and gradient route with signalBars(...), bandMask(...), and scanWarp(...) for tearing and distortion after the base scan/ramp model is already clear.",
        snippet = """
            val bars = signalBars(uv.y, density = density, width = 0.28f, phase = time * 0.65f, feather = 0.10f)
            val lock = bandMask(uv.y, center = 0.22f + pingPong(time * 0.12f, 1f) * 0.56f, width = 0.14f, feather = 0.08f)
            val driftUv = scanWarp(uv, time = time, amplitude = warp, density = density, speed = 2.2f, noiseAmount = 0.55f)
            maskedMix(base, glitched, max(bars, lock), amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Radar,
        title = "Radar",
        subtitle = "Polar sweep masks and arc helpers for rotating scan interfaces.",
        focus = "Shows polarAngle01(...), polarCoordinates(...), angularSweep(...), and arcMask(...) as reusable polar authoring blocks for scanners, orbital indicators, and radial UI logic.",
        snippet = """
            val polar = polarCoordinates(uv)
            val sweep = angularSweep(uv, angle = sweepAngle, width = 0.12f, feather = 0.03f)
            val arc = arcMask(uv, radius = radius, ringWidth = 0.09f, angle = sweepAngle, arcWidth = 0.18f, feather = 0.03f)
            maskedScreen(base, tint, sweep * beam + arc, amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Halo,
        title = "Halo",
        subtitle = "Aspect-corrected glow and rim lighting for sci-fi UI accents.",
        focus = "Shows centeredUv(...), aspectCenteredUv(...), radialDirection(...), centerGlow(...), and rimLight(...) as coordinate-first lighting helpers instead of one-off radial math inside every effect.",
        snippet = """
            val local = centeredUv(uv)
            val aspectLocal = aspectCenteredUv(uv, resolution)
            val dir = radialDirection(uv, resolution)
            val glow = centerGlow(uv, resolution, radius = radius * pulse, feather = 0.18f)
            val rim = rimLight(uv, resolution, radius = radius + 0.08f * pulse, width = 0.075f, feather = 0.024f)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Circuit,
        title = "Circuit",
        subtitle = "A small PCB scene with tappable nodes and animated signal routes.",
        focus = "Shows how SDF primitives, segmentMask(...), segmentPulse(...), and ordinary Compose interaction can already build a board-like scene where taps switch active electrical paths.",
        snippet = """
            val chip = softFill(sdRoundedBox(point = board - chipPos, halfSize = float2(0.22f, 0.13f), radius = 0.035f), 0.016f)
            val trace = segmentMask(point = board, start = sourcePos, end = chipPos, thickness = 0.05f, feather = 0.018f)
            val pulse = segmentPulse(point = board, start = sourcePos, end = chipPos, phase = fract(time * 0.42f), bandWidth = 0.22f, thickness = 0.05f)
            val routeMask = ifElse(route lt 0.5f, sourceFlow, ifElse(route lt 1.5f, chipFlow, outputFlow))
            maskedOverlay(copper, signalTint, routeMask, amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Sigil,
        title = "Sigil",
        subtitle = "Signed-distance shapes composed into a cyber glyph.",
        focus = "Shows sdCircle(...), sdBox(...), sdRoundedBox(...), fill(...), softFill(...), stroke(...), and softStroke(...) as a reusable SDF authoring layer instead of hand-written edge math in every shader.",
        snippet = """
            val sigil = aspectCenteredUv(uv, resolution)
            val frame = softStroke(sdRoundedBox(sigil, halfSize = float2(0.35f, 0.35f), radius = 0.16f), width = 0.028f, feather = 0.012f)
            val ring = softStroke(sdCircle(sigil, radius = 0.26f + pulse * 0.03f), width = 0.032f, feather = 0.014f)
            val spine = softFill(sdBox(sigil, halfSize = float2(0.05f, 0.22f + pulse * 0.05f)), feather = 0.012f)
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
    ),
    DemoInfo(
        id = DemoId.Aurora,
        title = "Aurora",
        subtitle = "Hero showcase: iridescent rim, rotating sweep, and chromatic polish.",
        focus = "Combines rimLight(...), angularSweep(...), cosinePalette(...), and chromaticOffset(...) into one readable compositing stack for marketing-grade visuals.",
        snippet = """
            val phase = fract(time * speed)
            val sweep = angularSweep(uv, angle = phase, width = 0.26f, feather = 0.09f)
            val rim = rimLight(uv, resolution, radius = 0.4f, width = 0.085f, feather = 0.03f)
            val mask = saturate(max(rim, sweep * 0.72f))
            val pal = cosinePalette(luma + uv.x * spectral + phase * 0.55f)
            val split = chromaticOffset(offset = chromaPx, direction = float2(1f, -0.25f), amount = amount)
            maskedMix(split, blendScreen(base, color(pal, base.a), mask * amount), mask, amount)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.LiquidGlass,
        title = "Liquid Glass",
        subtitle = "Stylized glass: flowing refraction, Fresnel edge, cool tint.",
        focus = "Single-pass AGSL cannot do true multi-tap frosted blur; this demo fakes liquid glass with " +
            "domainWarp(...) + sampleUv(...), rim/shell masks, manual per-channel sampling for edge chroma, " +
            "and blendScreen(...). The sample UI shows a translucent pill button on a gradient so the effect " +
            "reads as glass over a control. Pair with platform blur under the content if you need real frosting.",
        snippet = """
            val warpedUv = saturate(domainWarp(uv * 3.2f, time * speed, refraction))
            val glass = sampleUv(warpedUv)
            val px = chromaPx / max(resolution.x, 0.0001f)
            val chromaGlass = color(sampleUv(warpedUv - float2(px, 0f)).r, glass.g, sampleUv(warpedUv + float2(px, 0f)).b, glass.a)
            val edge = max(rim, shell)
            mix(glass, chromaGlass, edge * chromaMix)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.AnimatedGradient,
        title = "Animated Gradient",
        subtitle = "Classic RGB sin waves over UV — straight port from raw AGSL.",
        focus = "No sampling, no stdlib: only fragCoord, resolution, uniformTime, and sin/mix-style " +
            "math. Use it as a reference when translating Shadertoy or hand-written AGSL into RedByteFX.",
        snippet = """
            val uv = let(fragCoord / resolution, "uv")
            val t = let(time * speed, "t")
            val r = let(0.5f + 0.5f * sin(3f * uv.x + t * 0.7f), "r")
            val g = let(0.5f + 0.5f * sin(3f * uv.y + t * 1.1f), "g")
            val b = let(0.5f + 0.5f * sin(3f * (uv.x + uv.y) + t * 0.9f), "b")
            color(float3(r, g, b), 1f)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.TouchRipple,
        title = "Touch Ripple",
        subtitle = "Pointer-driven ripples on live content with bindFloat2(...).",
        focus = "Shows how Compose pointerInput + normalized UV maps to a float2 uniform so runtime " +
            "interaction and shader rings stay in sync. Core DSL only: length, sin, mix — no stdlib.",
        snippet = """
            val d = length(uv - pointer)
            val waves = sin(d * 32f - time * 2.4f) * 0.5f + 0.5f
            val ripple = waves / (1f + d * 14f)
            mix(base, tint, ripple * strength)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.Metaballs,
        title = "Metaballs",
        subtitle = "Three moving circles merged with smooth-min into soft blobs.",
        focus = "Builds on sdCircle(...) and softFill(...) with a polynomial smin between fields so " +
            "metaball-style merging stays readable in generated AGSL.",
        snippet = """
            val m12 = sminPoly(d1, d2, 0.085f)
            val field = sminPoly(m12, d3, blendK)
            val blob = softFill(field, feather = 0.035f)
            mix(bg, shaded, blob)
        """.trimIndent()
    ),
    DemoInfo(
        id = DemoId.CrtTerminal,
        title = "CRT Terminal",
        subtitle = "Barrel warp, scanlines, edge RGB split, and subtle flicker.",
        focus = "Post-process stack on sampleUv(...): per-channel offsets from edge mask, " +
            "scanlines(fragCoord.y, ...), and vignette — a recognizable retro screen without leaving the stdlib sampling path.",
        snippet = """
            val warpedUv = float2(saturate(uv.x + delta.x), saturate(uv.y + delta.y))
            val base = sampleUv(warpedUv)
            val split = color(sampleUv(warpedUv - float2(px, 0f)).r, base.g, sampleUv(warpedUv + float2(px, 0f)).b, base.a)
            mix(base, split, edgeAmt * 0.88f) * scanMod * flicker * vignette
        """.trimIndent()
    )
)

fun demoInfo(id: DemoId): DemoInfo = DemoCatalog.first { it.id == id }

fun recommendedFollowUps(
    id: DemoId,
    excludeIds: Set<DemoId> = emptySet(),
    limit: Int = 3
): List<DemoFollowUp> {
    val current = demoInfo(id)

    return DemoCatalog
        .asSequence()
        .filter { candidate -> candidate.id != id && candidate.id !in excludeIds }
        .map { candidate ->
            buildDemoFollowUp(current = current, candidate = candidate)
        }
        .filter { (_, score) -> score > 0 }
        .sortedWith(
            compareByDescending<Pair<DemoFollowUp, Int>> { it.second }
                .thenBy { (followUp, _) -> DemoCatalog.indexOf(followUp.demo) }
        )
        .take(limit)
        .map { (followUp, _) -> followUp }
        .toList()
}

private fun buildDemoFollowUp(
    current: DemoInfo,
    candidate: DemoInfo
): Pair<DemoFollowUp, Int> {
    val sharedTags = current.focusTags.intersect(candidate.focusTags.toSet()).sorted()
    val sameSection = current.section == candidate.section
    val sameLayer = current.layer == candidate.layer
    val sameMotion = current.isAnimated == candidate.isAnimated
    val currentPath = current.pathSignal
    val candidatePath = candidate.pathSignal
    val candidateIsCurated = candidatePath.kind == DemoPathKind.StartHere || candidatePath.kind == DemoPathKind.Canonical
    val currentIsCurated = currentPath.kind == DemoPathKind.StartHere || currentPath.kind == DemoPathKind.Canonical

    var score = 0
    if (sameSection) score += 6
    if (sharedTags.isNotEmpty()) score += 4 + sharedTags.size
    if (sameLayer) score += 2
    if (sameMotion) score += 1
    if (!currentIsCurated && candidateIsCurated) score += 5
    if (currentPath.kind == DemoPathKind.Exploratory && candidatePath.kind == DemoPathKind.StartHere) score += 3
    if (currentIsCurated && candidatePath.kind == DemoPathKind.Exploratory) score += 4
    if (currentPath.family != null && currentPath.family == candidatePath.family) score += 4

    val (label, description) = when {
        !currentIsCurated && candidatePath.kind == DemoPathKind.StartHere -> {
            "BACK TO START HERE" to
                "Jump back to a recommended first-stop demo so the broader helper surface maps onto the curated mental model."
        }

        !currentIsCurated && candidatePath.kind == DemoPathKind.Canonical -> {
            "BACK TO CANONICAL" to
                "Use this curated demo to reconnect the current effect with the recommended helper family before returning to richer variants."
        }

        currentIsCurated && candidatePath.kind == DemoPathKind.Exploratory -> {
            "RICHER VARIANT" to
                "See how the same mental model expands into a more stylized or helper-heavier demo once the canonical path is clear."
        }

        currentPath.family != null && currentPath.family == candidatePath.family -> {
            "SAME PATH FAMILY" to
                "Stay inside the same curated family and compare another demo that reinforces the same teaching surface."
        }

        sameSection && !sameLayer -> {
            "SAME TOPIC, OTHER LAYER" to
                "Compare the same ${current.section.title.lowercase()} ideas through ${candidate.layer.label.lowercase()} authoring."
        }

        sameSection && sharedTags.isNotEmpty() -> {
            "NEXT IN ${candidate.section.title.uppercase()}" to
                "Stay in ${candidate.section.title.lowercase()} and follow the shared ${sharedTags.first()} thread into a nearby demo."
        }

        sharedTags.isNotEmpty() -> {
            "SHARES ${sharedTags.first().uppercase()}" to
                "Jump sideways into another demo that reuses the same ${sharedTags.first()} idea in a different context."
        }

        sameLayer -> {
            "MORE ${candidate.layer.label}" to
                "Keep reading the same authoring layer before switching mental models."
        }

        sameMotion -> {
            if (candidate.isAnimated) {
                "MORE ANIMATED FLOW" to
                    "Compare another time-driven demo with a similar live-preview rhythm."
            } else {
                "MORE STATIC INSPECTION" to
                    "Compare another still demo where the generated AGSL is easier to read without animation noise."
            }
        }

        else -> {
            "KEEP EXPLORING" to
                "Use this as a nearby reference point while you map the catalog."
        }
    }

    return DemoFollowUp(
        demo = candidate,
        label = label,
        description = description
    ) to score
}
