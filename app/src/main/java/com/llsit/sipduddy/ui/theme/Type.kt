package com.llsit.sipduddy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.llsit.sipduddy.R

private val KanitFontFamily = FontFamily(
    Font(R.font.kanit_light, FontWeight.Light),
    Font(R.font.kanit_regular, FontWeight.Normal),
    Font(R.font.kanit_bold, FontWeight.Bold),
)

private val DefaultTypography = Typography()

// Keep Material 3 sizes and line heights; bundle fonts for offline use.
val Typography = with(DefaultTypography) {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Normal),
        bodyMedium = bodyMedium.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Normal),
        bodySmall = bodySmall.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Light),
        labelLarge = labelLarge.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Normal),
        labelMedium = labelMedium.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Light),
        labelSmall = labelSmall.copy(fontFamily = KanitFontFamily, fontWeight = FontWeight.Normal),
    )
}
