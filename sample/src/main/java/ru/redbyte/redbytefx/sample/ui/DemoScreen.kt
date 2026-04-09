package ru.redbyte.redbytefx.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ru.redbyte.redbytefx.sample.model.DemoCatalog
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.demoInfo

@Composable
fun DemoScreen(
    id: DemoId,
    onOpenDemo: (DemoId) -> Unit
) {
    val index = DemoCatalog.indexOfFirst { it.id == id }
    val previous = DemoCatalog.getOrNull(index - 1)
    val next = DemoCatalog.getOrNull(index + 1)

    CompositionLocalProvider(
        LocalDemoInfo provides demoInfo(id),
        LocalDemoNavigation provides DemoNavigation(
            previous = previous,
            next = next,
            onOpen = onOpenDemo
        )
    ) {
        when (id) {
            DemoId.Flip -> DemoFlip()
            DemoId.Mirror -> DemoMirror()
            DemoId.Rotate -> DemoRotate()
            DemoId.Scale -> DemoScale()
            DemoId.Offset -> DemoOffset()
            DemoId.Wave -> DemoWave()
            DemoId.Pulse -> DemoPulse()
            DemoId.Signal -> DemoSignal()
            DemoId.Posterize -> DemoPosterize()
            DemoId.Film -> DemoFilm()
            DemoId.Grade -> DemoGrade()
            DemoId.Warp -> DemoWarp()
            DemoId.Prism -> DemoPrism()
            DemoId.Spotlight -> DemoSpotlight()
            DemoId.Beacon -> DemoBeacon()
            DemoId.Composite -> DemoComposite()
            DemoId.Frame -> DemoFrame()
            DemoId.Corner -> DemoCorner()
            DemoId.Reveal -> DemoReveal()
            DemoId.Sweep -> DemoSweep()
            DemoId.Glitch -> DemoGlitch()
            DemoId.Radar -> DemoRadar()
            DemoId.Halo -> DemoHalo()
            DemoId.Circuit -> DemoCircuit()
            DemoId.Sigil -> DemoSigil()
            DemoId.Duotone -> DemoDuotone()
        }
    }
}
