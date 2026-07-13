package com.personal.aichat.ui

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.personal.aichat.data.ConversationExport
import com.personal.aichat.data.ConversationExportMessage
import com.personal.aichat.data.withoutToolMessages
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

object ConversationShareRenderer {
  // Keep each allocation below roughly 50 MB on common devices. Long chats are paged.
  private const val MaxImageHeight = 12000
  private const val ImageWidth = 1080
  private const val TitleLineHeight = 64f
  private const val MetaLineHeight = 40f
  private const val RoleRowHeight = 44f
  private const val BodyLineHeight = 54f
  private const val CodeLineHeight = 45f
  private const val HeadingLineHeight = 58f
  private const val TableLineHeight = 42f
  private const val ImagePageMessageHeightBudget = 10_000
  private const val ImageMessageHeightBudget = 7_600
  private const val MaxLineChunkLength = 1_200

  fun writeTextExport(context: Context, title: String, text: String): Uri {
    val file = exportFile(context, safeFileName(title, "md"))
    file.writeText(text, Charsets.UTF_8)
    return file.toShareUri(context)
  }

  fun writeImageExports(context: Context, export: ConversationExport): List<Uri> {
    val pages = imageExportPages(export)
    return pages.mapIndexed { index, page ->
      val bitmap = renderBitmap(page)
      try {
        val file = exportFile(context, imageFileName(export.title, index, pages.size))
        file.outputStream().use { output ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        file.toShareUri(context)
      } finally {
        bitmap.recycle()
      }
    }
  }

  fun saveImageExports(context: Context, export: ConversationExport): List<Uri>? {
    val resolver = context.contentResolver
    val pages = imageExportPages(export)
    val savedUris = mutableListOf<Uri>()
    return runCatching {
      pages.forEachIndexed { index, page ->
        val fileName = imageFileName(export.title, index, pages.size)
        val values = ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Chat")
            put(MediaStore.Images.Media.IS_PENDING, 1)
          }
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        savedUris += uri
        val bitmap = renderBitmap(page)
        try {
          checkNotNull(resolver.openOutputStream(uri)).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
          }
        } finally {
          bitmap.recycle()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          values.clear()
          values.put(MediaStore.Images.Media.IS_PENDING, 0)
          resolver.update(uri, values, null, null)
        }
      }
      savedUris.toList()
    }.getOrElse {
      savedUris.forEach { uri -> resolver.delete(uri, null, null) }
      null
    }
  }

  internal fun imageExportPages(export: ConversationExport): List<ConversationExport> {
    val shareableExport = export.withoutToolMessages()
    val pageMessages = mutableListOf<MutableList<ConversationExportMessage>>()
    var currentPage = mutableListOf<ConversationExportMessage>()
    var currentHeight = 0

    splitExportMessagesForImagePages(shareableExport.messages).forEach { message ->
      val messageHeight = estimateImageMessageHeight(message.content)
      if (currentPage.isNotEmpty() && currentHeight + messageHeight > ImagePageMessageHeightBudget) {
        pageMessages += currentPage
        currentPage = mutableListOf()
        currentHeight = 0
      }
      currentPage += message
      currentHeight += messageHeight
    }
    if (currentPage.isNotEmpty() || pageMessages.isEmpty()) {
      pageMessages += currentPage
    }

    val totalPages = pageMessages.size
    return pageMessages.mapIndexed { index, messages ->
      val pageTitle = if (totalPages == 1) {
        shareableExport.title
      } else {
        "${shareableExport.title} (${index + 1}/$totalPages)"
      }
      shareableExport.copy(title = pageTitle, messages = messages)
    }
  }

  private fun splitExportMessagesForImagePages(
    messages: List<ConversationExportMessage>
  ): List<ConversationExportMessage> = messages.flatMap { message ->
    splitExportMessageForImagePages(message)
  }

  private fun splitExportMessageForImagePages(
    message: ConversationExportMessage
  ): List<ConversationExportMessage> {
    val content = exportMessageContent(message)
    if (estimateImageContentHeight(content) <= ImageMessageHeightBudget) return listOf(message)

    val chunks = mutableListOf<String>()
    val chunk = StringBuilder()
    fun flushChunk() {
      if (chunk.isNotEmpty()) {
        chunks += chunk.toString()
        chunk.clear()
      }
    }
    content.lines().forEach { line ->
      splitImageExportLine(line).forEach { linePart ->
        val next = if (chunk.isEmpty()) linePart else "${chunk}\n$linePart"
        if (chunk.isNotEmpty() && estimateImageContentHeight(next) > ImageMessageHeightBudget) {
          flushChunk()
        }
        if (chunk.isNotEmpty()) chunk.append('\n')
        chunk.append(linePart)
      }
    }
    flushChunk()
    return chunks.mapIndexed { index, part ->
      message.copy(id = "${message.id}-image-page-$index", content = part)
    }
  }

  private fun splitImageExportLine(line: String): List<String> {
    if (line.length <= MaxLineChunkLength) return listOf(line)
    return line.chunked(MaxLineChunkLength)
  }

  private fun estimateImageMessageHeight(content: String): Int {
    return RoleRowHeight.toInt() + 64 + 22 + estimateImageContentHeight(content)
  }

  private fun estimateImageContentHeight(content: String): Int {
    var inCodeBlock = false
    return content.lines().sumOf { line ->
      val trimmed = line.trim()
      when {
        trimmed.startsWith("```") -> {
          inCodeBlock = !inCodeBlock
          36
        }
        inCodeBlock -> 52 + line.length * 3
        looksLikeMarkdownTableRow(trimmed) -> 96 + line.length * 3
        else -> 18 + line.length * 3
      }
    }.coerceAtLeast(BodyLineHeight.toInt())
  }

  private fun renderBitmap(export: ConversationExport): Bitmap {
    val shareableExport = export.withoutToolMessages()
    val width = ImageWidth
    val padding = 52f
    val bubblePadding = 32f
    val maxBubbleWidth = width - padding * 2
    val titlePaint = textPaint(50f, Color.rgb(27, 43, 35), Typeface.BOLD)
    val metaPaint = textPaint(28f, Color.rgb(91, 108, 99))
    val rolePaint = textPaint(28f, Color.rgb(91, 108, 99), Typeface.BOLD)
    val bodyPaint = textPaint(40f, Color.rgb(26, 32, 29))
    val boldPaint = textPaint(40f, Color.rgb(26, 32, 29), Typeface.BOLD)
    val codePaint = textPaint(34f, Color.rgb(35, 45, 40), Typeface.NORMAL, Typeface.MONOSPACE)
    val headingPaint = textPaint(46f, Color.rgb(27, 43, 35), Typeface.BOLD)
    val errorPaint = textPaint(40f, Color.rgb(170, 48, 38))
    val smallPaint = textPaint(26f, Color.rgb(112, 125, 118))
    val paints = MarkdownPaints(
      body = bodyPaint,
      bold = boldPaint,
      code = codePaint,
      heading = headingPaint,
      error = errorPaint
    )
    val titleLines = wrapText(shareableExport.title, titlePaint, width - padding * 2)
    val metaLines = listOfNotNull(
      shareableExport.groupName?.let { "分组：$it" },
      shareableExport.modelLabel?.let { "模型：$it" }
    )
    val messageLayouts = shareableExport.messages.map { message ->
      val content = exportMessageContent(message)
      RenderMessage(
        role = message.role,
        failed = message.status == MessageStatus.FAILED,
        time = formatImageTime(message.createdAt),
        blocks = parseImageMarkdownBlocks(
          markdown = content,
          paints = paints,
          maxWidth = maxBubbleWidth - bubblePadding * 2,
          failed = message.status == MessageStatus.FAILED
        )
      )
    }

    var height = padding
    height += (titleLines.size * TitleLineHeight) + 20
    height += max(1, metaLines.size) * MetaLineHeight + 28
    messageLayouts.forEach { item ->
      height += RoleRowHeight
      height += item.blocks.sumOf { it.height.toInt() } + bubblePadding.toInt() * 2
      height += 22
    }
    height += padding
    val bitmapHeight = height.coerceAtMost(MaxImageHeight.toFloat()).toInt()
    val bitmap = Bitmap.createBitmap(width, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(246, 243, 236))

    var y = padding
    titleLines.forEach { line ->
      canvas.drawText(line, padding, y + titlePaint.textSize, titlePaint)
      y += TitleLineHeight
    }
    y += 8
    if (metaLines.isEmpty()) {
      canvas.drawText("AI Chat 导出", padding, y + metaPaint.textSize, metaPaint)
      y += MetaLineHeight
    } else {
      metaLines.forEach { line ->
        canvas.drawText(line, padding, y + metaPaint.textSize, metaPaint)
        y += MetaLineHeight
      }
    }
    y += 24

    messageLayouts.forEach { item ->
      if (y > bitmapHeight - padding) return@forEach
      val roleName = when (item.role) {
        MessageRole.USER -> "我"
        MessageRole.ASSISTANT -> "AI"
        MessageRole.SYSTEM -> "系统"
        MessageRole.TOOL -> "工具"
      }
      canvas.drawText("$roleName · ${item.time}", padding, y + rolePaint.textSize, rolePaint)
      y += RoleRowHeight
      val bubbleHeight = item.blocks.sumOf { it.height.toInt() }.toFloat() + bubblePadding * 2
      val bubbleColor = when {
        item.failed -> Color.rgb(255, 239, 235)
        item.role == MessageRole.USER -> Color.rgb(216, 236, 220)
        item.role == MessageRole.ASSISTANT -> Color.WHITE
        else -> Color.rgb(231, 232, 226)
      }
      val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bubbleColor }
      canvas.drawRoundRect(
        padding,
        y,
        width - padding,
        (y + bubbleHeight).coerceAtMost(bitmapHeight - padding),
        30f,
        30f,
        rectPaint
      )
      var textY = y + bubblePadding
      item.blocks.forEach { block ->
        if (textY < bitmapHeight - padding) {
          drawMarkdownBlock(canvas, block, padding + bubblePadding, textY, width - padding - bubblePadding)
        }
        textY += block.height
      }
      y += bubbleHeight + 22
    }

    if (height > MaxImageHeight) {
      canvas.drawText("内容较长，图片已截断；建议使用“文件分享”导出完整 Markdown。", padding, bitmapHeight - padding, smallPaint)
    }
    return bitmap
  }

  private fun exportMessageContent(message: com.personal.aichat.data.ConversationExportMessage): String {
    if (message.status == MessageStatus.FAILED) {
      return message.content.ifBlank { message.errorMessage ?: "请求失败，但没有返回错误详情。" }
    }
    return message.content.ifBlank { " " }
  }

  private fun parseImageMarkdownBlocks(
    markdown: String,
    paints: MarkdownPaints,
    maxWidth: Float,
    failed: Boolean = false
  ): List<RenderBlock> {
    val blocks = mutableListOf<RenderBlock>()
    val paragraph = StringBuilder()
    val code = StringBuilder()
    val tableRows = mutableListOf<List<String>>()
    var inCode = false

    fun flushParagraph() {
      val text = paragraph.toString().trim()
      if (text.isNotBlank()) {
        val paint = if (failed) paints.error else paints.body
        val lines = wrapText(cleanInlineMarkdown(text), paint, maxWidth)
        blocks += RenderBlock.Text(lines, paint, BodyLineHeight, lines.size * BodyLineHeight + 10f)
      }
      paragraph.clear()
    }

    fun flushCode() {
      val text = code.toString().trimEnd()
      if (text.isNotBlank()) {
        val lines = wrapText(text, paints.code, maxWidth - 24f)
        blocks += RenderBlock.Code(lines, lines.size * CodeLineHeight + 32f)
      }
      code.clear()
    }

    fun flushTable() {
      if (tableRows.isNotEmpty()) {
        val columns = tableRows.maxOf { it.size }.coerceAtLeast(1)
        val columnWidth = (maxWidth - 24f) / columns
        var tableHeight = 18f
        tableRows.forEach { row ->
          val rowLines = (0 until columns).maxOf { column ->
            wrapText(cleanInlineMarkdown(row.getOrNull(column).orEmpty()), paints.body, columnWidth - 12f).size
          }
          tableHeight += rowLines * TableLineHeight + 22f
        }
        blocks += RenderBlock.Table(tableRows.toList(), columns, tableHeight)
        tableRows.clear()
      }
    }

    markdown.lines().forEach { rawLine ->
      val line = rawLine.trimEnd()
      if (line.trimStart().startsWith("```")) {
        if (inCode) {
          flushCode()
          inCode = false
        } else {
          flushParagraph()
          flushTable()
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
        flushParagraph()
        flushTable()
        return@forEach
      }

      if (isMarkdownTableSeparator(trimmed)) {
        flushParagraph()
        return@forEach
      }
      if (looksLikeMarkdownTableRow(trimmed)) {
        flushParagraph()
        tableRows += parseMarkdownTableRow(trimmed)
        return@forEach
      } else {
        flushTable()
      }

      val headingLevel = trimmed.takeWhile { it == '#' }.length
      if (headingLevel in 1..4 && trimmed.getOrNull(headingLevel) == ' ') {
        flushParagraph()
        val text = cleanInlineMarkdown(trimmed.drop(headingLevel).trim())
        val lines = wrapText(text, paints.heading, maxWidth)
        blocks += RenderBlock.Text(lines, paints.heading, HeadingLineHeight, lines.size * HeadingLineHeight + 14f)
        return@forEach
      }

      val unordered = listOf("- ", "* ", "+ ").firstOrNull { trimmed.startsWith(it) }
      val orderedMatch = Regex("^(\\d+)[.)]\\s+(.*)$").matchEntire(trimmed)
      when {
        unordered != null -> {
          flushParagraph()
          val text = "• " + cleanInlineMarkdown(trimmed.drop(unordered.length).trim())
          val lines = wrapText(text, paints.body, maxWidth)
          blocks += RenderBlock.Text(lines, paints.body, BodyLineHeight, lines.size * BodyLineHeight + 8f)
        }
        orderedMatch != null -> {
          flushParagraph()
          val text = "${orderedMatch.groupValues[1]}. " + cleanInlineMarkdown(orderedMatch.groupValues[2])
          val lines = wrapText(text, paints.body, maxWidth)
          blocks += RenderBlock.Text(lines, paints.body, BodyLineHeight, lines.size * BodyLineHeight + 8f)
        }
        else -> {
          if (paragraph.isNotEmpty()) paragraph.append('\n')
          paragraph.append(trimmed)
        }
      }
    }

    if (inCode) flushCode()
    flushParagraph()
    flushTable()
    return blocks.ifEmpty { listOf(RenderBlock.Text(listOf(" "), paints.body, BodyLineHeight, BodyLineHeight)) }
  }

  private fun drawMarkdownBlock(
    canvas: Canvas,
    block: RenderBlock,
    left: Float,
    top: Float,
    right: Float
  ) {
    when (block) {
      is RenderBlock.Text -> {
        var y = top + block.paint.textSize
        block.lines.forEach { line ->
          canvas.drawText(line, left, y, block.paint)
          y += block.lineHeight
        }
      }
      is RenderBlock.Code -> {
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(239, 243, 238) }
        canvas.drawRoundRect(left, top, right, top + block.height - 8f, 12f, 12f, background)
        val paint = textPaint(34f, Color.rgb(35, 45, 40), Typeface.NORMAL, Typeface.MONOSPACE)
        var y = top + 42f
        block.lines.forEach { line ->
          canvas.drawText(line, left + 12f, y, paint)
          y += CodeLineHeight
        }
      }
      is RenderBlock.Table -> {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(198, 211, 201)
          strokeWidth = 2f
          style = Paint.Style.STROKE
        }
        val rowLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(224, 231, 224)
          strokeWidth = 1.4f
        }
        val columnLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.rgb(211, 222, 213)
          strokeWidth = 1.2f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 242, 236) }
        val bodyPaint = textPaint(30f, Color.rgb(26, 32, 29))
        val headerTextPaint = textPaint(30f, Color.rgb(26, 32, 29), Typeface.BOLD)
        val columnWidth = (right - left) / block.columns
        var y = top
        block.rows.forEachIndexed { rowIndex, row ->
          val rowLineCounts = (0 until block.columns).map { column ->
            wrapText(cleanInlineMarkdown(row.getOrNull(column).orEmpty()), bodyPaint, columnWidth - 14f).size
          }
          val rowHeight = (rowLineCounts.maxOrNull() ?: 1) * TableLineHeight + 22f
          if (rowIndex == 0) {
            canvas.drawRoundRect(left, y, right, y + rowHeight, 8f, 8f, headerPaint)
          }
          canvas.drawLine(left, y, right, y, if (rowIndex == 0) borderPaint else rowLinePaint)
          (0 until block.columns).forEach { column ->
            val cellLeft = left + column * columnWidth
            val lines = wrapText(cleanInlineMarkdown(row.getOrNull(column).orEmpty()), bodyPaint, columnWidth - 14f)
            var textY = y + 36f
            lines.forEach { line ->
              canvas.drawText(line, cellLeft + 7f, textY, if (rowIndex == 0) headerTextPaint else bodyPaint)
              textY += TableLineHeight
            }
            if (column > 0) {
              canvas.drawLine(cellLeft, y + 4f, cellLeft, y + rowHeight - 4f, columnLinePaint)
            }
          }
          canvas.drawLine(left, y + rowHeight, right, y + rowHeight, rowLinePaint)
          y += rowHeight
        }
        canvas.drawRoundRect(left, top, right, y, 8f, 8f, borderPaint)
      }
    }
  }

  private fun cleanInlineMarkdown(text: String): String {
    return text
      .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
      .replace(Regex("`([^`]+)`"), "$1")
      .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
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
    return line.trim().trim('|').split('|').map { it.trim() }
  }

  private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val result = mutableListOf<String>()
    text.lines().forEach { paragraph ->
      if (paragraph.isBlank()) {
        result += ""
        return@forEach
      }
      var remaining = paragraph.trimEnd()
      while (remaining.isNotEmpty()) {
        val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
        result += remaining.take(count)
        remaining = remaining.drop(count)
      }
    }
    return result.ifEmpty { listOf("") }
  }

  private fun textPaint(
    size: Float,
    colorValue: Int,
    typefaceStyle: Int = Typeface.NORMAL,
    family: Typeface = Typeface.SANS_SERIF
  ): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = colorValue
      textSize = size
      typeface = Typeface.create(family, typefaceStyle)
    }
  }

  private fun safeFileName(title: String, ext: String): String {
    val cleaned = title.ifBlank { "AIChat" }
      .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
      .take(42)
      .ifBlank { "AIChat" }
    return "$cleaned.$ext"
  }

  private fun imageFileName(title: String, pageIndex: Int, pageCount: Int): String {
    val pageSuffix = if (pageCount > 1) "_part-${pageIndex + 1}-of-$pageCount" else ""
    val titleLimit = (42 - pageSuffix.length).coerceAtLeast(1)
    val cleanedTitle = title.ifBlank { "AIChat" }
      .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
      .take(titleLimit)
      .ifBlank { "AIChat" }
    return "$cleanedTitle$pageSuffix.png"
  }

  private fun exportFile(context: Context, name: String): File {
    val dir = File(context.cacheDir, "shared_exports").also { it.mkdirs() }
    return File(dir, name)
  }

  private fun File.toShareUri(context: Context): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)
  }

  private fun formatImageTime(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
      timeZone = TimeZone.getDefault()
    }.format(Date(timestamp))
  }

  private data class MarkdownPaints(
    val body: Paint,
    val bold: Paint,
    val code: Paint,
    val heading: Paint,
    val error: Paint
  )

  private data class RenderMessage(
    val role: MessageRole,
    val failed: Boolean,
    val time: String,
    val blocks: List<RenderBlock>
  )

  private sealed interface RenderBlock {
    val height: Float

    data class Text(
      val lines: List<String>,
      val paint: Paint,
      val lineHeight: Float,
      override val height: Float
    ) : RenderBlock

    data class Code(
      val lines: List<String>,
      override val height: Float
    ) : RenderBlock

    data class Table(
      val rows: List<List<String>>,
      val columns: Int,
      override val height: Float
    ) : RenderBlock
  }
}
