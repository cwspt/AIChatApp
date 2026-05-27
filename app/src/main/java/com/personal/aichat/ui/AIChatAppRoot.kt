package com.personal.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus

@Composable
fun AIChatAppRoot(viewModel: ChatViewModel) {
  val state by viewModel.uiState.collectAsState()
  val editingProvider = state.editingProvider

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      TopBar(
        state = state,
        onNewChat = viewModel::newConversation,
        onOpenSettings = { viewModel.openSettings() }
      )
      ConversationStrip(
        state = state,
        onSelectConversation = viewModel::selectConversation
      )
      MessageList(
        messages = state.messages,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
      Composer(
        input = state.input,
        onInput = viewModel::updateInput,
        onSend = viewModel::send,
        onRetry = viewModel::retryLast
      )
    }

    if (state.settingsOpen && editingProvider != null) {
      ProviderSettingsDialog(
        provider = editingProvider,
        onDismiss = viewModel::closeSettings,
        onSave = viewModel::saveProvider
      )
    }
  }
}

@Composable
private fun TopBar(
  state: ChatUiState,
  onNewChat: () -> Unit,
  onOpenSettings: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "AI Chat",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = state.selectedProvider?.displayName ?: "No provider",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    IconButton(onClick = onNewChat) {
      Icon(Icons.Outlined.Add, contentDescription = "New chat")
    }
    IconButton(onClick = onOpenSettings) {
      Icon(Icons.Outlined.Settings, contentDescription = "Provider settings")
    }
  }
}

@Composable
private fun ConversationStrip(
  state: ChatUiState,
  onSelectConversation: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .height(74.dp),
    horizontalAlignment = Alignment.Start
  ) {
    item {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        state.conversations.take(6).forEach { conversation ->
          AssistChip(
            onClick = { onSelectConversation(conversation.id) },
            label = {
              Text(
                text = conversation.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          )
        }
      }
    }
  }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier.padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    if (messages.isEmpty()) {
      item {
        EmptyState()
      }
    }
    items(messages, key = { it.id }) { message ->
      MessageBubble(message)
    }
  }
}

@Composable
private fun EmptyState() {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 42.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text("Start with a provider, a model, and a plain question.", fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(6.dp))
      Text(
        "TokenHub Proxy is preconfigured for local testing. Add the proxy key in settings before sending.",
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
  val isUser = message.role == MessageRole.USER
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
      contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.fillMaxWidth(if (isUser) 0.84f else 0.92f)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(message.content.ifBlank { if (message.status == MessageStatus.STREAMING) "..." else "" })
        if (message.status == MessageStatus.FAILED) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = message.errorMessage ?: "Request failed",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

@Composable
private fun Composer(
  input: String,
  onInput: (String) -> Unit,
  onSend: () -> Unit,
  onRetry: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .imePadding()
      .navigationBarsPadding()
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedTextField(
      value = input,
      onValueChange = onInput,
      modifier = Modifier.weight(1f),
      minLines = 1,
      maxLines = 5,
      placeholder = { Text("Message any configured model") }
    )
    Spacer(Modifier.width(8.dp))
    IconButton(onClick = onRetry) {
      Icon(Icons.Outlined.Refresh, contentDescription = "Retry last")
    }
    IconButton(onClick = onSend, enabled = input.isNotBlank()) {
      Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSettingsDialog(
  provider: ChatProviderConfig,
  onDismiss: () -> Unit,
  onSave: (ChatProviderConfig, String?) -> Unit
) {
  var displayName by remember(provider.id) { mutableStateOf(provider.displayName) }
  var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
  var model by remember(provider.id) { mutableStateOf(provider.defaultModel) }
  var apiKey by remember(provider.id) { mutableStateOf("") }
  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = {
          onSave(
            provider.copy(
              displayName = displayName.trim(),
              baseUrl = baseUrl.trim().trimEnd('/'),
              defaultModel = model.trim(),
              enabled = true
            ),
            apiKey.takeIf { it.isNotBlank() }
          )
        }
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    title = { Text("Provider settings") },
    text = {
      Column(
        modifier = Modifier.fillMaxHeight(0.72f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = !expanded }
        ) {
          OutlinedTextField(
            value = provider.type.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
          ) {
            DropdownMenuItem(
              text = { Text(provider.type.name) },
              onClick = { expanded = false }
            )
          }
        }
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("Display name") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = baseUrl,
          onValueChange = { baseUrl = it },
          label = { Text("Base URL") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = model,
          onValueChange = { model = it },
          label = { Text("Default model") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text("API key or proxy key") },
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          text = "Keys are stored through Android Keystore backed encrypted preferences.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(18.dp)
  )
}
