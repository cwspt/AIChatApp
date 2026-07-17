package com.personal.aichat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Filter3
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ImageGenerationBackground
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.domain.ImageGenerationOutputFormat
import com.personal.aichat.domain.ImageGenerationQuality
import com.personal.aichat.domain.ImageGenerationSize

internal enum class ComposerPrimaryAction {
  ADD,
  CLOSE_PANEL,
  SEND,
  STOP,
  NONE
}

internal enum class ComposerTool {
  GALLERY,
  CAMERA,
  FILE,
  RETRY,
  INLINE_IMAGES,
  IMAGE_SIZE,
  IMAGE_QUALITY,
  IMAGE_COUNT,
  IMAGE_FORMAT,
  IMAGE_BACKGROUND
}

internal fun resolveComposerPrimaryAction(
  text: String,
  hasAttachments: Boolean,
  isGenerating: Boolean,
  panelOpen: Boolean,
  hasTools: Boolean
): ComposerPrimaryAction = when {
  isGenerating -> ComposerPrimaryAction.STOP
  text.isNotBlank() || hasAttachments -> ComposerPrimaryAction.SEND
  panelOpen -> ComposerPrimaryAction.CLOSE_PANEL
  hasTools -> ComposerPrimaryAction.ADD
  else -> ComposerPrimaryAction.NONE
}

internal fun availableComposerTools(
  attachmentsEnabled: Boolean,
  imageMode: Boolean,
  inlineImagesAvailable: Boolean,
  showRetry: Boolean
): List<ComposerTool> = buildList {
  if (attachmentsEnabled) {
    add(ComposerTool.GALLERY)
    add(ComposerTool.CAMERA)
    if (!imageMode) add(ComposerTool.FILE)
  }
  if (imageMode) {
    add(ComposerTool.IMAGE_SIZE)
    add(ComposerTool.IMAGE_QUALITY)
    add(ComposerTool.IMAGE_COUNT)
    add(ComposerTool.IMAGE_FORMAT)
    add(ComposerTool.IMAGE_BACKGROUND)
  } else {
    if (showRetry) add(ComposerTool.RETRY)
    if (inlineImagesAvailable) add(ComposerTool.INLINE_IMAGES)
  }
}

@Composable
internal fun Composer(
  input: TextFieldValue,
  attachments: List<ChatAttachment>,
  attachmentsEnabled: Boolean,
  imageMode: Boolean = false,
  imageOptions: ImageGenerationOptions = ImageGenerationOptions(),
  inlineImagesAvailable: Boolean = false,
  inlineImagesAllowed: Boolean = false,
  panelStateKey: Any? = null,
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
  onInlineImagesAllowed: (Boolean) -> Unit = {},
  showRetry: Boolean = true
) {
  var panelOpen by remember(
    panelStateKey,
    attachmentsEnabled,
    imageMode,
    inlineImagesAvailable,
    showRetry
  ) { mutableStateOf(false) }
  var expandedSetting by remember(panelStateKey, imageMode) { mutableStateOf<ComposerTool?>(null) }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val tools = remember(attachmentsEnabled, imageMode, inlineImagesAvailable, showRetry) {
    availableComposerTools(attachmentsEnabled, imageMode, inlineImagesAvailable, showRetry)
  }
  val primaryAction = resolveComposerPrimaryAction(
    text = input.text,
    hasAttachments = attachments.isNotEmpty(),
    isGenerating = isGenerating,
    panelOpen = panelOpen,
    hasTools = tools.isNotEmpty()
  )

  fun closePanel() {
    panelOpen = false
    expandedSetting = null
  }

  fun openPanel() {
    focusManager.clearFocus(force = true)
    keyboardController?.hide()
    panelOpen = true
  }

  fun runAndClose(action: () -> Unit) {
    closePanel()
    action()
  }

  BackHandler(enabled = panelOpen) { closePanel() }
  LaunchedEffect(input.text.isNotBlank(), isGenerating) {
    if (input.text.isNotBlank() || isGenerating) closePanel()
  }

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

    when {
      imageMode -> {
        ComposerStatusBar(
          icon = Icons.Outlined.Tune,
          text = imageSettingsSummary(imageOptions),
          contentDescription = "打开生图设置",
          onClick = {
            if (!isGenerating) {
              if (panelOpen) closePanel() else openPanel()
            }
          }
        )
        Spacer(Modifier.height(6.dp))
      }
      inlineImagesAllowed -> {
        ComposerStatusBar(
          icon = Icons.Outlined.AddPhotoAlternate,
          text = "本次回复允许插图",
          contentDescription = "关闭本次回复插图",
          onClick = { runAndClose { onInlineImagesAllowed(false) } },
          trailingClose = true
        )
        Spacer(Modifier.height(6.dp))
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = input,
        onValueChange = onInput,
        modifier = Modifier
          .weight(1f)
          .onFocusChanged { focusState ->
            if (focusState.isFocused && panelOpen) closePanel()
          },
        minLines = 1,
        maxLines = 5,
        shape = RoundedCornerShape(8.dp),
        placeholder = { Text(if (imageMode) "描述要生成或编辑的图片" else "给当前模型发送消息") }
      )
      if (primaryAction != ComposerPrimaryAction.NONE) {
        Spacer(Modifier.width(8.dp))
        ComposerPrimaryButton(
          action = primaryAction,
          onClick = {
            when (primaryAction) {
              ComposerPrimaryAction.ADD -> openPanel()
              ComposerPrimaryAction.CLOSE_PANEL -> closePanel()
              ComposerPrimaryAction.SEND -> runAndClose(onSend)
              ComposerPrimaryAction.STOP -> runAndClose(onStopGenerating)
              ComposerPrimaryAction.NONE -> Unit
            }
          }
        )
      }
    }

    AnimatedVisibility(
      visible = panelOpen && tools.isNotEmpty(),
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      Column {
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        ComposerToolPanel(
          tools = tools,
          imageOptions = imageOptions,
          inlineImagesAllowed = inlineImagesAllowed,
          expandedSetting = expandedSetting,
          onExpandedSetting = { expandedSetting = it },
          onPickImages = { runAndClose(onPickImages) },
          onTakePhoto = { runAndClose(onTakePhoto) },
          onPickFiles = { runAndClose(onPickFiles) },
          onRetry = { runAndClose(onRetry) },
          onInlineImagesAllowed = { enabled -> runAndClose { onInlineImagesAllowed(enabled) } },
          onImageSize = onImageSize,
          onImageQuality = onImageQuality,
          onImageCount = onImageCount,
          onImageOutputFormat = onImageOutputFormat,
          onImageBackground = onImageBackground
        )
      }
    }
  }
}

@Composable
private fun ComposerPrimaryButton(action: ComposerPrimaryAction, onClick: () -> Unit) {
  val icon = when (action) {
    ComposerPrimaryAction.ADD -> Icons.Outlined.Add
    ComposerPrimaryAction.CLOSE_PANEL -> Icons.Outlined.Close
    ComposerPrimaryAction.SEND -> Icons.AutoMirrored.Outlined.Send
    ComposerPrimaryAction.STOP -> Icons.Outlined.Stop
    ComposerPrimaryAction.NONE -> return
  }
  val description = when (action) {
    ComposerPrimaryAction.ADD -> "展开更多工具"
    ComposerPrimaryAction.CLOSE_PANEL -> "收起更多工具"
    ComposerPrimaryAction.SEND -> "发送"
    ComposerPrimaryAction.STOP -> "停止输出"
    ComposerPrimaryAction.NONE -> ""
  }
  val filled = action == ComposerPrimaryAction.STOP
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
  ) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
      Icon(icon, contentDescription = description)
    }
  }
}

@Composable
private fun ComposerStatusBar(
  icon: ImageVector,
  text: String,
  contentDescription: String,
  onClick: () -> Unit,
  trailingClose: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
      .clickable(onClickLabel = contentDescription, onClick = onClick)
      .padding(start = 10.dp, end = if (trailingClose) 4.dp else 10.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.width(8.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    if (trailingClose) {
      Icon(Icons.Outlined.Close, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
  }
}

@Composable
private fun ComposerToolPanel(
  tools: List<ComposerTool>,
  imageOptions: ImageGenerationOptions,
  inlineImagesAllowed: Boolean,
  expandedSetting: ComposerTool?,
  onExpandedSetting: (ComposerTool?) -> Unit,
  onPickImages: () -> Unit,
  onTakePhoto: () -> Unit,
  onPickFiles: () -> Unit,
  onRetry: () -> Unit,
  onInlineImagesAllowed: (Boolean) -> Unit,
  onImageSize: (ImageGenerationSize) -> Unit,
  onImageQuality: (ImageGenerationQuality) -> Unit,
  onImageCount: (Int) -> Unit,
  onImageOutputFormat: (ImageGenerationOutputFormat) -> Unit,
  onImageBackground: (ImageGenerationBackground) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    tools.chunked(4).forEach { rowTools ->
      Row(modifier = Modifier.fillMaxWidth()) {
        rowTools.forEach { tool ->
          val icon = composerToolIcon(tool)
          val label = composerToolLabel(tool)
          val value = composerToolValue(tool, imageOptions)
          val selected = tool == ComposerTool.INLINE_IMAGES && inlineImagesAllowed
          ComposerToolTile(
            tool = tool,
            icon = icon,
            label = label,
            value = value,
            selected = selected,
            expanded = expandedSetting == tool,
            onClick = {
              when (tool) {
                ComposerTool.GALLERY -> onPickImages()
                ComposerTool.CAMERA -> onTakePhoto()
                ComposerTool.FILE -> onPickFiles()
                ComposerTool.RETRY -> onRetry()
                ComposerTool.INLINE_IMAGES -> onInlineImagesAllowed(!inlineImagesAllowed)
                else -> onExpandedSetting(if (expandedSetting == tool) null else tool)
              }
            },
            onDismissMenu = { onExpandedSetting(null) },
            imageOptions = imageOptions,
            onImageSize = { onImageSize(it); onExpandedSetting(null) },
            onImageQuality = { onImageQuality(it); onExpandedSetting(null) },
            onImageCount = { onImageCount(it); onExpandedSetting(null) },
            onImageOutputFormat = { onImageOutputFormat(it); onExpandedSetting(null) },
            onImageBackground = { onImageBackground(it); onExpandedSetting(null) }
          )
        }
        repeat(4 - rowTools.size) { Spacer(Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
private fun RowScope.ComposerToolTile(
  tool: ComposerTool,
  icon: ImageVector,
  label: String,
  value: String?,
  selected: Boolean,
  expanded: Boolean,
  onClick: () -> Unit,
  onDismissMenu: () -> Unit,
  imageOptions: ImageGenerationOptions,
  onImageSize: (ImageGenerationSize) -> Unit,
  onImageQuality: (ImageGenerationQuality) -> Unit,
  onImageCount: (Int) -> Unit,
  onImageOutputFormat: (ImageGenerationOutputFormat) -> Unit,
  onImageBackground: (ImageGenerationBackground) -> Unit
) {
  Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(86.dp)
        .clickable(onClickLabel = label, onClick = onClick)
        .padding(horizontal = 2.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
      value?.let {
        Text(
          it,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    when (tool) {
      ComposerTool.IMAGE_SIZE -> ComposerOptionMenu(
        expanded,
        ImageGenerationSize.entries,
        imageOptions.size,
        ::imageSizeLabel,
        onDismissMenu,
        onImageSize
      )
      ComposerTool.IMAGE_QUALITY -> ComposerOptionMenu(
        expanded,
        ImageGenerationQuality.entries,
        imageOptions.quality,
        ::imageQualityLabel,
        onDismissMenu,
        onImageQuality
      )
      ComposerTool.IMAGE_COUNT -> ComposerOptionMenu(
        expanded,
        (1..4).toList(),
        imageOptions.count,
        { "$it 张" },
        onDismissMenu,
        onImageCount
      )
      ComposerTool.IMAGE_FORMAT -> ComposerOptionMenu(
        expanded,
        ImageGenerationOutputFormat.entries,
        imageOptions.outputFormat,
        ::imageOutputFormatLabel,
        onDismissMenu,
        onImageOutputFormat
      )
      ComposerTool.IMAGE_BACKGROUND -> ComposerOptionMenu(
        expanded,
        ImageGenerationBackground.entries,
        imageOptions.background,
        ::imageBackgroundLabel,
        onDismissMenu,
        onImageBackground
      )
      else -> Unit
    }
  }
}

@Composable
private fun <T> ComposerOptionMenu(
  expanded: Boolean,
  values: List<T>,
  selected: T,
  valueLabel: (T) -> String,
  onDismiss: () -> Unit,
  onSelect: (T) -> Unit
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    values.forEach { value ->
      DropdownMenuItem(
        text = { Text(valueLabel(value)) },
        leadingIcon = {
          if (value == selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
        },
        onClick = { onSelect(value) }
      )
    }
  }
}

private fun composerToolIcon(tool: ComposerTool): ImageVector = when (tool) {
  ComposerTool.GALLERY -> Icons.Outlined.Image
  ComposerTool.CAMERA -> Icons.Outlined.PhotoCamera
  ComposerTool.FILE -> Icons.AutoMirrored.Outlined.InsertDriveFile
  ComposerTool.RETRY -> Icons.Outlined.Refresh
  ComposerTool.INLINE_IMAGES -> Icons.Outlined.AddPhotoAlternate
  ComposerTool.IMAGE_SIZE -> Icons.Outlined.AspectRatio
  ComposerTool.IMAGE_QUALITY -> Icons.Outlined.HighQuality
  ComposerTool.IMAGE_COUNT -> Icons.Outlined.Filter3
  ComposerTool.IMAGE_FORMAT -> Icons.Outlined.Image
  ComposerTool.IMAGE_BACKGROUND -> Icons.Outlined.Opacity
}

private fun composerToolLabel(tool: ComposerTool): String = when (tool) {
  ComposerTool.GALLERY -> "相册"
  ComposerTool.CAMERA -> "拍照"
  ComposerTool.FILE -> "文件"
  ComposerTool.RETRY -> "重试"
  ComposerTool.INLINE_IMAGES -> "回复插图"
  ComposerTool.IMAGE_SIZE -> "尺寸"
  ComposerTool.IMAGE_QUALITY -> "质量"
  ComposerTool.IMAGE_COUNT -> "数量"
  ComposerTool.IMAGE_FORMAT -> "格式"
  ComposerTool.IMAGE_BACKGROUND -> "背景"
}

private fun composerToolValue(tool: ComposerTool, options: ImageGenerationOptions): String? = when (tool) {
  ComposerTool.IMAGE_SIZE -> imageSizeLabel(options.size)
  ComposerTool.IMAGE_QUALITY -> imageQualityLabel(options.quality)
  ComposerTool.IMAGE_COUNT -> "${options.count} 张"
  ComposerTool.IMAGE_FORMAT -> imageOutputFormatLabel(options.outputFormat)
  ComposerTool.IMAGE_BACKGROUND -> imageBackgroundLabel(options.background)
  else -> null
}

private fun imageSettingsSummary(options: ImageGenerationOptions): String {
  val quality = when (options.quality) {
    ImageGenerationQuality.AUTO -> "自动质量"
    else -> "${imageQualityLabel(options.quality)}质量"
  }
  return listOf(
    imageSizeLabel(options.size),
    quality,
    "${options.count} 张",
    imageOutputFormatLabel(options.outputFormat),
    imageBackgroundLabel(options.background)
  ).joinToString(" · ")
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
  ImageGenerationBackground.AUTO -> "自动背景"
  ImageGenerationBackground.OPAQUE -> "不透明"
  ImageGenerationBackground.TRANSPARENT -> "透明"
}
