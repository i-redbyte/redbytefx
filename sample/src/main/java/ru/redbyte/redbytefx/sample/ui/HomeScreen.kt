@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import ru.redbyte.redbytefx.sample.model.canonicalFamily
import ru.redbyte.redbytefx.sample.model.focusTags
import ru.redbyte.redbytefx.sample.model.isCanonicalDemo
import ru.redbyte.redbytefx.sample.model.isAnimated
import ru.redbyte.redbytefx.sample.model.isStartHere
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

private enum class PathFilter(
    val label: String,
    val matches: (DemoInfo) -> Boolean
) {
    All("ALL", { true }),
    StartHere("START HERE", { it.isStartHere }),
    Canonical("CANONICAL", { it.isCanonicalDemo })
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
    var pathFilter by rememberSaveable { mutableStateOf(PathFilter.All) }
    val normalizedQuery = remember(searchQuery) { searchQuery.trim().lowercase() }
    val visibleDemos = remember(demos, normalizedQuery, layerFilter, motionFilter, pathFilter) {
        demos.filter { demo ->
            (normalizedQuery.isEmpty() || demo.catalogSearchText.contains(normalizedQuery)) &&
                layerFilter.matches(demo) &&
                motionFilter.matches(demo) &&
                pathFilter.matches(demo)
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
                title = "Read Masks And Compositing",
                summary = "See the canonical mask/compositing path with helpers like maskedMix(...), alphaMask(...), and maskedScreen(...).",
                demoId = DemoId.Composite
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
    val canonicalCount = remember(visibleDemos) { visibleDemos.count { it.isCanonicalDemo } }

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
                pathFilter = pathFilter,
                onQueryChange = { searchQuery = it },
                onLayerFilterChange = { layerFilter = it },
                onMotionFilterChange = { motionFilter = it },
                onPathFilterChange = { pathFilter = it },
                onClearAll = {
                    searchQuery = ""
                    layerFilter = LayerFilter.All
                    motionFilter = MotionFilter.All
                    pathFilter = PathFilter.All
                }
            )
        }

        item {
            CyberPanel(
                accent = MaterialTheme.colorScheme.secondary,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        value = canonicalCount.toString(),
                        label = "canonical",
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
    pathFilter: PathFilter,
    onQueryChange: (String) -> Unit,
    onLayerFilterChange: (LayerFilter) -> Unit,
    onMotionFilterChange: (MotionFilter) -> Unit,
    onPathFilterChange: (PathFilter) -> Unit,
    onClearAll: () -> Unit
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.tertiary,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = "SEARCH",
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = "$resultCount / $totalCount",
                accent = MaterialTheme.colorScheme.secondary
            )
            if (
                query.isNotBlank() ||
                layerFilter != LayerFilter.All ||
                motionFilter != MotionFilter.All ||
                pathFilter != PathFilter.All
            ) {
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
            pathFilter = pathFilter,
            onPathFilterChange = onPathFilterChange,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun ShowcaseQuickFilters(
    layerFilter: LayerFilter,
    motionFilter: MotionFilter,
    pathFilter: PathFilter,
    onLayerFilterChange: (LayerFilter) -> Unit,
    onMotionFilterChange: (MotionFilter) -> Unit,
    onPathFilterChange: (PathFilter) -> Unit,
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
        FilterRow(
            title = "PATH",
            options = PathFilter.entries,
            selected = pathFilter,
            onSelect = onPathFilterChange
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
    val defaultOption = options.first()
    val selectedLabel = when (selected) {
        is LayerFilter -> selected.label
        is MotionFilter -> selected.label
        is PathFilter -> selected.label
        else -> selected.name
    }
    val titleText = if (selected == defaultOption) {
        title
    } else {
        "$title: $selectedLabel"
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CyberBadge(
            text = titleText,
            modifier = if (selected != defaultOption) {
                Modifier.clickable { onSelect(defaultOption) }
            } else {
                Modifier
            },
            accent = if (selected == defaultOption) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f)
        )
        options.drop(1).forEach { option ->
            val label = when (option) {
                is LayerFilter -> option.label
                is MotionFilter -> option.label
                is PathFilter -> option.label
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            demo.canonicalFamily?.let { family ->
                CyberBadge(
                    text = family,
                    accent = MaterialTheme.colorScheme.primary
                )
            }
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
