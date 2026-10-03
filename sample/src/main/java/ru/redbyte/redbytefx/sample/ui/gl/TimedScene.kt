package ru.redbyte.redbytefx.sample.ui.gl

import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.Uniform

internal class TimedScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val time: Uniform<Flt<High>>,
    val aspect: Uniform<Flt<High>>,
)

internal fun GlFrame.bind(scene: TimedScene) {
    runtime.set(scene.time, seconds)
    runtime.set(scene.aspect, aspect)
}
