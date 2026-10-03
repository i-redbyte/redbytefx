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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch
import ru.redbyte.redbytefx.sample.model.CanonicalGuide
import ru.redbyte.redbytefx.sample.model.CanonicalGuideCatalog
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.DemoInfo
import ru.redbyte.redbytefx.sample.model.DemoLayer
import ru.redbyte.redbytefx.sample.model.DemoSection
import ru.redbyte.redbytefx.sample.model.Phrase
import ru.redbyte.redbytefx.sample.model.catalogSearchText
import ru.redbyte.redbytefx.sample.model.canonicalFamily
import ru.redbyte.redbytefx.sample.model.focusTags
import ru.redbyte.redbytefx.sample.model.isCanonicalDemo
import ru.redbyte.redbytefx.sample.model.isAnimated
import ru.redbyte.redbytefx.sample.model.isStartHere
import ru.redbyte.redbytefx.sample.model.layer
import ru.redbyte.redbytefx.sample.model.section

private enum class LayerFilter(
    val label: Phrase,
    val matches: (DemoInfo) -> Boolean,
) {
    All(Phrase("ALL", "ВСЕ"), { true }),
    Core(Phrase("CORE", "ЯДРО"), { it.layer == DemoLayer.Core }),
    Stdlib(Phrase("STDLIB", "БИБЛИОТЕКА"), { it.layer == DemoLayer.Stdlib }),
}

private enum class MotionFilter(
    val label: Phrase,
    val matches: (DemoInfo) -> Boolean,
) {
    All(Phrase("ALL", "ВСЕ"), { true }),
    Animated(Phrase("ANIMATED", "ЖИВЫЕ"), { it.isAnimated }),
    Static(Phrase("STATIC", "НЕПОДВИЖНЫЕ"), { !it.isAnimated }),
}

private enum class PathFilter(
    val label: Phrase,
    val matches: (DemoInfo) -> Boolean,
) {
    All(Phrase("ALL", "ВСЕ"), { true }),
    StartHere(Phrase("START HERE", "С ЧЕГО НАЧАТЬ"), { it.isStartHere }),
    Canonical(Phrase("CANONICAL", "ОСНОВНОЙ ПУТЬ"), { it.isCanonicalDemo }),
}

private data class StarterRoute(
    val label: Phrase,
    val title: Phrase,
    val summary: Phrase,
    val demoId: DemoId,
)

@Composable
fun HomeScreen(
    demos: List<DemoInfo>,
    onOpen: (DemoId) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
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
                label = Phrase("PATH 00", "ПУТЬ 00"),
                title = Phrase("Hero: Aurora Iridescence", "Витрина: перелив сияния"),
                summary = Phrase(
                    "Open the marketing-style showcase: rim light, rotating sweep, cosine palette, and chromatic polish in one shader.",
                    "Откройте витрину: свет кромки, вращающийся проход, косинусная палитра и цветовое смещение в одном шейдере.",
                ),
                demoId = DemoId.Aurora,
            ),
            StarterRoute(
                label = Phrase("PATH 01", "ПУТЬ 01"),
                title = Phrase("Start With Raw DSL", "Начните с прямого описания"),
                summary = Phrase(
                    "See direct coordinate math and compare it against generated AGSL without stdlib abstraction first.",
                    "Посмотрите прямую математику координат и сравните её с получившимся AGSL, пока без библиотеки.",
                ),
                demoId = DemoId.Wave,
            ),
            StarterRoute(
                label = Phrase("PATH 01a", "ПУТЬ 01а"),
                title = Phrase("Port A Classic AGSL Gradient", "Перенесите классический градиент AGSL"),
                summary = Phrase(
                    "RGB sin waves over UV with uniform time - no sampling, no stdlib, ideal for line-by-line AGSL ports.",
                    "Синусы RGB по координатам и общее время: без выборки и без библиотеки, удобно сверять перенос строка за строкой.",
                ),
                demoId = DemoId.AnimatedGradient,
            ),
            StarterRoute(
                label = Phrase("PATH 01b", "ПУТЬ 01б"),
                title = Phrase("Compose Touch Into Uniforms", "Касание Compose в параметрах"),
                summary = Phrase(
                    "Drag on the preview: a float2 uniform tracks the pointer while animated ripples run in the shader.",
                    "Ведите палец по картинке: параметр float2 следит за ним, а рябь живёт в шейдере.",
                ),
                demoId = DemoId.TouchRipple,
            ),
            StarterRoute(
                label = Phrase("PATH 02", "ПУТЬ 02"),
                title = Phrase("See Stdlib Recipes", "Посмотрите приёмы библиотеки"),
                summary = Phrase(
                    "Jump into reusable helpers and watch how masks, patterns, and modulation still stay readable in AGSL shape.",
                    "Перейдите к готовым функциям и посмотрите, как маски, узоры и модуляция остаются читаемыми в форме AGSL.",
                ),
                demoId = DemoId.Signal,
            ),
            StarterRoute(
                label = Phrase("PATH 03", "ПУТЬ 03"),
                title = Phrase("Read Masks And Compositing", "Разберите маски и сборку"),
                summary = Phrase(
                    "See the canonical mask/compositing path with helpers like maskedMix(...), alphaMask(...), and maskedScreen(...).",
                    "Основной путь масок и сборки: maskedMix(...), alphaMask(...) и maskedScreen(...).",
                ),
                demoId = DemoId.Composite,
            ),
            StarterRoute(
                label = Phrase("PATH 04", "ПУТЬ 04"),
                title = Phrase("Finish With A Rich Scene", "Закончите богатой сценой"),
                summary = Phrase(
                    "Open the larger board-style showcase to see how the same primitives scale into a more serious composed demo.",
                    "Откройте плату и посмотрите, как те же примитивы складываются в более серьёзный пример.",
                ),
                demoId = DemoId.Circuit,
            )
        ).mapNotNull { route ->
            demos.firstOrNull { it.id == route.demoId }?.let { demo -> route to demo }
        }
    }
    val canonicalGuides = remember(demos) {
        CanonicalGuideCatalog.mapNotNull { guide ->
            val guideDemos = guide.demoIds.mapNotNull { id -> demos.firstOrNull { it.id == id } }
            guide.takeIf { guideDemos.isNotEmpty() }?.let { it to guideDemos }
        }
    }
    val coreCount = remember(visibleDemos) { visibleDemos.count { it.layer == DemoLayer.Core } }
    val canonicalCount = remember(visibleDemos) { visibleDemos.count { it.isCanonicalDemo } }
    val sectionAnchors = remember(visibleDemos, normalizedQuery, layerFilter, motionFilter, pathFilter) {
        val showIntroPanels = normalizedQuery.isEmpty() &&
            layerFilter == LayerFilter.All &&
            motionFilter == MotionFilter.All &&
            pathFilter == PathFilter.All
        var itemIndex = 2 + if (showIntroPanels) 2 else 0
        buildMap<DemoSection, Int> {
            DemoSection.entries.forEach { section ->
                val count = visibleDemos.count { it.section == section }
                if (count > 0) {
                    put(section, itemIndex)
                    itemIndex += 1 + count
                }
            }
        }
    }

    LazyColumn(
        state = listState,
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
                },
                sectionAnchors = sectionAnchors,
                onJumpToSection = { section ->
                    sectionAnchors[section]?.let { index ->
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
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
                        text = say("AGSL // Kotlin DSL", "AGSL // описание на Kotlin"),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    CyberBadge(
                        text = say("Android runtime", "Запуск на Android"),
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = say(
                        "A live shader field manual for RedByteFX.",
                        "Живой полевой справочник шейдеров RedByteFX.",
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    text = say(
                        "Browse the language by section, inspect generated AGSL, and stress-test runtime bindings against real animated previews.",
                        "Смотрите язык по разделам, читайте получившийся AGSL и проверяйте живые параметры на настоящей движущейся картинке.",
                    ),
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
                        label = if (normalizedQuery.isEmpty()) {
                            say("demos", "примеры")
                        } else {
                            say("results", "найдено")
                        },
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    ShowcaseMetric(
                        value = coreCount.toString(),
                        label = say("core", "ядро"),
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    ShowcaseMetric(
                        value = canonicalCount.toString(),
                        label = say("canonical", "основной путь"),
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
            item(key = "canonical-map") {
                CanonicalMapPanel(
                    guides = canonicalGuides,
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
                            text = say("NO MATCHES", "НИЧЕГО НЕ НАЙДЕНО"),
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                        CyberBadge(
                            text = say("TRY TITLE OR SECTION", "ИЩИТЕ НАЗВАНИЕ ИЛИ РАЗДЕЛ"),
                            accent = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = say(
                            "No demos matched \"$searchQuery\". Try a shorter term or search by helper name, section, or demo title.",
                            "По запросу «$searchQuery» примеров нет. Укоротите фразу или ищите по имени функции, разделу или названию.",
                        ),
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
    onClearAll: () -> Unit,
    sectionAnchors: Map<DemoSection, Int>,
    onJumpToSection: (DemoSection) -> Unit
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
                text = say("SEARCH", "ПОИСК"),
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
                    text = say("CLEAR", "СБРОСИТЬ"),
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
                Text(say("Search demos", "Искать примеры"))
            },
            placeholder = {
                Text(say("Circuit, reveal, stdlib, glow...", "Плата, проявление, библиотека, свечение..."))
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
        if (sectionAnchors.size > 1) {
            SectionJumpRow(
                anchors = sectionAnchors,
                onJumpToSection = onJumpToSection,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
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
            title = Phrase("LAYER", "СЛОЙ"),
            options = LayerFilter.entries,
            selected = layerFilter,
            onSelect = onLayerFilterChange
        )
        FilterRow(
            title = Phrase("MOTION", "ДВИЖЕНИЕ"),
            options = MotionFilter.entries,
            selected = motionFilter,
            onSelect = onMotionFilterChange
        )
        FilterRow(
            title = Phrase("PATH", "ПУТЬ"),
            options = PathFilter.entries,
            selected = pathFilter,
            onSelect = onPathFilterChange
        )
    }
}

@Composable
private fun <T> FilterRow(
    title: Phrase,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) where T : Enum<T> {
    val defaultOption = options.first()
    val selectedLabel = when (selected) {
        is LayerFilter -> selected.label.show()
        is MotionFilter -> selected.label.show()
        is PathFilter -> selected.label.show()
        else -> selected.name
    }
    val titleText = if (selected == defaultOption) {
        title.show()
    } else {
        "${title.show()}: $selectedLabel"
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
                is LayerFilter -> option.label.show()
                is MotionFilter -> option.label.show()
                is PathFilter -> option.label.show()
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
private fun SectionJumpRow(
    anchors: Map<DemoSection, Int>,
    onJumpToSection: (DemoSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = say("SECTIONS", "РАЗДЕЛЫ"),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = say(
                    "${anchors.size} JUMPS",
                    ruCount(anchors.size, "переход", "перехода", "переходов"),
                ),
                accent = MaterialTheme.colorScheme.secondary
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoSection.entries.forEach { section ->
                if (section in anchors) {
                    CyberBadge(
                        text = section.title.showUpper(),
                        modifier = Modifier.clickable { onJumpToSection(section) },
                        accent = MaterialTheme.colorScheme.tertiary,
                        fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f)
                    )
                }
            }
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
                text = say("START HERE", "С ЧЕГО НАЧАТЬ"),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = say(
                    "${routes.size} ROUTES",
                    ruCount(routes.size, "маршрут", "маршрута", "маршрутов"),
                ),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = say(
                "If you're new to RedByteFX, these routes give a faster introduction than scanning all demos at once.",
                "Если RedByteFX для вас нов, эти маршруты вводят быстрее, чем просмотр всего каталога подряд.",
            ),
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
private fun CanonicalMapPanel(
    guides: List<Pair<CanonicalGuide, List<DemoInfo>>>,
    onOpen: (DemoId) -> Unit
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
                text = say("CANONICAL MAP", "КАРТА ОСНОВНОГО ПУТИ"),
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = say(
                    "${guides.size} FAMILIES",
                    ruCount(guides.size, "семейство", "семейства", "семейств"),
                ),
                accent = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = say(
                "These families mirror the curated helper surface in :redbytefx-stdlib, so the sample teaches the same path as the package docs instead of treating every helper equally.",
                "Эти семейства повторяют подобранную поверхность :redbytefx-stdlib, поэтому примеры ведут тем же путём, что и описание пакета, а не ставят все функции в один ряд.",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            guides.forEach { (guide, demos) ->
                CanonicalGuideCard(
                    guide = guide,
                    demos = demos,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun CanonicalGuideCard(
    guide: CanonicalGuide,
    demos: List<DemoInfo>,
    onOpen: (DemoId) -> Unit
) {
    CyberPanel(
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberBadge(
                text = guide.label.show(),
                accent = MaterialTheme.colorScheme.primary
            )
            demos.forEach { demo ->
                CyberBadge(
                    text = demo.shownTitle().uppercase(),
                    modifier = Modifier.clickable { onOpen(demo.id) },
                    accent = MaterialTheme.colorScheme.tertiary,
                    fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                )
            }
        }
        Text(
            text = guide.title.show(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = guide.summary.show(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = guide.helperPreview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
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
                text = route.label.show(),
                accent = MaterialTheme.colorScheme.tertiary
            )
            CyberBadge(
                text = demo.shownTitle().uppercase(),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = demo.layer.label.show(),
                accent = MaterialTheme.colorScheme.secondary
            )
            demo.canonicalFamily?.let { family ->
                CyberBadge(
                    text = shownFamily(family),
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = route.title.show(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = route.summary.show(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = demo.shownFocus(),
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
                text = section.title.showUpper(),
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = say("$count demos", ruCount(count, "пример", "примера", "примеров")),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = section.subtitle.show(),
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
                text = demo.shownTitle().uppercase(),
                accent = MaterialTheme.colorScheme.primary
            )
            CyberBadge(
                text = demo.layer.label.show(),
                accent = MaterialTheme.colorScheme.secondary
            )
            CyberBadge(
                text = if (demo.isAnimated) say("ANIMATED", "ЖИВОЙ") else say("STATIC", "НЕПОДВИЖНЫЙ"),
                accent = if (demo.isAnimated) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.widthIn(max = 132.dp)
            )
            if (demo.isStartHere) {
                CyberBadge(
                    text = say("START HERE", "С ЧЕГО НАЧАТЬ"),
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
            demo.canonicalFamily?.let { family ->
                CyberBadge(
                    text = shownFamily(family),
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = demo.shownSubtitle(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = demo.shownFocus(),
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
