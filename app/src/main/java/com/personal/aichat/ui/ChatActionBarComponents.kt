package com.personal.aichat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatActionBar(
  state: ChatUiState,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit
) {
  val selectedConversation = state.selectedConversation
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = selectedConversation?.title ?: "未选择对话",
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = selectedConversation?.let { "${conversationGroupLabel(it.groupName)} / ${it.model}" } ?: "从左上角聊天列表选择",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
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
      ConversationShareMenu(
        selectionMode = state.messageSelectionMode,
        selectedCount = state.selectedMessageIds.size,
        onToggleSelectionMode = onToggleSelectionMode,
        onShareText = onShareText,
        onShareSelected = onShareSelected,
        onShareImage = onShareImage,
        onShareSelectedImage = onShareSelectedImage,
        onShareMarkdown = onShareMarkdown
      )
    }
  }
}
