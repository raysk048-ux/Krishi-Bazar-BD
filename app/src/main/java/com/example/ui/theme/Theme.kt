package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val KrishiLightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = LeafSecondary,
    onSecondary = LeafOnSecondary,
    secondaryContainer = LeafSecondaryContainer,
    onSecondaryContainer = LeafOnSecondaryContainer,
    tertiary = HarvestGoldTertiary,
    onTertiary = HarvestOnTertiary,
    tertiaryContainer = HarvestTertiaryContainer,
    onTertiaryContainer = HarvestOnTertiaryContainer,
    background = CrispBackground,
    onBackground = DeepForestInk,
    surface = CrispSurface,
    onSurface = DeepForestInk,
    surfaceVariant = SoftMintSurfaceVariant,
    onSurfaceVariant = MutedForestInk,
    outline = SoftBorderOutline,
    error = AlertRed,
    errorContainer = AlertRedContainer,
    onErrorContainer = OnAlertRedContainer
)

private val KrishiDarkColors = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkEmeraldPrimary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = HarvestGoldTertiary,
    onTertiary = HarvestOnTertiary,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkSurfaceVariant,
    error = AlertRed,
    errorContainer = AlertRedContainer,
    onErrorContainer = OnAlertRedContainer
)

val KrishiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KrishiBazarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KrishiDarkColors
        else -> KrishiLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KrishiTypography,
        shapes = KrishiShapes,
        content = content
    )
}
