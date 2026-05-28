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
  val enabled: Boolean,
  val supportsStreaming: Boolean,
  val supportsAttachments: Boolean = false,
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
  val groupName: String = "",
  val forkedFromConversationId: String? = null,
  val forkedFromMessageId: String? = null,
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
  val attachmentsJson: String = ""
)
