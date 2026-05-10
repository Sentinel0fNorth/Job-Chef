package com.jobchef.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBoardroomSlate,
    onPrimary = SurfaceWhite,
    secondary = SecondaryCoolSteelGrey,
    onSecondary = TextPrimaryDark,
    background = BackgroundCream,
    onBackground = TextPrimaryDark,
    surface = SurfaceWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCreamDeep,
    onSurfaceVariant = TextSecondaryGray
)

@Composable
fun JobMatcherTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
