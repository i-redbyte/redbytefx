package ru.redbyte.redbytefx.sample.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.sample.R
import ru.redbyte.redbytefx.sample.model.DemoCatalog
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.ui.CyberBackdrop
import ru.redbyte.redbytefx.sample.ui.CyberBadge
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.ui.DemoScreen
import ru.redbyte.redbytefx.sample.ui.HomeScreen

@Composable
fun RedByteFxSampleApp() {
    var currentDemo: DemoId? by rememberSaveable { mutableStateOf(null) }
    val appName = stringResource(id = R.string.app_name)

    val title = currentDemo?.let { id ->
        DemoCatalog.firstOrNull { it.id == id }?.title ?: "Demo"
    } ?: appName

    BackHandler(enabled = currentDemo != null) {
        currentDemo = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CyberBackdrop()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CyberPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    )
                ) {
                    Column {
                        if (currentDemo != null) {
                            CyberBadge(
                                text = "Back",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { currentDemo = null },
                                accent = MaterialTheme.colorScheme.tertiary,
                                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                textColor = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            CyberBadge(
                                text = "Live cookbook",
                                accent = MaterialTheme.colorScheme.secondary,
                                fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            text = if (currentDemo == null) {
                                "matrix://shader-lab / redbytefx.sample"
                            } else {
                                "demo://${currentDemo!!.name.lowercase()} / runtime: live"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                AnimatedContent(
                    targetState = currentDemo,
                    transitionSpec = {
                        val forward = targetState != null
                        if (forward) {
                            slideInHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 420,
                                    easing = FastOutSlowInEasing
                                ),
                                initialOffsetX = { it / 6 }
                            ) + fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(420)
                            ) togetherWith slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 320,
                                    easing = FastOutSlowInEasing
                                ),
                                targetOffsetX = { -it / 10 }
                            ) + fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(250)
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 360,
                                    easing = FastOutSlowInEasing
                                ),
                                initialOffsetX = { -it / 10 }
                            ) + fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(320)
                            ) togetherWith slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 280,
                                    easing = FastOutSlowInEasing
                                ),
                                targetOffsetX = { it / 12 }
                            ) + fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(220)
                            )
                        }.using(SizeTransform(clip = false))
                    },
                    label = "sample_navigation"
                ) { id ->
                    if (id == null) {
                        HomeScreen(
                            demos = DemoCatalog,
                            onOpen = { demoId -> currentDemo = demoId }
                        )
                    } else {
                        DemoScreen(id = id)
                    }
                }
            }
        }
    }
}
