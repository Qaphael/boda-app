package com.example.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun BodaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val m = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)

    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = m,
            shape = MaterialTheme.shapes.medium,
        ) { content() }
    } else {
        ElevatedCard(
            modifier = m,
            shape = MaterialTheme.shapes.medium,
        ) { content() }
    }
}
