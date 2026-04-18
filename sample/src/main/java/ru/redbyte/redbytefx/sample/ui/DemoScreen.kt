package ru.redbyte.redbytefx.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ru.redbyte.redbytefx.sample.model.DemoCatalog
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.demoInfo
import ru.redbyte.redbytefx.sample.model.recommendedFollowUps
import ru.redbyte.redbytefx.sample.ui.demos.DemoAnimatedGradient
import ru.redbyte.redbytefx.sample.ui.demos.DemoCrtTerminal
import ru.redbyte.redbytefx.sample.ui.demos.DemoAurora
import ru.redbyte.redbytefx.sample.ui.demos.DemoBeacon
import ru.redbyte.redbytefx.sample.ui.demos.DemoComposite
import ru.redbyte.redbytefx.sample.ui.demos.DemoCorner
import ru.redbyte.redbytefx.sample.ui.demos.DemoDuotone
import ru.redbyte.redbytefx.sample.ui.demos.DemoFilm
import ru.redbyte.redbytefx.sample.ui.demos.DemoFlip
import ru.redbyte.redbytefx.sample.ui.demos.DemoFrame
import ru.redbyte.redbytefx.sample.ui.demos.DemoGlitch
import ru.redbyte.redbytefx.sample.ui.demos.DemoGrade
import ru.redbyte.redbytefx.sample.ui.demos.DemoHalo
import ru.redbyte.redbytefx.sample.ui.demos.DemoLiquidGlass
import ru.redbyte.redbytefx.sample.ui.demos.DemoMetaballs
import ru.redbyte.redbytefx.sample.ui.demos.DemoMirror
import ru.redbyte.redbytefx.sample.ui.demos.DemoOffset
import ru.redbyte.redbytefx.sample.ui.demos.DemoPhysicsBubble
import ru.redbyte.redbytefx.sample.ui.demos.DemoPosterize
import ru.redbyte.redbytefx.sample.ui.demos.DemoPrism
import ru.redbyte.redbytefx.sample.ui.demos.DemoPulse
import ru.redbyte.redbytefx.sample.ui.demos.DemoRadar
import ru.redbyte.redbytefx.sample.ui.demos.DemoReveal
import ru.redbyte.redbytefx.sample.ui.demos.DemoRotate
import ru.redbyte.redbytefx.sample.ui.demos.DemoScale
import ru.redbyte.redbytefx.sample.ui.demos.DemoSigil
import ru.redbyte.redbytefx.sample.ui.demos.DemoSignal
import ru.redbyte.redbytefx.sample.ui.demos.DemoSpotlight
import ru.redbyte.redbytefx.sample.ui.demos.DemoSweep
import ru.redbyte.redbytefx.sample.ui.demos.DemoTouchRipple
import ru.redbyte.redbytefx.sample.ui.demos.DemoWarp
import ru.redbyte.redbytefx.sample.ui.demos.DemoWave

@Composable
fun DemoScreen(
    id: DemoId,
    onOpenDemo: (DemoId) -> Unit
) {
    val index = DemoCatalog.indexOfFirst { it.id == id }
    val previous = DemoCatalog.getOrNull(index - 1)
    val next = DemoCatalog.getOrNull(index + 1)
    val related = recommendedFollowUps(
        id = id,
        excludeIds = buildSet {
            previous?.id?.let(::add)
            next?.id?.let(::add)
        }
    )

    CompositionLocalProvider(
        LocalDemoInfo provides demoInfo(id),
        LocalDemoNavigation provides DemoNavigation(
            previous = previous,
            next = next,
            related = related,
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
            DemoId.Aurora -> DemoAurora()
            DemoId.LiquidGlass -> DemoLiquidGlass()
            DemoId.AnimatedGradient -> DemoAnimatedGradient()
            DemoId.PhysicsBubble -> DemoPhysicsBubble()
            DemoId.TouchRipple -> DemoTouchRipple()
            DemoId.Metaballs -> DemoMetaballs()
            DemoId.CrtTerminal -> DemoCrtTerminal()
        }
    }
}
