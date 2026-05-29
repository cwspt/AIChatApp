package com.personal.aichat.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.FavoriteSnippetMessage
import com.personal.aichat.domain.GroupChatMember
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.GroupTurnTrigger
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ConversationType
import com.personal.aichat.domain.ImageGenerationApiMode
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort

private val mapperGson = Gson()
private val attachmentListType = object : TypeToken<List<ChatAttachment>>() {}.type
private val favoriteMessageListType = object : TypeToken<List<FavoriteSnippetMessage>>() {}.type
private val stringListType = object : TypeToken<List<String>>() {}.type

fun ProviderEntity.toDomain(): ChatProviderConfig = ChatProviderConfig(
  id = id,
  displayName = displayName,
  type = ProviderType.valueOf(type),
  baseUrl = baseUrl,
  defaultModel = defaultModel,
  contextWindowTokensOverride = contextWindowTokensOverride,
  enabled = enabled,
  supportsStreaming = supportsStreaming,
  supportsAttachments = supportsAttachments,
  supportsImageGeneration = supportsImageGeneration,
  imageGenerationApiMode = runCatching { ImageGenerationApiMode.valueOf(imageGenerationApiMode) }.getOrDefault(ImageGenerationApiMode.RESPONSES_TOOL),
  imageGenerationModel = imageGenerationModel,
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
  contextWindowTokensOverride = contextWindowTokensOverride,
  enabled = enabled,
  supportsStreaming = supportsStreaming,
  supportsAttachments = supportsAttachments,
  supportsImageGeneration = supportsImageGeneration,
  imageGenerationApiMode = imageGenerationApiMode.name,
  imageGenerationModel = imageGenerationModel,
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
  type = runCatching { ConversationType.valueOf(type) }.getOrDefault(ConversationType.CHAT),
  groupName = groupName,
  forkedFromConversationId = forkedFromConversationId,
  forkedFromMessageId = forkedFromMessageId,
  contextSummary = contextSummary,
  contextSummaryCutoffMessageId = contextSummaryCutoffMessageId,
  contextSummaryUpdatedAt = contextSummaryUpdatedAt,
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
  type = type.name,
  groupName = groupName,
  forkedFromConversationId = forkedFromConversationId,
  forkedFromMessageId = forkedFromMessageId,
  contextSummary = contextSummary,
  contextSummaryCutoffMessageId = contextSummaryCutoffMessageId,
  contextSummaryUpdatedAt = contextSummaryUpdatedAt,
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

fun FavoriteSnippetEntity.toDomain(): FavoriteSnippet = FavoriteSnippet(
  id = id,
  title = title,
  description = description,
  tags = parseTags(tagsJson),
  messages = parseFavoriteMessages(messagesJson),
  searchText = searchText,
  sourceConversationId = sourceConversationId,
  sourceConversationTitle = sourceConversationTitle,
  sourceProviderId = sourceProviderId,
  sourceProviderName = sourceProviderName,
  sourceModel = sourceModel,
  sourceGroupName = sourceGroupName,
  sourceFirstMessageId = sourceFirstMessageId,
  sourceLastMessageId = sourceLastMessageId,
  messageCount = messageCount,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun FavoriteSnippet.toEntity(): FavoriteSnippetEntity = FavoriteSnippetEntity(
  id = id,
  title = title,
  description = description,
  tagsJson = formatTags(tags),
  messagesJson = formatFavoriteMessages(messages),
  searchText = searchText,
  sourceConversationId = sourceConversationId,
  sourceConversationTitle = sourceConversationTitle,
  sourceProviderId = sourceProviderId,
  sourceProviderName = sourceProviderName,
  sourceModel = sourceModel,
  sourceGroupName = sourceGroupName,
  sourceFirstMessageId = sourceFirstMessageId,
  sourceLastMessageId = sourceLastMessageId,
  messageCount = messageCount,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun AiBotEntity.toDomain(): AiBot = AiBot(
  id = id,
  name = name,
  providerId = providerId,
  model = model,
  systemPrompt = systemPrompt,
  bubbleColorKey = bubbleColorKey,
  enabled = enabled,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun AiBot.toEntity(): AiBotEntity = AiBotEntity(
  id = id,
  name = name,
  providerId = providerId,
  model = model,
  systemPrompt = systemPrompt,
  bubbleColorKey = bubbleColorKey,
  enabled = enabled,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun GroupChatRoomEntity.toDomain(): GroupChatRoom = GroupChatRoom(
  id = id,
  title = title,
  topic = topic,
  summary = summary,
  contextSummary = contextSummary,
  contextSummaryCutoffMessageId = contextSummaryCutoffMessageId,
  contextSummaryUpdatedAt = contextSummaryUpdatedAt,
  createdAt = createdAt,
  updatedAt = updatedAt,
  isArchived = isArchived,
  isDeleted = isDeleted
)

fun GroupChatRoom.toEntity(): GroupChatRoomEntity = GroupChatRoomEntity(
  id = id,
  title = title,
  topic = topic,
  summary = summary,
  contextSummary = contextSummary,
  contextSummaryCutoffMessageId = contextSummaryCutoffMessageId,
  contextSummaryUpdatedAt = contextSummaryUpdatedAt,
  createdAt = createdAt,
  updatedAt = updatedAt,
  isArchived = isArchived,
  isDeleted = isDeleted
)

fun GroupChatMemberEntity.toDomain(): GroupChatMember = GroupChatMember(
  groupId = groupId,
  botId = botId,
  sortOrder = sortOrder,
  enabled = enabled,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun GroupMessageEntity.toDomain(): GroupChatMessage = GroupChatMessage(
  id = id,
  groupId = groupId,
  senderType = GroupMessageSenderType.valueOf(senderType),
  botId = botId,
  senderName = senderName,
  role = MessageRole.valueOf(role),
  content = content,
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
  attachments = parseAttachments(attachmentsJson),
  turnTrigger = runCatching { GroupTurnTrigger.valueOf(turnTrigger) }.getOrDefault(GroupTurnTrigger.UNKNOWN),
  turnRound = turnRound,
  turnIndex = turnIndex,
  turnMemberCount = turnMemberCount
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

fun normalizeTags(input: String): List<String> {
  return input
    .split(',', '，')
    .flatMap { item -> item.split(Regex("\\s+")) }
    .map { it.trim().trimStart('#') }
    .filter { it.isNotBlank() }
    .distinctBy { it.lowercase() }
}

fun formatTags(tags: List<String>): String {
  return if (tags.isEmpty()) "" else mapperGson.toJson(tags)
}

fun parseTags(json: String?): List<String> {
  if (json.isNullOrBlank()) return emptyList()
  return runCatching {
    mapperGson.fromJson<List<String>>(json, stringListType)
  }.getOrDefault(emptyList())
}

fun formatFavoriteMessages(messages: List<FavoriteSnippetMessage>): String {
  return if (messages.isEmpty()) "" else mapperGson.toJson(messages)
}

fun parseFavoriteMessages(json: String?): List<FavoriteSnippetMessage> {
  if (json.isNullOrBlank()) return emptyList()
  return runCatching {
    mapperGson.fromJson<List<FavoriteSnippetMessage>>(json, favoriteMessageListType)
  }.getOrDefault(emptyList())
}
