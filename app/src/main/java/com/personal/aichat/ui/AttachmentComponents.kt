package com.personal.aichat.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.MessageContentPart
import com.personal.aichat.domain.MessageContentPartStatus
import com.personal.aichat.domain.MessageContentPartType
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

internal fun openAttachment(context: Context, attachment: ChatAttachment) {
  val file = File(attachment.localPath)
  if (!file.exists()) return
  runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, attachment.mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, attachment.displayName))
  }.onFailure {
    Toast.makeText(context, "无法用系统应用打开该文件", Toast.LENGTH_SHORT).show()
  }
}

internal fun ChatAttachment.canPreviewInApp(): Boolean = isPdfAttachment() || isTextAttachment()

private fun ChatAttachment.isPdfAttachment(): Boolean {
  return mimeType.equals("application/pdf", ignoreCase = true) || displayName.endsWith(".pdf", ignoreCase = true)
}

private fun ChatAttachment.isTextAttachment(): Boolean {
  val lowerMimeType = mimeType.lowercase(Locale.getDefault())
  val lowerName = displayName.lowercase(Locale.getDefault())
  return lowerMimeType.startsWith("text/") ||
    lowerMimeType in setOf(
      "application/json",
      "application/xml",
      "application/javascript",
      "application/x-javascript",
      "application/yaml",
      "application/x-yaml"
    ) ||
    lowerName.endsWith(".txt") ||
    lowerName.endsWith(".md") ||
    lowerName.endsWith(".json") ||
    lowerName.endsWith(".xml") ||
    lowerName.endsWith(".csv") ||
    lowerName.endsWith(".log") ||
    lowerName.endsWith(".yaml") ||
    lowerName.endsWith(".yml")
}

private fun readAttachmentPreviewText(path: String, maxChars: Int = 40_000): String? {
  val file = File(path)
  if (!file.exists() || !file.isFile) return null
  return runCatching {
    file.bufferedReader().use { reader ->
      val buffer = CharArray(maxChars + 1)
      val count = reader.read(buffer, 0, buffer.size).coerceAtLeast(0)
      buildString {
        append(buffer.concatToString(0, count.coerceAtMost(maxChars)))
        if (count > maxChars) append("\n\n... 预览已截断")
      }
    }
  }.getOrNull()
}

private data class PdfPagePreview(
  val bitmap: androidx.compose.ui.graphics.ImageBitmap?,
  val pageCount: Int
)

private fun renderPdfFirstPage(path: String): PdfPagePreview {
  val file = File(path)
  if (!file.exists() || !file.isFile) return PdfPagePreview(bitmap = null, pageCount = 0)
  return runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
      PdfRenderer(descriptor).use { renderer ->
        if (renderer.pageCount <= 0) {
          PdfPagePreview(bitmap = null, pageCount = 0)
        } else {
          renderer.openPage(0).use { page ->
            val maxWidth = 1200
            val scale = (maxWidth.toFloat() / page.width.toFloat()).coerceAtMost(2f).coerceAtLeast(1f)
            val bitmap = Bitmap.createBitmap(
              (page.width * scale).roundToInt().coerceAtLeast(1),
              (page.height * scale).roundToInt().coerceAtLeast(1),
              Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            PdfPagePreview(bitmap = bitmap.asImageBitmap(), pageCount = renderer.pageCount)
          }
        }
      }
    }
  }.getOrDefault(PdfPagePreview(bitmap = null, pageCount = 0))
}

private fun formatAttachmentSize(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024f
  if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
  val mb = kb / 1024f
  return String.format(Locale.getDefault(), "%.1f MB", mb)
}

@Composable
internal fun AttachmentStrip(
  attachments: List<ChatAttachment>,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onRemoveAttachment: ((String) -> Unit)?,
  compact: Boolean
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    items(attachments, key = { it.id }) { attachment ->
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (compact) 0.88f else 0.64f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onOpenAttachment(attachment) }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          AttachmentIconOrThumbnail(attachment)
          Spacer(Modifier.width(6.dp))
          Column(modifier = Modifier.width(if (compact) 132.dp else 180.dp)) {
            Text(
              text = attachment.displayName,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = formatAttachmentSize(attachment.sizeBytes),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
              maxLines = 1
            )
          }
          if (onRemoveAttachment != null) {
            IconButton(
              onClick = { onRemoveAttachment(attachment.id) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "移除附件", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AttachmentIconOrThumbnail(attachment: ChatAttachment) {
  if (attachment.isImage) {
    val bitmap = remember(attachment.localPath) {
      BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
    }
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(42.dp)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
      )
      return
    }
  }
  Icon(
    if (attachment.isImage) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.InsertDriveFile,
    contentDescription = null,
    modifier = Modifier.size(18.dp)
  )
}

@Composable
internal fun GeneratedImageGrid(
  attachments: List<ChatAttachment>,
  onOpenAttachment: (ChatAttachment) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    attachments.forEach { attachment ->
      val bitmap = remember(attachment.localPath) {
        BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenAttachment(attachment) }
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = attachment.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 180.dp, max = 420.dp)
              .padding(4.dp)
          )
        } else {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Outlined.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(attachment.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}

@Composable
internal fun MessageContentRenderer(
  message: ChatMessage,
  interactiveLinks: Boolean,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onRetryImage: (String) -> Unit
) {
  OrderedMessageContentRenderer(
    content = message.content,
    parts = message.contentParts,
    attachments = message.attachments,
    interactiveLinks = interactiveLinks,
    onOpenAttachment = onOpenAttachment,
    onRetryImage = onRetryImage
  )
}

@Composable
internal fun OrderedMessageContentRenderer(
  content: String,
  parts: List<MessageContentPart>,
  attachments: List<ChatAttachment>,
  interactiveLinks: Boolean,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onRetryImage: ((String) -> Unit)? = null
) {
  if (parts.none { it.type == MessageContentPartType.IMAGE }) {
    MarkdownPreview(content, interactiveLinks = interactiveLinks)
    return
  }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    parts.forEach { part ->
      when (part.type) {
        MessageContentPartType.TEXT -> if (part.text.isNotBlank()) {
          MarkdownPreview(part.text, interactiveLinks = interactiveLinks)
        }
        MessageContentPartType.IMAGE -> {
          val attachment = part.attachmentId?.let { id -> attachments.firstOrNull { it.id == id } }
          val ratio = if (part.width != null && part.height != null && part.width > 0 && part.height > 0) {
            (part.width.toFloat() / part.height.toFloat()).coerceIn(0.25f, 4f)
          } else {
            1f
          }
          when (part.status) {
            MessageContentPartStatus.GENERATING -> Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
            MessageContentPartStatus.COMPLETE -> {
              val bitmap = remember(attachment?.localPath) {
                attachment?.localPath?.let(BitmapFactory::decodeFile)?.asImageBitmap()
              }
              if (attachment != null && bitmap != null) {
                Image(
                  bitmap = bitmap,
                  contentDescription = part.revisedPrompt ?: part.prompt ?: attachment.displayName,
                  contentScale = ContentScale.Fit,
                  modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .heightIn(max = 420.dp)
                    .clickable { onOpenAttachment(attachment) }
                )
              } else {
                InlineImageFailure(
                  message = "图片文件不可用",
                  retryEnabled = onRetryImage != null && (!part.prompt.isNullOrBlank() || !part.revisedPrompt.isNullOrBlank()),
                  onRetry = { onRetryImage?.invoke(part.id) }
                )
              }
            }
            MessageContentPartStatus.FAILED -> InlineImageFailure(
              message = part.errorMessage ?: "插图生成失败",
              retryEnabled = onRetryImage != null && (!part.prompt.isNullOrBlank() || !part.revisedPrompt.isNullOrBlank()),
              onRetry = { onRetryImage?.invoke(part.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InlineImageFailure(
  message: String,
  retryEnabled: Boolean,
  onRetry: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    Spacer(Modifier.width(8.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.weight(1f)
    )
    if (retryEnabled) {
      IconButton(onClick = onRetry) {
        Icon(Icons.Outlined.Refresh, contentDescription = "重试这张插图")
      }
    }
  }
}

@Composable
internal fun ImagePreviewDialog(
  attachment: ChatAttachment,
  onDismiss: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val bitmap = remember(attachment.localPath) {
    BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
  }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = attachment.displayName,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭预览")
          }
        }
        Spacer(Modifier.height(8.dp))
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = attachment.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 180.dp, max = 620.dp)
          )
        } else {
          Text("无法在应用内预览这张图片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onOpenExternal) {
            Text("用系统应用打开")
          }
        }
      }
    }
  }
}

@Composable
internal fun AttachmentPreviewDialog(
  attachment: ChatAttachment,
  onDismiss: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val textPreview = remember(attachment.localPath, attachment.mimeType) {
    if (attachment.isTextAttachment()) readAttachmentPreviewText(attachment.localPath) else null
  }
  val pdfPreview = remember(attachment.localPath, attachment.mimeType) {
    if (attachment.isPdfAttachment()) renderPdfFirstPage(attachment.localPath) else null
  }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = attachment.displayName,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${attachment.mimeType} · ${formatAttachmentSize(attachment.sizeBytes)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭预览")
          }
        }
        when {
          textPreview != null -> {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 560.dp)
            ) {
              SelectionContainer {
                Text(
                  text = textPreview,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
                )
              }
            }
          }
          pdfPreview?.bitmap != null -> {
            Text(
              text = "PDF 共 ${pdfPreview.pageCount} 页，当前预览第 1 页。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Image(
              bitmap = pdfPreview.bitmap,
              contentDescription = attachment.displayName,
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 620.dp)
            )
          }
          else -> {
            Text("无法在应用内预览该附件。", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onOpenExternal) {
            Text("用系统应用打开")
          }
        }
      }
    }
  }
}
