package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors for Gulu
val GuluYellow = Color(0xFFFDB913) // MTN Yellow
val GuluRed = Color(0xFFE4002B)    // Airtel Red
val GuluBlue = Color(0xFF0061A4)   // Gulu Primary Blue

// Global state to track dynamic dark/light mode
var isAppInDarkMode by mutableStateOf(true)

val AppBackground: Color
    @Composable get() = if (isAppInDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)

val AppCardBackground: Color
    @Composable get() = if (isAppInDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)

val AppTextPrimary: Color
    @Composable get() = if (isAppInDarkMode) Color.White else Color(0xFF0F172A)

val AppTextSecondary: Color
    @Composable get() = if (isAppInDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

val AppBorder: Color
    @Composable get() = if (isAppInDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

val MtnYellow = Color(0xFF0061A4) // M3 Primary Blue
val AirtelRed = Color(0xFFE4002B)
val DarkSlate = Color(0xFFFDFBFF) // Clean light background
val CardSlate = Color(0xFFFFFFFF) // Clean card background
val LightSlate = Color(0xFFD1E4FF) // Blue container tint

val Purple80 = Color(0xFF0061A4)
val PurpleGrey80 = Color(0xFF64748B)
val Pink80 = Color(0xFFE4002B)

val Purple40 = Color(0xFF0061A4)
val PurpleGrey40 = Color(0xFFFDFBFF)
val Pink40 = Color(0xFFE4002B)

