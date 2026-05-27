package com.personal.aichat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
  primary = Moss,
  secondary = Copper,
  background = Paper,
  surface = Cloud,
  surfaceVariant = Mist,
  onPrimary = Paper,
  onSecondary = Paper,
  onBackground = Ink,
  onSurface = Ink,
  onSurfaceVariant = Muted,
  error = ErrorRed
)

@Composable
fun AIChatTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColors,
    typography = AIChatTypography,
    content = content
  )
}
