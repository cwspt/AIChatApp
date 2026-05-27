package com.personal.aichat.ui

import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig

data class ChatUiState(
  val providers: List<ChatProviderConfig> = emptyList(),
  val conversations: List<ChatConversation> = emptyList(),
  val archivedConversations: List<ChatConversation> = emptyList(),
  val conversationGroups: List<ChatConversationGroup> = emptyList(),
  val messages: List<ChatMessage> = emptyList(),
  val selectedConversationId: String? = null,
  val selectedProviderId: String? = null,
  val input: String = "",
  val selectedMessageIds: Set<String> = emptySet(),
  val messageSelectionMode: Boolean = false,
  val providerManagerOpen: Boolean = false,
  val settingsOpen: Boolean = false,
  val editingProvider: ChatProviderConfig? = null,
  val editingProviderHasApiKey: Boolean = false,
  val deleteConfirmOpen: Boolean = false,
  val deleteTargetConversationId: String? = null,
  val error: String? = null
) {
  val selectedConversation: ChatConversation?
    get() = conversations.firstOrNull { it.id == selectedConversationId }

  val selectedProvider: ChatProviderConfig?
    get() = providers.firstOrNull { it.id == selectedConversation?.providerId }
      ?: providers.firstOrNull { it.id == selectedProviderId }
      ?: providers.firstOrNull()
}
