package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GuluYellow,
    secondary = GuluBlue,
    tertiary = GuluRed,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GuluBlue,
    secondary = GuluYellow,
    tertiary = GuluRed,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F172A),
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disabled for performance optimization on budget devices
  content: @Composable () -> Unit,
) {
  // Sync global dark theme state
  isAppInDarkMode = darkTheme

  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
