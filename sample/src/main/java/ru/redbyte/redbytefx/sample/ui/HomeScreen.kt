package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.DemoInfo
import ru.redbyte.redbytefx.sample.model.DemoLayer
import ru.redbyte.redbytefx.sample.model.DemoSection
import ru.redbyte.redbytefx.sample.model.catalogSearchText
import ru.redbyte.redbytefx.sample.model.focusTags
import ru.redbyte.redbytefx.sample.model.isAnimated
import ru.redbyte.redbytefx.sample.model.layer
import ru.redbyte.redbytefx.sample.model.section

private enum class LayerFilter(
    val label: String,
    val matches: (DemoInfo) -> Boolean
) {
    All("ALL", { true }),
    Core("CORE", { it.layer == DemoLayer.Core }),
    Stdlib("STDLIB", { it.layer == DemoLayer.Stdlib })
}

private enum class MotionFilter(
    val label: String,
    val matches: (DemoInfo) -> Boolean
) {
    All("ALL", { true }),
    Animated("ANIMATED", { it.isAnimated }),
    Static("STATIC", { !it.isAnimated })
}

private data class StarterRoute(
    val label: String,
    val title: String,
    val summary: String,
    val demoId: DemoId
)

@Composable
fun HomeScreen(
    demos: List<DemoInfo>,
    onOpen: (DemoId) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var layerFilter by rememberSaveable { mutableStateOf(LayerFilter.All) }
    var motionFilter by rememberSaveable { mutableStateOf(MotionFilter.All) }
    val normalizedQuery = remember(searchQuery) { searchQuery.trim().lowercase() }
    val visibleDemos = remember(demos, normalizedQuery, layerFilter, motionFilter) {
        demos.filter { demo ->
            (normalizedQuery.isEmpty() || demo.catalogSearchText.contains(normalizedQuery)) &&
                layerFilter.matches(demo) &&
                motionFilter.matches(demo)
        }
    }
    val demoIndexById = remember(demos) {
        demos.mapIndexed { index, demo -> demo.id to index }.toMap()
    }
    val starterRoutes = remember(demos) {
        listOf(
            StarterRoute(
                label = "PATH 01",
                title = "Start With Raw DSL",
                summary = "See direct coordinate math and compare it against generated AGSL without stdlib abstraction first.",
                demoId = DemoId.Wave
            ),
            StarterRoute(
                label = "PATH 02",
                title = "See Stdlib Recipes",
                summary = "Jump into reusable helpers and watch how masks, patterns, and modulation still stay readable in AGSL shape.",
                demoId = DemoId.Signal
            ),
            StarterRoute(
                label = "PATH 03",
                title = "Read Functions And Color",
                summary = "Use a compact color-first demo to inspect locals, helper functions, and the authored-vs-generated comparison flow.",
                demoId = DemoId.Duotone
            ),
            StarterRoute(
                label = "PATH 04",
                title = "Finish With A Rich Scene",
                summary = "Open the larger board-style showcase to see how the same primitives scale into a more serious composed demo.",
                demoId = DemoId.Circuit
            )
        ).mapNotNull { route ->
            demos.firstOrNull { it.id == route.demoId }?.let { demo -> route to demo }
        }
    }
    val coreCount = remember(visibleDemos) { visibleDemos.count { it.layer == DemoLayer.Core } }
    val animatedCount = remember(visibleDemos) { visibleDemos.count { it.isAnimated } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "search") {
            ShowcaseSearchPanel(
                query = searchQuery,
                resultCount = visibleDemos.size,
                totalCount = demos.size,
                layerFilter = layerFilter,
                motionFilter = motionFilter,
                onQueryChange = { searchQuery = it },
                onLayerFilterChange = { layerFilter = it },
                onMotionFilterChange = { motionFilter = it },
                onClearAll = {
                    searchQuery = ""
                    layerFilter = LayerFilter.All
                    motionFilter = MotionFilter.All
                }
            )
        }

        item {
            CyberPanel(
                accent = MaterialTheme.colorScheme.secondary,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberBadge(
                        text = "AGSL // Kotlin DSL",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    CyberBadge(
                        text = "Android runtime",
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = "A live shader field manual for RedByteFX.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    text = "Browse the language by section, inspect generated AGSL, and stress-test runtime bindings against real animated previews.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShowcaseMetric(
                        value = visibleDemos.size.toString(),
                        label = if (normalizedQuery.isEmpty()) "demos" else "results",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    ShowcaseMetric(
                        value = coreCount.toString(),
                        label = "core",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    ShowcaseMetric(
                        value = animatedCount.toString(),
                        label = "animated",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        if (normalizedQuery.isEmpty() && layerFilter == LayerFilter.All && motionFilter == MotionFilter.All) {
            item(key = "starter-routes") {
                StarterRoutesPanel(
                    routes = starterRoutes,
                    onOpen = onOpen
                )
            }
        }

        if (visibleDemos.isEmpty()) {
            item(key = "empty") {
                CyberPanel(
                    accent = MaterialTheme.colorScheme.tertiary,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyberBadge(
                            text = "NO MATCHES",
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                        CyberBadge(
                            text = "TRY TITLE OR SECTION",
                            accent = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = "No demos matched \"$searchQuery\". Try a shorter term or search by helper name, section, or demo title.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        DemoSection.entries.forEach { section ->
            val sectionDemos = visibleDemos.filter { it.section == section }
            if (sectionDemos.isNotEmpty()) {
                item(key = "section-${section.name}") {
                    ShowcaseSectionHeader(
                        section = section,
                        count = sectionDemos.size
                    )
                }

                items(
                    items = sectionDemos,
                    key = { it.id.name }
                ) { demo ->
                    DemoCatalogCard(
                        index = demoIndexById.getValue(demo.id),
                        demo = demo,
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseSearchPanel(
    query: String,
    resultCount: Int,
    totalCount: Int,
    layerFilter: LayerFilter,
    motionFilter: MotionFilter,
    onQueryChange: (String) -> Unit,
    onLayerFilterChange: (LayerFilter) -> Unit,
    onMotionFilterChange: (MotionFilter) -> Unit,
    onClearAll: () -> Unit
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.tertiary,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = "SEARCH",
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = "$resultCount / $totalCount",
                accent = MaterialTheme.colorScheme.secondary
            )
            if (query.isNotBlank() || layerFilter != LayerFilter.All || motionFilter != MotionFilter.All) {
                CyberBadge(
                    text = "CLEAR",
                    modifier = Modifier.clickable { onClearAll() },
                    accent = MaterialTheme.colorScheme.primary,
                    fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
                )
            }
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            label = {
                Text("Search demos")
            },
            placeholder = {
                Text("Circuit, reveal, stdlib, glow...")
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                cursorColor = MaterialTheme.colorScheme.tertiary
            ),
            shape = MaterialTheme.shapes.large
        )
        ShowcaseQuickFilters(
            layerFilter = layerFilter,
            motionFilter = motionFilter,
            onLayerFilterChange = onLayerFilterChange,
            onMotionFilterChange = onMotionFilterChange,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun ShowcaseQuickFilters(
    layerFilter: LayerFilter,
    motionFilter: MotionFilter,
    onLayerFilterChange: (LayerFilter) -> Unit,
    onMotionFilterChange: (MotionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterRow(
            title = "LAYER",
            options = LayerFilter.entries,
            selected = layerFilter,
            onSelect = onLayerFilterChange
        )
        FilterRow(
            title = "MOTION",
            options = MotionFilter.entries,
            selected = motionFilter,
            onSelect = onMotionFilterChange
        )
    }
}

@Composable
private fun <T> FilterRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit
) where T : Enum<T> {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CyberBadge(
            text = title,
            accent = MaterialTheme.colorScheme.secondary,
            fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f)
        )
        options.forEach { option ->
            val label = when (option) {
                is LayerFilter -> option.label
                is MotionFilter -> option.label
                else -> option.name
            }
            val isSelected = option == selected
            CyberBadge(
                text = label,
                modifier = Modifier.clickable { onSelect(option) },
                accent = if (isSelected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                fill = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                }
            )
        }
    }
}

@Composable
private fun ShowcaseMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color
) {
    CyberPanel(
        modifier = modifier,
        accent = accent,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StarterRoutesPanel(
    routes: List<Pair<StarterRoute, DemoInfo>>,
    onOpen: (DemoId) -> Unit
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = "START HERE",
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = "${routes.size} ROUTES",
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = "If you're new to RedByteFX, these routes give a faster introduction than scanning all demos at once.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            routes.forEach { (route, demo) ->
                StarterRouteCard(
                    route = route,
                    demo = demo,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun StarterRouteCard(
    route: StarterRoute,
    demo: DemoInfo,
    onOpen: (DemoId) -> Unit
) {
    CyberPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(demo.id) },
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = route.label,
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = demo.title.uppercase(),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = demo.layer.label,
                accent = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = route.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = route.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = demo.focus,
            style = MaterialTheme.typography.bodySmall,
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
private fun ShowcaseSectionHeader(
    section: DemoSection,
    count: Int
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CyberBadge(
                text = section.title.uppercase(),
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = "$count demos",
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = section.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun DemoCatalogCard(
    index: Int,
    demo: DemoInfo,
    onOpen: (DemoId) -> Unit
) {
    CyberPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(demo.id) },
        accent = if (index % 2 == 0) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        },
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CyberBadge(
                text = "#${(index + 1).toString().padStart(2, '0')}",
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = demo.title.uppercase(),
                accent = MaterialTheme.colorScheme.primary
            )
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
                },
                modifier = Modifier.widthIn(max = 132.dp)
            )
        }
        Text(
            text = demo.subtitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = demo.focus,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        DemoFocusTags(
            tags = demo.focusTags,
            modifier = Modifier.padding(top = 12.dp),
            accent = MaterialTheme.colorScheme.tertiary,
            maxVisible = 3
        )
    }
}
