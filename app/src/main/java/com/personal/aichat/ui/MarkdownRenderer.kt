package com.personal.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

private const val UrlAnnotationTag = "markdown_url"

internal data class MarkdownRenderColors(
  val content: Color,
  val muted: Color,
  val blockContainer: Color,
  val blockHeader: Color,
  val border: Color,
  val divider: Color,
  val link: Color? = null
)

@Composable
internal fun MarkdownPreview(
  content: String,
  colors: MarkdownRenderColors? = null,
  interactiveLinks: Boolean = true
) {
  val blocks = remember(content) { parseMarkdownBlocks(content) }
  val linkColor = colors?.link ?: MaterialTheme.colorScheme.primary
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    blocks.forEach { block ->
      when (block) {
        is MarkdownBlock.Code -> Surface(
          color = colors?.blockContainer ?: MaterialTheme.colorScheme.background,
          contentColor = colors?.content ?: MaterialTheme.colorScheme.onSurface,
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = block.text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyMedium
          )
        }
        is MarkdownBlock.Heading -> MarkdownInlineText(
          text = renderInlineMarkdown(block.text, linkColor),
          style = when (block.level) {
            1 -> MaterialTheme.typography.titleLarge
            2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
          },
          fontWeight = FontWeight.SemiBold,
          color = colors?.content ?: Color.Unspecified,
          interactiveLinks = interactiveLinks
        )
        is MarkdownBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(block.marker, color = colors?.muted ?: MaterialTheme.colorScheme.onSurfaceVariant)
          MarkdownInlineText(
            text = renderInlineMarkdown(block.text, linkColor),
            modifier = Modifier.weight(1f),
            color = colors?.content ?: Color.Unspecified,
            interactiveLinks = interactiveLinks
          )
        }
        is MarkdownBlock.BlockQuote -> Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .width(3.dp)
              .fillMaxHeight()
              .background(colors?.divider ?: MaterialTheme.colorScheme.outlineVariant)
          )
          MarkdownInlineText(
            text = renderInlineMarkdown(block.text, linkColor),
            modifier = Modifier.weight(1f),
            color = colors?.muted ?: MaterialTheme.colorScheme.onSurfaceVariant,
            interactiveLinks = interactiveLinks
          )
        }
        is MarkdownBlock.Table -> MarkdownTable(block, colors, interactiveLinks)
        MarkdownBlock.Divider -> Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors?.divider ?: MaterialTheme.colorScheme.outlineVariant)
        )
        is MarkdownBlock.Paragraph -> MarkdownInlineText(
          text = renderInlineMarkdown(block.text, linkColor),
          color = colors?.content ?: Color.Unspecified,
          interactiveLinks = interactiveLinks
        )
      }
    }
  }
}

internal sealed interface MarkdownBlock {
  data class Paragraph(val text: String) : MarkdownBlock
  data class Heading(val level: Int, val text: String) : MarkdownBlock
  data class ListItem(val marker: String, val text: String) : MarkdownBlock
  data class BlockQuote(val text: String) : MarkdownBlock
  data class Code(val text: String) : MarkdownBlock
  data class Table(val rows: List<List<String>>) : MarkdownBlock
  data object Divider : MarkdownBlock
}

@Composable
private fun MarkdownTable(
  table: MarkdownBlock.Table,
  colors: MarkdownRenderColors? = null,
  interactiveLinks: Boolean
) {
  if (table.rows.isEmpty()) return
  val columnCount = table.rows.maxOf { it.size }.coerceAtLeast(1)
  val linkColor = colors?.link ?: MaterialTheme.colorScheme.primary
  Surface(
    color = colors?.blockContainer ?: MaterialTheme.colorScheme.background,
    contentColor = colors?.content ?: MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .padding(8.dp)
        .border(1.dp, colors?.border ?: MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
    ) {
      table.rows.forEachIndexed { rowIndex, row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(
              color = if (rowIndex == 0) colors?.blockHeader ?: MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
              shape = RoundedCornerShape(4.dp)
            )
            .border(0.5.dp, colors?.border ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            .padding(vertical = 0.dp)
        ) {
          repeat(columnCount) { column ->
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(0.5.dp, colors?.border?.copy(alpha = 0.75f) ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
              MarkdownInlineText(
                text = renderInlineMarkdown(row.getOrNull(column).orEmpty(), linkColor),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                color = colors?.content ?: Color.Unspecified,
                interactiveLinks = interactiveLinks
              )
            }
          }
        }
      }
    }
  }
}

internal fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
  val blocks = mutableListOf<MarkdownBlock>()
  val paragraph = StringBuilder()
  val code = StringBuilder()
  val quote = StringBuilder()
  val tableRows = mutableListOf<List<String>>()
  var inCode = false

  fun flushParagraph() {
    val text = paragraph.toString().trim()
    if (text.isNotBlank()) blocks += MarkdownBlock.Paragraph(text)
    paragraph.clear()
  }

  fun flushTable() {
    if (tableRows.isNotEmpty()) {
      blocks += MarkdownBlock.Table(tableRows.toList())
      tableRows.clear()
    }
  }

  fun flushQuote() {
    val text = quote.toString().trimEnd()
    if (text.isNotBlank()) blocks += MarkdownBlock.BlockQuote(text)
    quote.clear()
  }

  markdown.lines().forEach { rawLine ->
    val line = rawLine.trimEnd()
    if (line.trimStart().startsWith("```")) {
      flushQuote()
      if (inCode) {
        blocks += MarkdownBlock.Code(code.toString().trimEnd())
        code.clear()
        inCode = false
      } else {
        flushParagraph()
        inCode = true
      }
      return@forEach
    }

    if (inCode) {
      code.append(rawLine).append('\n')
      return@forEach
    }

    val trimmed = line.trim()
    if (trimmed.startsWith('>')) {
      flushTable()
      flushParagraph()
      if (quote.isNotEmpty()) quote.append('\n')
      quote.append(trimmed.drop(1).trimStart())
      return@forEach
    }
    flushQuote()

    if (trimmed.isBlank()) {
      flushTable()
      flushParagraph()
      return@forEach
    }

    if (isMarkdownDivider(trimmed)) {
      flushTable()
      flushParagraph()
      blocks += MarkdownBlock.Divider
      return@forEach
    }

    if (isMarkdownTableSeparator(trimmed)) {
      if (tableRows.isNotEmpty()) {
        flushParagraph()
      } else if (paragraph.isNotEmpty()) {
        paragraph.append('\n').append(trimmed)
      } else {
        paragraph.append(trimmed)
      }
      return@forEach
    }

    if (looksLikeMarkdownTableRow(trimmed)) {
      if (tableRows.isNotEmpty()) {
        tableRows += parseMarkdownTableRow(trimmed)
        return@forEach
      }
      val paragraphLines = paragraph.lines()
      if (paragraphLines.isNotEmpty() && isMarkdownTableSeparator(paragraphLines.last().trim())) {
        val header = paragraphLines.dropLast(1).lastOrNull()?.trim()
        if (header != null && looksLikeMarkdownTableRow(header)) {
          val body = paragraphLines.dropLast(2).joinToString("\n").trim()
          paragraph.clear()
          if (body.isNotBlank()) blocks += MarkdownBlock.Paragraph(body)
          tableRows += parseMarkdownTableRow(header)
          tableRows += parseMarkdownTableRow(trimmed)
          return@forEach
        }
      }
    }
    flushTable()

    val headingLevel = trimmed.takeWhile { it == '#' }.length
    if (headingLevel in 1..6 && trimmed.getOrNull(headingLevel) == ' ') {
      flushParagraph()
      blocks += MarkdownBlock.Heading(headingLevel, trimmed.drop(headingLevel).trim())
      return@forEach
    }

    val unordered = listOf("- ", "* ", "+ ").firstOrNull { trimmed.startsWith(it) }
    if (unordered != null) {
      flushParagraph()
      val itemText = trimmed.drop(unordered.length).trim()
      val task = Regex("^\\[([ xX])]\\s+(.*)$").matchEntire(itemText)
      blocks += if (task != null) {
        MarkdownBlock.ListItem(
          marker = if (task.groupValues[1].equals("x", ignoreCase = true)) "☑" else "☐",
          text = task.groupValues[2]
        )
      } else {
        MarkdownBlock.ListItem("•", itemText)
      }
      return@forEach
    }

    val orderedMatch = Regex("^(\\d+)[.)]\\s+(.*)$").matchEntire(trimmed)
    if (orderedMatch != null) {
      flushParagraph()
      blocks += MarkdownBlock.ListItem("${orderedMatch.groupValues[1]}.", orderedMatch.groupValues[2])
      return@forEach
    }

    if (paragraph.isNotEmpty()) paragraph.append('\n')
    paragraph.append(trimmed)
  }

  if (inCode) blocks += MarkdownBlock.Code(code.toString().trimEnd())
  flushQuote()
  flushTable()
  flushParagraph()
  return blocks
}

internal fun isMarkdownDivider(line: String): Boolean {
  val compact = line.filterNot { it.isWhitespace() }
  if (compact.length < 3) return false
  val marker = compact.first()
  return marker in listOf('-', '*', '_') && compact.all { it == marker }
}

private fun looksLikeMarkdownTableRow(line: String): Boolean {
  return line.count { it == '|' } >= 2
}

private fun isMarkdownTableSeparator(line: String): Boolean {
  val cells = parseMarkdownTableRow(line)
  return cells.isNotEmpty() && cells.all { cell ->
    cell.isNotBlank() && cell.all { it == '-' || it == ':' }
  }
}

private fun parseMarkdownTableRow(line: String): List<String> {
  return line
    .trim()
    .trim('|')
    .split('|')
    .map { it.trim() }
}

@Composable
private fun MarkdownInlineText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  fontWeight: FontWeight? = null,
  color: Color = Color.Unspecified,
  interactiveLinks: Boolean
) {
  val context = LocalContext.current
  val hasUrl = remember(text) { text.getStringAnnotations(UrlAnnotationTag, 0, text.length).isNotEmpty() }
  var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  var activeUrl by remember(text) { mutableStateOf<String?>(null) }
  var popupOffset by remember(text) { mutableStateOf(IntOffset.Zero) }
  val linkModifier = if (interactiveLinks && hasUrl) {
    Modifier.pointerInput(text, layoutResult) {
      detectTapGestures { position ->
        layoutResult?.let { layout ->
          val offset = layout.getOffsetForPosition(position)
          val annotationEnd = (offset + 1).coerceAtMost(text.length)
          val url = text.getStringAnnotations(UrlAnnotationTag, offset, annotationEnd).firstOrNull()?.item
          if (url != null) {
            activeUrl = url
            popupOffset = IntOffset(position.x.toInt(), position.y.toInt() + 12)
          }
        }
      }
    }
  } else {
    Modifier
  }
  Box(modifier = modifier) {
    Text(
      text = text,
      modifier = linkModifier,
      style = style,
      fontWeight = fontWeight,
      color = color,
      onTextLayout = { layoutResult = it }
    )
    activeUrl?.let { url ->
      UrlActionMenu(
        offset = popupOffset,
        onDismiss = { activeUrl = null },
        onCopy = {
          copyUrlToClipboard(context, url)
          activeUrl = null
        },
        onOpen = {
          openUrl(context, url)
          activeUrl = null
        }
      )
    }
  }
}

@Composable
private fun UrlActionMenu(
  offset: IntOffset,
  onDismiss: () -> Unit,
  onCopy: () -> Unit,
  onOpen: () -> Unit
) {
  Popup(
    alignment = Alignment.TopStart,
    offset = offset,
    onDismissRequest = onDismiss
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      shadowElevation = 6.dp
    ) {
      androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onOpen) {
          Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "打开链接")
        }
        IconButton(onClick = onCopy) {
          Icon(Icons.Outlined.ContentCopy, contentDescription = "复制链接")
        }
      }
    }
  }
}

private fun copyUrlToClipboard(context: Context, url: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("AI Chat URL", url))
}

private fun openUrl(context: Context, url: String) {
  runCatching {
    context.startActivity(
      Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    )
  }
}

internal enum class MarkdownInlineStyle {
  BODY,
  BOLD,
  ITALIC,
  BOLD_ITALIC,
  STRIKETHROUGH,
  INLINE_CODE,
  LINK
}

internal data class MarkdownInlineToken(
  val text: String,
  val style: MarkdownInlineStyle,
  val url: String? = null
)

private val InlineMarkdownPattern = Regex(
  """(`[^`\n]+`)|(\[[^]\n]+\]\((?:https?://)?(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?:/[^\s)]*)?\))|(<https?://[^\s<>]+>)|(\*\*\*[^*\n]+\*\*\*)|(___[^_\n]+___)|(\*\*[^*\n]+\*\*)|(__[^_\n]+__)|(~~[^~\n]+~~)|((?<!\*)\*[^*\n]+\*(?!\*))|((?<![\w_])_[^_\n]+_(?![\w_]))|(https?://[^\s<>()\[\]{}"']+)|((?<![@\w.-])(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?:/[^\s<>()\[\]{}"']+)?)"""
)

internal fun parseInlineMarkdown(text: String): List<MarkdownInlineToken> {
  val tokens = mutableListOf<MarkdownInlineToken>()

  fun append(value: String, style: MarkdownInlineStyle, url: String? = null) {
    if (value.isEmpty()) return
    val previous = tokens.lastOrNull()
    if (previous != null && previous.style == style && previous.url == url) {
      tokens[tokens.lastIndex] = previous.copy(text = previous.text + value)
    } else {
      tokens += MarkdownInlineToken(value, style, url)
    }
  }

  var index = 0
  InlineMarkdownPattern.findAll(text).forEach { match ->
    append(text.substring(index, match.range.first), MarkdownInlineStyle.BODY)
    val value = match.value
    when {
      value.startsWith("`") -> append(
        value.removePrefix("`").removeSuffix("`"),
        MarkdownInlineStyle.INLINE_CODE
      )
      value.startsWith("[") -> {
        val label = value.substringAfter('[').substringBefore("](")
        val url = value.substringAfter("](").removeSuffix(")")
        append(label, MarkdownInlineStyle.LINK, normalizeHttpUrl(url))
      }
      value.startsWith("<") -> {
        val url = value.drop(1).dropLast(1)
        append(url, MarkdownInlineStyle.LINK, normalizeHttpUrl(url))
      }
      value.startsWith("***") || value.startsWith("___") -> append(
        value.drop(3).dropLast(3),
        MarkdownInlineStyle.BOLD_ITALIC
      )
      value.startsWith("**") || value.startsWith("__") -> append(
        value.drop(2).dropLast(2),
        MarkdownInlineStyle.BOLD
      )
      value.startsWith("~~") -> append(
        value.drop(2).dropLast(2),
        MarkdownInlineStyle.STRIKETHROUGH
      )
      value.startsWith("*") || value.startsWith("_") -> append(
        value.drop(1).dropLast(1),
        MarkdownInlineStyle.ITALIC
      )
      else -> {
        val cleanUrl = value.trimEnd(
          '.', ',', ';', ':', '!', '?', ')', ']', '}',
          '\uFF0C', '\u3002', '\uFF1B', '\uFF1A', '\uFF01', '\uFF1F'
        )
        append(cleanUrl, MarkdownInlineStyle.LINK, normalizeHttpUrl(cleanUrl))
        append(value.removePrefix(cleanUrl), MarkdownInlineStyle.BODY)
      }
    }
    index = match.range.last + 1
  }
  append(text.substring(index), MarkdownInlineStyle.BODY)
  return tokens.ifEmpty { listOf(MarkdownInlineToken("", MarkdownInlineStyle.BODY)) }
}

internal fun renderInlineMarkdown(text: String, linkColor: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
  parseInlineMarkdown(text).forEach { token ->
    when (token.style) {
      MarkdownInlineStyle.BODY -> append(token.text)
      MarkdownInlineStyle.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(token.text)
      }
      MarkdownInlineStyle.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
        append(token.text)
      }
      MarkdownInlineStyle.BOLD_ITALIC -> withStyle(
        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
      ) {
        append(token.text)
      }
      MarkdownInlineStyle.STRIKETHROUGH -> withStyle(
        SpanStyle(textDecoration = TextDecoration.LineThrough)
      ) {
        append(token.text)
      }
      MarkdownInlineStyle.INLINE_CODE -> withStyle(
        SpanStyle(
          background = Color(0x1A2F5E47),
          fontWeight = FontWeight.Medium
        )
      ) {
        append(token.text)
      }
      MarkdownInlineStyle.LINK -> appendUrlLink(token.text, checkNotNull(token.url), linkColor)
    }
  }
}

private fun AnnotatedString.Builder.appendUrlLink(label: String, url: String, linkColor: Color) {
  if (url.isBlank()) {
    append(label)
    return
  }
  pushStringAnnotation(UrlAnnotationTag, normalizeHttpUrl(url))
  withStyle(
    SpanStyle(
      color = linkColor,
      textDecoration = TextDecoration.Underline,
      fontWeight = FontWeight.Medium
    )
  ) {
    append(label)
  }
  pop()
}

private fun normalizeHttpUrl(url: String): String {
  return if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
    url
  } else {
    "https://$url"
  }
}
