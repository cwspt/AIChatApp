package com.personal.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

internal data class MarkdownRenderColors(
  val content: Color,
  val muted: Color,
  val blockContainer: Color,
  val blockHeader: Color,
  val border: Color,
  val divider: Color
)

@Composable
internal fun MarkdownPreview(content: String, colors: MarkdownRenderColors? = null) {
  val blocks = remember(content) { parseMarkdownBlocks(content) }
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
        is MarkdownBlock.Heading -> Text(
          text = renderInlineMarkdown(block.text),
          style = when (block.level) {
            1 -> MaterialTheme.typography.titleLarge
            2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
          },
          fontWeight = FontWeight.SemiBold,
          color = colors?.content ?: Color.Unspecified
        )
        is MarkdownBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(block.marker, color = colors?.muted ?: MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            renderInlineMarkdown(block.text),
            modifier = Modifier.weight(1f),
            color = colors?.content ?: Color.Unspecified
          )
        }
        is MarkdownBlock.Table -> MarkdownTable(block, colors)
        MarkdownBlock.Divider -> Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors?.divider ?: MaterialTheme.colorScheme.outlineVariant)
        )
        is MarkdownBlock.Paragraph -> Text(
          renderInlineMarkdown(block.text),
          color = colors?.content ?: Color.Unspecified
        )
      }
    }
  }
}

private sealed interface MarkdownBlock {
  data class Paragraph(val text: String) : MarkdownBlock
  data class Heading(val level: Int, val text: String) : MarkdownBlock
  data class ListItem(val marker: String, val text: String) : MarkdownBlock
  data class Code(val text: String) : MarkdownBlock
  data class Table(val rows: List<List<String>>) : MarkdownBlock
  data object Divider : MarkdownBlock
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table, colors: MarkdownRenderColors? = null) {
  if (table.rows.isEmpty()) return
  val columnCount = table.rows.maxOf { it.size }.coerceAtLeast(1)
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
              Text(
                text = renderInlineMarkdown(row.getOrNull(column).orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                color = colors?.content ?: Color.Unspecified
              )
            }
          }
        }
      }
    }
  }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
  val blocks = mutableListOf<MarkdownBlock>()
  val paragraph = StringBuilder()
  val code = StringBuilder()
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

  markdown.lines().forEach { rawLine ->
    val line = rawLine.trimEnd()
    if (line.trimStart().startsWith("```")) {
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
    if (headingLevel in 1..4 && trimmed.getOrNull(headingLevel) == ' ') {
      flushParagraph()
      blocks += MarkdownBlock.Heading(headingLevel, trimmed.drop(headingLevel).trim())
      return@forEach
    }

    val unordered = listOf("- ", "* ", "+ ").firstOrNull { trimmed.startsWith(it) }
    if (unordered != null) {
      flushParagraph()
      blocks += MarkdownBlock.ListItem("•", trimmed.drop(unordered.length).trim())
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
  flushTable()
  flushParagraph()
  return blocks
}

private fun isMarkdownDivider(line: String): Boolean {
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

private fun renderInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
  var index = 0
  val pattern = Regex("(\\*\\*[^*]+\\*\\*)|(`[^`]+`)|(\\[[^]]+\\]\\([^)]+\\))")
  pattern.findAll(text).forEach { match ->
    append(text.substring(index, match.range.first))
    val value = match.value
    when {
      value.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(value.removePrefix("**").removeSuffix("**"))
      }
      value.startsWith("`") -> withStyle(
        SpanStyle(
          background = Color(0x1A2F5E47),
          fontWeight = FontWeight.Medium
        )
      ) {
        append(value.removePrefix("`").removeSuffix("`"))
      }
      value.startsWith("[") -> {
        val label = value.substringAfter("[").substringBefore("]")
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
          append(label)
        }
      }
    }
    index = match.range.last + 1
  }
  append(text.substring(index))
}
