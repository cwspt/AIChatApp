package com.personal.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.StreamingBubbleMotion

internal data class ToolCallBubbleColors(
  val container: Color,
  val content: Color,
  val metadata: Color,
  val accent: Color,
  val detailContainer: Color
)

private fun toolCallVisualIcon(kind: ToolCallVisualKind) = when (kind) {
  ToolCallVisualKind.SEARCH -> Icons.Outlined.Search
  ToolCallVisualKind.PAGE -> Icons.AutoMirrored.Outlined.OpenInNew
  ToolCallVisualKind.FILE -> Icons.AutoMirrored.Outlined.InsertDriveFile
  ToolCallVisualKind.TOOL -> Icons.Outlined.AttachFile
}

@Composable
internal fun defaultToolCallBubbleColors(botColors: BotBubbleColors? = null): ToolCallBubbleColors {
  return if (botColors == null) {
    ToolCallBubbleColors(
      container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
      content = MaterialTheme.colorScheme.onSurface,
      metadata = MaterialTheme.colorScheme.onSurfaceVariant,
      accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
      detailContainer = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    )
  } else {
    ToolCallBubbleColors(
      container = botColors.container.copy(alpha = 0.74f),
      content = botColors.content,
      metadata = botColors.content.copy(alpha = 0.72f),
      accent = botColors.accent,
      detailContainer = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
    )
  }
}

@Composable
internal fun ToolCallGroupBubble(
  messages: List<ChatMessage>,
  title: String,
  metadata: String?,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onCopy: () -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onFavorite: () -> Unit,
  modifier: Modifier = Modifier,
  colors: ToolCallBubbleColors? = null,
  expanded: Boolean? = null,
  canToggleExpanded: Boolean = true,
  onToggleExpanded: (() -> Unit)? = null,
  streamingBubbleMotion: StreamingBubbleMotion = StreamingBubbleMotion.STANDARD
) {
  if (messages.isEmpty()) return
  val resolvedColors = colors ?: defaultToolCallBubbleColors()
  val details = remember(messages) { messages.map { parseToolCallDetails(it.content) } }
  val visualIcon = toolCallVisualIcon(toolCallGroupVisualKind(details))
  val isStreaming = messages.any { it.status == MessageStatus.STREAMING }
  var localExpanded by remember(messages.first().id) { mutableStateOf(false) }
  val effectiveExpanded = isStreaming || (expanded ?: localExpanded)
  var shareMenuOpen by remember(messages.first().id) { mutableStateOf(false) }
  val toggleExpanded = {
    if (onToggleExpanded != null) {
      onToggleExpanded()
    } else {
      localExpanded = !localExpanded
    }
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = Arrangement.Start
  ) {
    StreamingBubbleFrame(
      streaming = isStreaming,
      motion = streamingBubbleMotion,
      accent = resolvedColors.accent,
      containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else resolvedColors.container,
      contentColor = resolvedColors.content,
      selected = selected,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(0.86f)
        .height(IntrinsicSize.Min),
      baseBorderColor = resolvedColors.accent.copy(alpha = 0.55f)
    ) {
      Box {
        Icon(
          visualIcon,
          contentDescription = null,
          tint = resolvedColors.accent.copy(alpha = 0.10f),
          modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = 18.dp, y = 12.dp)
            .size(88.dp)
        )
        Row {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .width(4.dp)
              .background(
                if (isStreaming && streamingBubbleMotion != StreamingBubbleMotion.OFF) {
                  val pulse = streamingPulse(true)
                  resolvedColors.accent.copy(alpha = 0.58f + 0.42f * pulse)
                } else {
                  resolvedColors.accent
                }
              )
          )
          Column(
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(visualIcon, contentDescription = null, tint = resolvedColors.accent, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(7.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = title,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = resolvedColors.content
                )
                metadata?.takeIf { it.isNotBlank() }?.let {
                  Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = resolvedColors.metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
              IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "复制工具调用", tint = resolvedColors.content)
              }
              Box {
                IconButton(onClick = { shareMenuOpen = true }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Outlined.Share, contentDescription = "分享工具调用", tint = resolvedColors.content)
                }
                DropdownMenu(expanded = shareMenuOpen, onDismissRequest = { shareMenuOpen = false }) {
                  DropdownMenuItem(
                    text = { Text("分享文本") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = {
                      shareMenuOpen = false
                      onShareText()
                    }
                  )
                  DropdownMenuItem(
                    text = { Text("分享长图") },
                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                    onClick = {
                      shareMenuOpen = false
                      onShareImage()
                    }
                  )
                }
              }
              IconButton(
                onClick = onFavorite,
                enabled = messages.none { it.status == MessageStatus.STREAMING },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Outlined.Bookmark, contentDescription = "收藏工具调用", tint = resolvedColors.content)
              }
            }

            Text(
              text = groupToolSummary(details, messages.size, isStreaming),
              maxLines = if (effectiveExpanded) 3 else 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodyMedium,
              color = resolvedColors.content
            )

            if (effectiveExpanded) {
              details.forEachIndexed { index, item ->
                ToolCallDetail(
                  index = index + 1,
                  details = item,
                  colors = resolvedColors
                )
              }
            }

            if (isStreaming) {
              StreamingStatusIndicator(
                text = "工具调用中",
                accent = resolvedColors.accent,
                textColor = resolvedColors.metadata,
                motion = streamingBubbleMotion
              )
            }

            if (selectionMode) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
                if (canSelectRangeTo) {
                  SelectRangeToChip(onClick = onSelectRangeTo)
                }
              }
            }

            if (canToggleExpanded && !isStreaming) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CompactExpandToggle(expanded = effectiveExpanded, onClick = toggleExpanded)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToolCallDetail(
  index: Int,
  details: ToolCallDetails,
  colors: ToolCallBubbleColors
) {
  val visualIcon = toolCallVisualIcon(toolCallVisualKind(details.name))
  Surface(
    color = colors.detailContainer,
    contentColor = colors.content,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(visualIcon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text("工具 $index · ${details.name}", fontWeight = FontWeight.SemiBold, color = colors.content)
      }
      details.query?.takeIf { it.isNotBlank() }?.let {
        ToolCallSection("查询词", it, labelColor = colors.accent, textColor = colors.metadata)
      }
      if (details.openedUrls.isNotEmpty()) {
        ToolCallSection("打开 URL", details.openedUrls.joinToString("\n"), labelColor = colors.accent, textColor = colors.metadata)
      }
      if (details.citations.isNotEmpty()) {
        ToolCallSection(
          "Citation URL",
          details.citations.joinToString("\n\n") { citation ->
            listOfNotNull(citation.title?.takeIf { it.isNotBlank() }, citation.url).joinToString("\n")
          },
          labelColor = colors.accent,
          textColor = colors.metadata
        )
      }
      details.input?.takeIf { it.isNotBlank() }?.let {
        ToolCallSection("原始输入", it, labelColor = colors.accent, textColor = colors.metadata)
      }
      details.output?.takeIf { it.isNotBlank() }?.let {
        ToolCallSection("原始输出", it, labelColor = colors.accent, textColor = colors.metadata)
      }
      if (details.input.isNullOrBlank() && details.output.isNullOrBlank()) {
        Text("暂无详情", style = MaterialTheme.typography.bodySmall, color = colors.metadata)
      }
    }
  }
}

@Composable
internal fun CompactExpandToggle(
  expanded: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.primary,
    shape = RoundedCornerShape(999.dp),
    modifier = modifier
      .height(30.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        modifier = Modifier.size(17.dp)
      )
      Spacer(Modifier.width(3.dp))
      Text(
        if (expanded) "折叠" else "展开",
        style = MaterialTheme.typography.bodySmall
      )
    }
  }
}

@Composable
private fun SelectRangeToChip(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(999.dp)
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = shape,
    modifier = modifier
      .defaultMinSize(minHeight = 30.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
      Text("选择到此", style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
  }
}

@Composable
internal fun ToolCallItem(
  message: ChatMessage,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onCopy: () -> Unit,
  onFavorite: () -> Unit
) {
  val details = remember(message.content) { parseToolCallDetails(message.content) }
  val visualIcon = toolCallVisualIcon(toolCallVisualKind(details.name))
  var expanded by remember(message.id) { mutableStateOf(false) }
  val isSearching = message.status == MessageStatus.STREAMING
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = Arrangement.Start
  ) {
    Surface(
      color = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
      },
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .then(
          if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
          } else {
            Modifier
          }
        )
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(visualIcon, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (details.name == "web_search") {
                if (isSearching) "Searching web" else "Searched web"
              } else {
                if (isSearching) "Running ${details.name}" else "Ran ${details.name}"
              },
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            details.summary?.takeIf { it.isNotBlank() }?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Icon(
            if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = if (expanded) "折叠工具调用" else "展开工具调用",
            modifier = Modifier.size(20.dp)
          )
        }
        if (expanded) {
          Spacer(Modifier.height(8.dp))
          ToolCallSection("工具", details.name)
          details.input?.takeIf { it.isNotBlank() }?.let { ToolCallSection("输入", it) }
          details.output?.takeIf { it.isNotBlank() }?.let { ToolCallSection("输出", it) }
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
              Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
              if (canSelectRangeTo) {
                SelectRangeToChip(onClick = onSelectRangeTo)
              }
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Outlined.ContentCopy, contentDescription = "复制工具调用")
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Outlined.Bookmark, contentDescription = "收藏工具调用")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToolCallSection(
  label: String,
  text: String,
  labelColor: Color = MaterialTheme.colorScheme.primary,
  textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
  Text(label, style = MaterialTheme.typography.bodySmall, color = labelColor, fontWeight = FontWeight.SemiBold)
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = textColor
  )
  Spacer(Modifier.height(6.dp))
}
