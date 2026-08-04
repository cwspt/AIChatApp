package com.personal.aichat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
  @PrimaryKey val id: String,
  val displayName: String,
  val type: String,
  val baseUrl: String,
  val defaultModel: String,
  val contextWindowTokensOverride: Int? = null,
  val enabled: Boolean,
  val supportsStreaming: Boolean,
  val supportsAttachments: Boolean = false,
  val supportsImageGeneration: Boolean = false,
  val imageGenerationApiMode: String = "RESPONSES_TOOL",
  val imageGenerationModel: String = "",
  val extraHeadersJson: String,
  val reasoningEffort: String = "AUTO",
  val secretRef: String?,
  val sortOrder: Int
)

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey val id: String,
  val title: String,
  val providerId: String,
  val model: String,
  val type: String = "CHAT",
  val groupName: String = "",
  val forkedFromConversationId: String? = null,
  val forkedFromMessageId: String? = null,
  val contextSummary: String = "",
  val contextSummaryCutoffMessageId: String? = null,
  val contextSummaryUpdatedAt: Long? = null,
  val createdAt: Long,
  val updatedAt: Long,
  val isArchived: Boolean = false,
  val isDeleted: Boolean = false,
  val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
  @PrimaryKey val id: String,
  val conversationId: String,
  val role: String,
  val content: String,
  val status: String,
  val providerId: String?,
  val model: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val errorMessage: String?,
  val totalDurationMs: Long? = null,
  val firstTokenDurationMs: Long? = null,
  val promptTokens: Int? = null,
  val completionTokens: Int? = null,
  val totalTokens: Int? = null,
  val rawResponseLog: String? = null,
  val attachmentsJson: String = "",
  val contentPartsJson: String = "",
  val reasoningContent: String = ""
)

@Entity(tableName = "favorite_snippets")
data class FavoriteSnippetEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val tagsJson: String,
  val messagesJson: String,
  val searchText: String,
  val sourceConversationId: String,
  val sourceConversationTitle: String,
  val sourceProviderId: String?,
  val sourceProviderName: String?,
  val sourceModel: String?,
  val sourceGroupName: String?,
  val sourceFirstMessageId: String?,
  val sourceLastMessageId: String?,
  val messageCount: Int,
  val createdAt: Long,
  val updatedAt: Long
)

@Entity(tableName = "ai_bots")
data class AiBotEntity(
  @PrimaryKey val id: String,
  val name: String,
  val providerId: String,
  val model: String,
  val systemPrompt: String,
  val bubbleColorKey: String = "AUTO",
  val enabled: Boolean = true,
  val createdAt: Long,
  val updatedAt: Long
)

@Entity(tableName = "group_chat_rooms")
data class GroupChatRoomEntity(
  @PrimaryKey val id: String,
  val title: String,
  val topic: String,
  val summary: String = "",
  val contextSummary: String = "",
  val contextSummaryCutoffMessageId: String? = null,
  val contextSummaryUpdatedAt: Long? = null,
  val createdAt: Long,
  val updatedAt: Long,
  val isArchived: Boolean = false,
  val isDeleted: Boolean = false
)

@Entity(tableName = "group_chat_members", primaryKeys = ["groupId", "botId"])
data class GroupChatMemberEntity(
  val groupId: String,
  val botId: String,
  val sortOrder: Int,
  val enabled: Boolean = true,
  val createdAt: Long,
  val updatedAt: Long
)

@Entity(tableName = "group_messages")
data class GroupMessageEntity(
  @PrimaryKey val id: String,
  val groupId: String,
  val senderType: String,
  val botId: String?,
  val senderName: String,
  val role: String,
  val content: String,
  val status: String,
  val providerId: String?,
  val model: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val errorMessage: String?,
  val totalDurationMs: Long? = null,
  val firstTokenDurationMs: Long? = null,
  val promptTokens: Int? = null,
  val completionTokens: Int? = null,
  val totalTokens: Int? = null,
  val attachmentsJson: String = "",
  val contentPartsJson: String = "",
  val turnTrigger: String = "UNKNOWN",
  val turnRound: Int? = null,
  val turnIndex: Int? = null,
  val turnMemberCount: Int? = null,
  val reasoningContent: String = ""
)
