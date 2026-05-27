package com.personal.aichat.data

import com.personal.aichat.data.local.ChatDao
import com.personal.aichat.data.local.ConversationEntity
import com.personal.aichat.data.local.MessageEntity
import com.personal.aichat.data.local.ProviderEntity
import com.personal.aichat.data.local.toDomain
import com.personal.aichat.data.remote.OpenAiCompatibleChatAdapter
import com.personal.aichat.data.remote.OpenAiResponsesAdapter
import com.personal.aichat.data.remote.TokenHubProxyAdapter
import com.personal.aichat.data.security.ApiKeyStore
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
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
import javax.net.ssl.SSLHandshakeException
import java.util.UUID

class ChatRepository(
  private val dao: ChatDao,
  private val preferencesRepository: ChatPreferencesRepository,
  private val apiKeyStore: ApiKeyStore
) {
  private val adapters: Map<ProviderType, ProviderAdapter> = mapOf(
    ProviderType.OPENAI_RESPONSES to OpenAiResponsesAdapter(),
    ProviderType.OPENAI_COMPATIBLE_CHAT to OpenAiCompatibleChatAdapter(),
    ProviderType.TOKENHUB_PROXY to TokenHubProxyAdapter()
  )

  val providers: Flow<List<ChatProviderConfig>> = dao.observeProviders().map { items ->
    items.map { it.toDomain() }
  }

  val conversations: Flow<List<ChatConversation>> = dao.observeConversations().map { items ->
    items.map { it.toDomain() }
  }

  val archivedConversations: Flow<List<ChatConversation>> = dao.observeArchivedConversations().map { items ->
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

  suspend fun switchConversationProvider(conversationId: String?, providerId: String) {
    val provider = dao.providerById(providerId)?.toDomain() ?: return
    preferencesRepository.setSelectedProvider(providerId)
    if (conversationId.isNullOrBlank()) return
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
        extraHeadersJson = provider.extraHeadersJson,
        reasoningEffort = provider.reasoningEffort.name,
        secretRef = secretRef,
        sortOrder = System.currentTimeMillis().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
      )
    )
  }

  suspend fun sendMessage(conversationId: String, text: String) {
    val cleanText = text.trim()
    if (cleanText.isEmpty()) return
    val conversation = dao.conversationById(conversationId) ?: return
    val provider = dao.providerById(conversation.providerId)?.toDomain() ?: return
    val now = System.currentTimeMillis()
    val userMessage = MessageEntity(
      id = newId("msg"),
      conversationId = conversationId,
      role = MessageRole.USER.name,
      content = cleanText,
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
      createdAt = now + 1,
      updatedAt = now + 1,
      errorMessage = null
    )
    dao.upsertMessage(userMessage)
    dao.upsertMessage(assistantMessage)
    if (conversation.title == "新对话" || conversation.title == "New chat") {
      dao.updateConversationTitle(conversationId, cleanText.take(40), now)
    } else {
      dao.upsertConversation(conversation.copy(updatedAt = now))
    }
    streamAssistant(conversationId, provider, assistantMessage.id)
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

  suspend fun retryLast(conversationId: String) {
    val conversation = dao.conversationById(conversationId) ?: return
    val provider = dao.providerById(conversation.providerId)?.toDomain() ?: return
    val lastUser = dao.lastUserMessage(conversationId) ?: return
    val now = System.currentTimeMillis()
    val assistantMessage = MessageEntity(
      id = newId("msg"),
      conversationId = conversationId,
      role = MessageRole.ASSISTANT.name,
      content = "",
      status = MessageStatus.STREAMING.name,
      providerId = provider.id,
      model = lastUser.model ?: conversation.model,
      createdAt = now,
      updatedAt = now,
      errorMessage = null
    )
    dao.upsertMessage(assistantMessage)
    streamAssistant(conversationId, provider, assistantMessage.id)
  }

  private suspend fun streamAssistant(
    conversationId: String,
    provider: ChatProviderConfig,
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
    val messages = dao.messagesForConversation(conversationId)
      .filter { it.id != assistantMessageId }
      .map { it.toDomain() }
    val apiKey = apiKeyStore.read(provider.secretRef)
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
    try {
      adapter.streamChat(
        config = provider,
        apiKey = apiKey,
        messages = messages,
        options = ChatCompletionOptions(model = provider.defaultModel, stream = provider.supportsStreaming)
      ).collect { event ->
        when (event) {
          ChatStreamEvent.Started -> Unit
          is ChatStreamEvent.TextDelta -> {
            output += event.text
            dao.updateMessage(
              assistantMessageId,
              output,
              MessageStatus.STREAMING.name,
              System.currentTimeMillis(),
              null
            )
          }
          ChatStreamEvent.Completed -> {
            dao.updateMessage(
              assistantMessageId,
              output,
              MessageStatus.COMPLETE.name,
              System.currentTimeMillis(),
              null
            )
          }
          is ChatStreamEvent.Failed -> {
            dao.updateMessage(
              assistantMessageId,
              output,
              MessageStatus.FAILED.name,
              System.currentTimeMillis(),
              event.message
            )
          }
        }
      }
    } catch (error: Exception) {
      val friendlyMessage = friendlyNetworkErrorMessage(error)
      dao.updateMessage(
        assistantMessageId,
        output,
        MessageStatus.FAILED.name,
        System.currentTimeMillis(),
        friendlyMessage
      )
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
        extraHeadersJson = "",
        reasoningEffort = ReasoningEffort.AUTO.name,
        secretRef = "provider_openai-compatible",
        sortOrder = 2
      )
    )
  }

  private fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"

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
}
