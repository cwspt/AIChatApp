package com.personal.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.personal.aichat.domain.ChatProviderConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderMenu(
  state: ChatUiState,
  onSelectProvider: (String) -> Unit,
  onOpenProviderManager: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
  ) {
    AssistChip(
      onClick = { expanded = true },
      label = {
        Text(
          text = state.selectedProvider?.displayName ?: "Provider",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      },
      modifier = Modifier.menuAnchor()
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      state.providers.forEach { provider ->
        DropdownMenuItem(
          text = {
            Column {
              Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                provider.defaultModel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          },
          onClick = {
            onSelectProvider(provider.id)
            expanded = false
          }
        )
      }
      DropdownMenuItem(
        text = { Text("管理 API 配置") },
        onClick = {
          expanded = false
          onOpenProviderManager()
        }
      )
    }
  }
}

@Composable
internal fun NewConversationProviderDialog(
  providers: List<ChatProviderConfig>,
  selectedProviderId: String?,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit,
  onSelectImageProvider: (String) -> Unit
) {
  val imageProviders = providers.filter { it.supportsImageGeneration }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("新对话模型") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("普通对话", fontWeight = FontWeight.SemiBold)
        ProviderChoiceList(
          providers = providers,
          selectedProviderId = selectedProviderId,
          onSelectProvider = onSelectProvider
        )
        Text("生图对话", fontWeight = FontWeight.SemiBold)
        if (imageProviders.isEmpty()) {
          Text(
            "当前没有开启生图能力的 GPT / OpenAI Responses 配置。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          ProviderChoiceList(
            providers = imageProviders,
            selectedProviderId = selectedProviderId,
            onSelectProvider = onSelectImageProvider
          )
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

@Composable
internal fun ForkProviderDialog(
  providers: List<ChatProviderConfig>,
  selectedProviderId: String?,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("用其他模型分叉") },
    text = {
      ProviderChoiceList(
        providers = providers,
        selectedProviderId = selectedProviderId,
        onSelectProvider = onSelectProvider
      )
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun ProviderChoiceList(
  providers: List<ChatProviderConfig>,
  selectedProviderId: String?,
  onSelectProvider: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    providers.forEach { provider ->
      TextButton(
        onClick = { onSelectProvider(provider.id) },
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          RadioButton(
            selected = provider.id == selectedProviderId,
            onClick = { onSelectProvider(provider.id) }
          )
          Spacer(Modifier.width(8.dp))
          Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
            Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              provider.defaultModel,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}
