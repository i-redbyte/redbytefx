package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.FxEffect

@Composable
internal fun rememberGeneratedAgsl(effect: FxEffect): String =
    remember(effect) { effect.agslSource() }

