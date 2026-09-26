package com.theseuntaylor.bookawards.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BookAwardsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme,
        content = content
    )
}
