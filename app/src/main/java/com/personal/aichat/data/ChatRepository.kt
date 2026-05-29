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
import com.personal.aichat.domain.GroupTurnTrigger
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.ConversationType
import com.personal.aichat.domain.ImageGenerationApiMode
import com.personal.aichat.domain.ImageGenerationOptions
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
import java.io.File
import java.util.Base64
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
  val supportsImageGeneration: Boolean = false,
  val imageGenerationApiMode: String = ImageGenerationApiMode.RESPONSES_TOOL.name,
  val imageGenerationModel: String = "",
  val extraHeadersJson: String = "",
  val reasoningEffort: String = ReasoningEffort.AUTO.name,
  val apiKey: String? = null
)

data class DeleteProviderResult(
  val deleted: Boolean,
  val blockingBots: List<AiBot> = emptyList()
)

class ChatRepository(
  private val dao: ChatDao,
  private val preferencesRepository: ChatSelectionStore,
  private val apiKeyStore: ApiKeyStore,
  private val adapters: Map<ProviderType, ProviderAdapter> = defaultAdapters(),
  private val generatedImageDir: File = File(System.getProperty("java.io.tmpdir"), "aichat_generated_images")
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
    list.filter { it.groupName.isNotBlank() }
      .groupBy { it.groupName }
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

  suspend fun appendGroupMessagesToFavoriteSnippet(
    favoriteId: String,
    groupId: String,
    messageIds: Set<String>
  ): FavoriteSnippet? {
    require(messageIds.isNotEmpty()) { "请先选择要追加的群消息" }
    val existing = dao.favoriteSnippetById(favoriteId)?.toDomain() ?: return null
    check(existing.sourceConversationId == groupId) { "只能追加同一来源群聊里的消息" }
    val sourceMessages = dao.groupMessages(groupId)
      .filter { it.id in messageIds }
      .sortedBy { it.createdAt }
    require(sourceMessages.isNotEmpty()) { "请先选择要追加的群消息" }
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

  suspend fun createAiBot(
    name: String,
    providerId: String,
    model: String,
    systemPrompt: String,
    bubbleColorKey: String = "AUTO"
  ): AiBot {
    val provider = dao.providerById(providerId) ?: error("请选择有效的 API 配置")
    val now = System.currentTimeMillis()
    val bot = AiBotEntity(
      id = newId("bot"),
      name = name.trim().ifBlank { provider.displayName },
      providerId = provider.id,
      model = model.trim().ifBlank { provider.defaultModel },
      systemPrompt = systemPrompt.trim(),
      bubbleColorKey = bubbleColorKey.trim().ifBlank { "AUTO" },
      enabled = true,
      createdAt = now,
      updatedAt = now
    )
    dao.upsertAiBot(bot)
    return bot.toDomain()
  }

  suspend fun updateAiBot(
    botId: String,
    name: String,
    providerId: String,
    model: String,
    systemPrompt: String,
    bubbleColorKey: String = "AUTO"
  ): AiBot? {
    val existing = dao.aiBotById(botId) ?: return null
    val provider = dao.providerById(providerId) ?: error("请选择有效的 API 配置")
    val updated = existing.copy(
      name = name.trim().ifBlank { existing.name },
      providerId = provider.id,
      model = model.trim().ifBlank { provider.defaultModel },
      systemPrompt = systemPrompt.trim(),
      bubbleColorKey = bubbleColorKey.trim().ifBlank { "AUTO" },
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

  suspend fun updateGroupChat(groupId: String, title: String, topic: String, botIds: List<String>) {
    require(botIds.isNotEmpty()) { "请至少选择一个机器人" }
    val room = dao.groupChatRoomById(groupId) ?: error("群聊不存在")
    val now = System.currentTimeMillis()
    dao.updateGroupChatRoomMeta(room.id, title.trim().ifBlank { "AI 群聊" }, topic.trim(), now)
    val selectedBotIds = botIds.distinct()
    val existingByBotId = dao.allGroupChatMembers(room.id).associateBy { it.botId }
    selectedBotIds.forEachIndexed { index, botId ->
      val bot = dao.aiBotById(botId) ?: return@forEachIndexed
      val existing = existingByBotId[bot.id]
      dao.upsertGroupChatMember(
        GroupChatMemberEntity(
          groupId = room.id,
          botId = bot.id,
          sortOrder = index,
          enabled = true,
          createdAt = existing?.createdAt ?: now,
          updatedAt = now
        )
      )
    }
    existingByBotId.keys
      .filter { it !in selectedBotIds }
      .forEach { botId -> dao.removeGroupChatMember(room.id, botId, now) }
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

  suspend fun sendGroupBotTurn(
    groupId: String,
    botId: String,
    summarize: Boolean = false,
    trigger: GroupTurnTrigger = GroupTurnTrigger.MANUAL
  ): MessageStatus {
    val room = dao.groupChatRoomById(groupId) ?: return MessageStatus.FAILED
    val bot = dao.aiBotById(botId) ?: return MessageStatus.FAILED
    val provider = dao.providerById(bot.providerId)?.toDomain() ?: return MessageStatus.FAILED
    val now = System.currentTimeMillis()
    val turnInfo = nextGroupTurnInfo(groupId, botId, if (summarize) GroupTurnTrigger.SUMMARY else trigger)
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
      errorMessage = null,
      turnTrigger = turnInfo.trigger.name,
      turnRound = turnInfo.round,
      turnIndex = turnInfo.index,
      turnMemberCount = turnInfo.memberCount
    )
    dao.upsertGroupMessage(message)
    return streamGroupBot(room, bot.toDomain(), provider, message.id, summarize, turnInfo)
  }

  private suspend fun nextGroupTurnInfo(
    groupId: String,
    botId: String,
    trigger: GroupTurnTrigger
  ): GroupTurnInfo {
    val messages = dao.groupMessages(groupId)
    return when (trigger) {
      GroupTurnTrigger.AUTO -> {
        val memberCount = dao.groupChatMembers(groupId).count { it.enabled }.coerceAtLeast(1)
        val turnNumber = messages.count {
          it.senderType == GroupMessageSenderType.BOT.name &&
            it.role == MessageRole.ASSISTANT.name &&
            it.turnTrigger == GroupTurnTrigger.AUTO.name
        } + 1
        GroupTurnInfo(
          trigger = trigger,
          round = ((turnNumber - 1) / memberCount) + 1,
          index = ((turnNumber - 1) % memberCount) + 1,
          memberCount = memberCount
        )
      }
      GroupTurnTrigger.SUMMARY -> {
        val count = messages.count {
          it.senderType == GroupMessageSenderType.BOT.name &&
            it.role == MessageRole.ASSISTANT.name &&
            it.turnTrigger == GroupTurnTrigger.SUMMARY.name
        } + 1
        GroupTurnInfo(trigger = trigger, round = null, index = count, memberCount = null)
      }
      GroupTurnTrigger.MANUAL -> {
        val count = messages.count {
          it.senderType == GroupMessageSenderType.BOT.name &&
            it.role == MessageRole.ASSISTANT.name &&
            it.turnTrigger == GroupTurnTrigger.MANUAL.name
        } + 1
        GroupTurnInfo(trigger = trigger, round = null, index = count, memberCount = null)
      }
      GroupTurnTrigger.UNKNOWN -> {
        val count = messages.count {
          it.senderType == GroupMessageSenderType.BOT.name &&
            it.role == MessageRole.ASSISTANT.name &&
            it.botId == botId
        } + 1
        GroupTurnInfo(trigger = GroupTurnTrigger.MANUAL, round = null, index = count, memberCount = null)
      }
    }
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
        supportsImageGeneration = true,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL,
        imageGenerationModel = "",
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
        supportsImageGeneration = false,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL,
        imageGenerationModel = "",
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
        supportsImageGeneration = false,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL,
        imageGenerationModel = "",
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
        supportsImageGeneration = false,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL,
        imageGenerationModel = "",
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
        supportsImageGeneration = false,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL,
        imageGenerationModel = "",
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

  suspend fun deleteProvider(providerId: String): DeleteProviderResult {
    val provider = dao.providerById(providerId) ?: return DeleteProviderResult(deleted = true)
    val blockingBots = dao.aiBotsByProviderId(providerId).map { it.toDomain() }
    if (blockingBots.isNotEmpty()) {
      return DeleteProviderResult(deleted = false, blockingBots = blockingBots)
    }
    dao.deleteProvider(providerId)
    provider.secretRef?.let { apiKeyStore.delete(it) }
    val remainingProviders = dao.observeProviders().first()
    val nextProvider = remainingProviders.firstOrNull()?.toDomain()
      ?: defaultProviders().first().also { dao.upsertProvider(it) }.toDomain()
    preferencesRepository.setSelectedProvider(nextProvider.id)
    return DeleteProviderResult(deleted = true)
  }

  suspend fun rebindProviderBotsAndDelete(providerId: String, targetProviderId: String): DeleteProviderResult {
    require(providerId != targetProviderId) { "请选择另一个 API 配置作为机器人新绑定。" }
    val provider = dao.providerById(providerId) ?: return DeleteProviderResult(deleted = true)
    val targetProvider = dao.providerById(targetProviderId) ?: error("请选择有效的目标 API 配置")
    val blockingBots = dao.aiBotsByProviderId(providerId)
    val now = System.currentTimeMillis()
    blockingBots.forEach { bot ->
      dao.upsertAiBot(
        bot.copy(
          providerId = targetProvider.id,
          model = targetProvider.defaultModel,
          updatedAt = now
        )
      )
    }
    dao.deleteProvider(providerId)
    provider.secretRef?.let { apiKeyStore.delete(it) }
    preferencesRepository.setSelectedProvider(targetProvider.id)
    return DeleteProviderResult(deleted = true)
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
    return createConversation(providerId, model, ConversationType.CHAT)
  }

  suspend fun createImageConversation(providerId: String, model: String): ChatConversation {
    return createConversation(providerId, model, ConversationType.IMAGE)
  }

  private suspend fun createConversation(providerId: String, model: String, type: ConversationType): ChatConversation {
    val now = System.currentTimeMillis()
    val conversation = ConversationEntity(
      id = newId("conv"),
      title = if (type == ConversationType.IMAGE) "新生图" else "新对话",
      providerId = providerId,
      model = model,
      type = type.name,
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
      type = sourceConversation.type,
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

  suspend fun clearConversationGroup(groupName: String) {
    val trimmed = groupName.trim()
    if (trimmed.isBlank()) return
    dao.clearConversationGroup(trimmed, System.currentTimeMillis())
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
        supportsImageGeneration = provider.supportsImageGeneration,
        imageGenerationApiMode = provider.imageGenerationApiMode.name,
        imageGenerationModel = provider.imageGenerationModel.trim(),
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
    if (conversation.type == ConversationType.IMAGE.name) {
      sendImageMessage(conversationId, text, attachments, ImageGenerationOptions())
      return
    }
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

  suspend fun sendImageMessage(
    conversationId: String,
    text: String,
    attachments: List<ChatAttachment> = emptyList(),
    options: ImageGenerationOptions = ImageGenerationOptions()
  ) {
    val cleanText = text.trim()
    val imageAttachments = attachments.filter { it.isImage }
    if (cleanText.isEmpty() && imageAttachments.isEmpty()) return
    val conversation = dao.conversationById(conversationId) ?: return
    val provider = dao.providerById(conversation.providerId)?.toDomain() ?: return
    val now = System.currentTimeMillis()
    val userMessage = MessageEntity(
      id = newId("msg"),
      conversationId = conversationId,
      role = MessageRole.USER.name,
      content = cleanText,
      attachmentsJson = formatAttachments(imageAttachments),
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
      content = "正在生成图片...",
      status = MessageStatus.STREAMING.name,
      providerId = provider.id,
      model = conversation.model,
      createdAt = now + 1_000,
      updatedAt = now + 1_000,
      errorMessage = null
    )
    dao.upsertMessage(userMessage)
    dao.upsertMessage(assistantMessage)
    if (conversation.title == "新生图" || conversation.title == "新对话" || conversation.title == "New chat") {
      val titleSource = cleanText.takeIf { it.isNotBlank() } ?: imageAttachments.firstOrNull()?.displayName ?: "生图对话"
      dao.updateConversationTitle(conversationId, titleSource.take(40), now)
    } else {
      dao.upsertConversation(conversation.copy(updatedAt = now))
    }

    val startedAt = System.currentTimeMillis()
    generateImagesForAssistant(conversation, provider, assistantMessage.id, options, startedAt)
  }

  private suspend fun generateImagesForAssistant(
    conversation: ConversationEntity,
    provider: ChatProviderConfig,
    assistantMessageId: String,
    options: ImageGenerationOptions,
    startedAt: Long
  ) {
    val conversationId = conversation.id
    val adapter = adapters[provider.type]
    val captureRawResponseLog = preferencesRepository.appSettings.first().debugResponseLogging
    val requestOptions = options.copy(captureRawResponseLog = captureRawResponseLog)
    val rawResponseLog = StringBuilder()
    var promptTokens: Int? = null
    var completionTokens: Int? = null
    var totalTokens: Int? = null

    fun appendRawFrame(event: String?, data: String) {
      if (!captureRawResponseLog || rawResponseLog.length >= MaxRawResponseLogChars) return
      val frame = buildString {
        if (!event.isNullOrBlank()) append("event: ").append(event).append('\n')
        append("data: ").append(data).append("\n\n")
      }
      val remaining = MaxRawResponseLogChars - rawResponseLog.length
      rawResponseLog.append(frame.take(remaining))
      if (frame.length > remaining) {
        rawResponseLog.append("\n... raw response log truncated ...")
      }
    }

    suspend fun updateAssistant(
      content: String,
      status: MessageStatus,
      errorMessage: String?,
      generated: List<ChatAttachment>
    ) {
      val updatedAt = System.currentTimeMillis()
      dao.updateMessageWithMetadata(
        id = assistantMessageId,
        content = content,
        status = status.name,
        updatedAt = updatedAt,
        errorMessage = errorMessage,
        totalDurationMs = updatedAt - startedAt,
        firstTokenDurationMs = null,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        rawResponseLog = rawResponseLog.toString().takeIf { captureRawResponseLog && it.isNotBlank() }
      )
      dao.updateMessageAttachments(assistantMessageId, formatAttachments(generated), updatedAt)
      dao.touchConversation(conversationId, updatedAt)
    }

    if (!provider.supportsImageGeneration || adapter == null) {
      updateAssistant("", MessageStatus.FAILED, "当前 GPT 配置不支持图片生成", emptyList())
      return
    }
    val apiKey = apiKeyStore.read(provider.secretRef)
    if (apiKey.isNullOrBlank()) {
      updateAssistant("", MessageStatus.FAILED, "当前 API 配置还没有保存 Key，请在 API 配置中填写后再发送。", emptyList())
      return
    }

    val generated = mutableListOf<ChatAttachment>()
    var revisedPrompt: String? = null
    try {
      val count = options.count.coerceIn(1, 4)
      repeat(count) { index ->
        val history = dao.messagesForConversation(conversationId)
          .filter { it.id != assistantMessageId }
          .map { it.toDomain() }
        adapter.generateImages(
          config = provider.copy(defaultModel = conversation.model),
          apiKey = apiKey,
          messages = history,
          options = requestOptions.copy(count = 1)
        ).collect { event ->
          when (event) {
            ChatStreamEvent.Started -> Unit
            is ChatStreamEvent.ImageGenerated -> {
              revisedPrompt = event.revisedPrompt ?: revisedPrompt
              generated += saveGeneratedImage(event.base64Data, event.mimeType, conversationId, index + 1)
              updateAssistant(
                content = imageGenerationContent(generated.size, count, options, revisedPrompt),
                status = MessageStatus.STREAMING,
                errorMessage = null,
                generated = generated
              )
            }
            is ChatStreamEvent.Failed -> throw IllegalStateException(event.message)
            is ChatStreamEvent.Usage -> {
              promptTokens = event.promptTokens ?: promptTokens
              completionTokens = event.completionTokens ?: completionTokens
              totalTokens = event.totalTokens ?: totalTokens
              if (event.raw != null) appendRawFrame("usage", event.raw)
            }
            is ChatStreamEvent.RawFrame -> appendRawFrame(event.event, event.data)
            else -> Unit
          }
        }
      }
      if (generated.isEmpty()) {
        throw IllegalStateException("OpenAI did not return image data")
      }
      val completedAt = System.currentTimeMillis()
      val completedContent = imageGenerationContent(generated.size, count, options, revisedPrompt)
      dao.updateMessageWithMetadata(
        id = assistantMessageId,
        content = completedContent,
        status = MessageStatus.COMPLETE.name,
        updatedAt = completedAt,
        errorMessage = null,
        totalDurationMs = completedAt - startedAt,
        firstTokenDurationMs = null,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        rawResponseLog = rawResponseLog.toString().takeIf { captureRawResponseLog && it.isNotBlank() }
      )
      dao.updateMessageAttachments(assistantMessageId, formatAttachments(generated), completedAt)
      dao.touchConversation(conversationId, completedAt)
    } catch (error: CancellationException) {
      updateAssistant(imageGenerationContent(generated.size, generated.size.coerceAtLeast(1), options, revisedPrompt), MessageStatus.FAILED, "已停止", generated)
      throw error
    } catch (error: Exception) {
      updateAssistant(imageGenerationContent(generated.size, generated.size.coerceAtLeast(1), options, revisedPrompt), MessageStatus.FAILED, friendlyNetworkErrorMessage(error), generated)
    }
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

  suspend fun groupChatShareText(groupId: String, includeTimestamps: Boolean = true): String {
    val export = groupChatExport(groupId) ?: return ""
    return buildConversationShareText(export, includeTimestamps)
  }

  suspend fun groupChatShareText(
    groupId: String,
    messageIds: Set<String>,
    includeTimestamps: Boolean = true
  ): String {
    if (messageIds.isEmpty()) return groupChatShareText(groupId, includeTimestamps)
    val export = groupChatExport(groupId) ?: return ""
    val selectedMessages = dao.groupMessages(groupId)
      .filter { it.id in messageIds }
      .sortedBy { it.createdAt }
      .map { it.toDomain() }
    return buildConversationShareText(
      export.copy(
        title = "${export.title}（节选）",
        messages = selectedMessages.map { it.toExportMessage() }
      ),
      includeTimestamps
    )
  }

  suspend fun groupChatExport(groupId: String): ConversationExport? {
    val room = dao.groupChatRoomById(groupId) ?: return null
    val messages = dao.groupMessages(groupId).map { it.toDomain() }
    val modelLabel = messages
      .mapNotNull { message ->
        when {
          message.senderType == GroupMessageSenderType.BOT && !message.model.isNullOrBlank() ->
            "${message.senderName} / ${message.model}"
          message.senderType == GroupMessageSenderType.TOOL && !message.model.isNullOrBlank() ->
            "${message.senderName.removeSuffix(" 的工具")} / ${message.model}"
          else -> null
        }
      }
      .distinct()
      .take(4)
      .joinToString("；")
      .ifBlank { null }
    return ConversationExport(
      title = room.title,
      groupName = "AI 群聊",
      modelLabel = modelLabel,
      messages = messages.map { it.toExportMessage() }
    )
  }

  suspend fun groupMessageShareText(groupId: String, messageId: String, includeTimestamps: Boolean = true): String {
    val export = groupChatExport(groupId) ?: return ""
    val message = export.messages.firstOrNull { it.id == messageId } ?: return ""
    return buildConversationShareText(
      export.copy(
        title = "${export.title}（单条群消息）",
        messages = listOf(message)
      ),
      includeTimestamps
    )
  }

  suspend fun groupMessageExport(groupId: String, messageId: String): ConversationExport? {
    val export = groupChatExport(groupId) ?: return null
    val message = export.messages.firstOrNull { it.id == messageId } ?: return null
    return export.copy(
      title = "${export.title}（单条群消息）",
      messages = listOf(message)
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
    if (conversation.type == ConversationType.IMAGE.name) {
      generateImagesForAssistant(conversation, provider, assistantMessage.id, ImageGenerationOptions(), now)
    } else {
      streamAssistant(conversationId, provider, conversation.model, assistantMessage.id)
    }
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
          supportsImageGeneration = provider.supportsImageGeneration,
          imageGenerationApiMode = provider.imageGenerationApiMode.name,
          imageGenerationModel = provider.imageGenerationModel,
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
          supportsImageGeneration = item.supportsImageGeneration && type == ProviderType.OPENAI_RESPONSES,
          imageGenerationApiMode = runCatching { ImageGenerationApiMode.valueOf(item.imageGenerationApiMode) }.getOrDefault(ImageGenerationApiMode.RESPONSES_TOOL),
          imageGenerationModel = item.imageGenerationModel.trim(),
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
          is ChatStreamEvent.ImageGenerated -> Unit
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
    summarize: Boolean,
    turnInfo: GroupTurnInfo
  ): MessageStatus {
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
      return MessageStatus.FAILED
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
      return MessageStatus.FAILED
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
            createdAt = (dao.groupMessages(room.id).firstOrNull { it.id == botMessageId }?.createdAt ?: now + 1_000) - 500 + toolMessageIds.size,
            updatedAt = now,
            errorMessage = null,
            turnTrigger = turnInfo.trigger.name,
            turnRound = turnInfo.round,
            turnIndex = turnInfo.index,
            turnMemberCount = turnInfo.memberCount
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

    var finalStatus = MessageStatus.FAILED
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
          is ChatStreamEvent.ImageGenerated -> Unit
          ChatStreamEvent.Completed -> {
            updateBotMessage(MessageStatus.COMPLETE, null)
            finalStatus = MessageStatus.COMPLETE
            if (summarize && output.isNotBlank()) {
              dao.updateGroupChatSummary(room.id, output.trim(), System.currentTimeMillis())
            }
          }
          is ChatStreamEvent.Failed -> {
            finalStatus = MessageStatus.FAILED
            updateBotMessage(MessageStatus.FAILED, event.message)
          }
        }
      }
    } catch (error: CancellationException) {
      updateBotMessage(MessageStatus.FAILED, "已停止")
      throw error
    } catch (error: Exception) {
      updateBotMessage(MessageStatus.FAILED, friendlyNetworkErrorMessage(error))
      finalStatus = MessageStatus.FAILED
    }
    return finalStatus
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
    val recentMessages = messages.takeLast(GroupRecentMessageLimit)
    if (recentMessages.isEmpty()) {
      result += ChatMessage(
        id = "group-initial-task-${room.id}-${bot.id}",
        conversationId = room.id,
        role = MessageRole.USER,
        content = buildInitialGroupTaskContent(room, bot, summarize),
        status = MessageStatus.COMPLETE,
        providerId = null,
        model = null,
        createdAt = room.createdAt,
        updatedAt = room.updatedAt,
        errorMessage = null
      )
    }
    recentMessages.forEach { message ->
      result += ChatMessage(
        id = message.id,
        conversationId = room.id,
        role = MessageRole.USER,
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

  private fun buildInitialGroupTaskContent(room: GroupChatRoom, bot: AiBot, summarize: Boolean): String {
    return buildString {
      append("请以「${bot.name}」的身份开始这个群聊。")
      append("围绕群主题「${room.topic.ifBlank { room.title }}」给出第一轮发言。")
      if (summarize) {
        append("如果当前还没有讨论内容，请说明暂无可总结内容，并提出建议的讨论起点。")
      } else {
        append("如果需要最新信息，可以先进行网页搜索。")
      }
    }
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
      if (message.status == MessageStatus.FAILED && message.errorMessage == "已停止") {
        append("[已停止] ")
      }
      append(message.content)
      if (!providerSupportsAttachments || message.senderType != GroupMessageSenderType.USER) {
        append(attachmentText)
      }
    }.trim()
  }

  private fun GroupChatMessage.toExportMessage(): ConversationExportMessage {
    val exportRole = when (senderType) {
      GroupMessageSenderType.USER -> MessageRole.USER
      GroupMessageSenderType.BOT -> MessageRole.ASSISTANT
      GroupMessageSenderType.SYSTEM -> MessageRole.SYSTEM
      GroupMessageSenderType.TOOL -> MessageRole.TOOL
    }
    val label = when (senderType) {
      GroupMessageSenderType.USER -> senderName.ifBlank { "我" }
      GroupMessageSenderType.BOT -> senderName.ifBlank { "AI" }
      GroupMessageSenderType.SYSTEM -> "系统"
      GroupMessageSenderType.TOOL -> senderName.ifBlank { "工具" }
    }
    val turnText = formatGroupTurnLabel(this)
    val header = listOfNotNull(
      label,
      model?.takeIf { it.isNotBlank() },
      turnText
    ).joinToString(" · ")
    val body = buildString {
      appendLine("[$header]")
      if (status == MessageStatus.FAILED && errorMessage == "已停止") {
        appendLine("[已停止]")
      }
      append(content.ifBlank { if (status == MessageStatus.STREAMING) "输出中..." else "" })
    }.trim()
    return ConversationExportMessage(
      id = id,
      role = exportRole,
      content = body,
      status = status,
      errorMessage = errorMessage,
      createdAt = createdAt
    )
  }

  private fun formatGroupTurnLabel(message: GroupChatMessage): String? {
    return when (message.turnTrigger) {
      GroupTurnTrigger.AUTO -> {
        val round = message.turnRound ?: return "自动发言"
        val index = message.turnIndex ?: return "自动第 $round 轮"
        val total = message.turnMemberCount
        if (total != null && total > 0) "自动第 $round 轮 第 $index/$total 个发言" else "自动第 $round 轮 第 $index 个发言"
      }
      GroupTurnTrigger.MANUAL -> message.turnIndex?.let { "点名第 $it 次发言" } ?: "点名发言"
      GroupTurnTrigger.SUMMARY -> message.turnIndex?.let { "总结第 $it 次" } ?: "总结发言"
      GroupTurnTrigger.UNKNOWN -> null
    }
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
        supportsImageGeneration = false,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL.name,
        imageGenerationModel = "",
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
        supportsImageGeneration = true,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL.name,
        imageGenerationModel = "",
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
        supportsImageGeneration = false,
        imageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL.name,
        imageGenerationModel = "",
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

  private fun saveGeneratedImage(
    base64Data: String,
    mimeType: String,
    conversationId: String,
    sequence: Int
  ): ChatAttachment {
    generatedImageDir.mkdirs()
    val normalizedMimeType = mimeType.ifBlank { "image/png" }
    val extension = when (normalizedMimeType.substringAfter('/', "png").substringBefore(';').lowercase()) {
      "jpeg", "jpg" -> "jpg"
      "webp" -> "webp"
      else -> "png"
    }
    val id = newId("img")
    val displayName = "generated_${conversationId.takeLast(6)}_${System.currentTimeMillis()}_${sequence}.$extension"
    val target = File(generatedImageDir, "${id}_$displayName")
    target.writeBytes(Base64.getDecoder().decode(base64Data.substringAfter("base64,", base64Data)))
    return ChatAttachment(
      id = id,
      displayName = displayName,
      mimeType = normalizedMimeType,
      sizeBytes = target.length(),
      localPath = target.absolutePath
    )
  }

  private fun imageGenerationContent(
    generatedCount: Int,
    targetCount: Int,
    options: ImageGenerationOptions,
    revisedPrompt: String?
  ): String {
    return buildString {
      if (generatedCount > 0) {
        append("已生成 $generatedCount")
        if (targetCount > generatedCount) append("/$targetCount")
        append(" 张图片")
      } else {
        append("正在生成图片")
      }
      append("。尺寸：${options.size.apiValue}，质量：${options.quality.apiValue}，格式：${options.outputFormat.apiValue}，背景：${options.background.apiValue}")
      revisedPrompt?.takeIf { it.isNotBlank() }?.let {
        append("\n\n修订提示词：")
        append(it)
      }
    }
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
      raw.contains("HTTP 400", ignoreCase = true) || raw.contains("invalid_request_error", ignoreCase = true) ->
        "请求参数不被 Provider 接受。请检查模型名、Base URL、接口模式、附件类型和当前模型是否支持这些参数；开启原始响应日志可查看服务端返回详情。"
      raw.contains("HTTP 401", ignoreCase = true) ||
        raw.contains("invalid_api_key", ignoreCase = true) ||
        raw.contains("incorrect api key", ignoreCase = true) ||
        raw.contains("unauthorized", ignoreCase = true) ->
        "API Key 无效或未被服务端接受。请检查 API 配置中的 Key 是否正确、是否属于当前 Base URL 对应的服务商，以及是否复制了多余空格。"
      raw.contains("HTTP 403", ignoreCase = true) ||
        raw.contains("permission", ignoreCase = true) ||
        raw.contains("forbidden", ignoreCase = true) ->
        "当前 Key 没有访问该模型或功能的权限。请检查服务商后台是否开通了对应模型、生图/搜索/附件能力，或更换有权限的 Key。"
      raw.contains("HTTP 404", ignoreCase = true) ||
        raw.contains("not found", ignoreCase = true) ||
        raw.contains("model_not_found", ignoreCase = true) ->
        "接口地址或模型不存在。请检查 Base URL 是否包含正确前缀（例如 /v1）、接口模式是否匹配服务商能力，以及模型名是否填写正确。"
      raw.contains("HTTP 429", ignoreCase = true) ||
        raw.contains("rate_limit", ignoreCase = true) ||
        raw.contains("too many requests", ignoreCase = true) ->
        "请求过于频繁或额度被限流。请稍后重试，降低并发/频率，或检查服务商的速率限制和套餐额度。"
      raw.contains("insufficient_quota", ignoreCase = true) ||
        raw.contains("quota", ignoreCase = true) ||
        raw.contains("billing", ignoreCase = true) ||
        raw.contains("balance", ignoreCase = true) ->
        "账号额度或余额不足。请检查服务商后台余额、账单状态、套餐配额或中转额度。"
      raw.contains("HTTP 500", ignoreCase = true) ||
        raw.contains("HTTP 502", ignoreCase = true) ||
        raw.contains("HTTP 503", ignoreCase = true) ||
        raw.contains("HTTP 504", ignoreCase = true) ->
        "Provider 或上游服务临时异常。请求已经到达服务端但未成功处理；请稍后重试，如果使用中转 Base URL，也请检查中转服务状态。"
      raw.contains("response.failed", ignoreCase = true) && raw.contains("Upstream request failed", ignoreCase = true) ->
        "上游模型请求失败。若本轮开启了网页搜索，通常是搜索工具或代理服务临时失败；请稍后重试，或暂时关闭网页搜索后再让群聊继续。"
      raw.contains("upstream_error", ignoreCase = true) ||
        raw.contains("Upstream service temporarily unavailable", ignoreCase = true) ->
        "上游服务暂时不可用。请求已到达 Provider，但 OpenAI 或中转上游当前不可用；请稍后重试，如果使用的是代理 Base URL，也请检查代理服务状态。"
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

private data class GroupTurnInfo(
  val trigger: GroupTurnTrigger,
  val round: Int?,
  val index: Int?,
  val memberCount: Int?
)

