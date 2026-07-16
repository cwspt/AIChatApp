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
import kotlin.math.ceil
import kotlin.math.max

object ConversationShareRenderer {
  // Keep each allocation below roughly 50 MB on common devices. Long chats are paged.
  private const val MaxImageHeight = 12000
  private const val MaxSingleImageHeight = 24000
  private const val ImageWidth = 1080
  private const val ImagePadding = 52f
  private const val BubblePadding = 32f
  private const val TitleLineHeight = 64f
  private const val MetaLineHeight = 40f
  private const val RoleRowHeight = 44f
  private const val BodyLineHeight = 54f
  private const val CodeLineHeight = 45f
  private const val HeadingOneLineHeight = 70f
  private const val HeadingTwoLineHeight = 64f
  private const val HeadingThreeLineHeight = 58f
  private const val TableLineHeight = 42f
  private const val ImagePageMessageHeightBudget = 10_000
  private const val ImageMessageHeightBudget = 7_600
  private const val MaxLineChunkLength = 1_200

  internal enum class ImageExportMode {
    SINGLE,
    PAGED
  }

  internal data class ImageExportPlan(
    val standardHeightPx: Int,
    val pageCount: Int,
    val singleImageAllowed: Boolean
  )

  fun writeTextExport(context: Context, title: String, text: String): Uri {
    val file = exportFile(context, safeFileName(title, "md"))
    file.writeText(text, Charsets.UTF_8)
    return file.toShareUri(context)
  }

  internal fun writeImageExports(
    context: Context,
    export: ConversationExport,
    mode: ImageExportMode
  ): List<Uri> {
    val pages = imageExportsForMode(export, mode)
    return pages.mapIndexed { index, page ->
      val bitmap = renderBitmap(
        export = page,
        maxHeight = imageMaxHeight(mode),
        config = imageBitmapConfig(mode)
      )
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

  internal fun saveImageExports(
    context: Context,
    export: ConversationExport,
    mode: ImageExportMode
  ): List<Uri>? {
    val resolver = context.contentResolver
    val pages = imageExportsForMode(export, mode)
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
        val bitmap = renderBitmap(
          export = page,
          maxHeight = imageMaxHeight(mode),
          config = imageBitmapConfig(mode)
        )
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

  internal fun imageExportPlan(export: ConversationExport): ImageExportPlan {
    val shareableExport = export.withoutToolMessages()
    val standardHeight = buildImageRenderLayout(shareableExport).height
    val pageCount = if (standardHeight <= MaxImageHeight) 1 else imageExportPages(shareableExport).size
    return ImageExportPlan(
      standardHeightPx = standardHeight,
      pageCount = pageCount,
      singleImageAllowed = standardHeight <= MaxSingleImageHeight
    )
  }

  internal fun imageMaxHeight(mode: ImageExportMode): Int = when (mode) {
    ImageExportMode.SINGLE -> MaxSingleImageHeight
    ImageExportMode.PAGED -> MaxImageHeight
  }

  internal fun imageBitmapConfig(mode: ImageExportMode): Bitmap.Config = when (mode) {
    ImageExportMode.SINGLE -> Bitmap.Config.RGB_565
    ImageExportMode.PAGED -> Bitmap.Config.ARGB_8888
  }

  private fun imageExportsForMode(
    export: ConversationExport,
    mode: ImageExportMode
  ): List<ConversationExport> {
    val shareableExport = export.withoutToolMessages()
    return when (mode) {
      ImageExportMode.PAGED -> imageExportPages(shareableExport)
      ImageExportMode.SINGLE -> {
        val height = buildImageRenderLayout(shareableExport).height
        require(height <= MaxSingleImageHeight) {
          "Single image height $height exceeds $MaxSingleImageHeight"
        }
        listOf(shareableExport)
      }
    }
  }

  internal fun imageExportPages(export: ConversationExport): List<ConversationExport> {
    val shareableExport = export.withoutToolMessages()
    if (buildImageRenderLayout(shareableExport).height <= MaxImageHeight) {
      return listOf(shareableExport)
    }
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

    val safePageMessages = pageMessages.flatMap { messages ->
      splitPageMessagesToExactHeight(shareableExport, messages)
    }
    val totalPages = safePageMessages.size
    return safePageMessages.mapIndexed { index, messages ->
      val pageTitle = if (totalPages == 1) {
        shareableExport.title
      } else {
        "${shareableExport.title} (${index + 1}/$totalPages)"
      }
      shareableExport.copy(title = pageTitle, messages = messages)
    }
  }

  private fun splitPageMessagesToExactHeight(
    export: ConversationExport,
    messages: List<ConversationExportMessage>
  ): List<List<ConversationExportMessage>> {
    if (messages.isEmpty()) return listOf(emptyList())
    val measurementExport = export.copy(
      title = "${export.title} (999/999)",
      messages = messages
    )
    if (buildImageRenderLayout(measurementExport).height <= MaxImageHeight) {
      return listOf(messages)
    }
    if (messages.size > 1) {
      val splitIndex = messages.size / 2
      return splitPageMessagesToExactHeight(export, messages.take(splitIndex)) +
        splitPageMessagesToExactHeight(export, messages.drop(splitIndex))
    }
    val message = messages.single()
    val content = exportMessageContent(message)
    require(content.length > 1) { "Image export message cannot fit within $MaxImageHeight px" }
    val splitIndex = preferredContentSplitIndex(content)
    val first = message.copy(id = "${message.id}-exact-0", content = content.substring(0, splitIndex))
    val second = message.copy(id = "${message.id}-exact-1", content = content.substring(splitIndex))
    return splitPageMessagesToExactHeight(export, listOf(first)) +
      splitPageMessagesToExactHeight(export, listOf(second))
  }

  private fun preferredContentSplitIndex(content: String): Int {
    val midpoint = content.length / 2
    val before = content.lastIndexOf('\n', startIndex = midpoint)
    val after = content.indexOf('\n', startIndex = midpoint)
    return when {
      before > 0 && midpoint - before <= content.length / 4 -> before + 1
      after in 1 until content.lastIndex && after - midpoint <= content.length / 4 -> after + 1
      else -> midpoint.coerceIn(1, content.lastIndex)
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
        inCodeBlock -> 58 + line.length * 3
        looksLikeMarkdownTableRow(trimmed) -> 104 + line.length * 3
        trimmed.startsWith("#") -> 42 + line.length * 4
        else -> 24 + line.length * 3
      }
    }.coerceAtLeast(BodyLineHeight.toInt())
  }

  private fun buildImageRenderLayout(export: ConversationExport): ImageRenderLayout {
    val shareableExport = export.withoutToolMessages()
    val width = ImageWidth
    val padding = ImagePadding
    val bubblePadding = BubblePadding
    val maxBubbleWidth = width - padding * 2
    val titlePaint = textPaint(50f, Color.rgb(27, 43, 35), Typeface.BOLD)
    val metaPaint = textPaint(28f, Color.rgb(91, 108, 99))
    val rolePaint = textPaint(28f, Color.rgb(91, 108, 99), Typeface.BOLD)
    val bodyPaint = textPaint(40f, Color.rgb(26, 32, 29))
    val boldPaint = textPaint(40f, Color.rgb(26, 32, 29), Typeface.BOLD)
    val codePaint = textPaint(34f, Color.rgb(35, 45, 40), Typeface.NORMAL, Typeface.MONOSPACE)
    val linkPaint = textPaint(40f, Color.rgb(25, 103, 83), Typeface.BOLD)
    val headingOnePaint = textPaint(52f, Color.rgb(27, 43, 35), Typeface.BOLD)
    val headingTwoPaint = textPaint(46f, Color.rgb(27, 43, 35), Typeface.BOLD)
    val headingThreePaint = textPaint(42f, Color.rgb(27, 43, 35), Typeface.BOLD)
    val errorPaint = textPaint(40f, Color.rgb(170, 48, 38))
    val errorBoldPaint = textPaint(40f, Color.rgb(170, 48, 38), Typeface.BOLD)
    val tableHeaderPaint = textPaint(30f, Color.rgb(26, 32, 29), Typeface.BOLD)
    val paints = MarkdownPaints(
      body = bodyPaint,
      bold = boldPaint,
      code = codePaint,
      link = linkPaint,
      headingOne = headingOnePaint,
      headingTwo = headingTwoPaint,
      headingThree = headingThreePaint,
      error = errorPaint,
      errorBold = errorBoldPaint,
      tableHeader = tableHeaderPaint
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
    return ImageRenderLayout(
      width = width,
      padding = padding,
      bubblePadding = bubblePadding,
      titlePaint = titlePaint,
      metaPaint = metaPaint,
      rolePaint = rolePaint,
      paints = paints,
      titleLines = titleLines,
      metaLines = metaLines,
      messages = messageLayouts,
      height = ceil(height.toDouble()).toInt().coerceAtLeast(1)
    )
  }

  private fun renderBitmap(
    export: ConversationExport,
    maxHeight: Int,
    config: Bitmap.Config
  ): Bitmap {
    val layout = buildImageRenderLayout(export)
    require(layout.height <= maxHeight) {
      "Image height ${layout.height} exceeds $maxHeight"
    }
    val bitmap = Bitmap.createBitmap(layout.width, layout.height, config)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(246, 243, 236))

    var y = layout.padding
    layout.titleLines.forEach { line ->
      canvas.drawText(line, layout.padding, y + layout.titlePaint.textSize, layout.titlePaint)
      y += TitleLineHeight
    }
    y += 8
    if (layout.metaLines.isEmpty()) {
      canvas.drawText("AI Chat 导出", layout.padding, y + layout.metaPaint.textSize, layout.metaPaint)
      y += MetaLineHeight
    } else {
      layout.metaLines.forEach { line ->
        canvas.drawText(line, layout.padding, y + layout.metaPaint.textSize, layout.metaPaint)
        y += MetaLineHeight
      }
    }
    y += 24

    layout.messages.forEach { item ->
      val roleName = when (item.role) {
        MessageRole.USER -> "我"
        MessageRole.ASSISTANT -> "AI"
        MessageRole.SYSTEM -> "系统"
        MessageRole.TOOL -> "工具"
      }
      canvas.drawText(
        "$roleName · ${item.time}",
        layout.padding,
        y + layout.rolePaint.textSize,
        layout.rolePaint
      )
      y += RoleRowHeight
      val bubbleHeight = item.blocks.sumOf { it.height.toInt() }.toFloat() + layout.bubblePadding * 2
      val bubbleColor = when {
        item.failed -> Color.rgb(255, 239, 235)
        item.role == MessageRole.USER -> Color.rgb(216, 236, 220)
        item.role == MessageRole.ASSISTANT -> Color.WHITE
        else -> Color.rgb(231, 232, 226)
      }
      val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bubbleColor }
      canvas.drawRoundRect(
        layout.padding,
        y,
        layout.width - layout.padding,
        y + bubbleHeight,
        30f,
        30f,
        rectPaint
      )
      var textY = y + layout.bubblePadding
      item.blocks.forEach { block ->
        drawMarkdownBlock(
          canvas = canvas,
          block = block,
          left = layout.padding + layout.bubblePadding,
          top = textY,
          right = layout.width - layout.padding - layout.bubblePadding,
          paints = layout.paints
        )
        textY += block.height
      }
      y += bubbleHeight + 22
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
        val style = if (failed) ImageInlineStyle.ERROR else ImageInlineStyle.BODY
        blocks += richTextBlock(
          text = text,
          defaultStyle = style,
          paints = paints,
          maxWidth = maxWidth,
          lineHeight = BodyLineHeight,
          bottomPadding = 12f
        )
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
        tableRows.forEachIndexed { rowIndex, row ->
          val cellStyle = if (rowIndex == 0) ImageInlineStyle.TABLE_HEADER else ImageInlineStyle.BODY
          val rowLines = (0 until columns).maxOf { column ->
            layoutInlineText(
              imageInlineMarkdownSpans(row.getOrNull(column).orEmpty()),
              cellStyle,
              paints,
              columnWidth - 12f
            ).size
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
        val style = when (headingLevel) {
          1 -> ImageInlineStyle.HEADING_ONE
          2 -> ImageInlineStyle.HEADING_TWO
          else -> ImageInlineStyle.HEADING_THREE
        }
        val lineHeight = when (headingLevel) {
          1 -> HeadingOneLineHeight
          2 -> HeadingTwoLineHeight
          else -> HeadingThreeLineHeight
        }
        blocks += richTextBlock(
          text = trimmed.drop(headingLevel).trim(),
          defaultStyle = style,
          paints = paints,
          maxWidth = maxWidth,
          lineHeight = lineHeight,
          bottomPadding = 18f
        )
        return@forEach
      }

      val unordered = listOf("- ", "* ", "+ ").firstOrNull { trimmed.startsWith(it) }
      val orderedMatch = Regex("^(\\d+)[.)]\\s+(.*)$").matchEntire(trimmed)
      when {
        unordered != null -> {
          flushParagraph()
          blocks += richTextBlock(
            text = trimmed.drop(unordered.length).trim(),
            defaultStyle = if (failed) ImageInlineStyle.ERROR else ImageInlineStyle.BODY,
            paints = paints,
            maxWidth = maxWidth,
            lineHeight = BodyLineHeight,
            bottomPadding = 8f,
            marker = "-"
          )
        }
        orderedMatch != null -> {
          flushParagraph()
          blocks += richTextBlock(
            text = orderedMatch.groupValues[2],
            defaultStyle = if (failed) ImageInlineStyle.ERROR else ImageInlineStyle.BODY,
            paints = paints,
            maxWidth = maxWidth,
            lineHeight = BodyLineHeight,
            bottomPadding = 8f,
            marker = "${orderedMatch.groupValues[1]}."
          )
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
    return blocks.ifEmpty {
      listOf(
        richTextBlock(
          text = " ",
          defaultStyle = ImageInlineStyle.BODY,
          paints = paints,
          maxWidth = maxWidth,
          lineHeight = BodyLineHeight,
          bottomPadding = 0f
        )
      )
    }
  }

  private fun drawMarkdownBlock(
    canvas: Canvas,
    block: RenderBlock,
    left: Float,
    top: Float,
    right: Float,
    paints: MarkdownPaints
  ) {
    when (block) {
      is RenderBlock.Text -> {
        val markerWidth = block.marker?.let { marker ->
          paintForStyle(block.defaultStyle, paints).measureText(marker) + 18f
        } ?: 0f
        var lineTop = top
        block.lines.forEachIndexed { index, line ->
          val baseline = lineTop + lineMaxTextSize(line, paints)
          if (index == 0 && block.marker != null) {
            canvas.drawText(block.marker, left, baseline, paintForStyle(block.defaultStyle, paints))
          }
          drawRichTextLine(canvas, line, left + markerWidth, baseline, paints)
          lineTop += block.lineHeight
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
        val columnWidth = (right - left) / block.columns
        var y = top
        block.rows.forEachIndexed { rowIndex, row ->
          val cellStyle = if (rowIndex == 0) ImageInlineStyle.TABLE_HEADER else ImageInlineStyle.BODY
          val rowLineCounts = (0 until block.columns).map { column ->
            layoutInlineText(
              imageInlineMarkdownSpans(row.getOrNull(column).orEmpty()),
              cellStyle,
              paints,
              columnWidth - 14f
            ).size
          }
          val rowHeight = (rowLineCounts.maxOrNull() ?: 1) * TableLineHeight + 22f
          if (rowIndex == 0) {
            canvas.drawRoundRect(left, y, right, y + rowHeight, 8f, 8f, headerPaint)
          }
          canvas.drawLine(left, y, right, y, if (rowIndex == 0) borderPaint else rowLinePaint)
          (0 until block.columns).forEach { column ->
            val cellLeft = left + column * columnWidth
            val lines = layoutInlineText(
              imageInlineMarkdownSpans(row.getOrNull(column).orEmpty()),
              cellStyle,
              paints,
              columnWidth - 14f
            )
            var textTop = y + 10f
            lines.forEach { line ->
              drawRichTextLine(
                canvas,
                line,
                cellLeft + 7f,
                textTop + lineMaxTextSize(line, paints),
                paints
              )
              textTop += TableLineHeight
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

  private fun richTextBlock(
    text: String,
    defaultStyle: ImageInlineStyle,
    paints: MarkdownPaints,
    maxWidth: Float,
    lineHeight: Float,
    bottomPadding: Float,
    marker: String? = null
  ): RenderBlock.Text {
    val markerWidth = marker?.let { paintForStyle(defaultStyle, paints).measureText(it) + 18f } ?: 0f
    val lines = layoutInlineText(
      imageInlineMarkdownSpans(text),
      defaultStyle,
      paints,
      (maxWidth - markerWidth).coerceAtLeast(48f)
    )
    return RenderBlock.Text(
      lines = lines,
      defaultStyle = defaultStyle,
      marker = marker,
      lineHeight = lineHeight,
      height = lines.size * lineHeight + bottomPadding
    )
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

  internal fun imageInlineMarkdownSpans(text: String): List<ImageInlineSpan> {
    val spans = mutableListOf<ImageInlineSpan>()
    fun append(text: String, style: ImageInlineStyle) {
      if (text.isEmpty()) return
      val previous = spans.lastOrNull()
      if (previous != null && previous.style == style) {
        spans[spans.lastIndex] = previous.copy(text = previous.text + text)
      } else {
        spans += ImageInlineSpan(text, style)
      }
    }

    val pattern = Regex(
      """(\*\*[^*]+\*\*)|(`[^`]+`)|(\[[^]]+\]\((?:https?://)?(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?:/[^\s)]*)?\))|(https?://[^\s<>()\[\]{}"']+)|((?<![@\w.-])(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}(?:/[^\s<>()\[\]{}"']+)?)"""
    )
    var start = 0
    pattern.findAll(text).forEach { match ->
      append(text.substring(start, match.range.first), ImageInlineStyle.BODY)
      val value = match.value
      when {
        value.startsWith("**") -> append(value.removePrefix("**").removeSuffix("**"), ImageInlineStyle.BOLD)
        value.startsWith("`") -> append(value.removePrefix("`").removeSuffix("`"), ImageInlineStyle.INLINE_CODE)
        value.startsWith("[") -> append(value.substringAfter('[').substringBefore("]("), ImageInlineStyle.LINK)
        else -> {
          val link = value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '\uFF0C', '\u3002', '\uFF1B', '\uFF1A', '\uFF01', '\uFF1F')
          append(link, ImageInlineStyle.LINK)
          append(value.removePrefix(link), ImageInlineStyle.BODY)
        }
      }
      start = match.range.last + 1
    }
    append(text.substring(start), ImageInlineStyle.BODY)
    return spans.ifEmpty { listOf(ImageInlineSpan("", ImageInlineStyle.BODY)) }
  }

  private fun layoutInlineText(
    spans: List<ImageInlineSpan>,
    defaultStyle: ImageInlineStyle,
    paints: MarkdownPaints,
    maxWidth: Float
  ): List<RenderTextLine> {
    val lines = mutableListOf<RenderTextLine>()
    val current = mutableListOf<ImageInlineSpan>()
    var currentWidth = 0f

    fun appendCurrent(text: String, style: ImageInlineStyle) {
      if (text.isEmpty()) return
      val previous = current.lastOrNull()
      if (previous != null && previous.style == style) {
        current[current.lastIndex] = previous.copy(text = previous.text + text)
      } else {
        current += ImageInlineSpan(text, style)
      }
    }

    fun finishLine() {
      while (current.lastOrNull()?.text?.lastOrNull()?.isWhitespace() == true) {
        val previous = current.removeAt(current.lastIndex)
        val trimmed = previous.text.trimEnd()
        if (trimmed.isNotEmpty()) current += previous.copy(text = trimmed)
      }
      lines += RenderTextLine(current.toList())
      current.clear()
      currentWidth = 0f
    }

    fun appendToken(token: String, style: ImageInlineStyle) {
      if (token.isEmpty()) return
      val paint = paintForStyle(style, paints)
      if (current.isEmpty() && token.all(Char::isWhitespace)) return
      val width = paint.measureText(token)
      if (current.isNotEmpty() && currentWidth + width > maxWidth) {
        finishLine()
      }
      if (current.isEmpty() && width > maxWidth && token.length > 1) {
        var remaining = token
        while (remaining.isNotEmpty()) {
          val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
          val part = remaining.take(count)
          appendCurrent(part, style)
          currentWidth = paint.measureText(part)
          remaining = remaining.drop(count)
          if (remaining.isNotEmpty()) finishLine()
        }
      } else {
        appendCurrent(token, style)
        currentWidth += width
      }
    }

    spans.forEach { span ->
      val style = effectiveInlineStyle(span.style, defaultStyle)
      span.text.split('\n').forEachIndexed { index, part ->
        splitInlineWrapTokens(part).forEach { token -> appendToken(token, style) }
        if (index < span.text.count { it == '\n' }) finishLine()
      }
    }
    if (current.isNotEmpty() || lines.isEmpty()) finishLine()
    return lines
  }

  private fun splitInlineWrapTokens(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    return Regex("\\s+|[A-Za-z0-9_./:@%?&=#+~-]+|.").findAll(text).map { it.value }.toList()
  }

  private fun effectiveInlineStyle(style: ImageInlineStyle, defaultStyle: ImageInlineStyle): ImageInlineStyle {
    return when (style) {
      ImageInlineStyle.BODY -> defaultStyle
      ImageInlineStyle.BOLD -> when (defaultStyle) {
        ImageInlineStyle.ERROR -> ImageInlineStyle.ERROR_BOLD
        ImageInlineStyle.TABLE_HEADER -> ImageInlineStyle.TABLE_HEADER
        else -> ImageInlineStyle.BOLD
      }
      else -> style
    }
  }

  private fun paintForStyle(style: ImageInlineStyle, paints: MarkdownPaints): Paint {
    return when (style) {
      ImageInlineStyle.BODY -> paints.body
      ImageInlineStyle.BOLD -> paints.bold
      ImageInlineStyle.INLINE_CODE -> paints.code
      ImageInlineStyle.LINK -> paints.link
      ImageInlineStyle.HEADING_ONE -> paints.headingOne
      ImageInlineStyle.HEADING_TWO -> paints.headingTwo
      ImageInlineStyle.HEADING_THREE -> paints.headingThree
      ImageInlineStyle.ERROR -> paints.error
      ImageInlineStyle.ERROR_BOLD -> paints.errorBold
      ImageInlineStyle.TABLE_HEADER -> paints.tableHeader
    }
  }

  private fun lineMaxTextSize(line: RenderTextLine, paints: MarkdownPaints): Float {
    return line.spans.maxOfOrNull { paintForStyle(it.style, paints).textSize } ?: paints.body.textSize
  }

  private fun drawRichTextLine(
    canvas: Canvas,
    line: RenderTextLine,
    left: Float,
    baseline: Float,
    paints: MarkdownPaints
  ) {
    var x = left
    line.spans.forEach { span ->
      val paint = paintForStyle(span.style, paints)
      val width = paint.measureText(span.text)
      if (span.style == ImageInlineStyle.INLINE_CODE && span.text.isNotEmpty()) {
        val codeBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(225, 235, 227) }
        canvas.drawRoundRect(
          x - 5f,
          baseline - paint.textSize - 5f,
          x + width + 5f,
          baseline + 10f,
          7f,
          7f,
          codeBackground
        )
      }
      canvas.drawText(span.text, x, baseline, paint)
      if (span.style == ImageInlineStyle.LINK && span.text.isNotEmpty()) {
        val underline = Paint(paint).apply { strokeWidth = 2f }
        canvas.drawLine(x, baseline + 5f, x + width, baseline + 5f, underline)
      }
      x += width
    }
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

  private data class ImageRenderLayout(
    val width: Int,
    val padding: Float,
    val bubblePadding: Float,
    val titlePaint: Paint,
    val metaPaint: Paint,
    val rolePaint: Paint,
    val paints: MarkdownPaints,
    val titleLines: List<String>,
    val metaLines: List<String>,
    val messages: List<RenderMessage>,
    val height: Int
  )

  private data class MarkdownPaints(
    val body: Paint,
    val bold: Paint,
    val code: Paint,
    val link: Paint,
    val headingOne: Paint,
    val headingTwo: Paint,
    val headingThree: Paint,
    val error: Paint,
    val errorBold: Paint,
    val tableHeader: Paint
  )

  internal enum class ImageInlineStyle {
    BODY,
    BOLD,
    INLINE_CODE,
    LINK,
    HEADING_ONE,
    HEADING_TWO,
    HEADING_THREE,
    ERROR,
    ERROR_BOLD,
    TABLE_HEADER
  }

  internal data class ImageInlineSpan(
    val text: String,
    val style: ImageInlineStyle
  )

  private data class RenderTextLine(val spans: List<ImageInlineSpan>)

  private data class RenderMessage(
    val role: MessageRole,
    val failed: Boolean,
    val time: String,
    val blocks: List<RenderBlock>
  )

  private sealed interface RenderBlock {
    val height: Float

    data class Text(
      val lines: List<RenderTextLine>,
      val defaultStyle: ImageInlineStyle,
      val marker: String?,
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
