package com.personal.aichat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ProviderType

@Composable
internal fun ProviderManagerDialog(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit,
  onEditProvider: (ChatProviderConfig?) -> Unit,
  onCloneProvider: (String) -> Unit,
  onDeleteProvider: (String) -> Unit,
  onCreateProvider: (ProviderType) -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 360.dp, max = 680.dp)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "API 配置",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "可保存多组 GPT、DeepSeek 或代理配置，并随时切换。被 AI 机器人使用的配置需先改绑或删除机器人后再删除。",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))
        ProviderCapabilityMatrix(providers = state.providers)
        Spacer(Modifier.height(12.dp))
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          state.providers.forEach { provider ->
            ProviderConfigRow(
              provider = provider,
              selected = provider.id == state.selectedProviderId,
              onSelect = { onSelectProvider(provider.id) },
              onEdit = { onEditProvider(provider) },
              onClone = { onCloneProvider(provider.id) },
              onDelete = { onDeleteProvider(provider.id) }
            )
          }
        }
        Spacer(Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TextButton(onClick = { onCreateProvider(ProviderType.OPENAI_RESPONSES) }) {
            Text("新增 GPT")
          }
          TextButton(onClick = { onCreateProvider(ProviderType.OPENAI_COMPATIBLE_CHAT) }) {
            Text("新增 DeepSeek")
          }
          TextButton(onClick = { onCreateProvider(ProviderType.TOKENHUB_PROXY) }) {
            Text("新增代理")
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("关闭")
          }
        }
      }
    }
  }
}

@Composable
private fun ProviderConfigRow(
  provider: ChatProviderConfig,
  selected: Boolean,
  onSelect: () -> Unit,
  onEdit: () -> Unit,
  onClone: () -> Unit,
  onDelete: () -> Unit
) {
  var confirmDelete by remember(provider.id) { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      RadioButton(selected = selected, onClick = onSelect)
      Column(modifier = Modifier.weight(1f)) {
        Text(provider.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${provider.type.label} / ${provider.defaultModel}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          provider.baseUrl,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        ProviderCapabilityBadges(provider = provider)
      }
      IconButton(onClick = onClone) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = "克隆配置")
      }
      IconButton(onClick = onEdit) {
        Icon(Icons.Outlined.Edit, contentDescription = "编辑配置")
      }
      IconButton(onClick = { confirmDelete = true }) {
        Icon(Icons.Outlined.Delete, contentDescription = "删除配置", tint = MaterialTheme.colorScheme.error)
      }
    }
  }
  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("删除 API 配置") },
      text = {
        Text("确定要删除「${provider.displayName}」吗？如果有 AI 机器人正在使用它，删除会被阻止，并提示你先删除机器人或把机器人切换到其他 API 配置。")
      },
      confirmButton = {
        Button(
          onClick = {
            confirmDelete = false
            onDelete()
          }
        ) {
          Text("删除")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) {
          Text("取消")
        }
      }
    )
  }
}

@Composable
internal fun ProviderRebindDeleteDialog(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onRebindAndDelete: (String) -> Unit
) {
  val sourceProvider = state.providers.firstOrNull { it.id == state.providerRebindDeleteSourceId }
  val bots = state.aiBots.filter { it.id in state.providerRebindDeleteBotIds }
  val targets = state.providers.filter { it.id != state.providerRebindDeleteSourceId }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("改绑机器人后删除") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("「${sourceProvider?.displayName ?: "此 API 配置"}」仍被以下机器人使用：")
        bots.forEach { bot ->
          Text("· ${bot.name} / ${bot.model}", style = MaterialTheme.typography.bodySmall)
        }
        Text(
          "选择一个现有 API 配置后，这些机器人会全部改绑到目标配置，并使用目标配置的默认模型，然后删除原 API 配置。",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall
        )
        if (targets.isEmpty()) {
          Text(
            "当前没有其他 API 配置可用于改绑。请先新增一个 API 配置，或删除这些机器人。",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        } else {
          targets.forEach { provider ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onRebindAndDelete(provider.id) }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(provider.displayName, fontWeight = FontWeight.SemiBold)
                Text(
                  "${provider.type.label} / ${provider.defaultModel}",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}
