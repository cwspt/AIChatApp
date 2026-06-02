package com.personal.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.ConversationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun ConversationDrawer(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onOpenFavorites: () -> Unit,
  onOpenGroups: () -> Unit,
  onNewGroup: () -> Unit,
  onSelectGroup: (String) -> Unit,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onRestore: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onRenameGroup: (String, String) -> Unit,
  onClearGroup: (String) -> Unit
) {
  var collapsedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.28f))
        .clickable(onClick = onDismiss)
    )
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.82f)
        .align(Alignment.CenterStart)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Folder, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("聊天列表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭")
          }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item(key = "favorites-entry") {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFavorites)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Outlined.Bookmark, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("收藏夹", fontWeight = FontWeight.SemiBold)
                  Text(
                    "${state.favoriteSnippets.size} 个收藏片段",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
          item(key = "groups-entry") {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGroups)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("AI 群聊", fontWeight = FontWeight.SemiBold)
                  Text(
                    "${state.groupChats.size} 个群聊 · ${state.aiBots.size} 个机器人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                TextButton(onClick = onNewGroup) {
                  Text("新建")
                }
              }
            }
          }
          if (state.groupChats.isNotEmpty()) {
            item(key = "section-groups") {
              Text("群聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            items(state.groupChats, key = { "group-${it.id}" }) { group ->
              Surface(
                color = if (group.id == state.selectedGroupChatId && state.groupChatPageOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectGroup(group.id) }
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(group.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                  Text(group.topic.ifBlank { "未填写主题" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
          val folderedConversationIds = state.conversationGroups.flatMap { it.conversations }.map { it.id }.toSet()
          val visibleLooseConversations = state.conversations.filterNot { it.id in folderedConversationIds }
          val pinnedConversations = visibleLooseConversations.filter { it.isPinned }
          val normalConversations = visibleLooseConversations.filterNot { it.isPinned }
          val pinnedFolders = state.conversationGroups.filter { group -> group.conversations.any { it.isPinned } }
          val normalFolders = state.conversationGroups.filter { group -> group.conversations.none { it.isPinned } }
          val forkSourceConversations = state.conversations + state.archivedConversations
          drawerSection("置顶", pinnedConversations) { conversation ->
            ConversationDrawerRow(
              conversation,
              state.selectedConversationId,
              conversationForkSourceLabel(conversation, forkSourceConversations),
              onSelectConversation,
              onTogglePin,
              onArchive,
              onDelete,
              onRename
            )
          }
          drawerFolderSection("置顶文件夹", pinnedFolders, collapsedFolders, { key ->
            collapsedFolders = if (key in collapsedFolders) collapsedFolders - key else collapsedFolders + key
          }, state.selectedConversationId, forkSourceConversations, onSelectConversation, onTogglePin, onArchive, onDelete, onRename, onRenameGroup, onClearGroup)
          drawerDatedConversationSections(normalConversations, state.selectedConversationId, forkSourceConversations, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          drawerFolderSection("普通文件夹", normalFolders, collapsedFolders, { key ->
            collapsedFolders = if (key in collapsedFolders) collapsedFolders - key else collapsedFolders + key
          }, state.selectedConversationId, forkSourceConversations, onSelectConversation, onTogglePin, onArchive, onDelete, onRename, onRenameGroup, onClearGroup)
          drawerArchivedSection("已归档", state.archivedConversations, forkSourceConversations, onRestore, onDelete)
        }
      }
    }
  }
}

private fun LazyListScope.drawerDatedConversationSections(
  conversations: List<ChatConversation>,
  selectedConversationId: String?,
  forkSourceConversations: List<ChatConversation>,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  conversations.groupBy { conversationDateBucket(it.updatedAt) }.forEach { (bucket, items) ->
    drawerSection(bucket, items) { conversation ->
      ConversationDrawerRow(conversation, selectedConversationId, conversationForkSourceLabel(conversation, forkSourceConversations), onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
    }
  }
}

private fun LazyListScope.drawerSection(
  title: String,
  conversations: List<ChatConversation>,
  row: @Composable (ChatConversation) -> Unit
) {
  if (conversations.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  items(conversations, key = { "$title-${it.id}" }) { conversation -> row(conversation) }
}

private fun LazyListScope.drawerFolderSection(
  title: String,
  groups: List<ChatConversationGroup>,
  collapsedFolders: Set<String>,
  onToggleFolder: (String) -> Unit,
  selectedConversationId: String?,
  forkSourceConversations: List<ChatConversation>,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onRenameGroup: (String, String) -> Unit,
  onClearGroup: (String) -> Unit
) {
  if (groups.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  groups.forEach { group ->
    val folderKey = "$title:${group.name}"
    item(key = "folder-${title}-${group.name}") {
      DrawerFolderHeader(
        title = group.name,
        count = group.conversations.size,
        collapsed = folderKey in collapsedFolders,
        onToggle = { onToggleFolder(folderKey) },
        onRename = { newName -> onRenameGroup(group.name, newName) },
        onDelete = { onClearGroup(group.name) }
      )
    }
    if (folderKey !in collapsedFolders) {
      items(group.conversations, key = { "$title-${group.name}-${it.id}" }) { conversation ->
        ConversationDrawerRow(conversation, selectedConversationId, conversationForkSourceLabel(conversation, forkSourceConversations), onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
      }
    }
  }
}

@Composable
private fun DrawerFolderHeader(
  title: String,
  count: Int,
  collapsed: Boolean,
  onToggle: () -> Unit,
  onRename: (String) -> Unit,
  onDelete: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var editing by remember(title) { mutableStateOf(false) }
  var deleteConfirmOpen by remember(title) { mutableStateOf(false) }
  var newName by remember(title) { mutableStateOf(title) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    TextButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
      Icon(
        if (collapsed) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
      Spacer(Modifier.width(6.dp))
      Text(
        "$title ($count)",
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Box {
      IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Outlined.MoreVert, contentDescription = "文件夹操作")
      }
      DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
          text = { Text("重命名文件夹") },
          leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
          onClick = {
            menuOpen = false
            editing = true
          }
        )
        DropdownMenuItem(
          text = { Text("删除文件夹") },
          leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
          onClick = {
            menuOpen = false
            deleteConfirmOpen = true
          }
        )
      }
    }
  }
  if (editing) {
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("重命名文件夹") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("文件夹名称") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(onClick = {
          onRename(newName.trim())
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
  if (deleteConfirmOpen) {
    AlertDialog(
      onDismissRequest = { deleteConfirmOpen = false },
      title = { Text("删除文件夹") },
      text = { Text("将 $count 个聊天移出「$title」文件夹，不会删除聊天内容。") },
      confirmButton = {
        Button(onClick = {
          onDelete()
          deleteConfirmOpen = false
        }) {
          Text("移出并删除文件夹")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteConfirmOpen = false }) {
          Text("取消")
        }
      }
    )
  }
}

private fun LazyListScope.drawerArchivedSection(
  title: String,
  conversations: List<ChatConversation>,
  forkSourceConversations: List<ChatConversation>,
  onRestore: (String) -> Unit,
  onDelete: (String) -> Unit
) {
  if (conversations.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  items(conversations, key = { "archived-${it.id}" }) { conversation ->
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(conversationGroupLabel(conversation.groupName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          conversationForkSourceLabel(conversation, forkSourceConversations)?.let { label ->
            ForkSourceLabel(label)
          }
        }
        IconButton(onClick = { onRestore(conversation.id) }) {
          Icon(Icons.Outlined.Unarchive, contentDescription = "恢复归档")
        }
        IconButton(onClick = { onDelete(conversation.id) }) {
          Icon(Icons.Outlined.Delete, contentDescription = "删除")
        }
      }
    }
  }
}

@Composable
private fun ConversationDrawerRow(
  conversation: ChatConversation,
  selectedConversationId: String?,
  forkSourceLabel: String?,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (conversation.id == selectedConversationId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = { onSelectConversation(conversation.id) }, modifier = Modifier.weight(1f)) {
        Column(horizontalAlignment = Alignment.Start) {
          val typePrefix = if (conversation.type == ConversationType.IMAGE) "生图 · " else ""
          Text(if (conversation.isPinned) "置顶 · $typePrefix${conversation.title}" else "$typePrefix${conversation.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(conversation.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "创建 ${formatConversationTime(conversation.createdAt)} · 最后 ${formatConversationTime(conversation.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          forkSourceLabel?.let { label ->
            ForkSourceLabel(label)
          }
        }
      }
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
            leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
            onClick = {
              menuOpen = false
              onTogglePin(conversation.id, conversation.isPinned)
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
    }
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

@Composable
private fun ForkSourceLabel(label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      Icons.AutoMirrored.Outlined.CallSplit,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(14.dp)
    )
    Spacer(Modifier.width(4.dp))
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
internal fun MoveConversationDialog(
  conversationTitle: String,
  initialGroupName: String,
  onDismiss: () -> Unit,
  onMove: (String) -> Unit
) {
  var targetGroup by remember(conversationTitle) { mutableStateOf(initialGroupName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("移动到文件夹") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(conversationTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        OutlinedTextField(
          value = targetGroup,
          onValueChange = { targetGroup = it },
          label = { Text("文件夹名称") },
          placeholder = { Text("留空则不放入文件夹") },
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          "文件夹用于收拢有共同归属的对话。加入文件夹后，该对话不再显示在普通列表里；留空可移出文件夹。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(onClick = { onMove(targetGroup.trim()) }) {
        Text("移动")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

internal fun conversationGroupLabel(groupName: String): String =
  groupName.ifBlank { "未分组" }

internal fun formatConversationTime(timestamp: Long): String {
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
  }.format(Date(timestamp))
}

private fun conversationDateBucket(timestamp: Long): String {
  val now = java.util.Calendar.getInstance()
  val target = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
  val todayStart = (now.clone() as java.util.Calendar).apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
  }
  val targetStart = (target.clone() as java.util.Calendar).apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
  }
  val days = ((todayStart.timeInMillis - targetStart.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()
  return when {
    days <= 0 -> "今天"
    days == 1 -> "昨天"
    days < 7 -> "最近 7 天"
    days < 30 -> "最近 30 天"
    else -> SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(timestamp))
  }
}
