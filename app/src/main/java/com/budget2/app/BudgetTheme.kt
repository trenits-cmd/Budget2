package com.budget2.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Forest = Color(0xFF0F172A)
internal val Paper = Color(0xFFF4F7FB)
internal val Ink = Color(0xFF102033)
internal val Muted = Color(0xFF62748A)
internal val PaidBackground = Color(0xFFE7F6EC)
internal val UnpaidBackground = Color(0xFFFFE9E6)
internal val PaidTint = Color(0xFF28734A)
internal val UnpaidTint = Color(0xFFB34135)

@Composable
internal fun BudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest,
            onPrimary = Color.White,
            background = Paper,
            surface = Color.White,
            onSurface = Ink,
        ),
        content = content,
    )
}
