package com.personal.aichat.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort

private val mapperGson = Gson()
private val attachmentListType = object : TypeToken<List<ChatAttachment>>() {}.type

fun ProviderEntity.toDomain(): ChatProviderConfig = ChatProviderConfig(
  id = id,
  displayName = displayName,
  type = ProviderType.valueOf(type),
  baseUrl = baseUrl,
  defaultModel = defaultModel,
  enabled = enabled,
  supportsStreaming = supportsStreaming,
  supportsAttachments = supportsAttachments,
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
  supportsAttachments = supportsAttachments,
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
  forkedFromConversationId = forkedFromConversationId,
  forkedFromMessageId = forkedFromMessageId,
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
  forkedFromConversationId = forkedFromConversationId,
  forkedFromMessageId = forkedFromMessageId,
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
  attachments = parseAttachments(attachmentsJson),
  status = MessageStatus.valueOf(status),
  providerId = providerId,
  model = model,
  createdAt = createdAt,
  updatedAt = updatedAt,
  errorMessage = errorMessage,
  totalDurationMs = totalDurationMs,
  firstTokenDurationMs = firstTokenDurationMs,
  promptTokens = promptTokens,
  completionTokens = completionTokens,
  totalTokens = totalTokens,
  rawResponseLog = rawResponseLog
)

fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
  id = id,
  conversationId = conversationId,
  role = role.name,
  content = content,
  attachmentsJson = formatAttachments(attachments),
  status = status.name,
  providerId = providerId,
  model = model,
  createdAt = createdAt,
  updatedAt = updatedAt,
  errorMessage = errorMessage,
  totalDurationMs = totalDurationMs,
  firstTokenDurationMs = firstTokenDurationMs,
  promptTokens = promptTokens,
  completionTokens = completionTokens,
  totalTokens = totalTokens,
  rawResponseLog = rawResponseLog
)

fun formatAttachments(attachments: List<ChatAttachment>): String {
  return if (attachments.isEmpty()) "" else mapperGson.toJson(attachments)
}

fun parseAttachments(json: String?): List<ChatAttachment> {
  if (json.isNullOrBlank()) return emptyList()
  return runCatching {
    mapperGson.fromJson<List<ChatAttachment>>(json, attachmentListType)
  }.getOrDefault(emptyList())
}
