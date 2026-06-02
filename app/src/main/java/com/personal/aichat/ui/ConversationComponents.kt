package com.personal.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
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

@Composable
internal fun ConversationStrip(
  state: ChatUiState,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var listExpanded by remember { mutableStateOf(false) }
  val selectedConversation = state.selectedConversation
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Button(
        onClick = { listExpanded = !listExpanded },
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
          Text("对话列表", fontWeight = FontWeight.SemiBold)
          Text(
            text = selectedConversation?.let {
              "${conversationGroupLabel(it.groupName)} / ${it.title}"
            } ?: "点击切换历史对话",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          if (listExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
      }
      if (selectedConversation != null) {
        ConversationQuickActions(
          conversation = selectedConversation,
          onTogglePin = onTogglePin,
          onArchive = onArchive,
          onDelete = onDelete,
          onRename = onRename
        )
      }
    }

    if (listExpanded) {
      ConversationPicker(
        state = state,
        onSelectConversation = {
          onSelectConversation(it)
          listExpanded = false
        },
        onTogglePin = onTogglePin,
        onArchive = onArchive,
        onDelete = onDelete,
        onRename = onRename
      )
    }
  }
}

@Composable
internal fun ConversationQuickActions(
  conversation: ChatConversation,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var editing by remember(conversation.id) { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  var title by remember(conversation.id) { mutableStateOf(conversation.title) }
  var groupName by remember(conversation.id) { mutableStateOf(conversation.groupName) }

  Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(
      onClick = { onTogglePin(conversation.id, conversation.isPinned) },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.PushPin, contentDescription = "置顶")
    }
    IconButton(
      onClick = { editing = true },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Edit, contentDescription = "编辑标题和分组")
    }
    IconButton(
      onClick = { moving = true },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Folder, contentDescription = "移动到文件夹")
    }
    IconButton(
      onClick = { onArchive(conversation.id) },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Archive, contentDescription = "归档")
    }
    IconButton(
      onClick = { onDelete(conversation.id) },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Delete, contentDescription = "删除")
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

@Composable
private fun ConversationPicker(
  state: ChatUiState,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var expandedGroups by remember(state.conversationGroups) {
    mutableStateOf(state.conversationGroups.associate { it.name to true }.toMutableMap())
  }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = 260.dp)
      .padding(top = 6.dp)
  ) {
    LazyColumn(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      state.conversationGroups.forEach { group ->
        item(key = "group-${group.name}") {
          TextButton(onClick = {
            expandedGroups[group.name] = expandedGroups[group.name] == false
          }) {
            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "${if (expandedGroups[group.name] == false) "+" else "-"} ${group.name} (${group.conversations.size})",
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        if (expandedGroups[group.name] != false) {
          item(key = "row-${group.name}") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              itemsIndexed(group.conversations, key = { _, conversation -> conversation.id }) { _, conversation ->
                ConversationChipRow(
                  conversation = conversation,
                  selected = conversation.id == state.selectedConversationId,
                  onSelectConversation = onSelectConversation,
                  onTogglePin = onTogglePin,
                  onArchive = onArchive,
                  onDelete = onDelete,
                  onRename = onRename
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConversationChipRow(
  conversation: ChatConversation,
  selected: Boolean,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.width(220.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        color = Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f)
      ) {
        TextButton(onClick = { onSelectConversation(conversation.id) }) {
          Text(
            text = if (conversation.isPinned) "置顶 · ${conversation.title}" else conversation.title,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Box {
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
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
