package com.llsit.sipduddy.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFF9EF)
val Ink = Color(0xFF244B60)
val Blue = Color(0xFF3B8CAE)
val PaleBlue = Color(0xFFE1F2F8)

@Composable
fun SipDuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue, onPrimary = Color.White, primaryContainer = PaleBlue, onPrimaryContainer = Ink,
            secondary = Color(0xFF7D6848), secondaryContainer = Color(0xFFFFE8BD),
            background = Cream, onBackground = Ink, surface = Cream, onSurface = Ink,
            surfaceVariant = Color(0xFFF1EDE3), onSurfaceVariant = Color(0xFF61717A),
            outline = Color(0xFF73858D), outlineVariant = Color(0xFFCCDADF),
            surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEAF3F4),
            surfaceContainer = Color(0xFFF1EDE3), surfaceContainerHigh = Color(0xFFE8ECE7),
            surfaceContainerHighest = Color(0xFFE1E7E5),
        ),
        typography = Typography,
        content = content,
    )
}
