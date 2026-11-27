package com.company.voltmate.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = WhiteCanvas,
    primaryContainer = TintGreen,
    onPrimaryContainer = DeepGreen,
    secondary = DeepGreen,
    onSecondary = WhiteCanvas,
    tertiary = AccentGreen,
    background = WhiteCanvas,
    onBackground = InkPrimary,
    surface = SurfaceBg,
    onSurface = InkPrimary,
    surfaceVariant = WhiteCanvas,
    onSurfaceVariant = InkSecondary,
    surfaceContainer = WhiteCanvas,
    surfaceContainerLow = SurfaceBg,
    surfaceContainerLowest = WhiteCanvas,
    surfaceContainerHigh = WhiteCanvas,
    surfaceContainerHighest = SurfaceBg,
    outline = BorderHairline,
    outlineVariant = BorderHairline,
    error = ErrorRed,
    onError = WhiteCanvas
)

val VoltMateShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp), // 14dp card radius
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp) // 56dp pill button radius
)

@Composable
fun VoltMateTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = WhiteCanvas.toArgb()
                window.navigationBarColor = WhiteCanvas.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoltMateTypography,
        shapes = VoltMateShapes,
        content = content
    )
}
