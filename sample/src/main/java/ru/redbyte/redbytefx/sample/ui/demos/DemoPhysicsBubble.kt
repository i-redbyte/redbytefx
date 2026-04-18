// The original shader is taken from https://gist.github.com/Mikkareem/1a80129f67bbca3e1a2837e29116801f?permalink_comment_id=6079666#file-02_physicsbubblescreen-kt
package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.FxController
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.valueNoise

private object BubbleConfig {
    const val BOTTOM_ORB_RATIO = 0.92f
    const val TOP_ORB_RATIO = 0.28f
    val MAX_ORB_RADIUS = 250.dp
    val MIN_ORB_RADIUS = 85.dp
    const val TEXT_Y_BOTTOM_RATIO = 0.48f
    const val TEXT_Y_TOP_RATIO = 0.42f
    const val SNAP_UNLOCK_THRESHOLD = 10f
    const val DRAG_OVERSHOOT_RATIO = 0.05f
    const val DEFORMATION_FACTOR = 0.015f
    const val DEFORMATION_CLAMP = 0.6f
    const val VELOCITY_SMOOTHING = 0.15f
    const val THEME_REVEAL_DURATION = 1100
    const val POP_DURATION = 150
    const val POP_DELAY = 2000L
    const val TEXT_ANIM_DURATION = 700
}

private object BubbleColors {
    val LIGHT_CENTER = Color(0xFFFFFFFF)
    val LIGHT_MID1 = Color(0xFFFBF8F6)
    val LIGHT_MID2 = Color(0xFFF5EFEE)
    val LIGHT_EDGE = Color(0xFFEEEAE8)

    val DARK_CENTER = Color(0xFF2A2D34)
    val DARK_MID = Color(0xFF16171B)
    val DARK_EDGE = Color(0xFF0A0B0D)

    val LIGHT_MAIN_TEXT = Color(0xFF4A403A)
    val DARK_MAIN_TEXT = Color(0xFFE5E5EA)
    val LIGHT_TITLE = Color(0xFF1F1A17)
    val DARK_TITLE = Color(0xFFF5F5F7)
    val LIGHT_SUBTITLE = Color(0xFF8A807A)
    val DARK_SUBTITLE = Color(0xFFA1A1A6)
}

private val SnapBackSpring = spring<Offset>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessLow
)

private val UnlockedSnapSpring = spring<Offset>(
    dampingRatio = 0.45f,
    stiffness = Spring.StiffnessLow
)

@Stable
class PhysicsBubbleState(
    private val screenHeightPx: Float,
    orbRadiusMaxPx: Float,
    orbRadiusMinPx: Float,
    val centerX: Float,
) {
    val bottomOrbCenterY = screenHeightPx * BubbleConfig.BOTTOM_ORB_RATIO
    val topOrbCenterY = screenHeightPx * BubbleConfig.TOP_ORB_RATIO
    val midPoint = (bottomOrbCenterY + topOrbCenterY) / 2f
    val maxDragY = bottomOrbCenterY + (screenHeightPx * BubbleConfig.DRAG_OVERSHOOT_RATIO)

    private val orbRadiusMax = orbRadiusMaxPx
    private val orbRadiusMin = orbRadiusMinPx
    private val orbRange = bottomOrbCenterY - topOrbCenterY
    private val textYBottom = screenHeightPx * BubbleConfig.TEXT_Y_BOTTOM_RATIO
    private val textYTop = screenHeightPx * BubbleConfig.TEXT_Y_TOP_RATIO

    val bubblePos = Animatable(Offset(centerX, bottomOrbCenterY), Offset.VectorConverter)
    val deformationAnim = Animatable(Offset.Zero, Offset.VectorConverter)
    val popAnim = Animatable(0f)
    val themeRevealProgress = Animatable(1f)

    val progress: Float
        get() = ((bottomOrbCenterY - bubblePos.value.y) / orbRange).coerceIn(0f, 1f)

    val currentOrbRadius: Float
        get() = androidx.compose.ui.util.lerp(orbRadiusMax, orbRadiusMin, progress)

    val textYOffsetPx: Float
        get() = androidx.compose.ui.util.lerp(textYBottom, textYTop, progress)

    fun isAtTop(): Boolean = bubblePos.value.y <= topOrbCenterY + BubbleConfig.SNAP_UNLOCK_THRESHOLD
}

private data class PhysicsBubbleFxSetup(
    val effect: FxEffect,
    val center: FxParam.Float2,
    val radius: FxParam.Float,
    val deformation: FxParam.Float2,
    val popProgress: FxParam.Float,
    val time: FxParam.Float
)

private fun rgb(color: ColorExpr): Float3Expr = float3(color.r, color.g, color.b)

private fun normalize2(vector: Float2Expr): Float2Expr =
    vector / max(length(vector), 0.0001f)

private fun normalize3(vector: Float3Expr): Float3Expr =
    vector / max(length(vector), 0.0001f)

private fun safeSqrt(value: FloatExpr): FloatExpr =
    pow(max(value, 0f), 0.5f)

private fun reflect3(
    incident: Float3Expr,
    normal: Float3Expr
): Float3Expr = incident - 2f * dot(normal, incident) * normal

@Composable
private fun rememberBubbleState(
    screenWidthPx: Float,
    screenHeightPx: Float
): PhysicsBubbleState {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { BubbleConfig.MAX_ORB_RADIUS.toPx() }
    val minRadiusPx = with(density) { BubbleConfig.MIN_ORB_RADIUS.toPx() }

    return remember(screenWidthPx, screenHeightPx) {
        PhysicsBubbleState(
            screenHeightPx = screenHeightPx,
            orbRadiusMaxPx = maxRadiusPx,
            orbRadiusMinPx = minRadiusPx,
            centerX = screenWidthPx / 2f
        )
    }
}

@Composable
private fun rememberPhysicsBubbleFx(): PhysicsBubbleFxSetup =
    remember {
        var centerParam: FxParam.Float2? = null
        var radiusParam: FxParam.Float? = null
        var deformationParam: FxParam.Float2? = null
        var popParam: FxParam.Float? = null
        var timeParam: FxParam.Float? = null

        val effect = redbytefx {
            val bubbleCenter = uniformFloat2(0f, 0f, "bubble_center")
            val bubbleRadius = uniformFloat(1f, "bubble_radius")
            val bubbleDeformation = uniformFloat2(0f, 0f, "bubble_deformation")
            val bubblePop = uniformFloat(0f, "bubble_pop")
            val bubbleTime = uniformTime(name = "bubble_time")
            centerParam = bubbleCenter
            radiusParam = bubbleRadius
            deformationParam = bubbleDeformation
            popParam = bubblePop
            timeParam = bubbleTime

            val rawBackground = let(sampleUnclamped(), "raw_background")
            val rawUv = let(fragCoord - bubbleCenter, "raw_uv")
            val speed = let(length(bubbleDeformation), "speed")
            val moveDir = let(
                ifElse(
                    speed gt 0.001f,
                    normalize2(bubbleDeformation),
                    float2(0f, 1f)
                ),
                "move_dir"
            )
            val parallelDist = let(dot(rawUv, moveDir), "parallel_dist")
            val perpVector = let(rawUv - moveDir * parallelDist, "perp_vector")
            val stretch = let(1f + speed, "stretch")
            val squash = let(1f / safeSqrt(stretch), "squash")
            val uv = let(
                moveDir * (parallelDist / stretch) + perpVector / squash,
                "uv"
            )
            val dist = let(length(uv), "dist")
            val activeRadius = let(bubbleRadius * (1f + bubblePop * 1.5f), "active_radius")
            val nUv = let(uv / max(activeRadius, 0.0001f), "n_uv")
            val distSq = let(dot(nUv, nUv), "dist_sq")
            val z = let(safeSqrt(1f - distSq), "z")
            val normal = let(normalize3(float3(nUv, z)), "normal")
            val viewDir = let(float3(0f, 0f, 1f), "view_dir")
            val nDotV = let(max(dot(normal, viewDir), 0f), "n_dot_v")

            val magnification = 0.45f
            val lensDeform = let((1f - z) * magnification * (1f - bubblePop), "lens_deform")
            val refractScale = let(nUv * activeRadius * lensDeform, "refract_scale")
            val refUvR = let(fragCoord - refractScale * 0.88f, "ref_uv_r")
            val refUvG = let(fragCoord - refractScale, "ref_uv_g")
            val refUvB = let(fragCoord - refractScale * 1.12f, "ref_uv_b")
            val bgColor = let(
                float3(
                    sampleUnclamped(refUvR).r,
                    sampleUnclamped(refUvG).g,
                    sampleUnclamped(refUvB).b
                ),
                "bg_color"
            )

            val reflectionDir = let(reflect3(-viewDir, normal), "reflection_dir")
            val lightDir1 = let(normalize3(float3(0.6f, 0.7f, 0.8f)), "light_dir_1")
            val lightDir2 = let(normalize3(float3(-0.5f, -0.4f, 0.6f)), "light_dir_2")
            val lightAlign1 = let(max(dot(reflectionDir, lightDir1), 0f), "light_align_1")
            val lightAlign2 = let(max(dot(reflectionDir, lightDir2), 0f), "light_align_2")

            val nFilm = 1.33f
            val r0 = let(float(0.02005931f), "r0")
            val fresnel = let(
                r0 + (1f - r0) * pow(1f - nDotV, 5f),
                "fresnel"
            )
            val sinThetaI = let(safeSqrt(1f - nDotV * nDotV), "sin_theta_i")
            val sinThetaT = let(sinThetaI / nFilm, "sin_theta_t")
            val cosThetaT = let(safeSqrt(1f - sinThetaT * sinThetaT), "cos_theta_t")

            val swirl = let(
                valueNoise(nUv * 3f + float2(bubbleTime * 0.12f, bubbleTime * 0.12f)),
                "swirl"
            )
            val thicknessNoise = let(
                valueNoise(nUv * 5f - float2(bubbleTime * 0.08f, bubbleTime * 0.08f)),
                "thickness_noise"
            )
            val thickness = let(
                clamp(
                    300f + nUv.y * 120f + swirl * 100f + thicknessNoise * 40f,
                    80f,
                    900f
                ),
                "thickness"
            )
            val opd = let(2f * nFilm * thickness * cosThetaT, "opd")
            val phase = let(6.2831855f * opd, "phase")
            val oscR = let(0.5f + 0.5f * cos(phase / 650f), "osc_r")
            val oscG = let(0.5f + 0.5f * cos(phase / 532f), "osc_g")
            val oscB = let(0.5f + 0.5f * cos(phase / 450f), "osc_b")
            val interferenceColor = let(float3(oscR, oscG, oscB), "interference_color")
            val interferenceStrength = let(
                smoothstep(0f, 0.20f, nDotV),
                "interference_strength"
            )
            val filmReflection = let(interferenceColor * fresnel * 2f, "film_reflection")
            val whiteReflection = let(float3(fresnel, fresnel, fresnel), "white_reflection")
            val thinFilmColor = let(
                mix(whiteReflection, filmReflection, interferenceStrength),
                "thin_film_color"
            )

            val spec1 = let(pow(lightAlign1, 250f) * 2.5f, "spec_1")
            val spec2 = let(pow(lightAlign2, 60f) * 0.5f, "spec_2")
            val highlights = let(
                float3(spec1 + spec2, spec1 + spec2, spec1 + spec2),
                "highlights"
            )

            val reflectOffset = let(float2(normal.x, normal.y) * 50f, "reflect_offset")
            val envCenter = let(fragCoord + reflectOffset, "env_center")
            val blurStep = 20f
            val envSample = let(
                rgb(sampleUnclamped(envCenter)) * 0.4f +
                    rgb(sampleUnclamped(envCenter + float2(blurStep, 0f))) * 0.15f +
                    rgb(sampleUnclamped(envCenter - float2(blurStep, 0f))) * 0.15f +
                    rgb(sampleUnclamped(envCenter + float2(0f, blurStep))) * 0.15f +
                    rgb(sampleUnclamped(envCenter - float2(0f, blurStep))) * 0.15f,
                "env_sample"
            )
            val envReflection = let(envSample * fresnel * 0.4f, "env_reflection")
            val rimShadow = let(smoothstep(0.92f, 1f, safeSqrt(distSq)), "rim_shadow")
            val shadedBg = let(bgColor * (1f - rimShadow * 0.25f), "shaded_bg")
            val fresnelRgb = let(float3(fresnel, fresnel, fresnel), "fresnel_rgb")
            val finalColor = let(
                shadedBg * (float3(1f, 1f, 1f) - fresnelRgb) +
                    thinFilmColor +
                    envReflection +
                    highlights,
                "final_color"
            )
            val fadeOut = let(1f - safeSqrt(bubblePop), "fade_out")
            val mixedRgb = let(mix(rgb(rawBackground), finalColor, fadeOut), "mixed_rgb")
            val outsideBubble = let((bubblePop gte 1f) or (dist gte activeRadius), "outside_bubble")

            ifElse(
                outsideBubble,
                rawBackground,
                color(mixedRgb, rawBackground.a)
            )
        }

        PhysicsBubbleFxSetup(
            effect = effect,
            center = centerParam!!,
            radius = radiusParam!!,
            deformation = deformationParam!!,
            popProgress = popParam!!,
            time = timeParam!!
        )
    }

@Composable
private fun BubbleFxBindingLoop(
    fx: FxController,
    setup: PhysicsBubbleFxSetup,
    state: PhysicsBubbleState
) {
    LaunchedEffect(fx, setup, state) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                val elapsedSeconds = (frame - start) / 1_000_000_000f
                fx.runBatch {
                    fx.setFloat(setup.time, elapsedSeconds)
                    fx.setFloat2(setup.center, state.bubblePos.value.x, state.bubblePos.value.y)
                    fx.setFloat(setup.radius, state.currentOrbRadius)
                    fx.setFloat2(
                        setup.deformation,
                        state.deformationAnim.value.x,
                        state.deformationAnim.value.y
                    )
                    fx.setFloat(setup.popProgress, state.popAnim.value)
                }
            }
        }
    }
}

@Composable
fun DemoPhysicsBubble(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val state = rememberBubbleState(screenWidthPx, screenHeightPx)
        val setup = rememberPhysicsBubbleFx()
        val fx = rememberFxController(setup.effect)

        BubbleFxBindingLoop(fx, setup, state)
        DeformationFrameLoop(state)
        PhysicsBubbleContent(
            state = state,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            modifier = Modifier.redbyteFx(fx)
        )
    }
}

@Composable
private fun PhysicsBubbleContent(
    state: PhysicsBubbleState,
    screenWidthPx: Float,
    screenHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var isDarkTheme by remember { mutableStateOf(false) }
    var previousIsDark by remember { mutableStateOf(false) }

    val lightBrush = remember(screenWidthPx, screenHeightPx) {
        createRadialBrush(
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            center = BubbleColors.LIGHT_CENTER,
            mid1 = BubbleColors.LIGHT_MID1,
            mid2 = BubbleColors.LIGHT_MID2,
            edge = BubbleColors.LIGHT_EDGE
        )
    }
    val darkBrush = remember(screenWidthPx, screenHeightPx) {
        createRadialBrush(
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            center = BubbleColors.DARK_CENTER,
            mid1 = BubbleColors.DARK_MID,
            mid2 = BubbleColors.DARK_MID,
            edge = BubbleColors.DARK_EDGE
        )
    }

    val textTween = remember {
        tween<Color>(BubbleConfig.TEXT_ANIM_DURATION, easing = FastOutLinearInEasing)
    }
    val mainTextColor by animateColorAsState(
        targetValue = if (isDarkTheme) BubbleColors.DARK_MAIN_TEXT else BubbleColors.LIGHT_MAIN_TEXT,
        animationSpec = textTween,
        label = "bubble_main_text"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isDarkTheme) BubbleColors.DARK_TITLE else BubbleColors.LIGHT_TITLE,
        animationSpec = textTween,
        label = "bubble_title_text"
    )
    val subtitleColor by animateColorAsState(
        targetValue = if (isDarkTheme) BubbleColors.DARK_SUBTITLE else BubbleColors.LIGHT_SUBTITLE,
        animationSpec = textTween,
        label = "bubble_subtitle_text"
    )

    val revealClipPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .bubbleDragInput(state, scope)
            .bubbleTapInput(state, scope)
            .drawBehind {
                drawThemeBackground(
                    isDarkTheme = isDarkTheme,
                    previousIsDark = previousIsDark,
                    revealProgress = state.themeRevealProgress.value,
                    lightBrush = lightBrush,
                    darkBrush = darkBrush,
                    reusablePath = revealClipPath
                )
            }
    ) {
        ThemeToggleButton(
            isDarkTheme = isDarkTheme,
            onToggle = {
                if (state.themeRevealProgress.isRunning) return@ThemeToggleButton
                previousIsDark = isDarkTheme
                isDarkTheme = !isDarkTheme
                scope.launch {
                    state.themeRevealProgress.snapTo(0f)
                    state.themeRevealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BubbleConfig.THEME_REVEAL_DURATION,
                            easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1f)
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
        )

        Text(
            text = "Pixels are now\nphysical.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 40.sp,
            color = mainTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .graphicsLayer {
                    alpha = 1f - (state.progress * 4f).coerceIn(0f, 1f)
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, state.textYOffsetPx.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = (state.progress * 3f).coerceIn(0f, 1f)
                }
            ) {
                Text(
                    text = "RedByteFX Bubble",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = titleColor
                )
                Text(
                    text = "Thin-film interference\ndriven by RedByteFX.",
                    fontSize = 24.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    color = subtitleColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (isDarkTheme) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "bubble_theme_morph"
    )
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isDarkTheme) {
        scaleAnim.snapTo(0.85f)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
        )
    }

    val mainPath = remember { Path() }
    val cutoutPath = remember { Path() }
    val finalPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .padding(6.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f
        val currentColor = lerp(
            Color(0xFFFDB813),
            Color(0xFFE5E5EA),
            progress
        )

        rotate(degrees = progress * -90f, pivot = center) {
            val rayAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
            if (rayAlpha > 0f) {
                val rayLength = maxRadius * 0.25f
                val rayOffset = maxRadius * 0.6f
                for (index in 0 until 8) {
                    rotate(degrees = index * 45f, pivot = center) {
                        drawLine(
                            color = currentColor.copy(alpha = rayAlpha),
                            start = center.copy(y = center.y - rayOffset),
                            end = center.copy(y = center.y - rayOffset - rayLength),
                            strokeWidth = maxRadius * 0.15f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            val currentRadius = androidx.compose.ui.util.lerp(
                maxRadius * 0.45f,
                maxRadius * 0.85f,
                progress
            )
            mainPath.reset()
            mainPath.addOval(
                Rect(
                    left = center.x - currentRadius,
                    top = center.y - currentRadius,
                    right = center.x + currentRadius,
                    bottom = center.y + currentRadius
                )
            )

            val cutoutStartOffset = Offset(center.x + maxRadius * 2f, center.y - maxRadius * 2f)
            val cutoutEndOffset = Offset(
                x = center.x + currentRadius * 0.3f,
                y = center.y - currentRadius * 0.3f
            )
            val cutoutX = androidx.compose.ui.util.lerp(cutoutStartOffset.x, cutoutEndOffset.x, progress)
            val cutoutY = androidx.compose.ui.util.lerp(cutoutStartOffset.y, cutoutEndOffset.y, progress)
            val cutoutRadius = currentRadius * 0.95f

            cutoutPath.reset()
            cutoutPath.addOval(
                Rect(
                    left = cutoutX - cutoutRadius,
                    top = cutoutY - cutoutRadius,
                    right = cutoutX + cutoutRadius,
                    bottom = cutoutY + cutoutRadius
                )
            )

            finalPath.reset()
            finalPath.op(mainPath, cutoutPath, PathOperation.Difference)
            drawPath(path = finalPath, color = currentColor)
        }
    }
}

@Composable
private fun DeformationFrameLoop(state: PhysicsBubbleState) {
    LaunchedEffect(state) {
        var previousActualPos = state.bubblePos.value
        var smoothedVelocity = Offset.Zero
        var defVelocity = Offset.Zero
        val stiffness = 1500f
        val damping = 34.8f
        var lastFrameTime = withFrameNanos { it }

        while (true) {
            val frameTime = withFrameNanos { it }
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.032f)
            lastFrameTime = frameTime

            val currentActualPos = state.bubblePos.value
            val rawVelocity = currentActualPos - previousActualPos

            smoothedVelocity = Offset(
                x = smoothedVelocity.x + (rawVelocity.x - smoothedVelocity.x) * BubbleConfig.VELOCITY_SMOOTHING,
                y = smoothedVelocity.y + (rawVelocity.y - smoothedVelocity.y) * BubbleConfig.VELOCITY_SMOOTHING
            )

            if (state.popAnim.value == 0f) {
                val targetDeformation = Offset(
                    x = (smoothedVelocity.x * BubbleConfig.DEFORMATION_FACTOR).coerceIn(
                        -BubbleConfig.DEFORMATION_CLAMP,
                        BubbleConfig.DEFORMATION_CLAMP
                    ),
                    y = (smoothedVelocity.y * BubbleConfig.DEFORMATION_FACTOR).coerceIn(
                        -BubbleConfig.DEFORMATION_CLAMP,
                        BubbleConfig.DEFORMATION_CLAMP
                    )
                )

                val currentDef = state.deformationAnim.value
                val forceX = (targetDeformation.x - currentDef.x) * stiffness - defVelocity.x * damping
                val forceY = (targetDeformation.y - currentDef.y) * stiffness - defVelocity.y * damping

                defVelocity = Offset(
                    x = defVelocity.x + forceX * dt,
                    y = defVelocity.y + forceY * dt
                )
                state.deformationAnim.snapTo(
                    Offset(
                        x = currentDef.x + defVelocity.x * dt,
                        y = currentDef.y + defVelocity.y * dt
                    )
                )
            } else {
                state.deformationAnim.snapTo(Offset.Zero)
                defVelocity = Offset.Zero
            }

            previousActualPos = currentActualPos
        }
    }
}

private fun Modifier.bubbleDragInput(
    state: PhysicsBubbleState,
    scope: CoroutineScope
): Modifier = pointerInput(Unit) {
    var isUnlocked = false
    detectDragGestures(
        onDragStart = { isUnlocked = state.isAtTop() },
        onDragEnd = {
            scope.launch {
                if (isUnlocked) {
                    val target = if (state.bubblePos.value.y < state.midPoint) {
                        Offset(state.centerX, state.topOrbCenterY)
                    } else {
                        Offset(state.centerX, state.bottomOrbCenterY)
                    }
                    state.bubblePos.animateTo(target, if (target.y == state.topOrbCenterY) UnlockedSnapSpring else SnapBackSpring)
                } else {
                    val targetY = if (state.bubblePos.value.y < state.midPoint) {
                        state.topOrbCenterY
                    } else {
                        state.bottomOrbCenterY
                    }
                    state.bubblePos.animateTo(Offset(state.centerX, targetY), SnapBackSpring)
                }
            }
        }
    ) { change, dragAmount ->
        if (state.popAnim.value > 0f) return@detectDragGestures
        change.consume()
        val proposedY = state.bubblePos.value.y + dragAmount.y
        if (!isUnlocked && proposedY <= state.topOrbCenterY) {
            isUnlocked = true
        }

        scope.launch {
            if (isUnlocked) {
                state.bubblePos.snapTo(
                    Offset(
                        x = state.bubblePos.value.x + dragAmount.x,
                        y = proposedY
                    )
                )
            } else {
                state.bubblePos.snapTo(
                    Offset(
                        x = state.centerX,
                        y = proposedY.coerceAtMost(state.maxDragY)
                    )
                )
            }
        }
    }
}

private fun Modifier.bubbleTapInput(
    state: PhysicsBubbleState,
    scope: CoroutineScope
): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onTap = {
            if (state.popAnim.value == 0f) {
                scope.launch {
                    state.popAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BubbleConfig.POP_DURATION,
                            easing = FastOutLinearInEasing
                        )
                    )
                    delay(BubbleConfig.POP_DELAY)
                    state.popAnim.snapTo(0f)
                    state.bubblePos.snapTo(Offset(state.centerX, state.bottomOrbCenterY))
                }
            }
        }
    )
}

private fun createRadialBrush(
    screenWidthPx: Float,
    screenHeightPx: Float,
    center: Color,
    mid1: Color,
    mid2: Color,
    edge: Color
): Brush = Brush.radialGradient(
    colorStops = arrayOf(
        0f to center,
        0.3f to mid1,
        0.7f to mid2,
        1f to edge
    ),
    center = Offset(screenWidthPx / 2f, screenHeightPx * 0.4f)
)

private fun DrawScope.drawThemeBackground(
    isDarkTheme: Boolean,
    previousIsDark: Boolean,
    revealProgress: Float,
    lightBrush: Brush,
    darkBrush: Brush,
    reusablePath: Path
) {
    val currentBrush = if (isDarkTheme) darkBrush else lightBrush
    val previousBrush = if (previousIsDark) darkBrush else lightBrush

    drawRect(brush = previousBrush)
    if (revealProgress < 1f) {
        val maxRadius = hypot(size.width, size.height)
        val currentRevealRadius = revealProgress * maxRadius
        val epicenter = Offset(size.width - 100f, 150f)

        reusablePath.reset()
        reusablePath.addOval(
            Rect(
                left = epicenter.x - currentRevealRadius,
                top = epicenter.y - currentRevealRadius,
                right = epicenter.x + currentRevealRadius,
                bottom = epicenter.y + currentRevealRadius
            )
        )

        clipPath(reusablePath) {
            drawRect(brush = currentBrush)
        }
    } else {
        drawRect(brush = currentBrush)
    }
}
