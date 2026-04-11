@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ru.redbyte.redbytefx.sample.ui

import androidx.compose.animation.animateContentSize
import android.content.ClipData
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.redbyte.redbytefx.sample.model.DemoFollowUp
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.DemoPathKind
import ru.redbyte.redbytefx.sample.model.DemoPathSignal
import ru.redbyte.redbytefx.sample.model.DemoLayer
import ru.redbyte.redbytefx.sample.model.DemoInfo
import ru.redbyte.redbytefx.sample.model.canonicalFamily
import ru.redbyte.redbytefx.sample.model.focusTags
import ru.redbyte.redbytefx.sample.model.isAnimated
import ru.redbyte.redbytefx.sample.model.isStartHere
import ru.redbyte.redbytefx.sample.model.layer
import ru.redbyte.redbytefx.sample.model.pathSignal
import ru.redbyte.redbytefx.sample.model.section
import ru.redbyte.redbytefx.compose.FxController
import ru.redbyte.redbytefx.compose.redbyteFx

val LocalDemoInfo = staticCompositionLocalOf<DemoInfo?> { null }
val LocalDemoNavigation = staticCompositionLocalOf<DemoNavigation?> { null }

data class DemoNavigation(
    val previous: DemoInfo?,
    val next: DemoInfo?,
    val related: List<DemoFollowUp> = emptyList(),
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
    BoxWithConstraints {
        val isCompactPhone = maxWidth < 420.dp
        val usesWideInspectionLayout = maxWidth >= 980.dp
        val outerPadding = if (isCompactPhone) 8.dp else 16.dp
        val blockSpacing = if (isCompactPhone) 8.dp else 16.dp
        var showInspectionDialog by rememberSaveable(demo?.id?.name) { mutableStateOf(false) }

        if (isCompactPhone && showInspectionDialog && demo != null) {
            DemoInspectionDialog(
                demo = demo,
                generatedAgsl = generatedAgsl,
                onDismiss = { showInspectionDialog = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(outerPadding),
            verticalArrangement = Arrangement.spacedBy(blockSpacing)
        ) {
            if (demo != null && !isCompactPhone) {
                DemoInfoCard(
                    demo = demo,
                    generatedAgsl = generatedAgsl,
                    compact = isCompactPhone
                )
            }

            if (usesWideInspectionLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(blockSpacing)
                    ) {
                        DemoPreviewPanel(
                            preview = preview,
                            compact = isCompactPhone
                        )

                        if (navigation != null && (navigation.previous != null || navigation.next != null)) {
                            DemoNavigationStrip(
                                navigation = navigation,
                                compact = isCompactPhone
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(blockSpacing)
                    ) {
                        DemoControlsPanel(
                            controls = controls,
                            compact = isCompactPhone,
                            onOpenInspectionDialog = if (isCompactPhone && demo != null) {
                                { showInspectionDialog = true }
                            } else {
                                null
                            }
                        )
                    }
                }
            } else {
                DemoPreviewPanel(
                    preview = preview,
                    compact = isCompactPhone
                )
                DemoControlsPanel(
                    controls = controls,
                    compact = isCompactPhone,
                    onOpenInspectionDialog = if (isCompactPhone && demo != null) {
                        { showInspectionDialog = true }
                    } else {
                        null
                    }
                )

                if (demo != null && isCompactPhone) {
                    DemoInfoCard(
                        demo = demo,
                        generatedAgsl = generatedAgsl,
                        compact = isCompactPhone,
                        onOpenInspectionDialog = { showInspectionDialog = true }
                    )
                }

                if (navigation != null && (navigation.previous != null || navigation.next != null)) {
                    DemoNavigationStrip(
                        navigation = navigation,
                        compact = isCompactPhone
                    )
                }
            }

            if (navigation != null && navigation.related.isNotEmpty()) {
                DemoNextStepsPanel(
                    followUps = navigation.related,
                    onOpen = navigation.onOpen,
                    compact = isCompactPhone
                )
            }
        }
    }
}

@Composable
private fun DemoPreviewPanel(
    preview: @Composable () -> Unit,
    compact: Boolean
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = PaddingValues(if (compact) 12.dp else 18.dp)
    ) {
        preview()
    }
}

@Composable
private fun DemoControlsPanel(
    controls: @Composable () -> Unit,
    compact: Boolean,
    onOpenInspectionDialog: (() -> Unit)? = null
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.tertiary,
        contentPadding = PaddingValues(if (compact) 10.dp else 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberBadge(
                    text = "LIVE CONTROLS",
                    accent = MaterialTheme.colorScheme.tertiary
                )
                CyberBadge(
                    text = "RUNTIME BINDINGS",
                    accent = MaterialTheme.colorScheme.secondary
                )
                if (compact && onOpenInspectionDialog != null) {
                    CyberBadge(
                        text = "COMPARE CODE",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenInspectionDialog() },
                        accent = MaterialTheme.colorScheme.primary,
                        fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                    )
                }
            }
            Text(
                text = if (compact) {
                    "Tune here. Open DSL and AGSL in a quick compare view."
                } else {
                    "Tune uniforms first, then compare the DSL and AGSL panels above against the live preview."
                },
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            controls()
        }
    }
}

@Composable
private fun DemoInfoCard(
    demo: DemoInfo,
    generatedAgsl: String?,
    compact: Boolean,
    onOpenInspectionDialog: (() -> Unit)? = null
) {
    val subtitleStyle = if (compact) {
        MaterialTheme.typography.titleLarge.copy(
            fontSize = 17.sp,
            lineHeight = 22.sp
        )
    } else {
        MaterialTheme.typography.titleLarge
    }
    CyberPanel(
        accent = MaterialTheme.colorScheme.primary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(if (compact) 12.dp else 18.dp)
    ) {
        if (compact) {
            Text(
                text = "sample://${demo.id.name.lowercase()} / ${demo.section.title.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                CyberBadge(
                    text = demo.section.title.uppercase(),
                    accent = MaterialTheme.colorScheme.primary
                )
                if (demo.isStartHere) {
                    CyberBadge(
                        text = "START HERE",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
                demo.canonicalFamily?.let { family ->
                    CyberBadge(
                        text = family,
                        accent = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                text = demo.subtitle,
                style = subtitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = demo.focus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            DemoPathSignalBlock(
                signal = demo.pathSignal,
                modifier = Modifier.padding(top = 10.dp),
                compact = true
            )
            DemoFocusTags(
                tags = demo.focusTags,
                modifier = Modifier.padding(top = 10.dp),
                accent = MaterialTheme.colorScheme.secondary,
                maxVisible = 2
            )
            if (onOpenInspectionDialog != null) {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberBadge(
                        text = "COMPARE CODE",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenInspectionDialog() },
                        accent = MaterialTheme.colorScheme.primary,
                        fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                    )
                    CyberBadge(
                        text = if (generatedAgsl != null) "DSL + AGSL" else "DSL",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            return@CyberPanel
        }

        Column(modifier = Modifier.animateContentSize()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
        }
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            if (demo.isStartHere) {
                CyberBadge(
                    text = "START HERE",
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
            demo.canonicalFamily?.let { family ->
                CyberBadge(
                    text = family,
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = demo.subtitle,
            style = subtitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = demo.focus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        DemoPathSignalBlock(
            signal = demo.pathSignal,
            modifier = Modifier.padding(top = 12.dp),
            compact = false
        )
        DemoFocusTags(
            tags = demo.focusTags,
            modifier = Modifier.padding(top = 10.dp),
            accent = MaterialTheme.colorScheme.secondary,
            maxVisible = 4
        )
        Text(
            text = "Inspection flow",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = "PREVIEW",
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = "CONTROLS",
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = "DSL",
                accent = MaterialTheme.colorScheme.secondary
            )
            if (generatedAgsl != null) {
                CyberBadge(
                    text = "AGSL",
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = "Use the copy action to compare the authored DSL with the generated shader while iterating on live uniforms.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        DebugChecklist(
            demo = demo,
            modifier = Modifier.padding(top = 14.dp),
            compact = false
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
private fun DemoInspectionDialog(
    demo: DemoInfo,
    generatedAgsl: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 14.dp)
        ) {
            CyberPanel(
                modifier = Modifier.fillMaxSize(),
                accent = MaterialTheme.colorScheme.primary,
                contentPadding = PaddingValues(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberBadge(
                        text = "CODE COMPARE",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    CyberBadge(
                        text = demo.title.uppercase(),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    CyberBadge(
                        text = "CLOSE",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDismiss() },
                        accent = MaterialTheme.colorScheme.tertiary,
                        fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                    )
                }
                Text(
                    text = "Compare the authored DSL with the generated AGSL without losing the live controls flow.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                DemoPathSignalBlock(
                    signal = demo.pathSignal,
                    modifier = Modifier.padding(top = 10.dp),
                    compact = true
                )
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberBadge(
                        text = "DSL",
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    if (generatedAgsl != null) {
                        CyberBadge(
                            text = "AGSL",
                            accent = MaterialTheme.colorScheme.primary
                        )
                    }
                    CyberBadge(
                        text = "COPY READY",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpandableCodeBlock(
                        title = "DSL snippet",
                        text = demo.snippet,
                        collapsedLines = 10,
                        stateKey = "${demo.id.name}-dialog-dsl"
                    )
                    if (generatedAgsl != null) {
                        ExpandableCodeBlock(
                            title = "Generated AGSL",
                            text = generatedAgsl,
                            collapsedLines = 18,
                            stateKey = "${demo.id.name}-dialog-agsl"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoPathSignalBlock(
    signal: DemoPathSignal,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 16.dp)
    val accent = when (signal.kind) {
        DemoPathKind.StartHere -> MaterialTheme.colorScheme.tertiary
        DemoPathKind.Canonical -> MaterialTheme.colorScheme.primary
        DemoPathKind.Exploratory -> MaterialTheme.colorScheme.secondary
        DemoPathKind.Foundation -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (compact) 0.78f else 0.84f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.36f),
                shape = shape
            )
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 10.dp else 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberBadge(
                    text = "PATH SIGNAL",
                    accent = accent
                )
                CyberBadge(
                    text = signal.badge,
                    accent = MaterialTheme.colorScheme.secondary
                )
                signal.family?.let { family ->
                    CyberBadge(
                        text = family,
                        accent = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = signal.title,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = signal.body,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (compact) 4 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
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
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var expanded by rememberSaveable(stateKey) { mutableStateOf(false) }
    var copyFeedback by rememberSaveable("${stateKey}-copy") { mutableStateOf(0) }

    LaunchedEffect(copyFeedback) {
        if (copyFeedback == 0) return@LaunchedEffect
        val token = copyFeedback
        delay(1_400)
        if (copyFeedback == token) {
            copyFeedback = 0
        }
    }

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
            actions = buildList {
                add(
                    CyberCodeAction(
                        label = if (copyFeedback == 0) "COPY" else "COPIED",
                        onClick = {
                            coroutineScope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("redbytefx-$title", text).toClipEntry()
                                )
                            }
                            copyFeedback += 1
                        }
                    )
                )
                if (canExpand) {
                    add(
                        CyberCodeAction(
                            label = if (expanded) "COLLAPSE" else "EXPAND",
                            onClick = { expanded = !expanded }
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun DemoFocusTags(
    tags: List<String>,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    maxVisible: Int = 4
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(maxVisible).forEach { tag ->
            CyberBadge(
                text = tag.uppercase(),
                accent = accent,
                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DebugChecklist(
    demo: DemoInfo,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val signal = demo.pathSignal
    CyberPanel(
        modifier = modifier,
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = PaddingValues(if (compact) 12.dp else 14.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = "DEBUG CHECKLIST",
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = if (demo.layer == DemoLayer.Stdlib) "HELPER-FIRST" else "DSL-FIRST",
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = signal.badge,
                accent = when (signal.kind) {
                    DemoPathKind.StartHere -> MaterialTheme.colorScheme.tertiary
                    DemoPathKind.Canonical -> MaterialTheme.colorScheme.primary
                    DemoPathKind.Exploratory -> MaterialTheme.colorScheme.secondary
                    DemoPathKind.Foundation -> MaterialTheme.colorScheme.outline
                }
            )
        }
        DebugStep(
            label = "1",
            title = "Change one control at a time",
            body = if (demo.isAnimated) {
                "Start with speed/amount style controls so the preview change is easy to isolate before reading code."
            } else {
                "Start with the strongest visible control so the preview change is obvious before reading code."
            },
            modifier = Modifier.padding(top = 12.dp),
            compact = compact
        )
        DebugStep(
            label = "2",
            title = "Read the authoring intent in DSL",
            body = when (signal.kind) {
                DemoPathKind.StartHere -> {
                    if (demo.layer == DemoLayer.Stdlib) {
                        "Treat the named helpers here as the recommended starter path; this demo is meant to teach the canonical recipe before stylistic variants."
                    } else {
                        "Treat the locals, uniforms, and sample path here as the preferred first raw DSL mental model before adding higher-level helpers."
                    }
                }

                DemoPathKind.Canonical -> {
                    "Read the named helper family first; this demo is part of the curated surface and should stay legible before you compare the generated AGSL."
                }

                DemoPathKind.Exploratory -> {
                    "Read the named helpers, but keep mapping them back to the canonical basics they build on; this demo is intentionally past the first teaching surface."
                }

                DemoPathKind.Foundation -> {
                    "Look for direct coordinate math, locals, and uniforms first; this is close to AGSL shape even if it is not one of the primary starter demos."
                }
            },
            modifier = Modifier.padding(top = 12.dp),
            compact = compact
        )
        DebugStep(
            label = "3",
            title = "Compare the generated AGSL shape",
            body = "After copying the panel, verify uniforms, sample calls, and branches. If the preview feels wrong, simplify back toward `sample()` and add pieces again.",
            modifier = Modifier.padding(top = 12.dp),
            compact = compact
        )
    }
}

@Composable
private fun DebugStep(
    label: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = label,
                accent = MaterialTheme.colorScheme.primary,
                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            CyberBadge(
                text = label,
                accent = MaterialTheme.colorScheme.primary,
                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
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
private fun DemoNavigationStrip(
    navigation: DemoNavigation,
    compact: Boolean
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(if (compact) 14.dp else 18.dp)
    ) {
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NavigationCard(
                    label = "PREV",
                    demo = navigation.previous,
                    onOpen = navigation.onOpen
                )
                NavigationCard(
                    label = "NEXT",
                    demo = navigation.next,
                    onOpen = navigation.onOpen
                )
            }
        } else {
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
}

@Composable
private fun DemoNextStepsPanel(
    followUps: List<DemoFollowUp>,
    onOpen: (DemoId) -> Unit,
    compact: Boolean
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.primary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(if (compact) 14.dp else 18.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = "KEEP EXPLORING",
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = "${followUps.size} NEXT DEMOS",
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = "Follow the idea, not just the catalog order. These demos stay close by section, authoring layer, or shared shader concepts.",
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            followUps.forEach { followUp ->
                DemoFollowUpCard(
                    followUp = followUp,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun DemoFollowUpCard(
    followUp: DemoFollowUp,
    onOpen: (DemoId) -> Unit
) {
    val demo = followUp.demo

    CyberPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onOpen(demo.id) },
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = followUp.label,
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = demo.section.title.uppercase(),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = demo.layer.label,
                accent = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = demo.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = followUp.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = demo.focus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        DemoFocusTags(
            tags = demo.focusTags,
            modifier = Modifier.padding(top = 12.dp),
            accent = MaterialTheme.colorScheme.primary,
            maxVisible = 3
        )
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
    val compact = LocalCompactChrome.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = if (compact) Modifier.scale(0.9f) else Modifier
        )
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
    val compact = LocalCompactChrome.current
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
fun RadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    RadioRow(
        title = title,
        selected = selected,
        modifier = Modifier,
        onClick = onClick
    )
}

@Composable
fun RadioRow(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val compact = LocalCompactChrome.current
    if (compact) {
        val shape = RoundedCornerShape(14.dp)
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.74f)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    },
                    shape = shape
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactPreview = maxWidth < 380.dp
        val stageHeight = when {
            maxWidth < 380.dp -> 168.dp
            maxWidth < 420.dp -> 184.dp
            else -> 244.dp
        }
        val titleStyle = if (compactPreview) {
            MaterialTheme.typography.displayLarge.copy(
                fontSize = 26.sp,
                lineHeight = 30.sp
            )
        } else {
            MaterialTheme.typography.displayLarge
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight)
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
                .padding(if (compactPreview) 18.dp else 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (compactPreview) 76.dp else 94.dp)
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
                    .size(width = if (compactPreview) 48.dp else 62.dp, height = if (compactPreview) 112.dp else 150.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(if (compactPreview) 96.dp else 126.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(if (compactPreview) 8.dp else 10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = if (compactPreview) 4.dp else 8.dp)
                )
                if (!compactPreview) {
                    Text(
                        text = "kotlin.dsl // agsl // compose.runtime",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
    }
}

/**
 * Preview for the Liquid Glass demo: a centered translucent pill with a label, on a busy gradient so
 * refraction and rim highlights read clearly through [Modifier.redbyteFx].
 */
@Composable
fun DemoLiquidGlassPreviewStage(
    fx: FxController,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactPreview = maxWidth < 380.dp
        val stageHeight = when {
            maxWidth < 380.dp -> 168.dp
            maxWidth < 420.dp -> 184.dp
            else -> 244.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight)
                .then(modifier)
                .redbyteFx(fx)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.97f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    shape = shape
                )
                .padding(horizontal = if (compactPreview) 16.dp else 24.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compactPreview) 8.dp else 10.dp)
            ) {
                Text(
                    text = "Glass surface",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactPreview) 46.dp else 52.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.58f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
                        )
                    }
                }
                Text(
                    text = "Translucent fill + rim — shader adds flow and edge chroma",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}
