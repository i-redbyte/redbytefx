package ru.redbyte.redbytefx.sample.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.DemoInfo
import ru.redbyte.redbytefx.sample.model.isAnimated
import ru.redbyte.redbytefx.sample.model.layer
import ru.redbyte.redbytefx.sample.model.section

val LocalDemoInfo = staticCompositionLocalOf<DemoInfo?> { null }
val LocalDemoNavigation = staticCompositionLocalOf<DemoNavigation?> { null }

data class DemoNavigation(
    val previous: DemoInfo?,
    val next: DemoInfo?,
    val onOpen: (DemoId) -> Unit
)

@Composable
fun DemoLayout(
    generatedAgsl: String? = null,
    preview: @Composable () -> Unit,
    controls: @Composable () -> Unit
) {
    val demo = LocalDemoInfo.current
    val navigation = LocalDemoNavigation.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (demo != null) {
            DemoInfoCard(
                demo = demo,
                generatedAgsl = generatedAgsl
            )
        }

        CyberPanel(
            accent = MaterialTheme.colorScheme.secondary,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
        ) {
            preview()
        }

        CyberPanel(
            accent = MaterialTheme.colorScheme.tertiary,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberBadge(
                        text = "LIVE CONTROLS",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                    CyberBadge(
                        text = "RUNTIME BINDINGS",
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
                controls()
            }
        }

        if (navigation != null && (navigation.previous != null || navigation.next != null)) {
            DemoNavigationStrip(navigation = navigation)
        }
    }
}

@Composable
private fun DemoInfoCard(
    demo: DemoInfo,
    generatedAgsl: String?
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.primary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = demo.title.uppercase(),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = "sample://${demo.id.name.lowercase()}",
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = demo.section.title.uppercase(),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = demo.layer.label,
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = if (demo.isAnimated) "ANIMATED" else "STATIC",
                accent = if (demo.isAnimated) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
        Text(
            text = demo.subtitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = demo.focus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        ExpandableCodeBlock(
            title = "DSL snippet",
            text = demo.snippet,
            collapsedLines = 10,
            stateKey = "${demo.id.name}-dsl",
            modifier = Modifier.padding(top = 14.dp)
        )
        if (generatedAgsl != null) {
            ExpandableCodeBlock(
                title = "Generated AGSL",
                text = generatedAgsl,
                collapsedLines = 18,
                stateKey = "${demo.id.name}-agsl",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ExpandableCodeBlock(
    title: String,
    text: String,
    collapsedLines: Int,
    stateKey: String,
    modifier: Modifier = Modifier
) {
    val totalLines = previewLineCount(text)
    val canExpand = totalLines > collapsedLines
    var expanded by rememberSaveable(stateKey) { mutableStateOf(false) }

    SelectionContainer(modifier = modifier) {
        CyberCodeBlock(
            title = title,
            text = if (expanded || !canExpand) {
                text
            } else {
                previewShaderSource(text, collapsedLines)
            },
            maxLines = if (expanded || !canExpand) Int.MAX_VALUE else collapsedLines,
            meta = "$totalLines lines",
            actionLabel = if (canExpand) {
                if (expanded) "COLLAPSE" else "EXPAND"
            } else {
                null
            },
            onAction = if (canExpand) {
                { expanded = !expanded }
            } else {
                null
            }
        )
    }
}

internal fun previewShaderSource(
    source: String,
    maxLines: Int = 18
): String {
    val lines = source.lineSequence().toList()
    if (lines.size <= maxLines) return source
    return buildString {
        append(lines.take(maxLines).joinToString(separator = "\n"))
        append("\n...")
    }
}

private fun previewLineCount(source: String): Int = source.lineSequence().count()

@Composable
private fun DemoNavigationStrip(navigation: DemoNavigation) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NavigationCard(
                label = "PREV",
                demo = navigation.previous,
                modifier = Modifier.weight(1f),
                onOpen = navigation.onOpen
            )
            NavigationCard(
                label = "NEXT",
                demo = navigation.next,
                modifier = Modifier.weight(1f),
                onOpen = navigation.onOpen
            )
        }
    }
}

@Composable
private fun NavigationCard(
    label: String,
    demo: DemoInfo?,
    modifier: Modifier = Modifier,
    onOpen: (DemoId) -> Unit
) {
    if (demo == null) {
        Spacer(modifier = modifier)
        return
    }

    CyberPanel(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onOpen(demo.id) },
        accent = if (label == "NEXT") {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = label,
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = demo.layer.label,
                accent = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = demo.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = demo.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String = { it.toInt().toString() },
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = title.uppercase(),
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = formatValue(value),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun RadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DemoPreviewStage(
    modifier: Modifier = Modifier,
    label: String = "RedByteFX"
) {
    val shape = MaterialTheme.shapes.large
    val sweepTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    val lineTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val transition = rememberInfiniteTransition(label = "preview_stage")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_stage_sweep"
    )
    val pulse = transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "preview_stage_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp)
            .then(modifier)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                shape = shape
            )
            .drawWithCache {
                onDrawWithContent {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                sweepTint,
                                Color.Transparent
                            ),
                            startX = size.width * (sweep.value - 0.18f),
                            endX = size.width * (sweep.value + 0.18f)
                        ),
                        blendMode = BlendMode.Screen
                    )
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = lineTint,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += 16f
                    }
                    drawContent()
                }
            }
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(94.dp)
                .graphicsLayer {
                    rotationZ = 18f
                    translationX = -10f + 12f * sweep.value
                    translationY = 8f * pulse.value
                    alpha = 0.22f + 0.08f * pulse.value
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 8.dp)
                .size(width = 62.dp, height = 150.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(126.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberBadge(
                    text = "SHADER PREVIEW",
                    accent = MaterialTheme.colorScheme.secondary
                )
                CyberBadge(
                    text = "LIVE UNIFORMS",
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "kotlin.dsl // agsl // compose.runtime",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberBadge(
                    text = "TYPE-SAFE",
                    accent = MaterialTheme.colorScheme.primary
                )
                CyberBadge(
                    text = "HOT SIGNAL",
                    accent = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
