package com.personal.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ImageGenerationBackground
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.domain.ImageGenerationOutputFormat
import com.personal.aichat.domain.ImageGenerationQuality
import com.personal.aichat.domain.ImageGenerationSize

@Composable
internal fun Composer(
  input: TextFieldValue,
  attachments: List<ChatAttachment>,
  attachmentsEnabled: Boolean,
  imageMode: Boolean = false,
  imageOptions: ImageGenerationOptions = ImageGenerationOptions(),
  onInput: (TextFieldValue) -> Unit,
  onSend: () -> Unit,
  onRetry: () -> Unit,
  onPickImages: () -> Unit,
  onPickFiles: () -> Unit,
  onTakePhoto: () -> Unit,
  onRemoveAttachment: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  isGenerating: Boolean,
  onStopGenerating: () -> Unit,
  onImageSize: (ImageGenerationSize) -> Unit = {},
  onImageQuality: (ImageGenerationQuality) -> Unit = {},
  onImageCount: (Int) -> Unit = {},
  onImageOutputFormat: (ImageGenerationOutputFormat) -> Unit = {},
  onImageBackground: (ImageGenerationBackground) -> Unit = {},
  showRetry: Boolean = true
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .imePadding()
      .navigationBarsPadding()
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    if (attachments.isNotEmpty()) {
      AttachmentStrip(
        attachments = attachments,
        onOpenAttachment = onOpenAttachment,
        onRemoveAttachment = onRemoveAttachment,
        compact = true
      )
      Spacer(Modifier.height(6.dp))
    }
    if (imageMode) {
      ImageGenerationControls(
        options = imageOptions,
        onSize = onImageSize,
        onQuality = onImageQuality,
        onCount = onImageCount,
        onOutputFormat = onImageOutputFormat,
        onBackground = onImageBackground
      )
      Spacer(Modifier.height(6.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      var attachMenuOpen by remember { mutableStateOf(false) }
      if (attachmentsEnabled) {
        Box {
          IconButton(onClick = { attachMenuOpen = true }) {
            Icon(Icons.Outlined.AttachFile, contentDescription = "添加附件", tint = MaterialTheme.colorScheme.primary)
          }
          DropdownMenu(expanded = attachMenuOpen, onDismissRequest = { attachMenuOpen = false }) {
            DropdownMenuItem(
              text = { Text("选择图片") },
              leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
              onClick = {
                attachMenuOpen = false
                onPickImages()
              }
            )
            DropdownMenuItem(
              text = { Text("拍摄照片") },
              leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
              onClick = {
                attachMenuOpen = false
                onTakePhoto()
              }
            )
            DropdownMenuItem(
              text = { Text("选择文件") },
              leadingIcon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
              enabled = !imageMode,
              onClick = {
                attachMenuOpen = false
                onPickFiles()
              }
            )
          }
        }
      }
      OutlinedTextField(
        value = input,
        onValueChange = onInput,
        modifier = Modifier.weight(1f),
        minLines = 1,
        maxLines = 5,
        placeholder = { Text(if (imageMode) "描述要生成或编辑的图片" else "给当前模型发送消息") }
      )
      Spacer(Modifier.width(8.dp))
      if (!isGenerating && showRetry) {
        IconButton(onClick = onRetry) {
          Icon(Icons.Outlined.Refresh, contentDescription = "重试上一条", tint = MaterialTheme.colorScheme.primary)
        }
      }
      if (isGenerating) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.primary,
          tonalElevation = 1.dp
        ) {
          IconButton(onClick = onStopGenerating) {
            Icon(
              Icons.Outlined.Stop,
              contentDescription = "停止输出",
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
        }
      } else {
        val canSend = input.text.isNotBlank() || (attachmentsEnabled && attachments.isNotEmpty())
        IconButton(onClick = onSend, enabled = canSend) {
          Icon(
            Icons.AutoMirrored.Outlined.Send,
            contentDescription = "发送",
            tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun ImageGenerationControls(
  options: ImageGenerationOptions,
  onSize: (ImageGenerationSize) -> Unit,
  onQuality: (ImageGenerationQuality) -> Unit,
  onCount: (Int) -> Unit,
  onOutputFormat: (ImageGenerationOutputFormat) -> Unit,
  onBackground: (ImageGenerationBackground) -> Unit
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    item {
      ImageOptionMenu(
        label = "尺寸 ${imageSizeLabel(options.size)}",
        values = ImageGenerationSize.entries,
        selected = options.size,
        valueLabel = ::imageSizeLabel,
        onSelect = onSize
      )
    }
    item {
      ImageOptionMenu(
        label = "质量 ${imageQualityLabel(options.quality)}",
        values = ImageGenerationQuality.entries,
        selected = options.quality,
        valueLabel = ::imageQualityLabel,
        onSelect = onQuality
      )
    }
    item {
      ImageCountMenu(count = options.count, onCount = onCount)
    }
    item {
      ImageOptionMenu(
        label = "格式 ${imageOutputFormatLabel(options.outputFormat)}",
        values = ImageGenerationOutputFormat.entries,
        selected = options.outputFormat,
        valueLabel = ::imageOutputFormatLabel,
        onSelect = onOutputFormat
      )
    }
    item {
      ImageOptionMenu(
        label = "背景 ${imageBackgroundLabel(options.background)}",
        values = ImageGenerationBackground.entries,
        selected = options.background,
        valueLabel = ::imageBackgroundLabel,
        onSelect = onBackground
      )
    }
  }
}

@Composable
private fun <T> ImageOptionMenu(
  label: String,
  values: List<T>,
  selected: T,
  valueLabel: (T) -> String,
  onSelect: (T) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    AssistChip(onClick = { expanded = true }, label = { Text(label) })
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      values.forEach { value ->
        DropdownMenuItem(
          text = { Text(valueLabel(value)) },
          leadingIcon = {
            if (value == selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
          },
          onClick = {
            expanded = false
            onSelect(value)
          }
        )
      }
    }
  }
}

@Composable
private fun ImageCountMenu(count: Int, onCount: (Int) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    AssistChip(onClick = { expanded = true }, label = { Text("数量 $count") })
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      (1..4).forEach { value ->
        DropdownMenuItem(
          text = { Text("$value 张") },
          leadingIcon = {
            if (value == count) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
          },
          onClick = {
            expanded = false
            onCount(value)
          }
        )
      }
    }
  }
}

private fun imageSizeLabel(size: ImageGenerationSize): String = when (size) {
  ImageGenerationSize.AUTO -> "自动"
  ImageGenerationSize.SQUARE -> "方图"
  ImageGenerationSize.LANDSCAPE -> "横图"
  ImageGenerationSize.PORTRAIT -> "竖图"
}

private fun imageQualityLabel(quality: ImageGenerationQuality): String = when (quality) {
  ImageGenerationQuality.AUTO -> "自动"
  ImageGenerationQuality.LOW -> "低"
  ImageGenerationQuality.MEDIUM -> "中"
  ImageGenerationQuality.HIGH -> "高"
}

private fun imageOutputFormatLabel(format: ImageGenerationOutputFormat): String = when (format) {
  ImageGenerationOutputFormat.PNG -> "PNG"
  ImageGenerationOutputFormat.JPEG -> "JPEG"
  ImageGenerationOutputFormat.WEBP -> "WEBP"
}

private fun imageBackgroundLabel(background: ImageGenerationBackground): String = when (background) {
  ImageGenerationBackground.AUTO -> "自动"
  ImageGenerationBackground.OPAQUE -> "不透明"
  ImageGenerationBackground.TRANSPARENT -> "透明"
}
