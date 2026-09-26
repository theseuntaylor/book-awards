package com.theseuntaylor.bookawards.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BookAwardsTheme(
    dynamicColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = dynamicColorScheme
        ?: if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme, content = content)
}
