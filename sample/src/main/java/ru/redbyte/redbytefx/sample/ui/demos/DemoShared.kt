package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.ShaderProgram

@Composable
internal fun rememberGeneratedAgsl(effect: ShaderProgram): String =
    remember(effect) { effect.agslSource() }

