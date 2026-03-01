package ru.redbyte.redbytefx.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private fun lightScheme(): ColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,
    background = LightBg,
    surface = LightSurface
)

private fun darkScheme(): ColorScheme = darkColorScheme(
    primary = GreenTertiary,
    secondary = GreenPrimary,
    tertiary = GreenSecondary,
    background = DarkBg,
    surface = DarkSurface
)

@Composable
fun RedByteFxSampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) darkScheme() else lightScheme()

    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        content = content
    )
}