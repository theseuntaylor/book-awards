package com.theseuntaylor.bookawards.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.theseuntaylor.bookawards.resources.Res
import com.theseuntaylor.bookawards.resources.body_medium
import com.theseuntaylor.bookawards.resources.body_regular
import com.theseuntaylor.bookawards.resources.heading_medium
import com.theseuntaylor.bookawards.resources.heading_regular
import org.jetbrains.compose.resources.Font

// Same role split as Material Theme Builder: display, headline, and title roles use the heading font; body and label roles use the body font.
@Composable
fun bookAwardsTypography(): Typography {
    val heading = FontFamily(
        Font(Res.font.heading_regular, FontWeight.Normal),
        Font(Res.font.heading_medium, FontWeight.Medium)
    )
    val body = FontFamily(
        Font(Res.font.body_regular, FontWeight.Normal),
        Font(Res.font.body_medium, FontWeight.Medium)
    )
    val baseline = Typography()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = heading),
        displayMedium = baseline.displayMedium.copy(fontFamily = heading),
        displaySmall = baseline.displaySmall.copy(fontFamily = heading),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = heading),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = heading),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = heading),
        titleLarge = baseline.titleLarge.copy(fontFamily = heading),
        titleMedium = baseline.titleMedium.copy(fontFamily = heading),
        titleSmall = baseline.titleSmall.copy(fontFamily = heading),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = body),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = body),
        bodySmall = baseline.bodySmall.copy(fontFamily = body),
        labelLarge = baseline.labelLarge.copy(fontFamily = body),
        labelMedium = baseline.labelMedium.copy(fontFamily = body),
        labelSmall = baseline.labelSmall.copy(fontFamily = body)
    )
}
