package ru.redbyte.redbytefx.sample.ui

import androidx.compose.runtime.Composable
import ru.redbyte.redbytefx.sample.model.DemoId

@Composable
fun DemoScreen(id: DemoId) {
    when (id) {
        DemoId.Flip -> DemoFlip()
        DemoId.Mirror -> DemoMirror()
        DemoId.Rotate -> DemoRotate()
        DemoId.Scale -> DemoScale()
        DemoId.Offset -> DemoOffset()
    }
}