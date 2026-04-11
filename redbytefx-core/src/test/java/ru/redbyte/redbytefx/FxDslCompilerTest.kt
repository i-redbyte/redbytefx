package ru.redbyte.redbytefx

import org.junit.Assert.assertTrue
import org.junit.Test

class FxDslCompilerTest {

    @Test
    fun generatesNamedUniformsForCustomWaveEffect() {
        val effect = redbytefx {
            val amplitude = uniformFloat(0f, "wave_amplitude")
            val frequency = uniformFloat(0.08f, "wave_frequency")

            val waveOffset = float2(
                0f,
                sin(fragCoord.x * frequency) * amplitude
            )

            sample(fragCoord + waveOffset)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_wave_amplitude;"))
        assertTrue(source.contains("uniform float u_wave_frequency;"))
        assertTrue(source.contains("rb_sample"))
        assertTrue(source.contains("sin"))
    }

    @Test
    fun helperTransformsCompileIntoShaderSource() {
        val effect = redbytefx {
            val amount = uniformFloat(1f, "amount")
            sample(mirrorX(amount = amount, from = MirrorXFrom.Left))
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("mix"))
        assertTrue(source.contains("abs"))
    }

    @Test
    fun emitsLocalVariablesAndColorMath() {
        val effect = redbytefx {
            val amount = uniformFloat(0.5f, "amount")
            val base = let(sample(), "base")
            val mono = let(grayscale(base), "mono")
            val tone = let(withAlpha(color(1f, 0.5f, 0.2f), base.a), "tone")
            mix(base, mix(mono, tone, 0.75f), amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("half4 l_base = rb_sample(fragCoord);"))
        assertTrue(source.contains("half4 l_mono"))
        assertTrue(source.contains("half4 l_tone"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun emitsUserFunctionsAndVectorTypes() {
        val effect = redbytefx {
            val amount = uniformFloat(0.35f, "amount")
            val palette = fn(
                name = "palette_rgb",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = Float3Type
            ) { tone, warmth ->
                float3(
                    tone,
                    mix(tone, 1f - tone, warmth),
                    1f - tone
                )
            }

            val rgb = let(palette(amount, amount), "rgb")
            val rgba = let(float4(rgb, 1f), "rgba")
            color(rgba)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("float3 palette_rgb(float p0, float p1)"))
        assertTrue(source.contains("float3 l_rgb"))
        assertTrue(source.contains("float4 l_rgba"))
        assertTrue(source.contains("half4(l_rgba)"))
    }

    @Test
    fun emitsProceduralHelpersAndBoolLocals() {
        val effect = redbytefx {
            val density = uniformFloat(8f, "signal_density")
            val lineWidth = uniformFloat(0.08f, "signal_line_width")
            val pulseBand = fn(
                name = "pulse_band",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = FloatType
            ) { phase, threshold ->
                step(threshold, smoothstep(0.08f, 0.92f, fract(phase)))
            }

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val cell = let(fract(uv * density), "cell")
            val edgeX = let(min(cell.x, 1f - cell.x), "edge_x")
            val edgeY = let(min(cell.y, 1f - cell.y), "edge_y")
            val grid = let(
                max(
                    1f - smoothstep(0f, lineWidth, edgeX),
                    1f - smoothstep(0f, lineWidth, edgeY)
                ),
                "grid"
            )
            val scan = let(
                1f - smoothstep(0f, 3f, mod(fragCoord.y, 14f)),
                "scan"
            )
            val pulse = let(
                pulseBand(uv.y * density * 0.5f + grid * 0.35f, 0.55f),
                "pulse"
            )
            val hardMask = let(step(0.45f, scan * pulse), "hard_mask")
            val active = let((grid gt 0.05f) or (hardMask gt 0.5f), "active")
            val accent = let(color(float3(0.05f, 0.95f, 0.82f), base.a), "accent")

            ifElse(active, mix(base, accent, hardMask), base)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("float pulse_band(float p0, float p1)"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("mod"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("step"))
        assertTrue(source.contains("bool l_active"))
        assertTrue(source.contains("?"))
    }

    @Test
    fun emitsTimeUniformAndPowerMath() {
        val effect = redbytefx {
            val time = uniformTime(name = "pulse_time")
            val grid = uniformFloat(10f, "pulse_grid")
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val pixelUv = let(floor(uv * grid) / grid, "pixel_uv")
            val row = let(floor(uv.y * grid), "row")
            val wave = let(0.5f + 0.5f * sin(time + row * 0.7f), "wave")
            val glow = let(pow(wave, 3f), "glow")
            val active = let(ceil(smoothstep(0.82f, 0.98f, fract(uv.x * grid))), "active")

            mix(base, color(float3(glow, glow, glow), base.a), active)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_pulse_time;"))
        assertTrue(source.contains("floor"))
        assertTrue(source.contains("ceil"))
        assertTrue(source.contains("pow"))
    }

    @Test
    fun autoNamedUniformsUseDelegatedPropertyNames() {
        val effect = redbytefx {
            val amount by autoUniformFloat(0.5f)
            val time by autoUniformTime()
            val base = let(sample(), "base")
            val wave = let(0.5f + 0.5f * sin(time * 1.5f), "wave")

            mix(base, grayscale(base), amount * wave)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uniform float u_time;"))
    }

    @Test
    fun autoNamedUniformsNormalizeCamelCaseIntoReadableShaderNames() {
        val effect = redbytefx {
            val waveAmplitude by autoUniformFloat(0.5f)
            val edgeSoftness by autoUniformFloat(0.12f)
            mix(sample(), grayscale(sample()), waveAmplitude * edgeSoftness)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_wave_amplitude;"))
        assertTrue(source.contains("uniform float u_edge_softness;"))
    }

    @Test
    fun emitsAtanMathForPolarHelpers() {
        val effect = redbytefx {
            val center = uniformFloat2(0.5f, 0.5f, "center")
            val uv = let(fragCoord / resolution, "uv")
            val delta = let(uv - center, "delta")
            val angle = let(atan(delta.y, delta.x), "angle")

            color(angle, angle, angle)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float2 u_center;"))
        assertTrue(source.contains("atan"))
        assertTrue(source.contains("float l_angle"))
    }

    @Test
    fun emitsDotMathForVectorHelpers() {
        val effect = redbytefx {
            val direction = uniformFloat2(1f, 0f, "direction")
            val uv = let(fragCoord / resolution, "uv")
            val projection = let(dot(uv, direction), "projection")

            color(projection, projection, projection)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float2 u_direction;"))
        assertTrue(source.contains("dot"))
        assertTrue(source.contains("float l_projection"))
    }

    @Test
    fun functionAndLocalNamesNormalizeCamelCaseAndSuffixCollisions() {
        val effect = redbytefx {
            val pulseBand = fn(
                name = "PulseBand",
                arg1 = FloatType,
                returns = FloatType
            ) { phase ->
                sin(phase)
            }

            val firstOffset = let(float2(0.1f, 0.2f), "waveOffset")
            val secondOffset = let(float2(0.3f, 0.4f), "waveOffset")
            val drive = let(pulseBand(firstOffset.x + secondOffset.y), "signalMask")

            color(drive, drive, drive)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("float pulse_band(float p0)"))
        assertTrue(source.contains("float2 l_wave_offset = float2(0.1, 0.2);"))
        assertTrue(source.contains("float2 l_wave_offset_1 = float2(0.3, 0.4);"))
        assertTrue(source.contains("float l_signal_mask = pulse_band(((l_wave_offset).x + (l_wave_offset_1).y));"))
    }

    @Test
    fun userFunctionNamesAvoidAgslBuiltInsBySuffixing() {
        val effect = redbytefx {
            val sinHelper = fn(
                name = "sin",
                arg1 = FloatType,
                returns = FloatType
            ) { phase ->
                phase * 0.5f
            }
            val mixHelper = fn(
                name = "mix",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = FloatType
            ) { left, right ->
                (left + right) * 0.5f
            }

            val firstValue = let(sinHelper(1f), "firstValue")
            val mixedValue = let(mixHelper(firstValue, 0.25f), "mixedValue")
            color(mixedValue, mixedValue, mixedValue)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("float sin_1(float p0)"))
        assertTrue(source.contains("float mix_1(float p0, float p1)"))
        assertTrue(source.contains("float l_first_value = sin_1(1.0);"))
        assertTrue(source.contains("float l_mixed_value = mix_1(l_first_value, 0.25);"))
    }

    @Test
    fun intLiteralReceiversCompileIntoCanonicalCoreMath() {
        val effect = redbytefx {
            val amount by autoUniformFloat(0.35f)
            val time by autoUniformTime()
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val wave = let(1 - amount + 2 * sin(time * 3f), "wave")
            val scaledUv = let(2 * uv, "scaled_uv")
            val accent = let(2 * color(float3(0.08f, 0.95f, 1f), base.a), "accent")

            mix(base, accent, saturate(wave * scaledUv.x * 0.2f))
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("float l_wave = ((1.0 - u_amount) + (2.0 * sin((u_time * 3.0))));"))
        assertTrue(source.contains("float2 l_scaled_uv = (2.0 * l_uv);"))
        assertTrue(source.contains("half4 l_accent"))
        assertTrue(source.contains("2.0"))
        assertTrue(source.contains("(l_base).a"))
    }
}
