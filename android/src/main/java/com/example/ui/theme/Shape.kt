package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // extraSmall: chips, small badges
    extraSmall = RoundedCornerShape(4.dp),
    // small: text fields, snackbars, context menus
    small      = RoundedCornerShape(8.dp),
    // medium: cards, dialogs — this is the most used
    medium     = RoundedCornerShape(16.dp),
    // large: bottom sheets, navigation drawers
    large      = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    // extraLarge: full-screen modals
    extraLarge = RoundedCornerShape(28.dp),
)
