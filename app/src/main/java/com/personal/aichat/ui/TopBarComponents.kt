package com.personal.aichat.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ContextCapacity
import com.personal.aichat.domain.ContextCapacityStatus
import com.personal.aichat.domain.ConversationType
import com.personal.aichat.domain.GroupChatRoom

@Composable
internal fun ConversationShareMenu(
  selectionMode: Boolean,
  selectedCount: Int,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { menuOpen = true }) {
      Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text(if (selectionMode) "退出多选" else "多选消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        onClick = {
          menuOpen = false
          onToggleSelectionMode(!selectionMode)
        }
      )
      DropdownMenuItem(
        text = { Text("分享全文文本") },
        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareText()
        }
      )
      DropdownMenuItem(
        text = { Text("分享选中消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("生成长图分享") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareImage()
        }
      )
      DropdownMenuItem(
        text = { Text("选中消息生成长图") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelectedImage()
        }
      )
      DropdownMenuItem(
        text = { Text("导出 Markdown 文件") },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareMarkdown()
        }
      )
    }
  }
}

internal data class TopMetadataItem(
  val text: String,
  val status: ContextCapacityStatus? = null
)

private fun conversationMetadataItems(state: ChatUiState, conversation: ChatConversation?): List<TopMetadataItem> {
  if (conversation == null) return listOf(TopMetadataItem("未选择配置"))
  val providerName = state.providers.firstOrNull { it.id == conversation.providerId }?.displayName ?: conversation.providerId
  val items = mutableListOf<TopMetadataItem>()
  if (conversation.type == ConversationType.IMAGE) {
    items += TopMetadataItem("生图")
  }
  items += TopMetadataItem(conversationGroupLabel(conversation.groupName))
  items += TopMetadataItem(providerName)
  items += TopMetadataItem(conversation.model)
  if (state.isSelectedConversationStreaming) {
    items += TopMetadataItem("输出中")
  }
  formatContextCapacity(state.selectedContextCapacity)?.let {
    items += TopMetadataItem(it, state.selectedContextCapacity?.status)
  }
  return items
}

internal fun groupMetadataItems(state: ChatUiState, group: GroupChatRoom?, memberCount: Int): List<TopMetadataItem> {
  if (group == null) return listOf(TopMetadataItem("选择或新建一个群聊"))
  val items = mutableListOf(
    TopMetadataItem(group.topic.ifBlank { "手动点名机器人轮流发言" }),
    TopMetadataItem("$memberCount 个成员")
  )
  if (state.isSelectedGroupAutoPlaying) {
    items += TopMetadataItem("轮流发言中")
  } else if (state.isSelectedGroupStreaming) {
    items += TopMetadataItem("输出中")
  }
  formatContextCapacity(state.selectedGroupContextCapacity)?.let {
    items += TopMetadataItem(it, state.selectedGroupContextCapacity?.status)
  }
  return items
}

@Composable
internal fun TopMetadataStrip(items: List<TopMetadataItem>, modifier: Modifier = Modifier) {
  if (items.isEmpty()) return
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items.forEach { item ->
      TopMetadataPill(item)
    }
  }
}

@Composable
private fun TopMetadataPill(item: TopMetadataItem) {
  val background = when (item.status) {
    ContextCapacityStatus.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    ContextCapacityStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
  }
  val content = when (item.status) {
    ContextCapacityStatus.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
    ContextCapacityStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  Surface(
    color = background,
    contentColor = content,
    shape = RoundedCornerShape(8.dp)
  ) {
    Text(
      text = item.text,
      style = MaterialTheme.typography.bodySmall,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
internal fun TopBar(
  state: ChatUiState,
  onOpenConversationDrawer: () -> Unit,
  onNewChat: () -> Unit,
  onOpenSettings: () -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit,
  onCompressContext: () -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit
) {
  val selectedConversation = state.selectedConversation
  CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onOpenConversationDrawer) {
          Icon(Icons.Outlined.Menu, contentDescription = "打开聊天列表")
        }
        Text(
          text = selectedConversation?.title ?: "AI 聊天",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNewChat) {
          Icon(Icons.Outlined.Add, contentDescription = "新建对话")
        }
        IconButton(onClick = onOpenSettings) {
          Icon(Icons.Outlined.Settings, contentDescription = "设置")
        }
        if (selectedConversation != null) {
          ConversationOverflowMenu(
            conversation = selectedConversation,
            selectionMode = state.messageSelectionMode,
            selectedCount = state.selectedMessageIds.size,
            onTogglePin = onTogglePin,
            onArchive = onArchive,
            onDelete = onDelete,
            onRename = onRename,
            onToggleSelectionMode = onToggleSelectionMode,
            onShareText = onShareText,
            onShareSelected = onShareSelected,
            onShareImage = onShareImage,
            onShareSelectedImage = onShareSelectedImage,
            onShareMarkdown = onShareMarkdown,
            onCompressContext = onCompressContext,
            contextCompressionEnabled = selectedConversation.id !in state.compressingConversationIds,
            onFavoriteSelected = onFavoriteSelected,
            onAppendSelectedToFavorite = onAppendSelectedToFavorite
          )
        }
      }
      TopMetadataStrip(
        items = conversationMetadataItems(state, selectedConversation),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
      )
    }
  }
}

@Composable
private fun ConversationOverflowMenu(
  conversation: ChatConversation,
  selectionMode: Boolean,
  selectedCount: Int,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit,
  onCompressContext: () -> Unit,
  contextCompressionEnabled: Boolean,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var editing by remember(conversation.id) { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  var title by remember(conversation.id) { mutableStateOf(conversation.title) }
  var groupName by remember(conversation.id) { mutableStateOf(conversation.groupName) }

  Box {
    IconButton(onClick = { menuOpen = true }) {
      Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text(if (selectionMode) "退出多选" else "多选消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        onClick = {
          menuOpen = false
          onToggleSelectionMode(!selectionMode)
        }
      )
      DropdownMenuItem(
        text = { Text("分享全文文本") },
        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareText()
        }
      )
      DropdownMenuItem(
        text = { Text("分享选中消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("收藏选中消息") },
        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onFavoriteSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("追加到已有收藏") },
        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onAppendSelectedToFavorite()
        }
      )
      DropdownMenuItem(
        text = { Text("生成长图分享") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareImage()
        }
      )
      DropdownMenuItem(
        text = { Text("选中消息生成长图") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelectedImage()
        }
      )
      DropdownMenuItem(
        text = { Text("导出 Markdown 文件") },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareMarkdown()
        }
      )
      DropdownMenuItem(
        text = { Text("立即压缩上下文") },
        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
        enabled = contextCompressionEnabled,
        onClick = {
          menuOpen = false
          onCompressContext()
        }
      )
      DropdownMenuItem(
        text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
        onClick = {
          menuOpen = false
          onTogglePin(conversation.id, conversation.isPinned)
        }
      )
      DropdownMenuItem(
        text = { Text("编辑对话") },
        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        onClick = {
          menuOpen = false
          editing = true
        }
      )
      DropdownMenuItem(
        text = { Text("移动到文件夹") },
        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
        onClick = {
          menuOpen = false
          moving = true
        }
      )
      DropdownMenuItem(
        text = { Text("归档") },
        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
        onClick = {
          menuOpen = false
          onArchive(conversation.id)
        }
      )
      DropdownMenuItem(
        text = { Text("删除") },
        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        onClick = {
          menuOpen = false
          onDelete(conversation.id)
        }
      )
    }
  }

  if (editing) {
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("编辑对话") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("分组") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          onRename(conversation.id, title, groupName)
          editing = false
        }) {
          Text("保存")
        }
      },
      dismissButton = {
        TextButton(onClick = { editing = false }) {
          Text("取消")
        }
      }
    )
  }
  if (moving) {
    MoveConversationDialog(
      conversationTitle = conversation.title,
      initialGroupName = conversation.groupName,
      onDismiss = { moving = false },
      onMove = { targetGroup ->
        onRename(conversation.id, conversation.title, targetGroup)
        moving = false
      }
    )
  }
}

private fun formatContextCapacity(capacity: ContextCapacity?): String? {
  if (capacity == null) return null
  val percent = capacity.usedPercent
  if (percent == null || capacity.windowTokens == null) return "上下文上限未知"
  val remaining = capacity.remainingTokens ?: 0
  val summary = if (capacity.hasSummary) " · 已压缩" else ""
  return "约 $percent% · 剩余约 ${formatTokenCount(remaining)} tokens$summary"
}
