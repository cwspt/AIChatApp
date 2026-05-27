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
  val errorMessage: String?
)
