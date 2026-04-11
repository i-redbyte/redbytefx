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
    Duotone
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
        else -> null
    }

val DemoInfo.isCanonicalDemo: Boolean
    get() = canonicalFamily != null

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
        DemoId.Sweep -> DemoSection.Motion

        DemoId.Signal,
        DemoId.Film,
        DemoId.Warp,
        DemoId.Glitch,
        DemoId.Radar,
        DemoId.Circuit -> DemoSection.Procedural

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
        DemoId.Reveal -> DemoSection.Compositing
    }

val DemoInfo.layer: DemoLayer
    get() = when (id) {
        DemoId.Flip,
        DemoId.Mirror,
        DemoId.Rotate,
        DemoId.Scale,
        DemoId.Offset,
        DemoId.Wave,
        DemoId.Duotone -> DemoLayer.Core

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
        DemoId.Sigil -> DemoLayer.Stdlib
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
        DemoId.Sigil -> true

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
        id = DemoId.Frame,
        title = "Frame",
        subtitle = "Viewport frame masks and inner edge fades for UI shells.",
        focus = "Shows edgeDistance(...), edgeFade(...), and frameMask(...) working together with directionalSweep(...) to build animated panel borders and inner shell lighting.",
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
        subtitle = "Bracket-style corner accents for terminal and HUD panels.",
        focus = "Shows cornerMask(...) layered with directionalSweep(...) to build animated cyber corners without hand-written per-corner mask math in every shader.",
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
        subtitle = "Signal bars, lock bands, and scan warp from the stdlib layer.",
        focus = "Shows the next v0.2 recipe slice: reusable signal helpers that make tearing bands, rolling locks, and scan distortion readable without hiding the AGSL underneath.",
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

    var score = 0
    if (sameSection) score += 6
    if (sharedTags.isNotEmpty()) score += 4 + sharedTags.size
    if (sameLayer) score += 2
    if (sameMotion) score += 1

    val (label, description) = when {
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
