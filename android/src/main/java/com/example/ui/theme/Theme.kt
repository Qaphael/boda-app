package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val RydDarkColorScheme = darkColorScheme(
    primary                = RydPrimary,
    onPrimary              = RydOnPrimary,
    primaryContainer       = RydPrimaryContainer,
    onPrimaryContainer     = RydOnPrimaryContainer,
    secondary              = RydSecondary,
    onSecondary            = RydOnSecondary,
    secondaryContainer     = RydSecondaryContainer,
    onSecondaryContainer   = RydOnSecondaryContainer,
    tertiary               = RydTertiary,
    onTertiary             = RydOnTertiary,
    tertiaryContainer      = RydTertiaryContainer,
    onTertiaryContainer    = RydOnTertiaryContainer,
    error                  = RydError,
    onError                = RydOnError,
    errorContainer         = RydErrorContainer,
    onErrorContainer       = RydOnErrorContainer,
    background             = DarkBackground,
    onBackground           = DarkOnBackground,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVariant,
    surfaceContainer           = DarkSurfaceContainer,
    surfaceContainerHigh       = DarkSurfaceContainerHigh,
    surfaceContainerHighest    = DarkSurfaceContainerHighest,
)

private val RydLightColorScheme = lightColorScheme(
    primary                = RydPrimary,
    onPrimary              = RydOnPrimary,
    primaryContainer       = RydPrimaryContainer,
    onPrimaryContainer     = RydOnPrimaryContainer,
    secondary              = RydSecondary,
    onSecondary            = RydOnSecondary,
    secondaryContainer     = RydSecondaryContainer,
    onSecondaryContainer   = RydOnSecondaryContainer,
    tertiary               = RydTertiary,
    onTertiary             = RydOnTertiary,
    tertiaryContainer      = RydTertiaryContainer,
    onTertiaryContainer    = RydOnTertiaryContainer,
    error                  = RydError,
    onError                = RydOnError,
    errorContainer         = RydErrorContainer,
    onErrorContainer       = RydOnErrorContainer,
    background             = LightBackground,
    onBackground           = LightOnBackground,
    surface                = LightSurface,
    onSurface              = LightOnSurface,
    surfaceVariant         = LightSurfaceVariant,
    onSurfaceVariant       = LightOnSurfaceVariant,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVariant,
    surfaceContainer           = LightSurfaceContainer,
    surfaceContainerHigh       = LightSurfaceContainerHigh,
    surfaceContainerHighest    = LightSurfaceContainerHighest,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> RydDarkColorScheme
        else -> RydLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
