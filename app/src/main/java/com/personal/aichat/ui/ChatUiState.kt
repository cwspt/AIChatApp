package com.personal.aichat.ui

import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ContextCapacity
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.GroupChatMember
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.AppSettings
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.domain.MessageStatus
import androidx.compose.ui.text.input.TextFieldValue

data class IncomingShareDraft(
  val text: String = "",
  val attachments: List<ChatAttachment> = emptyList(),
  val failedCount: Int = 0,
  val open: Boolean = false
) {
  val hasContent: Boolean
    get() = text.isNotBlank() || attachments.isNotEmpty()

  val hasAttachments: Boolean
    get() = attachments.isNotEmpty()
}

data class ImageExportChoiceState(
  val pageCount: Int,
  val singleImageAllowed: Boolean
)

data class ChatUiState(
  val providers: List<ChatProviderConfig> = emptyList(),
  val conversations: List<ChatConversation> = emptyList(),
  val archivedConversations: List<ChatConversation> = emptyList(),
  val conversationGroups: List<ChatConversationGroup> = emptyList(),
  val favoriteSnippets: List<FavoriteSnippet> = emptyList(),
  val aiBots: List<AiBot> = emptyList(),
  val groupChats: List<GroupChatRoom> = emptyList(),
  val groupMembers: List<GroupChatMember> = emptyList(),
  val groupMessages: List<GroupChatMessage> = emptyList(),
  val messages: List<ChatMessage> = emptyList(),
  val selectedConversationId: String? = null,
  val selectedGroupChatId: String? = null,
  val selectedProviderId: String? = null,
  val input: TextFieldValue = TextFieldValue(""),
  val groupInput: TextFieldValue = TextFieldValue(""),
  val imageGenerationOptions: ImageGenerationOptions = ImageGenerationOptions(),
  val inlineImagesAllowedForNextSend: Boolean = false,
  val pendingAttachments: List<ChatAttachment> = emptyList(),
  val incomingShareDraft: IncomingShareDraft? = null,
  val appSettings: AppSettings = AppSettings(),
  val selectedMessageIds: Set<String> = emptySet(),
  val messageSelectionMode: Boolean = false,
  val settingsPageOpen: Boolean = false,
  val favoritePageOpen: Boolean = false,
  val groupChatPageOpen: Boolean = false,
  val botManagerOpen: Boolean = false,
  val newGroupChatDialogOpen: Boolean = false,
  val editingGroupChatId: String? = null,
  val providerManagerOpen: Boolean = false,
  val newConversationPickerOpen: Boolean = false,
  val settingsOpen: Boolean = false,
  val editingProvider: ChatProviderConfig? = null,
  val editingProviderHasApiKey: Boolean = false,
  val forkTargetMessageId: String? = null,
  val streamingConversationIds: Set<String> = emptySet(),
  val streamingGroupIds: Set<String> = emptySet(),
  val autoPlayingGroupIds: Set<String> = emptySet(),
  val compressingConversationIds: Set<String> = emptySet(),
  val compressingGroupIds: Set<String> = emptySet(),
  val selectedContextCapacity: ContextCapacity? = null,
  val selectedGroupContextCapacity: ContextCapacity? = null,
  val deleteConfirmOpen: Boolean = false,
  val deleteTargetConversationId: String? = null,
  val providerRebindDeleteSourceId: String? = null,
  val providerRebindDeleteBotIds: List<String> = emptyList(),
  val pendingImageExportChoice: ImageExportChoiceState? = null,
  val error: String? = null
) {
  val selectedConversation: ChatConversation?
    get() = conversations.firstOrNull { it.id == selectedConversationId }

  val selectedGroupChat: GroupChatRoom?
    get() = groupChats.firstOrNull { it.id == selectedGroupChatId }

  val selectedProvider: ChatProviderConfig?
    get() {
      val conversation = selectedConversation
      if (conversation != null) {
        return providers.firstOrNull { it.id == conversation.providerId }
          ?.copy(defaultModel = conversation.model)
      }
      return providers.firstOrNull { it.id == selectedProviderId }
        ?: providers.firstOrNull()
    }

  val isSelectedConversationStreaming: Boolean
    get() = selectedConversationId != null &&
      (selectedConversationId in streamingConversationIds || messages.any { it.status == MessageStatus.STREAMING })

  val isSelectedGroupStreaming: Boolean
    get() = selectedGroupChatId != null &&
      (selectedGroupChatId in streamingGroupIds || groupMessages.any { it.status == MessageStatus.STREAMING })

  val isSelectedGroupAutoPlaying: Boolean
    get() = selectedGroupChatId != null && selectedGroupChatId in autoPlayingGroupIds
}
