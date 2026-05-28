package com.personal.aichat.data

import com.google.gson.Gson
import com.personal.aichat.data.local.AiBotEntity
import com.personal.aichat.data.local.ChatDao
import com.personal.aichat.data.local.ConversationEntity
import com.personal.aichat.data.local.GroupChatMemberEntity
import com.personal.aichat.data.local.GroupChatRoomEntity
import com.personal.aichat.data.local.GroupMessageEntity
import com.personal.aichat.data.local.MessageEntity
import com.personal.aichat.data.local.ProviderEntity
import com.personal.aichat.data.local.formatAttachments
import com.personal.aichat.data.local.normalizeTags
import com.personal.aichat.data.local.toEntity
import com.personal.aichat.data.local.toDomain
import com.personal.aichat.data.remote.OpenAiCompatibleChatAdapter
import com.personal.aichat.data.remote.OpenAiResponsesAdapter
import com.personal.aichat.data.remote.TokenHubProxyAdapter
import com.personal.aichat.data.security.ApiKeyStore
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.FavoriteSnippetMessage
import com.personal.aichat.domain.GroupChatMember
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderAdapter
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import javax.net.ssl.SSLHandshakeException
import java.util.UUID

private const val MaxRawResponseLogChars = 64_000
private const val GroupRecentMessageLimit = 20

private data class ProviderConfigExport(
  val version: Int = 1,
  val exportedAt: Long = 0,
  val providers: List<ProviderConfigExportItem> = emptyList()
)

private data class ProviderConfigExportItem(
  val id: String = "",
  val displayName: String = "",
  val type: String = "",
  val baseUrl: String = "",
  val defaultModel: String = "",
  val enabled: Boolean = true,
  val supportsStreaming: Boolean = true,
  val supportsAttachments: Boolean = false,
  val extraHeadersJson: String = "",
  val reasoningEffort: String = ReasoningEffort.AUTO.name,
  val apiKey: String? = null
)

class ChatRepository(
  private val dao: ChatDao,
  private val preferencesRepository: ChatSelectionStore,
  private val apiKeyStore: ApiKeyStore,
  private val adapters: Map<ProviderType, ProviderAdapter> = defaultAdapters()
) {
  private val gson = Gson()

  val providers: Flow<List<ChatProviderConfig>> = dao.observeProviders().map { items ->
    items.map { it.toDomain() }
  }

  val conversations: Flow<List<ChatConversation>> = dao.observeConversations().map { items ->
    items.map { it.toDomain() }
  }

  val archivedConversations: Flow<List<ChatConversation>> = dao.observeArchivedConversations().map { items ->
    items.map { it.toDomain() }
  }

  val favoriteSnippets: Flow<List<FavoriteSnippet>> = dao.observeFavoriteSnippets().map { items ->
    items.map { it.toDomain() }
  }

  val aiBots: Flow<List<AiBot>> = dao.observeAiBots().map { items ->
    items.map { it.toDomain() }
  }

  val groupChatRooms: Flow<List<GroupChatRoom>> = dao.observeGroupChatRooms().map { items ->
    items.map { it.toDomain() }
  }

  val conversationGroups: Flow<List<ChatConversationGroup>> = conversations.map { list ->
    list.groupBy { it.groupName.ifBlank { "默认" } }
      .toSortedMap()
      .map { (name, items) ->
        ChatConversationGroup(
          name = name,
          conversations = items.sortedWith(
            compareByDescending<ChatConversation> { it.isPinned }.thenByDescending { it.updatedAt }
          )
        )
      }
  }

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
    return dao.observeMessages(conversationId).map { items -> items.map { it.toDomain() } }
  }

  fun observeGroupMessages(groupId: String): Flow<List<GroupChatMessage>> {
    return dao.observeGroupMessages(groupId).map { items -> items.map { it.toDomain() } }
  }

  fun observeGroupMembers(groupId: String): Flow<List<GroupChatMember>> {
    return dao.observeGroupChatMembers(groupId).map { items -> items.map { it.toDomain() } }
  }

  suspend fun favoriteSnippetById(id: String): FavoriteSnippet? {
    return dao.favoriteSnippetById(id)?.toDomain()
  }

  suspend fun createFavoriteSnippet(
    conversationId: String,
    messageIds: Set<String>,
    title: String,
    description: String,
    tagsInput: String
  ): FavoriteSnippet {
    require(messageIds.isNotEmpty()) { "请先选择要收藏的消息" }
    val conversation = dao.conversationById(conversationId) ?: error("来源对话不可用")
    val provider = dao.providerById(conversation.providerId)?.toDomain()
    val sourceMessages = dao.messagesForConversation(conversationId)
      .filter { it.id in messageIds }
      .sortedBy { it.createdAt }
    require(sourceMessages.isNotEmpty()) { "请先选择要收藏的消息" }
    check(sourceMessages.none { it.status == MessageStatus.STREAMING.name }) { "输出完成后再收藏" }
    val snapshots = sourceMessages.map { it.toDomain().toFavoriteMessage() }
    val tags = normalizeTags(tagsInput)
    val finalTitle = title.trim().ifBlank { defaultFavoriteTitle(conversation.title, snapshots) }
    val finalDescription = description.trim()
    val now = System.currentTimeMillis()
    val favorite = FavoriteSnippet(
      id = newId("fav"),
      title = finalTitle,
      description = finalDescription,
      tags = tags,
      messages = snapshots,
      searchText = buildFavoriteSearchText(
        title = finalTitle,
        description = finalDescription,
        tags = tags,
        sourceConversationTitle = conversation.title,
        sourceProviderName = provider?.displayName,
        sourceModel = conversation.model,
        messages = snapshots
      ),
      sourceConversationId = conversation.id,
      sourceConversationTitle = conversation.title,
      sourceProviderId = conversation.providerId,
      sourceProviderName = provider?.displayName,
      sourceModel = conversation.model,
      sourceGroupName = conversation.groupName.takeIf { it.isNotBlank() },
      sourceFirstMessageId = sourceMessages.firstOrNull()?.id,
      sourceLastMessageId = sourceMessages.lastOrNull()?.id,
      messageCount = snapshots.size,
      createdAt = now,
      updatedAt = now
    )
    dao.upsertFavoriteSnippet(favorite.toEntity())
    return favorite
  }

  suspend fun createFavoriteSnippetFromGroupMessages(
    groupId: String,
    messageIds: Set<String>,
    title: String,
    description: String,
    tagsInput: String
  ): FavoriteSnippet {
    require(messageIds.isNotEmpty()) { "请先选择要收藏的群消息" }
    val room = dao.groupChatRoomById(groupId) ?: error("来源群聊不可用")
    val sourceMessages = dao.groupMessages(groupId)
      .filter { it.id in messageIds }
      .sortedBy { it.createdAt }
    require(sourceMessages.isNotEmpty()) { "请先选择要收藏的群消息" }
    check(sourceMessages.none { it.status == MessageStatus.STREAMING.name }) { "输出完成后再收藏" }
    val snapshots = sourceMessages.map { it.toDomain().toFavoriteMessage() }
    val firstProviderId = sourceMessages.firstNotNullOfOrNull { it.providerId }
    val firstProvider = firstProviderId?.let { dao.providerById(it)?.toDomain() }
    val firstModel = sourceMessages.firstNotNullOfOrNull { it.model }
    val tags = normalizeTags(tagsInput)
    val finalTitle = title.trim().ifBlank { defaultFavoriteTitle(room.title, snapshots) }
    val finalDescription = description.trim()
    val now = System.currentTimeMillis()
    val favorite = FavoriteSnippet(
      id = newId("fav"),
      title = finalTitle,
      description = finalDescription,
      tags = tags,
      messages = snapshots,
      searchText = buildFavoriteSearchText(
        title = finalTitle,
        description = finalDescription,
        tags = tags,
        sourceConversationTitle = room.title,
        sourceProviderName = firstProvider?.displayName,
        sourceModel = firstModel,
        messages = snapshots
      ),
      sourceConversationId = room.id,
      sourceConversationTitle = room.title,
      sourceProviderId = firstProviderId,
      sourceProviderName = firstProvider?.displayName,
      sourceModel = firstModel,
      sourceGroupName = "AI 群聊",
      sourceFirstMessageId = sourceMessages.firstOrNull()?.id,
      sourceLastMessageId = sourceMessages.lastOrNull()?.id,
      messageCount = snapshots.size,
      createdAt = now,
      updatedAt = now
    )
    dao.upsertFavoriteSnippet(favorite.toEntity())
    return favorite
  }

  suspend fun updateFavoriteSnippetMetadata(
    favoriteId: String,
    title: String,
    description: String,
    tagsInput: String
  ): FavoriteSnippet? {
    val existing = dao.favoriteSnippetById(favoriteId)?.toDomain() ?: return null
    val tags = normalizeTags(tagsInput)
    val finalTitle = title.trim().ifBlank { existing.title }
    val finalDescription = description.trim()
    val updated = existing.copy(
      title = finalTitle,
      description = finalDescription,
      tags = tags,
      searchText = buildFavoriteSearchText(
        title = finalTitle,
        description = finalDescription,
        tags = tags,
        sourceConversationTitle = existing.sourceConversationTitle,
        sourceProviderName = existing.sourceProviderName,
        sourceModel = existing.sourceModel,
        messages = existing.messages
      ),
      updatedAt = System.currentTimeMillis()
    )
    dao.upsertFavoriteSnippet(updated.toEntity())
    return updated
  }

  suspend fun appendMessagesToFavoriteSnippet(
    favoriteId: String,
    conversationId: String,
    messageIds: Set<String>
  ): FavoriteSnippet? {
    require(messageIds.isNotEmpty()) { "请先选择要追加的消息" }
    val existing = dao.favoriteSnippetById(favoriteId)?.toDomain() ?: return null
    check(existing.sourceConversationId == conversationId) { "只能追加同一来源对话里的消息" }
    val sourceMessages = dao.messagesForConversation(conversationId)
      .filter { it.id in messageIds }
      .sortedBy { it.createdAt }
    require(sourceMessages.isNotEmpty()) { "请先选择要追加的消息" }
    check(sourceMessages.none { it.status == MessageStatus.STREAMING.name }) { "输出完成后再追加" }

    val mergedMessages = (existing.messages + sourceMessages.map { it.toDomain().toFavoriteMessage() })
      .distinctBy { it.id }
      .sortedBy { it.createdAt }
    val updated = existing.copy(
      messages = mergedMessages,
      searchText = buildFavoriteSearchText(
        title = existing.title,
        description = existing.description,
        tags = existing.tags,
        sourceConversationTitle = existing.sourceConversationTitle,
        sourceProviderName = existing.sourceProviderName,
        sourceModel = existing.sourceModel,
        messages = mergedMessages
      ),
      sourceFirstMessageId = mergedMessages.firstOrNull()?.id,
      sourceLastMessageId = mergedMessages.lastOrNull()?.id,
      messageCount = mergedMessages.size,
      updatedAt = System.currentTimeMillis()
    )
    dao.upsertFavoriteSnippet(updated.toEntity())
    return updated
  }

  suspend fun removeMessagesFromFavoriteSnippet(
    favoriteId: String,
    messageIds: Set<String>
  ): FavoriteSnippet? {
    require(messageIds.isNotEmpty()) { "请选择要移除的消息" }
    val existing = dao.favoriteSnippetById(favoriteId)?.toDomain() ?: return null
    val remaining = existing.messages.filterNot { it.id in messageIds }
    check(remaining.isNotEmpty()) { "收藏至少需要保留一条消息" }
    val updated = existing.copy(
      messages = remaining,
      searchText = buildFavoriteSearchText(
        title = existing.title,
        description = existing.description,
        tags = existing.tags,
        sourceConversationTitle = existing.sourceConversationTitle,
        sourceProviderName = existing.sourceProviderName,
        sourceModel = existing.sourceModel,
        messages = remaining
      ),
      sourceFirstMessageId = remaining.firstOrNull()?.id,
      sourceLastMessageId = remaining.lastOrNull()?.id,
      messageCount = remaining.size,
      updatedAt = System.currentTimeMillis()
    )
    dao.upsertFavoriteSnippet(updated.toEntity())
    return updated
  }

  suspend fun deleteFavoriteSnippet(favoriteId: String) {
    dao.deleteFavoriteSnippet(favoriteId)
  }

  suspend fun conversationById(conversationId: String): ChatConversation? {
    return dao.conversationById(conversationId)?.toDomain()
  }

  suspend fun createAiBot(name: String, providerId: String, model: String, systemPrompt: String): AiBot {
    val provider = dao.providerById(providerId) ?: error("请选择有效的 API 配置")
    val now = System.currentTimeMillis()
    val bot = AiBotEntity(
      id = newId("bot"),
      name = name.trim().ifBlank { provider.displayName },
      providerId = provider.id,
      model = model.trim().ifBlank { provider.defaultModel },
      systemPrompt = systemPrompt.trim(),
      enabled = true,
      createdAt = now,
      updatedAt = now
    )
    dao.upsertAiBot(bot)
    return bot.toDomain()
  }

  suspend fun updateAiBot(botId: String, name: String, providerId: String, model: String, systemPrompt: String): AiBot? {
    val existing = dao.aiBotById(botId) ?: return null
    val provider = dao.providerById(providerId) ?: error("请选择有效的 API 配置")
    val updated = existing.copy(
      name = name.trim().ifBlank { existing.name },
      providerId = provider.id,
      model = model.trim().ifBlank { provider.defaultModel },
      systemPrompt = systemPrompt.trim(),
      updatedAt = System.currentTimeMillis()
    )
    dao.upsertAiBot(updated)
    return updated.toDomain()
  }

  suspend fun setAiBotEnabled(botId: String, enabled: Boolean) {
    dao.setAiBotEnabled(botId, enabled, System.currentTimeMillis())
  }

  suspend fun deleteAiBot(botId: String) {
    dao.deleteAiBot(botId)
  }

  suspend fun createGroupChat(title: String, topic: String, botIds: List<String>): GroupChatRoom {
    require(botIds.isNotEmpty()) { "请至少选择一个机器人" }
    val now = System.currentTimeMillis()
    val room = GroupChatRoomEntity(
      id = newId("group"),
      title = title.trim().ifBlank { "AI 群聊" },
      topic = topic.trim(),
      summary = "",
      createdAt = now,
      updatedAt = now
    )
    dao.upsertGroupChatRoom(room)
    botIds.distinct().forEachIndexed { index, botId ->
      if (dao.aiBotById(botId) != null) {
        dao.upsertGroupChatMember(
          GroupChatMemberEntity(
            groupId = room.id,
            botId = botId,
            sortOrder = index,
            enabled = true,
            createdAt = now,
            updatedAt = now
          )
        )
      }
    }
    return room.toDomain()
  }

  suspend fun updateGroupChatMeta(groupId: String, title: String, topic: String) {
    dao.updateGroupChatRoomMeta(groupId, title.trim().ifBlank { "AI 群聊" }, topic.trim(), System.currentTimeMillis())
  }

  suspend fun addBotToGroup(groupId: String, botId: String) {
    val now = System.currentTimeMillis()
    val current = dao.groupChatMembers(groupId)
    dao.upsertGroupChatMember(
      GroupChatMemberEntity(
        groupId = groupId,
        botId = botId,
        sortOrder = current.size,
        enabled = true,
        createdAt = now,
        updatedAt = now
      )
    )
  }

  suspend fun removeBotFromGroup(groupId: String, botId: String) {
    dao.removeGroupChatMember(groupId, botId, System.currentTimeMillis())
  }

  suspend fun deleteGroupChat(groupId: String) {
    dao.deleteGroupChatRoom(groupId, System.currentTimeMillis())
  }

  suspend fun sendGroupUserMessage(groupId: String, text: String, attachments: List<ChatAttachment> = emptyList()) {
    val room = dao.groupChatRoomById(groupId) ?: return
    val cleanText = text.trim()
    if (cleanText.isBlank() && attachments.isEmpty()) return
    val now = System.currentTimeMillis()
    dao.upsertGroupMessage(
      GroupMessageEntity(
        id = newId("gmsg"),
        groupId = groupId,
        senderType = GroupMessageSenderType.USER.name,
        botId = null,
        senderName = "我",
        role = MessageRole.USER.name,
        content = cleanText,
        status = MessageStatus.COMPLETE.name,
        providerId = null,
        model = null,
        createdAt = now,
        updatedAt = now,
        errorMessage = null,
        attachmentsJson = formatAttachments(attachments)
      )
    )
    dao.touchGroupChatRoom(room.id, now)
  }

  suspend fun sendGroupBotTurn(groupId: String, botId: String, summarize: Boolean = false) {
    val room = dao.groupChatRoomById(groupId) ?: return
    val bot = dao.aiBotById(botId) ?: return
    val provider = dao.providerById(bot.providerId)?.toDomain() ?: return
    val now = System.currentTimeMillis()
    val message = GroupMessageEntity(
      id = newId("gmsg"),
      groupId = groupId,
      senderType = GroupMessageSenderType.BOT.name,
      botId = bot.id,
      senderName = bot.name,
      role = MessageRole.ASSISTANT.name,
      content = "",
      status = MessageStatus.STREAMING.name,
      providerId = provider.id,
      model = bot.model,
      createdAt = now + 1_000,
      updatedAt = now + 1_000,
      errorMessage = null
    )
    dao.upsertGroupMessage(message)
    streamGroupBot(room, bot.toDomain(), provider, message.id, summarize)
  }

  suspend fun bootstrapDefaults() {
    val existing = dao.observeProviders().first().map { it.id }.toSet()
    defaultProviders().forEach { provider ->
      if (provider.id !in existing) {
        dao.upsertProvider(provider)
      }
    }
  }

  fun providerTemplate(type: ProviderType): ChatProviderConfig {
    return when (type) {
      ProviderType.OPENAI_RESPONSES -> ChatProviderConfig(
        id = newId("provider"),
        displayName = "GPT (OpenAI)",
        type = ProviderType.OPENAI_RESPONSES,
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4.1-mini",
        enabled = true,
        supportsStreaming = true,
        supportsAttachments = true,
        extraHeadersJson = "",
        secretRef = null,
        reasoningEffort = ReasoningEffort.AUTO
      )
      ProviderType.OPENAI_COMPATIBLE_CHAT -> ChatProviderConfig(
        id = newId("provider"),
        displayName = "DeepSeek",
        type = ProviderType.OPENAI_COMPATIBLE_CHAT,
        baseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        enabled = true,
        supportsStreaming = true,
        supportsAttachments = false,
        extraHeadersJson = "",
        secretRef = null,
        reasoningEffort = ReasoningEffort.AUTO
      )
      ProviderType.TOKENHUB_PROXY -> ChatProviderConfig(
        id = newId("provider"),
        displayName = "TokenHub 代理",
        type = ProviderType.TOKENHUB_PROXY,
        baseUrl = "http://127.0.0.1:8787/v1",
        defaultModel = "glm-5.1",
        enabled = true,
        supportsStreaming = true,
        supportsAttachments = true,
        extraHeadersJson = "",
        secretRef = null,
        reasoningEffort = ReasoningEffort.AUTO
      )
      ProviderType.ANTHROPIC_MESSAGES -> ChatProviderConfig(
        id = newId("provider"),
        displayName = "Anthropic",
        type = ProviderType.ANTHROPIC_MESSAGES,
        baseUrl = "https://api.anthropic.com/v1",
        defaultModel = "claude-3-5-sonnet-latest",
        enabled = false,
        supportsStreaming = true,
        supportsAttachments = false,
        extraHeadersJson = "",
        secretRef = null,
        reasoningEffort = ReasoningEffort.AUTO
      )
      ProviderType.GEMINI_GENERATE_CONTENT -> ChatProviderConfig(
        id = newId("provider"),
        displayName = "Gemini",
        type = ProviderType.GEMINI_GENERATE_CONTENT,
        baseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-1.5-pro",
        enabled = false,
        supportsStreaming = true,
        supportsAttachments = false,
        extraHeadersJson = "",
        secretRef = null,
        reasoningEffort = ReasoningEffort.AUTO
      )
    }
  }

  suspend fun cloneProvider(providerId: String): ChatProviderConfig? {
    val source = dao.providerById(providerId)?.toDomain() ?: return null
    val clone = source.copy(
      id = newId("provider"),
      displayName = "${source.displayName} 副本",
      secretRef = null
    )
    saveProvider(clone, apiKey = null)
    return clone
  }

  fun hasApiKey(provider: ChatProviderConfig?): Boolean {
    return apiKeyStore.exists(provider?.secretRef)
  }

  suspend fun ensureConversation(): ChatConversation {
    val existing = dao.latestConversation()
    if (existing != null) return existing.toDomain()
    val provider = dao.observeProviders().first().firstOrNull()
      ?: defaultProviders().first().also { dao.upsertProvider(it) }
    return createConversation(provider.id, provider.defaultModel)
  }

  suspend fun createConversation(providerId: String, model: String): ChatConversation {
    val now = System.currentTimeMillis()
    val conversation = ConversationEntity(
      id = newId("conv"),
      title = "新对话",
      providerId = providerId,
      model = model,
      groupName = "",
      createdAt = now,
      updatedAt = now
    )
    dao.upsertConversation(conversation)
    preferencesRepository.setSelectedConversation(conversation.id)
    return conversation.toDomain()
  }

  suspend fun forkConversationAtMessage(
    sourceConversationId: String,
    sourceMessageId: String,
    targetProviderId: String
  ): ChatConversation? {
    val sourceConversation = dao.conversationById(sourceConversationId) ?: return null
    val targetProvider = dao.providerById(targetProviderId)?.toDomain() ?: return null
    val sourceMessages = dao.messagesForConversation(sourceConversationId)
    val forkIndex = sourceMessages.indexOfFirst { it.id == sourceMessageId }
    if (forkIndex < 0) return null

    val now = System.currentTimeMillis()
    val forkedConversation = ConversationEntity(
      id = newId("conv"),
      title = "${sourceConversation.title} - ${targetProvider.displayName}",
      providerId = targetProvider.id,
      model = targetProvider.defaultModel,
      groupName = sourceConversation.groupName,
      forkedFromConversationId = sourceConversation.id,
      forkedFromMessageId = sourceMessageId,
      createdAt = now,
      updatedAt = now
    )
    dao.upsertConversation(forkedConversation)

    val copiedMessages = sourceMessages.take(forkIndex + 1).map { source ->
      source.copy(
        id = newId("msg"),
        conversationId = forkedConversation.id
      )
    }
    copiedMessages.forEach { dao.upsertMessage(it) }

    preferencesRepository.setSelectedConversation(forkedConversation.id)
    preferencesRepository.setSelectedProvider(targetProvider.id)

    if (sourceMessages[forkIndex].role == MessageRole.USER.name) {
      val assistantMessage = MessageEntity(
        id = newId("msg"),
        conversationId = forkedConversation.id,
        role = MessageRole.ASSISTANT.name,
        content = "",
        status = MessageStatus.STREAMING.name,
        providerId = targetProvider.id,
        model = forkedConversation.model,
        createdAt = now + 1_000,
        updatedAt = now + 1_000,
        errorMessage = null
      )
      dao.upsertMessage(assistantMessage)
      streamAssistant(forkedConversation.id, targetProvider, forkedConversation.model, assistantMessage.id)
    }

    return forkedConversation.toDomain()
  }

  suspend fun setConversationPinned(conversationId: String, pinned: Boolean) {
    dao.setConversationPinned(conversationId, pinned, System.currentTimeMillis())
  }

  suspend fun archiveConversation(conversationId: String): ChatConversation {
    dao.archiveConversation(conversationId, System.currentTimeMillis())
    return ensureConversation()
  }

  suspend fun restoreConversation(conversationId: String): ChatConversation {
    dao.restoreConversation(conversationId, System.currentTimeMillis())
    return dao.conversationById(conversationId)?.toDomain() ?: ensureConversation()
  }

  suspend fun deleteConversation(conversationId: String): ChatConversation {
    dao.deleteConversation(conversationId, System.currentTimeMillis())
    return ensureConversation()
  }

  suspend fun updateConversationMeta(conversationId: String, title: String, groupName: String) {
    dao.updateConversationMeta(
      conversationId,
      title.trim(),
      groupName.trim(),
      System.currentTimeMillis()
    )
  }

  suspend fun renameConversationGroup(oldGroupName: String, newGroupName: String) {
    dao.renameConversationGroup(
      oldGroupName.trim(),
      newGroupName.trim(),
      System.currentTimeMillis()
    )
  }

  suspend fun switchConversationProvider(conversationId: String?, providerId: String) {
    val provider = dao.providerById(providerId)?.toDomain() ?: return
    preferencesRepository.setSelectedProvider(providerId)
    if (conversationId.isNullOrBlank()) return
    if (dao.messagesForConversation(conversationId).isNotEmpty()) return
    dao.updateConversationProvider(
      id = conversationId,
      providerId = provider.id,
      model = provider.defaultModel,
      updatedAt = System.currentTimeMillis()
    )
  }

  suspend fun saveProvider(provider: ChatProviderConfig, apiKey: String?) {
    val secretRef = "provider_${provider.id}"
    if (!apiKey.isNullOrBlank()) {
      apiKeyStore.write(secretRef, apiKey.trim())
    }
    dao.upsertProvider(
      ProviderEntity(
        id = provider.id,
        displayName = provider.displayName,
        type = provider.type.name,
        baseUrl = provider.baseUrl.trimEnd('/'),
        defaultModel = provider.defaultModel,
        enabled = provider.enabled,
        supportsStreaming = provider.supportsStreaming,
        supportsAttachments = provider.supportsAttachments,
        extraHeadersJson = provider.extraHeadersJson,
        reasoningEffort = provider.reasoningEffort.name,
        secretRef = secretRef,
        sortOrder = System.currentTimeMillis().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
      )
    )
  }

  suspend fun sendMessage(conversationId: String, text: String, attachments: List<ChatAttachment> = emptyList()) {
    val cleanText = text.trim()
    if (cleanText.isEmpty() && attachments.isEmpty()) return
    val conversation = dao.conversationById(conversationId) ?: return
    val provider = dao.providerById(conversation.providerId)?.toDomain() ?: return
    val now = System.currentTimeMillis()
    val userMessage = MessageEntity(
      id = newId("msg"),
      conversationId = conversationId,
      role = MessageRole.USER.name,
      content = cleanText,
      attachmentsJson = formatAttachments(attachments),
      status = MessageStatus.COMPLETE.name,
      providerId = provider.id,
      model = conversation.model,
      createdAt = now,
      updatedAt = now,
      errorMessage = null
    )
    val assistantMessage = MessageEntity(
      id = newId("msg"),
      conversationId = conversationId,
      role = MessageRole.ASSISTANT.name,
      content = "",
      status = MessageStatus.STREAMING.name,
      providerId = provider.id,
      model = conversation.model,
      createdAt = now + 1_000,
      updatedAt = now + 1_000,
      errorMessage = null
    )
    dao.upsertMessage(userMessage)
    dao.upsertMessage(assistantMessage)
    if (conversation.title == "新对话" || conversation.title == "New chat") {
      val titleSource = cleanText.takeIf { it.isNotBlank() } ?: attachments.firstOrNull()?.displayName ?: "带附件的对话"
      dao.updateConversationTitle(conversationId, titleSource.take(40), now)
    } else {
      dao.upsertConversation(conversation.copy(updatedAt = now))
    }
    streamAssistant(conversationId, provider, conversation.model, assistantMessage.id)
  }

  suspend fun conversationShareText(conversationId: String, includeTimestamps: Boolean = true): String {
    val export = conversationExport(conversationId) ?: return ""
    return buildConversationShareText(export, includeTimestamps)
  }

  suspend fun conversationShareText(
    conversationId: String,
    messageIds: Set<String>,
    includeTimestamps: Boolean = true
  ): String {
    if (messageIds.isEmpty()) return conversationShareText(conversationId, includeTimestamps)
    val export = conversationExport(conversationId) ?: return ""
    val selectedMessages = dao.messagesForConversation(conversationId)
      .filter { it.id in messageIds }
      .map { it.toDomain() }
    return buildConversationShareText(
      export.copy(
        title = "${export.title}（节选）",
        messages = selectedMessages.map {
          ConversationExportMessage(
            id = it.id,
            role = it.role,
            content = it.content,
            status = it.status,
            errorMessage = it.errorMessage,
            createdAt = it.createdAt
          )
        }
      ),
      includeTimestamps
    )
  }

  suspend fun conversationExport(conversationId: String): ConversationExport? {
    val conversation = dao.conversationById(conversationId) ?: return null
    val provider = dao.providerById(conversation.providerId)?.toDomain()
    val messages = dao.messagesForConversation(conversationId).map { it.toDomain() }
    return ConversationExport(
      title = conversation.title,
      groupName = conversation.groupName.takeIf { it.isNotBlank() },
      modelLabel = provider?.let { "${it.displayName} / ${conversation.model}" },
      messages = messages.map {
        ConversationExportMessage(
          id = it.id,
          role = it.role,
          content = it.content,
          status = it.status,
          errorMessage = it.errorMessage,
          createdAt = it.createdAt
        )
      }
    )
  }

  private fun buildConversationShareText(
    export: ConversationExport,
    includeTimestamps: Boolean = true
  ): String {
    val builder = StringBuilder()
    builder.appendLine("会话：${export.title}")
    if (!export.groupName.isNullOrBlank()) {
      builder.appendLine("分组：${export.groupName}")
    }
    if (!export.modelLabel.isNullOrBlank()) {
      builder.appendLine("模型：${export.modelLabel}")
    }
    builder.appendLine()
    export.messages.forEach { message ->
      val roleName = when (message.role) {
        MessageRole.USER -> "我"
        MessageRole.ASSISTANT -> "AI"
        MessageRole.SYSTEM -> "系统"
        MessageRole.TOOL -> "工具"
      }
      if (includeTimestamps) {
        builder.appendLine("[$roleName ${formatShareTime(message.createdAt)}]")
      } else {
        builder.appendLine("[$roleName]")
      }
      builder.appendLine(message.content)
      builder.appendLine()
    }
    return builder.toString().trim()
  }

  suspend fun messageShareText(conversationId: String, messageId: String, includeTimestamps: Boolean = true): String {
    val export = conversationExport(conversationId) ?: return ""
    val message = export.messages.firstOrNull { it.id == messageId } ?: return ""
    return buildConversationShareText(
      export.copy(
        title = "${export.title}（单条消息）",
        messages = listOf(message)
      ),
      includeTimestamps
    )
  }

  suspend fun messageExport(conversationId: String, messageId: String): ConversationExport? {
    val export = conversationExport(conversationId) ?: return null
    val message = export.messages.firstOrNull { it.id == messageId } ?: return null
    return export.copy(
      title = "${export.title}（单条消息）",
      messages = listOf(message)
    )
  }

  suspend fun favoriteSnippetShareText(favoriteId: String, includeTimestamps: Boolean = true): String {
    val favorite = favoriteSnippetById(favoriteId) ?: return ""
    return buildFavoriteShareText(favorite, includeTimestamps)
  }

  suspend fun favoriteSnippetExport(favoriteId: String): ConversationExport? {
    val favorite = favoriteSnippetById(favoriteId) ?: return null
    return favorite.toConversationExport()
  }

  suspend fun retryLast(conversationId: String) {
    val conversation = dao.conversationById(conversationId) ?: return
    val provider = dao.providerById(conversation.providerId)?.toDomain() ?: return
    dao.lastUserMessage(conversationId) ?: return
    val now = System.currentTimeMillis()
    val assistantMessage = MessageEntity(
      id = newId("msg"),
      conversationId = conversationId,
      role = MessageRole.ASSISTANT.name,
      content = "",
      status = MessageStatus.STREAMING.name,
      providerId = provider.id,
      model = conversation.model,
      createdAt = now + 1_000,
      updatedAt = now + 1_000,
      errorMessage = null
    )
    dao.upsertMessage(assistantMessage)
    streamAssistant(conversationId, provider, conversation.model, assistantMessage.id)
  }

  suspend fun exportProvidersText(includeApiKeys: Boolean = true): String {
    val providers = dao.observeProviders().first().map { it.toDomain() }
    val export = ProviderConfigExport(
      exportedAt = System.currentTimeMillis(),
      providers = providers.map { provider ->
        ProviderConfigExportItem(
          id = provider.id,
          displayName = provider.displayName,
          type = provider.type.name,
          baseUrl = provider.baseUrl,
          defaultModel = provider.defaultModel,
          enabled = provider.enabled,
          supportsStreaming = provider.supportsStreaming,
          supportsAttachments = provider.supportsAttachments,
          extraHeadersJson = provider.extraHeadersJson,
          reasoningEffort = provider.reasoningEffort.name,
          apiKey = if (includeApiKeys) apiKeyStore.read(provider.secretRef) else null
        )
      }
    )
    return gson.toJson(export)
  }

  suspend fun importProvidersText(text: String): Int {
    val export = runCatching {
      gson.fromJson(text.trim(), ProviderConfigExport::class.java)
    }.getOrNull() ?: return 0
    val existingIds = dao.observeProviders().first().map { it.id }.toMutableSet()
    var imported = 0
    export.providers.forEach { item ->
      val type = runCatching { ProviderType.valueOf(item.type) }.getOrNull() ?: return@forEach
      val reasoning = runCatching { ReasoningEffort.valueOf(item.reasoningEffort) }.getOrDefault(ReasoningEffort.AUTO)
      val id = item.id
        .takeIf { it.isNotBlank() && it !in existingIds }
        ?: newId("provider")
      existingIds += id
      saveProvider(
        provider = ChatProviderConfig(
          id = id,
          displayName = item.displayName.ifBlank { type.name },
          type = type,
          baseUrl = item.baseUrl.trimEnd('/'),
          defaultModel = item.defaultModel,
          enabled = item.enabled,
          supportsStreaming = item.supportsStreaming,
          supportsAttachments = item.supportsAttachments,
          extraHeadersJson = item.extraHeadersJson,
          secretRef = "provider_$id",
          reasoningEffort = reasoning
        ),
        apiKey = item.apiKey?.takeIf { it.isNotBlank() }
      )
      imported += 1
    }
    return imported
  }

  private suspend fun streamAssistant(
    conversationId: String,
    provider: ChatProviderConfig,
    model: String,
    assistantMessageId: String
  ) {
    val adapter = adapters[provider.type]
    if (adapter == null) {
      dao.updateMessage(
        id = assistantMessageId,
        content = "",
        status = MessageStatus.FAILED.name,
        updatedAt = System.currentTimeMillis(),
        errorMessage = "Provider ${provider.type} is not implemented yet"
      )
      return
    }
    val allMessages = dao.messagesForConversation(conversationId)
    val assistantMessage = allMessages.firstOrNull { it.id == assistantMessageId }
    val messages = allMessages
      .filter { it.id != assistantMessageId }
      .filter { it.role != MessageRole.TOOL.name }
      .map { it.toDomain() }
    val apiKey = apiKeyStore.read(provider.secretRef)
    val appSettings = preferencesRepository.appSettings.first()
    val captureRawResponseLog = appSettings.debugResponseLogging
    if (apiKey.isNullOrBlank() && provider.type != ProviderType.TOKENHUB_PROXY) {
      dao.updateMessage(
        assistantMessageId,
        "",
        MessageStatus.FAILED.name,
        System.currentTimeMillis(),
        "当前 API 配置还没有保存 Key，请在 API 配置中填写后再发送。"
      )
      return
    }
    var output = ""
    val startedAt = System.currentTimeMillis()
    var firstTokenAt: Long? = null
    var promptTokens: Int? = null
    var completionTokens: Int? = null
    var totalTokens: Int? = null
    val rawResponseLog = StringBuilder()
    val toolMessageIds = mutableMapOf<String, String>()
    var toolSequence = 0

    fun appendRawFrame(event: String?, data: String) {
      if (!captureRawResponseLog || rawResponseLog.length >= MaxRawResponseLogChars) return
      val frame = buildString {
        if (event != null) append("event: ").append(event).append('\n')
        append("data: ").append(data).append("\n\n")
      }
      val remaining = MaxRawResponseLogChars - rawResponseLog.length
      rawResponseLog.append(frame.take(remaining))
      if (frame.length > remaining) {
        rawResponseLog.append("\n... raw response log truncated ...")
      }
    }

    suspend fun updateFinalMessage(status: MessageStatus, errorMessage: String?) {
      val now = System.currentTimeMillis()
      dao.updateMessageWithMetadata(
        id = assistantMessageId,
        content = output,
        status = status.name,
        updatedAt = now,
        errorMessage = errorMessage,
        totalDurationMs = now - startedAt,
        firstTokenDurationMs = firstTokenAt?.let { it - startedAt },
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        rawResponseLog = rawResponseLog.toString().takeIf { captureRawResponseLog && it.isNotBlank() }
      )
    }

    suspend fun upsertToolMessage(event: ChatStreamEvent.ToolCall) {
      val key = event.id ?: "${event.name}-${toolSequence++}"
      val existingId = toolMessageIds[key]
      val now = System.currentTimeMillis()
      val content = formatToolCallMessage(event)
      if (existingId == null) {
        val messageId = newId("tool")
        toolMessageIds[key] = messageId
        dao.upsertMessage(
          MessageEntity(
            id = messageId,
            conversationId = conversationId,
            role = MessageRole.TOOL.name,
            content = content,
            status = if (event.output == null) MessageStatus.STREAMING.name else MessageStatus.COMPLETE.name,
            providerId = provider.id,
            model = model,
            createdAt = (assistantMessage?.createdAt ?: now + 1_000) - 500 + toolMessageIds.size,
            updatedAt = now,
            errorMessage = null
          )
        )
      } else {
        dao.updateMessage(
          id = existingId,
          content = content,
          status = if (event.output == null) MessageStatus.STREAMING.name else MessageStatus.COMPLETE.name,
          updatedAt = now,
          errorMessage = null
        )
      }
    }

    try {
      adapter.streamChat(
        config = provider,
        apiKey = apiKey,
        messages = messages,
        options = ChatCompletionOptions(
          model = model,
          stream = provider.supportsStreaming,
          captureRawResponseLog = captureRawResponseLog,
          webSearchMode = appSettings.webSearchMode
        )
      ).collect { event ->
        when (event) {
          ChatStreamEvent.Started -> Unit
          is ChatStreamEvent.TextDelta -> {
            if (firstTokenAt == null) firstTokenAt = System.currentTimeMillis()
            output += event.text
            val now = System.currentTimeMillis()
            dao.updateMessageWithMetadata(
              id = assistantMessageId,
              content = output,
              status = MessageStatus.STREAMING.name,
              updatedAt = now,
              errorMessage = null,
              totalDurationMs = now - startedAt,
              firstTokenDurationMs = firstTokenAt?.let { it - startedAt },
              promptTokens = promptTokens,
              completionTokens = completionTokens,
              totalTokens = totalTokens,
              rawResponseLog = rawResponseLog.toString().takeIf { captureRawResponseLog && it.isNotBlank() }
            )
          }
          is ChatStreamEvent.Usage -> {
            promptTokens = event.promptTokens ?: promptTokens
            completionTokens = event.completionTokens ?: completionTokens
            totalTokens = event.totalTokens ?: totalTokens
            if (event.raw != null) appendRawFrame("usage", event.raw)
          }
          is ChatStreamEvent.RawFrame -> {
            appendRawFrame(event.event, event.data)
          }
          is ChatStreamEvent.ToolCall -> {
            upsertToolMessage(event)
            appendRawFrame(
              "tool_call",
              buildString {
                append("name=").append(event.name)
                event.input?.let { append("\ninput=").append(it) }
                event.output?.let { append("\noutput=").append(it.take(2_000)) }
              }
            )
          }
          ChatStreamEvent.Completed -> {
            updateFinalMessage(MessageStatus.COMPLETE, null)
            dao.touchConversation(conversationId, System.currentTimeMillis())
          }
          is ChatStreamEvent.Failed -> {
            updateFinalMessage(MessageStatus.FAILED, event.message)
            dao.touchConversation(conversationId, System.currentTimeMillis())
          }
        }
      }
    } catch (error: CancellationException) {
      updateFinalMessage(MessageStatus.FAILED, "已停止")
      dao.touchConversation(conversationId, System.currentTimeMillis())
      throw error
    } catch (error: Exception) {
      val friendlyMessage = friendlyNetworkErrorMessage(error)
      updateFinalMessage(MessageStatus.FAILED, friendlyMessage)
    }
  }

  private suspend fun streamGroupBot(
    room: GroupChatRoomEntity,
    bot: AiBot,
    provider: ChatProviderConfig,
    botMessageId: String,
    summarize: Boolean
  ) {
    val adapter = adapters[provider.type]
    if (adapter == null) {
      dao.updateGroupMessageWithMetadata(
        id = botMessageId,
        content = "",
        status = MessageStatus.FAILED.name,
        updatedAt = System.currentTimeMillis(),
        errorMessage = "Provider ${provider.type} is not implemented yet",
        totalDurationMs = null,
        firstTokenDurationMs = null,
        promptTokens = null,
        completionTokens = null,
        totalTokens = null
      )
      return
    }
    val apiKey = apiKeyStore.read(provider.secretRef)
    if (apiKey.isNullOrBlank() && provider.type != ProviderType.TOKENHUB_PROXY) {
      dao.updateGroupMessageWithMetadata(
        id = botMessageId,
        content = "",
        status = MessageStatus.FAILED.name,
        updatedAt = System.currentTimeMillis(),
        errorMessage = "当前 API 配置还没有保存 Key，请在 API 配置中填写后再发送。",
        totalDurationMs = null,
        firstTokenDurationMs = null,
        promptTokens = null,
        completionTokens = null,
        totalTokens = null
      )
      return
    }
    val members = dao.groupChatMembers(room.id).mapNotNull { dao.aiBotById(it.botId)?.toDomain() }
    val groupMessages = dao.groupMessages(room.id).filter { it.id != botMessageId }.map { it.toDomain() }
    val contextMessages = buildGroupContextMessages(
      room = room.toDomain(),
      bot = bot,
      members = members,
      messages = groupMessages,
      providerSupportsAttachments = provider.supportsAttachments,
      summarize = summarize
    )
    val appSettings = preferencesRepository.appSettings.first()
    var output = ""
    val startedAt = System.currentTimeMillis()
    var firstTokenAt: Long? = null
    var promptTokens: Int? = null
    var completionTokens: Int? = null
    var totalTokens: Int? = null
    var toolSequence = 0
    val toolMessageIds = mutableMapOf<String, String>()

    suspend fun updateBotMessage(status: MessageStatus, errorMessage: String?) {
      val now = System.currentTimeMillis()
      dao.updateGroupMessageWithMetadata(
        id = botMessageId,
        content = output,
        status = status.name,
        updatedAt = now,
        errorMessage = errorMessage,
        totalDurationMs = now - startedAt,
        firstTokenDurationMs = firstTokenAt?.let { it - startedAt },
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
      )
      dao.touchGroupChatRoom(room.id, now)
    }

    suspend fun upsertGroupToolMessage(event: ChatStreamEvent.ToolCall) {
      val key = event.id ?: "${event.name}-${toolSequence++}"
      val existingId = toolMessageIds[key]
      val now = System.currentTimeMillis()
      val content = formatToolCallMessage(event)
      if (existingId == null) {
        val messageId = newId("gtool")
        toolMessageIds[key] = messageId
        dao.upsertGroupMessage(
          GroupMessageEntity(
            id = messageId,
            groupId = room.id,
            senderType = GroupMessageSenderType.TOOL.name,
            botId = bot.id,
            senderName = "${bot.name} 的工具",
            role = MessageRole.TOOL.name,
            content = content,
            status = if (event.output == null) MessageStatus.STREAMING.name else MessageStatus.COMPLETE.name,
            providerId = provider.id,
            model = bot.model,
            createdAt = now,
            updatedAt = now,
            errorMessage = null
          )
        )
      } else {
        dao.updateGroupMessageWithMetadata(
          id = existingId,
          content = content,
          status = if (event.output == null) MessageStatus.STREAMING.name else MessageStatus.COMPLETE.name,
          updatedAt = now,
          errorMessage = null,
          totalDurationMs = null,
          firstTokenDurationMs = null,
          promptTokens = null,
          completionTokens = null,
          totalTokens = null
        )
      }
    }

    try {
      adapter.streamChat(
        config = provider,
        apiKey = apiKey,
        messages = contextMessages,
        options = ChatCompletionOptions(
          model = bot.model,
          stream = provider.supportsStreaming,
          captureRawResponseLog = false,
          webSearchMode = appSettings.webSearchMode
        )
      ).collect { event ->
        when (event) {
          ChatStreamEvent.Started -> Unit
          is ChatStreamEvent.TextDelta -> {
            if (firstTokenAt == null) firstTokenAt = System.currentTimeMillis()
            output += event.text
            updateBotMessage(MessageStatus.STREAMING, null)
          }
          is ChatStreamEvent.Usage -> {
            promptTokens = event.promptTokens ?: promptTokens
            completionTokens = event.completionTokens ?: completionTokens
            totalTokens = event.totalTokens ?: totalTokens
          }
          is ChatStreamEvent.RawFrame -> Unit
          is ChatStreamEvent.ToolCall -> upsertGroupToolMessage(event)
          ChatStreamEvent.Completed -> {
            updateBotMessage(MessageStatus.COMPLETE, null)
            if (summarize && output.isNotBlank()) {
              dao.updateGroupChatSummary(room.id, output.trim(), System.currentTimeMillis())
            }
          }
          is ChatStreamEvent.Failed -> updateBotMessage(MessageStatus.FAILED, event.message)
        }
      }
    } catch (error: CancellationException) {
      updateBotMessage(MessageStatus.FAILED, "已停止")
      throw error
    } catch (error: Exception) {
      updateBotMessage(MessageStatus.FAILED, friendlyNetworkErrorMessage(error))
    }
  }

  private fun buildGroupContextMessages(
    room: GroupChatRoom,
    bot: AiBot,
    members: List<AiBot>,
    messages: List<GroupChatMessage>,
    providerSupportsAttachments: Boolean,
    summarize: Boolean
  ): List<ChatMessage> {
    val participantLines = members.joinToString("\n") { "- ${it.name} (${it.model})" }
    val systemPrompt = buildString {
      appendLine("你正在参与一个多 AI 回合制群聊。")
      appendLine("当前你只代表机器人「${bot.name}」发言，不要模拟其他成员。")
      appendLine("可以审阅、质疑、补充其他成员观点，也可以向用户或其他机器人提出下一步建议。")
      appendLine("回答要围绕群主题和最近上下文，避免重复，必要时指出不确定性。")
      appendLine("如果讨论已经充分，主动建议暂停或总结。")
      if (summarize) {
        appendLine("本轮任务是总结当前讨论，请输出结构化、可继续作为后续上下文的摘要。")
      }
      if (bot.systemPrompt.isNotBlank()) {
        appendLine()
        appendLine("你的角色提示词：")
        appendLine(bot.systemPrompt)
      }
      appendLine()
      appendLine("群主题：${room.topic.ifBlank { room.title }}")
      appendLine("群成员：")
      appendLine(participantLines.ifBlank { "- ${bot.name}" })
      if (room.summary.isNotBlank()) {
        appendLine()
        appendLine("群聊摘要：")
        appendLine(room.summary)
      }
    }
    val result = mutableListOf(
      ChatMessage(
        id = "group-system-${room.id}",
        conversationId = room.id,
        role = MessageRole.SYSTEM,
        content = systemPrompt.trim(),
        status = MessageStatus.COMPLETE,
        providerId = null,
        model = null,
        createdAt = room.createdAt,
        updatedAt = room.updatedAt,
        errorMessage = null
      )
    )
    messages.takeLast(GroupRecentMessageLimit).forEach { message ->
      result += ChatMessage(
        id = message.id,
        conversationId = room.id,
        role = when (message.senderType) {
          GroupMessageSenderType.USER -> MessageRole.USER
          GroupMessageSenderType.BOT -> MessageRole.ASSISTANT
          GroupMessageSenderType.SYSTEM -> MessageRole.SYSTEM
          GroupMessageSenderType.TOOL -> MessageRole.SYSTEM
        },
        content = formatGroupContextContent(message, providerSupportsAttachments),
        status = message.status,
        providerId = message.providerId,
        model = message.model,
        createdAt = message.createdAt,
        updatedAt = message.updatedAt,
        errorMessage = message.errorMessage,
        attachments = if (providerSupportsAttachments) message.attachments else emptyList()
      )
    }
    return result
  }

  private fun formatGroupContextContent(message: GroupChatMessage, providerSupportsAttachments: Boolean): String {
    val prefix = when (message.senderType) {
      GroupMessageSenderType.USER -> "[用户]"
      GroupMessageSenderType.BOT -> "[${message.senderName}]"
      GroupMessageSenderType.SYSTEM -> "[系统]"
      GroupMessageSenderType.TOOL -> "[工具结果]"
    }
    val attachmentText = if (message.attachments.isEmpty()) {
      ""
    } else {
      message.attachments.joinToString(prefix = "\n附件：\n", separator = "\n") {
        "- ${it.displayName} (${it.mimeType}, ${it.sizeBytes} bytes)"
      }
    }
    return buildString {
      append(prefix).append(' ')
      append(message.content)
      if (!providerSupportsAttachments || message.senderType != GroupMessageSenderType.USER) {
        append(attachmentText)
      }
    }.trim()
  }

  private fun defaultProviders(): List<ProviderEntity> {
    return listOf(
      ProviderEntity(
        id = "tokenhub-proxy",
        displayName = "TokenHub 代理",
        type = ProviderType.TOKENHUB_PROXY.name,
        baseUrl = "http://127.0.0.1:8787/v1",
        defaultModel = "glm-5.1",
        enabled = true,
        supportsStreaming = true,
        supportsAttachments = true,
        extraHeadersJson = "",
        reasoningEffort = ReasoningEffort.AUTO.name,
        secretRef = "provider_tokenhub-proxy",
        sortOrder = 0
      ),
      ProviderEntity(
        id = "openai-responses",
        displayName = "GPT (OpenAI)",
        type = ProviderType.OPENAI_RESPONSES.name,
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4.1-mini",
        enabled = false,
        supportsStreaming = true,
        supportsAttachments = true,
        extraHeadersJson = "",
        reasoningEffort = ReasoningEffort.AUTO.name,
        secretRef = "provider_openai-responses",
        sortOrder = 1
      ),
      ProviderEntity(
        id = "openai-compatible",
        displayName = "DeepSeek",
        type = ProviderType.OPENAI_COMPATIBLE_CHAT.name,
        baseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        enabled = false,
        supportsStreaming = true,
        supportsAttachments = false,
        extraHeadersJson = "",
        reasoningEffort = ReasoningEffort.AUTO.name,
        secretRef = "provider_openai-compatible",
        sortOrder = 2
      )
    )
  }

  private fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"

  private fun ChatMessage.toFavoriteMessage(): FavoriteSnippetMessage = FavoriteSnippetMessage(
    id = id,
    role = role,
    content = content,
    status = status,
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
    attachments = attachments
  )

  private fun GroupChatMessage.toFavoriteMessage(): FavoriteSnippetMessage = FavoriteSnippetMessage(
    id = id,
    role = role,
    content = buildString {
      if (senderName.isNotBlank()) {
        append('[').append(senderName).append("] ")
      }
      append(content)
    },
    status = status,
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
    attachments = attachments
  )

  private fun defaultFavoriteTitle(conversationTitle: String, messages: List<FavoriteSnippetMessage>): String {
    return messages
      .firstOrNull { it.role == MessageRole.ASSISTANT && it.content.isNotBlank() }
      ?.content
      ?.lineSequence()
      ?.firstOrNull { it.isNotBlank() }
      ?.trim()
      ?.take(40)
      ?: "${conversationTitle.ifBlank { "对话" }}（节选）"
  }

  private fun buildFavoriteSearchText(
    title: String,
    description: String,
    tags: List<String>,
    sourceConversationTitle: String,
    sourceProviderName: String?,
    sourceModel: String?,
    messages: List<FavoriteSnippetMessage>
  ): String {
    return buildString {
      appendLine(title)
      appendLine(description)
      appendLine(tags.joinToString(" "))
      appendLine(sourceConversationTitle)
      appendLine(sourceProviderName.orEmpty())
      appendLine(sourceModel.orEmpty())
      messages.forEach { message ->
        appendLine(message.content)
        message.attachments.forEach { appendLine(it.displayName) }
      }
    }.lowercase()
  }

  private fun buildFavoriteShareText(favorite: FavoriteSnippet, includeTimestamps: Boolean): String {
    val builder = StringBuilder()
    builder.appendLine("收藏：${favorite.title}")
    if (favorite.tags.isNotEmpty()) {
      builder.appendLine("标签：${favorite.tags.joinToString("、")}")
    }
    if (favorite.description.isNotBlank()) {
      builder.appendLine("描述：${favorite.description}")
    }
    builder.appendLine("来源：${favorite.sourceConversationTitle}")
    favorite.sourceProviderName?.let { provider ->
      builder.appendLine("模型：$provider / ${favorite.sourceModel.orEmpty()}".trimEnd())
    } ?: favorite.sourceModel?.let { model ->
      builder.appendLine("模型：$model")
    }
    builder.appendLine()
    favorite.messages.forEach { message ->
      val roleName = when (message.role) {
        MessageRole.USER -> "我"
        MessageRole.ASSISTANT -> "AI"
        MessageRole.SYSTEM -> "系统"
        MessageRole.TOOL -> "工具"
      }
      if (includeTimestamps) {
        builder.appendLine("[$roleName ${formatShareTime(message.createdAt)}]")
      } else {
        builder.appendLine("[$roleName]")
      }
      builder.appendLine(formatFavoriteMessageContent(message))
      builder.appendLine()
    }
    return builder.toString().trim()
  }

  private fun FavoriteSnippet.toConversationExport(): ConversationExport = ConversationExport(
    title = title,
    groupName = sourceGroupName,
    modelLabel = listOfNotNull(sourceProviderName, sourceModel).joinToString(" / ").takeIf { it.isNotBlank() },
    messages = messages.map { message ->
      ConversationExportMessage(
        id = message.id,
        role = message.role,
        content = formatFavoriteMessageContent(message),
        status = message.status,
        errorMessage = message.errorMessage,
        createdAt = message.createdAt
      )
    }
  )

  private fun formatFavoriteMessageContent(message: FavoriteSnippetMessage): String {
    if (message.attachments.isEmpty()) return message.content
    val attachments = message.attachments.joinToString(separator = "\n") { attachment ->
      "- ${attachment.displayName} (${formatAttachmentSize(attachment.sizeBytes)})"
    }
    return buildString {
      if (message.content.isNotBlank()) {
        appendLine(message.content)
        appendLine()
      }
      appendLine("附件：")
      append(attachments)
    }.trim()
  }

  private fun formatAttachmentSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
  }

  private fun formatShareTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).apply {
      timeZone = java.util.TimeZone.getDefault()
    }
    return sdf.format(java.util.Date(timestamp))
  }

  private fun friendlyNetworkErrorMessage(error: Exception): String {
    val raw = generateSequence(error as Throwable) { it.cause }
      .mapNotNull { it.message }
      .joinToString(" | ")
      .ifBlank { error.toString() }
    return when {
      raw.contains("Trust anchor for certification path not found", ignoreCase = true) ->
        "HTTPS 证书链无法被当前设备信任。DeepSeek 官方 Base URL 建议使用 https://api.deepseek.com；同时请检查设备日期时间、系统根证书、VPN/抓包代理/公司网络是否替换了证书。为安全起见，App 不会使用“信任所有证书”的连接方式。"
      error is SSLHandshakeException || raw.contains("SSLHandshakeException", ignoreCase = true) ->
        "HTTPS 握手失败。请检查 Base URL 是否为官方 HTTPS 地址、设备时间是否正确，以及是否存在 VPN、抓包代理或网络网关替换证书。"
      raw.contains("UnknownHostException", ignoreCase = true) ->
        "域名无法解析。请检查 Base URL 是否正确。"
      raw.contains("SocketTimeoutException", ignoreCase = true) ->
        "请求超时。请检查网络连接或增大请求超时时间。"
      raw.contains("failed to connect", ignoreCase = true) ->
        "无法连接到服务器。请检查 Base URL、网络和代理设置。"
      else -> error.message ?: "Provider request failed"
    }
  }

  private fun formatToolCallMessage(event: ChatStreamEvent.ToolCall): String {
    return buildString {
      appendLine("工具：${event.name}")
      event.input?.takeIf { it.isNotBlank() }?.let {
        appendLine("输入：")
        appendLine(it)
      }
      event.output?.takeIf { it.isNotBlank() }?.let {
        appendLine("输出：")
        appendLine(it)
      }
    }.trim()
  }

  companion object {
    private fun defaultAdapters(): Map<ProviderType, ProviderAdapter> = mapOf(
      ProviderType.OPENAI_RESPONSES to OpenAiResponsesAdapter(),
      ProviderType.OPENAI_COMPATIBLE_CHAT to OpenAiCompatibleChatAdapter(),
      ProviderType.TOKENHUB_PROXY to TokenHubProxyAdapter()
    )
  }
}

