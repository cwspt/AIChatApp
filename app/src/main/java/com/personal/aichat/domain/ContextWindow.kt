package com.personal.aichat.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MessageOverheadTokens = 8
private const val AttachmentOverheadTokens = 24

data class ContextCapacity(
  val windowTokens: Int?,
  val usedTokens: Int,
  val reservedOutputTokens: Int,
  val remainingTokens: Int?,
  val usedPercent: Int?,
  val status: ContextCapacityStatus,
  val hasSummary: Boolean
)

enum class ContextCapacityStatus {
  UNKNOWN,
  OK,
  WARNING,
  CRITICAL
}

data class ContextCompressionResult(
  val compressed: Boolean,
  val summary: String,
  val cutoffMessageId: String?,
  val estimatedTokensBefore: Int,
  val estimatedTokensAfter: Int
)

fun parseContextWindowTokensInput(input: String): Int? {
  val normalized = input
    .trim()
    .replace("_", "")
    .replace(",", "")
    .replace(" ", "")
    .lowercase()
  if (normalized.isBlank()) return null

  val multiplier = when {
    normalized.endsWith("k") -> 1_000L
    normalized.endsWith("m") -> 1_000_000L
    else -> 1L
  }
  val numberPart = if (multiplier == 1L) normalized else normalized.dropLast(1)
  if (numberPart.isBlank()) return null

  val number = numberPart.toDoubleOrNull() ?: return null
  if (number <= 0) return null
  val tokens = (number * multiplier).toLong()
  return tokens.takeIf { it in 1..Int.MAX_VALUE }?.toInt()
}

object KnownContextWindows {
  fun resolve(provider: ChatProviderConfig, model: String): Int? {
    provider.contextWindowTokensOverride?.takeIf { it > 0 }?.let { return it }
    val normalized = model.lowercase().trim()
    if (normalized.isBlank()) return null
    return when {
      normalized.contains("gpt-4.1") -> 1_000_000
      normalized.contains("gpt-4o") -> 128_000
      normalized.contains("o3") || normalized.contains("o4") -> 200_000
      normalized.contains("deepseek-v4") -> 1_000_000
      normalized == "deepseek-chat" || normalized == "deepseek-reasoner" -> 1_000_000
      normalized.contains("claude-3-5") || normalized.contains("claude-3.5") -> 200_000
      normalized.contains("claude-3-7") || normalized.contains("claude-3.7") -> 200_000
      normalized.contains("gemini-1.5-pro") -> 2_000_000
      normalized.contains("gemini-1.5-flash") -> 1_000_000
      normalized.contains("gemini-2") -> 1_000_000
      else -> null
    }
  }
}

object ContextTokenEstimator {
  fun estimateMessages(messages: List<ChatMessage>): Int {
    return messages.sumOf { estimateMessage(it.content, it.attachments) + MessageOverheadTokens }
  }

  fun estimateGroupMessages(messages: List<GroupChatMessage>): Int {
    return messages.sumOf { estimateMessage(it.content, it.attachments) + MessageOverheadTokens }
  }

  fun estimateMessage(content: String, attachments: List<ChatAttachment> = emptyList()): Int {
    val textTokens = estimateText(content)
    val attachmentTokens = attachments.sumOf { attachment ->
      val sizeCost = if (attachment.isImage) {
        max(1_200, ceil(attachment.payloadSizeBytes / 768.0).toInt())
      } else {
        ceil(attachment.payloadSizeBytes / 4_096.0).toInt()
      }
      AttachmentOverheadTokens + sizeCost
    }
    return textTokens + attachmentTokens
  }

  fun estimateText(text: String): Int {
    if (text.isBlank()) return 0
    var asciiRun = 0
    var tokens = 0
    fun flushAscii() {
      if (asciiRun > 0) {
        tokens += ceil(asciiRun / 4.0).toInt()
        asciiRun = 0
      }
    }
    text.forEach { char ->
      when {
        char.isWhitespace() -> flushAscii()
        isCjkLike(char) -> {
          flushAscii()
          tokens += 1
        }
        char.code > 0xFFFF -> {
          flushAscii()
          tokens += 2
        }
        char.isLetterOrDigit() -> asciiRun += 1
        else -> {
          flushAscii()
          tokens += 1
        }
      }
    }
    flushAscii()
    return max(1, tokens)
  }

  private fun isCjkLike(char: Char): Boolean {
    val block = Character.UnicodeBlock.of(char)
    return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
      block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
      block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
      block == Character.UnicodeBlock.HIRAGANA ||
      block == Character.UnicodeBlock.KATAKANA ||
      block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
      block == Character.UnicodeBlock.HANGUL_JAMO ||
      block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
  }
}

fun responseReserveTokens(windowTokens: Int): Int {
  return min(8_192, max(2_048, windowTokens / 20))
}

fun contextCapacity(
  windowTokens: Int?,
  usedTokens: Int,
  hasSummary: Boolean
): ContextCapacity {
  if (windowTokens == null || windowTokens <= 0) {
    return ContextCapacity(
      windowTokens = null,
      usedTokens = usedTokens,
      reservedOutputTokens = 0,
      remainingTokens = null,
      usedPercent = null,
      status = ContextCapacityStatus.UNKNOWN,
      hasSummary = hasSummary
    )
  }
  val reserve = responseReserveTokens(windowTokens)
  val total = usedTokens + reserve
  val remaining = (windowTokens - total).coerceAtLeast(0)
  val percent = ((total.toDouble() / windowTokens.toDouble()) * 100.0).roundToInt().coerceIn(0, 999)
  val status = when {
    percent >= 85 -> ContextCapacityStatus.CRITICAL
    percent >= 75 -> ContextCapacityStatus.WARNING
    else -> ContextCapacityStatus.OK
  }
  return ContextCapacity(
    windowTokens = windowTokens,
    usedTokens = usedTokens,
    reservedOutputTokens = reserve,
    remainingTokens = remaining,
    usedPercent = percent,
    status = status,
    hasSummary = hasSummary
  )
}
