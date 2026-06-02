package com.personal.aichat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.AiBot
import java.util.Locale

internal data class BotBubblePalette(
  val key: String,
  val label: String,
  val lightContainer: Color,
  val lightContent: Color,
  val lightAccent: Color,
  val darkContainer: Color,
  val darkContent: Color,
  val darkAccent: Color
)

internal data class BotBubbleColors(
  val key: String,
  val label: String,
  val container: Color,
  val content: Color,
  val accent: Color
)

internal data class UserBubbleColors(
  val container: Color,
  val content: Color,
  val metadata: Color
)

internal val BotBubblePalettes = listOf(
  BotBubblePalette("TEAL", "青绿", Color(0xFFD2F4EA), Color(0xFF073B32), Color(0xFF00866E), Color(0xFF163731), Color(0xFFE0FFF6), Color(0xFF46D3BA)),
  BotBubblePalette("BLUE", "蓝", Color(0xFFD8E9FF), Color(0xFF062B55), Color(0xFF1E6FD9), Color(0xFF162D47), Color(0xFFE5F1FF), Color(0xFF6EA8FF)),
  BotBubblePalette("PURPLE", "紫", Color(0xFFE8DDFF), Color(0xFF32105D), Color(0xFF7B43D6), Color(0xFF322845), Color(0xFFF0E8FF), Color(0xFFB994FF)),
  BotBubblePalette("ROSE", "玫红", Color(0xFFFFD9E6), Color(0xFF5F0B2D), Color(0xFFD43D75), Color(0xFF442632), Color(0xFFFFE4EE), Color(0xFFFF8BB4)),
  BotBubblePalette("ORANGE", "橙", Color(0xFFFFE1C7), Color(0xFF542600), Color(0xFFD66A00), Color(0xFF443021), Color(0xFFFFE9D6), Color(0xFFFFA857)),
  BotBubblePalette("GOLD", "金", Color(0xFFFFEDB5), Color(0xFF4B3500), Color(0xFFB98700), Color(0xFF42371E), Color(0xFFFFF0BE), Color(0xFFFFD35D)),
  BotBubblePalette("INDIGO", "靛蓝", Color(0xFFDEE3FF), Color(0xFF161F61), Color(0xFF4C5DD9), Color(0xFF252A49), Color(0xFFE8EBFF), Color(0xFF8FA0FF)),
  BotBubblePalette("CYAN", "湖蓝", Color(0xFFCFF3FF), Color(0xFF003847), Color(0xFF0089A8), Color(0xFF173642), Color(0xFFE2F8FF), Color(0xFF54D8F4))
)

private val AutoBotBubblePalette = BotBubblePalette(
  key = "AUTO",
  label = "自动",
  lightContainer = Color(0xFFE6ECE9),
  lightContent = Color(0xFF1C2623),
  lightAccent = Color(0xFF5B6F68),
  darkContainer = Color(0xFF293230),
  darkContent = Color(0xFFE8F0ED),
  darkAccent = Color(0xFF9CB0AA)
)

internal fun resolvedBotBubbleColorKey(botId: String, requestedKey: String): String {
  val explicitKey = requestedKey.trim().uppercase(Locale.ROOT)
  if (explicitKey != "AUTO" && BotBubblePalettes.any { it.key == explicitKey }) return explicitKey
  if (BotBubblePalettes.isEmpty()) return "AUTO"
  val hash = botId.fold(0) { acc, char -> (acc * 31 + char.code) and Int.MAX_VALUE }
  return BotBubblePalettes[hash % BotBubblePalettes.size].key
}

internal fun botAvatarLabel(name: String): String {
  val clean = name.trim()
  if (clean.isBlank()) return "AI"
  val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
  val initials = if (words.size >= 2) {
    words.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
  } else {
    clean.take(2).uppercase(Locale.getDefault())
  }
  return initials.ifBlank { "AI" }
}

internal fun botIdentityCode(seed: String?): String {
  val clean = seed?.trim().orEmpty()
  if (clean.isBlank()) return "#BOT"
  val hash = clean.fold(0) { acc, char -> (acc * 31 + char.code) and Int.MAX_VALUE }
  return "#${(hash and 0xFFFF).toString(16).uppercase(Locale.ROOT).padStart(4, '0')}"
}

@Composable
internal fun botBubbleColors(bot: AiBot?): BotBubbleColors {
  val requestedKey = bot?.bubbleColorKey ?: "AUTO"
  val key = bot?.let { resolvedBotBubbleColorKey(it.id, requestedKey) } ?: "AUTO"
  val palette = BotBubblePalettes.firstOrNull { it.key == key } ?: AutoBotBubblePalette
  val dark = isDarkThemeColors()
  return BotBubbleColors(
    key = palette.key,
    label = palette.label,
    container = if (dark) palette.darkContainer else palette.lightContainer,
    content = if (dark) palette.darkContent else palette.lightContent,
    accent = if (dark) palette.darkAccent else palette.lightAccent
  )
}

internal fun markdownColorsForBotBubble(colors: BotBubbleColors): MarkdownRenderColors {
  return MarkdownRenderColors(
    content = colors.content,
    muted = colors.content.copy(alpha = 0.72f),
    blockContainer = mixColors(colors.container, Color.Black, 0.12f),
    blockHeader = mixColors(colors.container, colors.accent, 0.18f),
    border = colors.accent.copy(alpha = 0.42f),
    divider = colors.accent.copy(alpha = 0.34f)
  )
}

@Composable
internal fun BotIdentityAvatar(
  label: String,
  colors: BotBubbleColors,
  modifier: Modifier = Modifier
) {
  Surface(
    color = colors.accent,
    contentColor = readableTextOn(colors.accent),
    shape = RoundedCornerShape(999.dp),
    modifier = modifier
      .size(30.dp)
      .border(1.dp, colors.content.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1
      )
    }
  }
}

private fun readableTextOn(color: Color): Color {
  val luminance = (0.2126f * color.red) + (0.7152f * color.green) + (0.0722f * color.blue)
  return if (luminance > 0.56f) Color(0xFF111111) else Color.White
}

internal fun mixColors(base: Color, overlay: Color, overlayAlpha: Float): Color {
  val alpha = overlayAlpha.coerceIn(0f, 1f)
  val inverse = 1f - alpha
  return Color(
    red = base.red * inverse + overlay.red * alpha,
    green = base.green * inverse + overlay.green * alpha,
    blue = base.blue * inverse + overlay.blue * alpha,
    alpha = base.alpha
  )
}

@Composable
internal fun userBubbleColors(): UserBubbleColors {
  val isDark = isDarkThemeColors()
  val content = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
  return UserBubbleColors(
    container = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
    content = content,
    metadata = content.copy(alpha = if (isDark) 0.72f else 0.78f)
  )
}

@Composable
internal fun isDarkThemeColors(): Boolean {
  val onBackground = MaterialTheme.colorScheme.onBackground
  return onBackground.red > 0.75f &&
    onBackground.green > 0.75f &&
    onBackground.blue > 0.75f
}
