package com.personal.aichat.data.local

import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort

fun ProviderEntity.toDomain(): ChatProviderConfig = ChatProviderConfig(
  id = id,
  displayName = displayName,
  type = ProviderType.valueOf(type),
  baseUrl = baseUrl,
  defaultModel = defaultModel,
  enabled = enabled,
  supportsStreaming = supportsStreaming,
  extraHeadersJson = extraHeadersJson,
  secretRef = secretRef,
  reasoningEffort = runCatching { ReasoningEffort.valueOf(reasoningEffort) }.getOrDefault(ReasoningEffort.AUTO)
)

fun ChatProviderConfig.toEntity(sortOrder: Int = 0): ProviderEntity = ProviderEntity(
  id = id,
  displayName = displayName,
  type = type.name,
  baseUrl = baseUrl,
  defaultModel = defaultModel,
  enabled = enabled,
  supportsStreaming = supportsStreaming,
  extraHeadersJson = extraHeadersJson,
  reasoningEffort = reasoningEffort.name,
  secretRef = secretRef,
  sortOrder = sortOrder
)

fun ConversationEntity.toDomain(): ChatConversation = ChatConversation(
  id = id,
  title = title,
  providerId = providerId,
  model = model,
  groupName = groupName,
  createdAt = createdAt,
  updatedAt = updatedAt,
  isArchived = isArchived,
  isDeleted = isDeleted,
  isPinned = isPinned
)

fun ChatConversation.toEntity(): ConversationEntity = ConversationEntity(
  id = id,
  title = title,
  providerId = providerId,
  model = model,
  groupName = groupName,
  createdAt = createdAt,
  updatedAt = updatedAt,
  isArchived = isArchived,
  isDeleted = isDeleted,
  isPinned = isPinned
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
  id = id,
  conversationId = conversationId,
  role = MessageRole.valueOf(role),
  content = content,
  status = MessageStatus.valueOf(status),
  providerId = providerId,
  model = model,
  createdAt = createdAt,
  updatedAt = updatedAt,
  errorMessage = errorMessage
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
  id = id,
  conversationId = conversationId,
  role = role.name,
  content = content,
  status = status.name,
  providerId = providerId,
  model = model,
  createdAt = createdAt,
  updatedAt = updatedAt,
  errorMessage = errorMessage
)
