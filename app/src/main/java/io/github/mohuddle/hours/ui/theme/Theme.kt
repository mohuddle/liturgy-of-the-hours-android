package io.github.mohuddle.hours.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HoursBackground = Color(0xFF1A1612)
val HoursForeground = Color(0xFFF4F1EA)
val HoursAccent = Color(0xFFD4B15A)
val HoursMuted = Color(0xFF8C8276)

private val HoursColors = darkColorScheme(
    background = HoursBackground,
    surface = HoursBackground,
    onBackground = HoursForeground,
    onSurface = HoursForeground,
    primary = HoursAccent,
    onPrimary = HoursBackground,
)

@Composable
fun HoursTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HoursColors, content = content)
}
