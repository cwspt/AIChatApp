package com.personal.aichat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.personal.aichat.domain.AppSettings
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette

private val MossLightColors = lightColorScheme(
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

private val MossDarkColors = darkColorScheme(
  primary = ColorMossDarkPrimary,
  secondary = ColorMossDarkSecondary,
  background = ColorDarkBackground,
  surface = ColorDarkSurface,
  surfaceVariant = ColorDarkSurfaceVariant,
  primaryContainer = ColorDarkPrimaryContainer,
  secondaryContainer = ColorDarkSecondaryContainer,
  onPrimary = ColorDarkButtonText,
  onSecondary = ColorDarkButtonText,
  onPrimaryContainer = ColorDarkOnSurface,
  onSecondaryContainer = ColorDarkOnSurface,
  onBackground = ColorDarkOnSurface,
  onSurface = ColorDarkOnSurface,
  onSurfaceVariant = ColorDarkMuted,
  error = ErrorRed
)

private val OceanLightColors = lightColorScheme(
  primary = OceanPrimary,
  secondary = OceanSecondary,
  background = OceanBackground,
  surface = OceanSurface,
  surfaceVariant = OceanSurfaceVariant,
  onPrimary = Paper,
  onSecondary = Paper,
  onBackground = Ink,
  onSurface = Ink,
  onSurfaceVariant = Muted,
  error = ErrorRed
)

private val OceanDarkColors = darkColorScheme(
  primary = OceanDarkPrimary,
  secondary = OceanDarkSecondary,
  background = ColorDarkBackground,
  surface = ColorDarkSurface,
  surfaceVariant = ColorDarkSurfaceVariant,
  primaryContainer = ColorDarkPrimaryContainer,
  secondaryContainer = ColorDarkSecondaryContainer,
  onPrimary = ColorDarkButtonText,
  onSecondary = ColorDarkButtonText,
  onPrimaryContainer = ColorDarkOnSurface,
  onSecondaryContainer = ColorDarkOnSurface,
  onBackground = ColorDarkOnSurface,
  onSurface = ColorDarkOnSurface,
  onSurfaceVariant = ColorDarkMuted,
  error = ErrorRed
)

private val SakuraLightColors = lightColorScheme(
  primary = SakuraPrimary,
  secondary = SakuraSecondary,
  background = SakuraBackground,
  surface = SakuraSurface,
  surfaceVariant = SakuraSurfaceVariant,
  onPrimary = Paper,
  onSecondary = Paper,
  onBackground = Ink,
  onSurface = Ink,
  onSurfaceVariant = Muted,
  error = ErrorRed
)

private val SakuraDarkColors = darkColorScheme(
  primary = SakuraDarkPrimary,
  secondary = SakuraDarkSecondary,
  background = ColorDarkBackground,
  surface = ColorDarkSurface,
  surfaceVariant = ColorDarkSurfaceVariant,
  primaryContainer = ColorDarkPrimaryContainer,
  secondaryContainer = ColorDarkSecondaryContainer,
  onPrimary = ColorDarkButtonText,
  onSecondary = ColorDarkButtonText,
  onPrimaryContainer = ColorDarkOnSurface,
  onSecondaryContainer = ColorDarkOnSurface,
  onBackground = ColorDarkOnSurface,
  onSurface = ColorDarkOnSurface,
  onSurfaceVariant = ColorDarkMuted,
  error = ErrorRed
)

private val AmberLightColors = lightColorScheme(
  primary = AmberPrimary,
  secondary = AmberSecondary,
  background = AmberBackground,
  surface = AmberSurface,
  surfaceVariant = AmberSurfaceVariant,
  onPrimary = Paper,
  onSecondary = Paper,
  onBackground = Ink,
  onSurface = Ink,
  onSurfaceVariant = Muted,
  error = ErrorRed
)

private val AmberDarkColors = darkColorScheme(
  primary = AmberDarkPrimary,
  secondary = AmberDarkSecondary,
  background = ColorDarkBackground,
  surface = ColorDarkSurface,
  surfaceVariant = ColorDarkSurfaceVariant,
  primaryContainer = ColorDarkPrimaryContainer,
  secondaryContainer = ColorDarkSecondaryContainer,
  onPrimary = ColorDarkButtonText,
  onSecondary = ColorDarkButtonText,
  onPrimaryContainer = ColorDarkOnSurface,
  onSecondaryContainer = ColorDarkOnSurface,
  onBackground = ColorDarkOnSurface,
  onSurface = ColorDarkOnSurface,
  onSurfaceVariant = ColorDarkMuted,
  error = ErrorRed
)

@Composable
fun AIChatTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
  val colors = when (settings.palette) {
    AppThemePalette.MOSS -> if (settings.themeMode == AppThemeMode.DARK) MossDarkColors else MossLightColors
    AppThemePalette.OCEAN -> if (settings.themeMode == AppThemeMode.DARK) OceanDarkColors else OceanLightColors
    AppThemePalette.SAKURA -> if (settings.themeMode == AppThemeMode.DARK) SakuraDarkColors else SakuraLightColors
    AppThemePalette.AMBER -> if (settings.themeMode == AppThemeMode.DARK) AmberDarkColors else AmberLightColors
  }
  MaterialTheme(
    colorScheme = colors,
    typography = scaledTypography(settings.fontScale),
    content = content
  )
}

private fun scaledTypography(scale: Float) = AIChatTypography.copy(
  bodySmall = AIChatTypography.bodySmall.copy(fontSize = 12.sp * scale, fontFamily = FontFamily.Default),
  bodyMedium = AIChatTypography.bodyMedium.copy(fontSize = 14.sp * scale, fontFamily = FontFamily.Default),
  bodyLarge = AIChatTypography.bodyLarge.copy(fontSize = 16.sp * scale, fontFamily = FontFamily.Default),
  titleSmall = AIChatTypography.titleSmall.copy(fontSize = 14.sp * scale),
  titleMedium = AIChatTypography.titleMedium.copy(fontSize = 16.sp * scale),
  titleLarge = AIChatTypography.titleLarge.copy(fontSize = 22.sp * scale),
  headlineSmall = AIChatTypography.headlineSmall.copy(fontSize = 24.sp * scale)
)
