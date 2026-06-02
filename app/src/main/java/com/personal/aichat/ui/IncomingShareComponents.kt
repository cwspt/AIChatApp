package com.personal.aichat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.GroupChatRoom

@Composable
internal fun IncomingShareTargetDialog(
  state: ChatUiState,
  draft: IncomingShareDraft,
  onDismiss: () -> Unit,
  onSelectConversation: (String) -> Unit,
  onSelectGroup: (String) -> Unit,
  onCreateConversation: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit
) {
  var targetQuery by remember(draft.text, draft.attachments) { mutableStateOf("") }
  val normalizedTargetQuery = targetQuery.trim()
  val providerById = remember(state.providers) { state.providers.associateBy { it.id } }
  val botById = remember(state.aiBots) { state.aiBots.associateBy { it.id } }
  val groupBotsByGroupId = remember(state.groupMembers, state.aiBots) {
    state.groupMembers
      .groupBy { it.groupId }
      .mapValues { (_, members) -> members.mapNotNull { botById[it.botId] } }
  }
  val creatableProviders = state.providers
    .filter { !draft.hasAttachments || it.supportsAttachments }
    .filter { it.matchesIncomingShareTargetQuery(normalizedTargetQuery) }
  val filteredConversations = state.conversations.filter { conversation ->
    conversation.matchesIncomingShareTargetQuery(providerById[conversation.providerId], normalizedTargetQuery)
  }
  val filteredGroups = state.groupChats.filter { group ->
    group.matchesIncomingShareTargetQuery(
      bots = groupBotsByGroupId[group.id].orEmpty(),
      providerById = providerById,
      query = normalizedTargetQuery
    )
  }
  val hasVisibleTarget = creatableProviders.isNotEmpty() || filteredConversations.isNotEmpty() || filteredGroups.isNotEmpty()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text("选择发送到", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              incomingShareSummary(draft),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          TextButton(onClick = onDismiss) {
            Text("取消")
          }
        }
        if (draft.failedCount > 0) {
          Text(
            "${draft.failedCount} 个文件导入失败，其余内容仍可发送",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        if (draft.text.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              draft.text,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
        if (draft.attachments.isNotEmpty()) {
          AttachmentStrip(
            attachments = draft.attachments,
            onOpenAttachment = onOpenAttachment,
            onRemoveAttachment = null,
            compact = true
          )
        }
        OutlinedTextField(
          value = targetQuery,
          onValueChange = { targetQuery = it },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          label = { Text("搜索单聊、群聊或 Provider") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            if (targetQuery.isNotBlank()) {
              IconButton(onClick = { targetQuery = "" }) {
                Icon(Icons.Outlined.Close, contentDescription = "清空搜索")
              }
            }
          }
        )
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item(key = "new-conversation-section") {
            Text("新建单聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
          if (creatableProviders.isEmpty()) {
            item(key = "new-conversation-empty") {
              Text(
                if (normalizedTargetQuery.isBlank()) "没有支持这些附件的单聊模型配置" else "没有匹配的单聊模型配置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            items(creatableProviders, key = { "new-provider-${it.id}" }) { provider ->
              IncomingShareProviderRow(
                provider = provider,
                onClick = { onCreateConversation(provider.id) }
              )
            }
          }
          if (filteredConversations.isNotEmpty()) {
            item(key = "conversation-section") {
              Text("已有单聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            }
            items(filteredConversations, key = { "conversation-${it.id}" }) { conversation ->
              val provider = providerById[conversation.providerId]
              val enabled = !draft.hasAttachments || provider?.supportsAttachments == true
              IncomingShareConversationRow(
                conversation = conversation,
                provider = provider,
                enabled = enabled,
                disabledReason = if (!enabled) "该模型不支持附件" else null,
                onClick = { onSelectConversation(conversation.id) }
              )
            }
          }
          if (filteredGroups.isNotEmpty()) {
            item(key = "group-section") {
              Text("已有群聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            }
            items(filteredGroups, key = { "group-${it.id}" }) { group ->
              IncomingShareGroupRow(
                group = group,
                memberCount = state.groupMembers.count { it.groupId == group.id },
                onClick = { onSelectGroup(group.id) }
              )
            }
          }
          if (normalizedTargetQuery.isNotBlank() && !hasVisibleTarget) {
            item(key = "target-search-empty") {
              Text(
                "没有匹配的发送目标",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
              )
            }
          }
        }
      }
    }
  }
}

private fun ChatProviderConfig.matchesIncomingShareTargetQuery(query: String): Boolean {
  if (query.isBlank()) return true
  return listOf(displayName, id, defaultModel, type.label).any { it.contains(query, ignoreCase = true) }
}

private fun ChatConversation.matchesIncomingShareTargetQuery(
  provider: ChatProviderConfig?,
  query: String
): Boolean {
  if (query.isBlank()) return true
  return listOf(
    title,
    model,
    groupName,
    providerId,
    provider?.displayName.orEmpty(),
    provider?.defaultModel.orEmpty(),
    provider?.type?.label.orEmpty()
  ).any { it.contains(query, ignoreCase = true) }
}

private fun GroupChatRoom.matchesIncomingShareTargetQuery(
  bots: List<AiBot>,
  providerById: Map<String, ChatProviderConfig>,
  query: String
): Boolean {
  if (query.isBlank()) return true
  val groupFields = listOf(title, topic, summary)
  val botFields = bots.flatMap { bot ->
    val provider = providerById[bot.providerId]
    listOf(
      bot.name,
      bot.model,
      bot.providerId,
      provider?.displayName.orEmpty(),
      provider?.defaultModel.orEmpty(),
      provider?.type?.label.orEmpty()
    )
  }
  return (groupFields + botFields).any { it.contains(query, ignoreCase = true) }
}

@Composable
private fun IncomingShareProviderRow(
  provider: ChatProviderConfig,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Outlined.Add, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
          provider.defaultModel,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun IncomingShareConversationRow(
  conversation: ChatConversation,
  provider: ChatProviderConfig?,
  enabled: Boolean,
  disabledReason: String?,
  onClick: () -> Unit
) {
  Surface(
    color = if (enabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
      Text(
        "${provider?.displayName ?: conversation.providerId} / ${conversation.model}",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        disabledReason ?: "最后 ${formatConversationTime(conversation.updatedAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
      )
    }
  }
}

@Composable
private fun IncomingShareGroupRow(
  group: GroupChatRoom,
  memberCount: Int,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Groups, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(group.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
          group.topic.ifBlank { "未填写主题" },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          "$memberCount 个成员 · 最后 ${formatConversationTime(group.updatedAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private fun incomingShareSummary(draft: IncomingShareDraft): String {
  val parts = mutableListOf<String>()
  if (draft.attachments.isNotEmpty()) parts += "${draft.attachments.size} 个附件"
  if (draft.text.isNotBlank()) parts += "包含文本"
  return parts.ifEmpty { listOf("分享内容") }.joinToString(" · ")
}
