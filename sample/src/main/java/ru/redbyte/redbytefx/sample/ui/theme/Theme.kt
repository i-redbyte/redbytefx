package ru.redbyte.redbytefx.sample.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CyberTypography: Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.6.sp
    )
)

private val CyberShapes: Shapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private fun cyberDarkScheme(): ColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = VoidBlack,
    primaryContainer = SurfaceThree,
    onPrimaryContainer = SoftTerminalText,
    secondary = NeonMint,
    onSecondary = VoidBlack,
    secondaryContainer = SurfaceTwo,
    onSecondaryContainer = SoftTerminalText,
    tertiary = AcidLime,
    onTertiary = VoidBlack,
    tertiaryContainer = SurfaceOne,
    onTertiaryContainer = SoftTerminalText,
    background = VoidBlack,
    onBackground = SoftTerminalText,
    surface = SurfaceZero,
    onSurface = SoftTerminalText,
    surfaceVariant = SurfaceTwo,
    onSurfaceVariant = MutedTerminal,
    surfaceContainer = SurfaceOne,
    surfaceContainerHigh = SurfaceTwo,
    surfaceContainerHighest = SurfaceThree,
    outline = GridLine.copy(alpha = 0.34f),
    outlineVariant = GridLine.copy(alpha = 0.18f),
    error = ReactorRed,
    onError = Color.White,
    errorContainer = ReactorRed.copy(alpha = 0.16f),
    onErrorContainer = Color(0xFFFFD7DC)
)

private fun cyberLightScheme(): ColorScheme = lightColorScheme(
    primary = MatrixGreen,
    onPrimary = VoidBlack,
    primaryContainer = Color(0xFF143625),
    onPrimaryContainer = SoftTerminalText,
    secondary = NeonMint,
    onSecondary = VoidBlack,
    secondaryContainer = Color(0xFF1B4230),
    onSecondaryContainer = SoftTerminalText,
    tertiary = AcidLime,
    onTertiary = VoidBlack,
    tertiaryContainer = Color(0xFF2B4724),
    onTertiaryContainer = SoftTerminalText,
    background = DeepGrid,
    onBackground = SoftTerminalText,
    surface = SurfaceZero,
    onSurface = SoftTerminalText,
    surfaceVariant = SurfaceTwo,
    onSurfaceVariant = MutedTerminal,
    surfaceContainer = SurfaceOne,
    surfaceContainerHigh = SurfaceTwo,
    surfaceContainerHighest = SurfaceThree,
    outline = GridLine.copy(alpha = 0.34f),
    outlineVariant = GridLine.copy(alpha = 0.18f),
    error = ReactorRed,
    onError = Color.White,
    errorContainer = ReactorRed.copy(alpha = 0.16f),
    onErrorContainer = Color(0xFFFFD7DC)
)

@Composable
fun RedByteFxSampleTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) cyberDarkScheme() else cyberLightScheme()

    MaterialTheme(
        colorScheme = scheme,
        typography = CyberTypography,
        shapes = CyberShapes,
        content = content
    )
}
