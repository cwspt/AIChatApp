package com.personal.aichat.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.ChatBackgroundPreset
import kotlin.math.roundToInt

internal fun ChatBackgroundPreset.matchesBackgroundPresetQuery(query: String): Boolean {
  val tokens = query
    .trim()
    .lowercase()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
  if (tokens.isEmpty()) return true
  val searchable = "${title.lowercase()}\n${cleanCategory().orEmpty().lowercase()}\n${content.lowercase()}"
  return tokens.all { token -> searchable.contains(token) }
}

internal fun ChatBackgroundPreset.cleanCategory(): String? = category?.trim()?.takeIf { it.isNotBlank() }

@Composable
internal fun AttachmentLimitSlider(
  title: String,
  description: String,
  valueMb: Int,
  range: IntRange,
  onValueChange: (Int) -> Unit
) {
  val cleanRange = if (range.first <= range.last) range else range.last..range.first
  val cleanValue = valueMb.coerceIn(cleanRange.first, cleanRange.last)
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
          description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text("${cleanValue}MB", fontWeight = FontWeight.SemiBold)
    }
    Slider(
      value = cleanValue.toFloat(),
      onValueChange = { next -> onValueChange(next.roundToInt().coerceIn(cleanRange.first, cleanRange.last)) },
      valueRange = cleanRange.first.toFloat()..cleanRange.last.toFloat(),
      steps = (cleanRange.last - cleanRange.first - 1).coerceAtLeast(0)
    )
  }
}

@Composable
internal fun BackgroundPresetImportDialog(
  onDismiss: () -> Unit,
  onImport: (String) -> Unit
) {
  var text by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("导入背景预设") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "粘贴从本 App 导出的背景预设 JSON。导入会追加为新预设，不会覆盖已有背景。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("背景预设 JSON") },
          minLines = 8,
          maxLines = 12,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = { onImport(text) }, enabled = text.isNotBlank()) {
        Text("导入")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
internal fun ProviderConfigImportDialog(
  onDismiss: () -> Unit,
  onImport: (String) -> Unit
) {
  var text by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("导入 API 配置文本") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "粘贴从本 App 导出的 JSON 配置文本或二维码内容。导入会新增 Provider；如果 ID 冲突会自动生成新 ID。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("配置 JSON / 二维码内容") },
          minLines = 8,
          maxLines = 12,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = { onImport(text) }, enabled = text.isNotBlank()) {
        Text("导入")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
internal fun ProviderConfigQrDialog(
  qrText: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val qrBitmap = remember(qrText) { renderQrBitmap(qrText) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("API 配置二维码") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "二维码包含 API Key，请只展示给可信设备扫码。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (qrBitmap != null) {
          Image(
            bitmap = qrBitmap,
            contentDescription = "API 配置二维码",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .fillMaxWidth()
              .heightIn(min = 240.dp, max = 360.dp)
          )
        } else {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 280.dp)
          ) {
            SelectionContainer {
              Text(
                text = qrText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                  .padding(10.dp)
                  .verticalScroll(rememberScrollState())
              )
            }
          }
          Text(
            "配置内容过长，无法生成单个二维码；可复制内容后用文本导入。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("完成")
      }
    },
    dismissButton = {
      TextButton(onClick = { copyToClipboard(context, qrText) }) {
        Text("复制内容")
      }
    }
  )
}

private fun renderQrBitmap(text: String, sizePx: Int = 960): ImageBitmap? {
  return runCatching {
    val hints = mapOf(
      EncodeHintType.CHARACTER_SET to "UTF-8",
      EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
      EncodeHintType.MARGIN to 1
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (y in 0 until sizePx) {
      for (x in 0 until sizePx) {
        bitmap.setPixel(
          x,
          y,
          if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        )
      }
    }
    bitmap.asImageBitmap()
  }.getOrNull()
}

@Composable
internal fun BackgroundPresetSettingsRow(
  preset: ChatBackgroundPreset,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(preset.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          preset.cleanCategory()?.let { category ->
            Text("#$category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          }
          Text(
            preset.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onEdit) {
          Icon(Icons.Outlined.Edit, contentDescription = "编辑背景预设")
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Outlined.Delete, contentDescription = "删除背景预设")
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onMoveUp, enabled = canMoveUp) {
          Text("上移")
        }
        TextButton(onClick = onMoveDown, enabled = canMoveDown) {
          Text("下移")
        }
      }
    }
  }
}

@Composable
internal fun BackgroundPresetEditorDialog(
  preset: ChatBackgroundPreset?,
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit
) {
  var title by remember(preset?.id) { mutableStateOf(preset?.title.orEmpty()) }
  var category by remember(preset?.id) { mutableStateOf(preset?.cleanCategory().orEmpty()) }
  var content by remember(preset?.id) { mutableStateOf(preset?.content.orEmpty()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (preset == null) "新增背景预设" else "编辑背景预设") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("名称") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("分类，可选") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("背景内容") },
          minLines = 5,
          maxLines = 10,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, content, category) },
        enabled = content.isNotBlank()
      ) {
        Text("保存")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      content()
    }
  }
}

internal fun AppThemePalette.previewColor(): Color = when (this) {
  AppThemePalette.MOSS -> Color(0xFF2F5E47)
  AppThemePalette.OCEAN -> Color(0xFF1F6D8C)
  AppThemePalette.SAKURA -> Color(0xFF9D3F68)
  AppThemePalette.AMBER -> Color(0xFF7A4B12)
}
