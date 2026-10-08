@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ru.redbyte.redbytefx.sample.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.redbyte.redbytefx.sample.R
import ru.redbyte.redbytefx.sample.model.DemoCatalog
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.ui.CyberBackdrop
import ru.redbyte.redbytefx.sample.ui.CyberBadge
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.ui.DemoScreen
import ru.redbyte.redbytefx.sample.ui.HomeScreen
import ru.redbyte.redbytefx.sample.ui.LabHome
import ru.redbyte.redbytefx.sample.ui.LocalCompactChrome
import ru.redbyte.redbytefx.sample.ui.gl.GlExample
import ru.redbyte.redbytefx.sample.ui.gl.GlExampleList
import ru.redbyte.redbytefx.sample.ui.gl.GlExampleScreen
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.sample.ui.show
import ru.redbyte.redbytefx.sample.ui.shownTitle

@Composable
fun RedByteFxSampleApp(
    initialDemo: DemoId? = null,
    launchDemoRequest: DemoId? = null,
) {
    var lab by rememberSaveable {
        mutableStateOf(if (initialDemo == null) SampleLab.Hub else SampleLab.Agsl)
    }
    var currentDemo: DemoId? by rememberSaveable { mutableStateOf(initialDemo) }
    var glExample: GlExample? by rememberSaveable { mutableStateOf(null) }
    val place = remember(lab, currentDemo, glExample) {
        SamplePlace(lab, currentDemo, glExample)
    }
    val appName = stringResource(id = R.string.app_name)
    val currentInfo = remember(currentDemo) {
        currentDemo?.let { id -> DemoCatalog.firstOrNull { it.id == id } }
    }

    val title = sampleTitle(lab, appName, currentInfo?.shownTitle(), glExample?.title?.show())
    val positionLabel = if (lab == SampleLab.Agsl && currentInfo != null) {
        "#${DemoCatalog.indexOf(currentInfo) + 1}/${DemoCatalog.size}"
    } else {
        null
    }
    val route = sampleRoute(lab, currentDemo?.name, glExample)

    fun retreat() {
        when {
            lab == SampleLab.Agsl && currentDemo != null -> currentDemo = null
            lab == SampleLab.Gl && glExample != null -> glExample = null
            else -> {
                currentDemo = null
                glExample = null
                lab = SampleLab.Hub
            }
        }
    }

    BackHandler(enabled = lab != SampleLab.Hub) {
        retreat()
    }

    LaunchedEffect(launchDemoRequest) {
        if (launchDemoRequest != null && launchDemoRequest != currentDemo) {
            glExample = null
            lab = SampleLab.Agsl
            currentDemo = launchDemoRequest
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactUi = maxWidth < 420.dp

        CompositionLocalProvider(LocalCompactChrome provides isCompactUi) {
            Box(modifier = Modifier.fillMaxSize()) {
                CyberBackdrop()

                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        BoxWithConstraints {
                            val isCompactPhone = maxWidth < 420.dp
                            val titleStyle = if (isCompactPhone) {
                                MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 25.sp,
                                )
                            } else {
                                MaterialTheme.typography.headlineLarge
                            }

                            CyberPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(
                                        WindowInsets.statusBars.only(
                                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                                        ),
                                    )
                                    .padding(
                                        start = if (isCompactPhone) 8.dp else 12.dp,
                                        end = if (isCompactPhone) 8.dp else 12.dp,
                                        top = if (isCompactPhone) 8.dp else 12.dp,
                                    )
                                    .clip(RoundedCornerShape(28.dp)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = if (isCompactPhone) 12.dp else 18.dp,
                                    vertical = if (isCompactPhone) 10.dp else 14.dp,
                                ),
                            ) {
                                Column {
                                    FlowRow(
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                    ) {
                                        if (lab != SampleLab.Hub) {
                                            CyberBadge(
                                                text = say("Back", "Назад"),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { retreat() },
                                                accent = MaterialTheme.colorScheme.tertiary,
                                                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                            )
                                        } else {
                                            CyberBadge(
                                                text = say("Live cookbook", "Живой сборник"),
                                                accent = MaterialTheme.colorScheme.secondary,
                                                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        if (positionLabel != null) {
                                            CyberBadge(
                                                text = positionLabel,
                                                accent = MaterialTheme.colorScheme.primary,
                                                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                    Text(
                                        text = title,
                                        style = titleStyle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = if (isCompactPhone) 6.dp else 10.dp),
                                    )
                                    Text(
                                        text = route,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .padding(top = if (isCompactPhone) 4.dp else 6.dp)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                                        Color.Transparent,
                                                    ),
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                            .padding(
                                                horizontal = if (isCompactPhone) 7.dp else 8.dp,
                                                vertical = if (isCompactPhone) 3.dp else 4.dp,
                                            ),
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .windowInsetsPadding(
                                WindowInsets.navigationBars.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                                ),
                            )
                            .imePadding(),
                    ) {
                        AnimatedContent(
                            targetState = place,
                            transitionSpec = {
                                val forward = targetState.depth >= initialState.depth
                                if (forward) {
                                    slideInHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 420,
                                            easing = FastOutSlowInEasing,
                                        ),
                                        initialOffsetX = { it / 6 },
                                    ) + fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(420),
                                    ) togetherWith slideOutHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 320,
                                            easing = FastOutSlowInEasing,
                                        ),
                                        targetOffsetX = { -it / 10 },
                                    ) + fadeOut(
                                        animationSpec = androidx.compose.animation.core.tween(250),
                                    )
                                } else {
                                    slideInHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 360,
                                            easing = FastOutSlowInEasing,
                                        ),
                                        initialOffsetX = { -it / 10 },
                                    ) + fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(320),
                                    ) togetherWith slideOutHorizontally(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 280,
                                            easing = FastOutSlowInEasing,
                                        ),
                                        targetOffsetX = { it / 12 },
                                    ) + fadeOut(
                                        animationSpec = androidx.compose.animation.core.tween(220),
                                    )
                                }.using(SizeTransform(clip = false))
                            },
                            label = "sample_navigation",
                        ) { shown ->
                            SampleDestination(
                                place = shown,
                                onOpenAgsl = {
                                    currentDemo = null
                                    glExample = null
                                    lab = SampleLab.Agsl
                                },
                                onOpenGl = {
                                    glExample = null
                                    currentDemo = null
                                    lab = SampleLab.Gl
                                },
                                onOpenDemo = { currentDemo = it },
                                onOpenGlExample = { glExample = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sampleTitle(
    lab: SampleLab,
    appName: String,
    demoTitle: String?,
    glTitle: String?,
): String = when {
    lab == SampleLab.Agsl && demoTitle != null -> demoTitle
    lab == SampleLab.Agsl -> "AGSL"
    lab == SampleLab.Gl && glTitle != null -> glTitle
    lab == SampleLab.Gl -> "OpenGL ES"
    else -> appName
}

@Composable
private fun sampleRoute(lab: SampleLab, demoName: String?, glExample: GlExample?): String = when {
    lab == SampleLab.Agsl && demoName != null -> say(
        "demo://${demoName.lowercase()} / runtime: live",
        "пример://${demoName.lowercase()} / показ: живой",
    )
    lab == SampleLab.Agsl -> say(
        "agsl://cookbook / runtime shader",
        "agsl://сборник / шейдер вживую",
    )
    lab == SampleLab.Gl && glExample != null -> say(
        "gles://${glExample.name.lowercase()} / ${glExample.api.removePrefix("OpenGL ").lowercase()}",
        "gles://${glExample.name.lowercase()} / ${glExample.api.removePrefix("OpenGL ").lowercase()}",
    )
    lab == SampleLab.Gl -> say(
        "gles://examples / es 3.0",
        "gles://примеры / es 3.0",
    )
    else -> say(
        "matrix://shader-lab / redbytefx.sample",
        "matrix://лаборатория / redbytefx.sample",
    )
}

@Composable
private fun SampleDestination(
    place: SamplePlace,
    onOpenAgsl: () -> Unit,
    onOpenGl: () -> Unit,
    onOpenDemo: (DemoId) -> Unit,
    onOpenGlExample: (GlExample) -> Unit,
) {
    when (place.lab) {
        SampleLab.Hub -> LabHome(onAgsl = onOpenAgsl, onGl = onOpenGl)
        SampleLab.Agsl -> {
            val id = place.demo
            if (id == null) {
                HomeScreen(demos = DemoCatalog, onOpen = onOpenDemo)
            } else {
                DemoScreen(id = id, onOpenDemo = onOpenDemo)
            }
        }
        SampleLab.Gl -> {
            val example = place.gl
            if (example == null) {
                GlExampleList(onOpen = onOpenGlExample)
            } else {
                GlExampleScreen(example)
            }
        }
    }
}

private enum class SampleLab {
    Hub,
    Agsl,
    Gl,
}

private data class SamplePlace(
    val lab: SampleLab,
    val demo: DemoId?,
    val gl: GlExample?,
) {
    val depth: Int
        get() = when (lab) {
            SampleLab.Hub -> 0
            SampleLab.Agsl -> if (demo == null) 1 else 2
            SampleLab.Gl -> if (gl == null) 1 else 2
        }
}
