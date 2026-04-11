@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ru.redbyte.redbytefx.sample.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.sample.ui.theme.GridLine
import ru.redbyte.redbytefx.sample.ui.theme.MatrixGreen
import ru.redbyte.redbytefx.sample.ui.theme.NeonMint
import ru.redbyte.redbytefx.sample.ui.theme.SurfaceOne
import ru.redbyte.redbytefx.sample.ui.theme.SurfaceThree
import ru.redbyte.redbytefx.sample.ui.theme.SurfaceTwo
import ru.redbyte.redbytefx.sample.ui.theme.TerminalGlow
import ru.redbyte.redbytefx.sample.ui.theme.VoidBlack

data class CyberCodeAction(
    val label: String,
    val onClick: () -> Unit
)

val LocalCompactChrome = staticCompositionLocalOf { false }

@Composable
fun CyberBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cyber_backdrop")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cyber_backdrop_sweep"
    )
    val pulse = transition.animateFloat(
        initialValue = 0.74f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyber_backdrop_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val baseBrush = Brush.verticalGradient(
                    colors = listOf(
                        VoidBlack,
                        SurfaceOne,
                        VoidBlack
                    )
                )
                onDrawBehind {
                    drawRect(brush = baseBrush)

                    val sweepX = size.width * (0.18f + sweep.value * 0.64f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TerminalGlow.copy(alpha = 0.18f * pulse.value),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.1f),
                            radius = size.minDimension * 0.85f
                        ),
                        blendMode = BlendMode.Screen
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MatrixGreen.copy(alpha = 0.13f * pulse.value),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.84f, size.height * 0.24f),
                            radius = size.minDimension * 0.52f
                        ),
                        blendMode = BlendMode.Screen
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonMint.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            startX = sweepX - size.width * 0.12f,
                            endX = sweepX + size.width * 0.12f
                        ),
                        blendMode = BlendMode.Screen
                    )

                    val verticalStep = 72f
                    var x = -size.height * 0.2f + sweep.value * verticalStep * 2f
                    while (x < size.width + size.height * 0.2f) {
                        drawLine(
                            color = GridLine.copy(alpha = 0.08f),
                            start = Offset(x, 0f),
                            end = Offset(x - size.height * 0.18f, size.height),
                            strokeWidth = 1f
                        )
                        x += verticalStep
                    }

                    val horizontalStep = 28f
                    var y = (sweep.value * horizontalStep * 5f) % horizontalStep
                    while (y < size.height) {
                        drawLine(
                            color = GridLine.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += horizontalStep
                    }
                }
            }
    )
}

@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.24f),
                shape = shape
            )
            .drawWithCache {
                onDrawWithContent {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.18f),
                                Color.Transparent,
                                accent.copy(alpha = 0.08f)
                            )
                        ),
                        blendMode = BlendMode.Screen
                    )
                    drawContent()
                    drawRoundRect(
                        color = accent.copy(alpha = 0.14f),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun CyberBadge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    fill: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val compact = LocalCompactChrome.current
    val shape = RoundedCornerShape(if (compact) 12.dp else 14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.28f),
                shape = shape
            )
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 4.dp else 6.dp
            )
    ) {
        Text(
            text = text,
            style = if (compact) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

@Composable
fun CyberCodeBlock(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int,
    meta: String? = null,
    actions: List<CyberCodeAction> = emptyList()
) {
    val shape = RoundedCornerShape(18.dp)
    val compact = LocalCompactChrome.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceThree.copy(alpha = 0.56f),
                        SurfaceTwo.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MatrixGreen.copy(alpha = 0.18f),
                shape = shape
            )
            .padding(if (compact) 12.dp else 14.dp)
    ) {
        Column {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberBadge(
                    text = title.uppercase(),
                    accent = NeonMint,
                    fill = SurfaceOne.copy(alpha = 0.92f),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (meta != null) {
                    CyberBadge(
                        text = meta.uppercase(),
                        accent = MaterialTheme.colorScheme.secondary,
                        fill = SurfaceOne.copy(alpha = 0.9f),
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                actions.forEach { action ->
                    CyberBadge(
                        text = action.label,
                        modifier = Modifier.clickable(onClick = action.onClick),
                        accent = MaterialTheme.colorScheme.tertiary,
                        fill = SurfaceOne.copy(alpha = 0.94f),
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
