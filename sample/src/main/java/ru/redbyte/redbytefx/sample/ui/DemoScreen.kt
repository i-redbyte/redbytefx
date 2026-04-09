package ru.redbyte.redbytefx.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.model.demoInfo

@Composable
fun DemoScreen(id: DemoId) {
    CompositionLocalProvider(LocalDemoInfo provides demoInfo(id)) {
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
            DemoId.Reveal -> DemoReveal()
            DemoId.Duotone -> DemoDuotone()
        }
    }
}
