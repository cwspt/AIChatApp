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
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderAdapter
import com.personal.aichat.domain.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
    return dao.observeMessages(conversationId).map { items -> items.map { it.toDomain() } }
  }

  suspend fun bootstrapDefaults() {
    if (dao.providerCount() > 0) return
    defaultProviders().forEachIndexed { index, provider ->
      dao.upsertProvider(provider.copy(sortOrder = index))
    }
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
      title = "New chat",
      providerId = providerId,
      model = model,
      createdAt = now,
      updatedAt = now
    )
    dao.upsertConversation(conversation)
    preferencesRepository.setSelectedConversation(conversation.id)
    return conversation.toDomain()
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
        secretRef = secretRef,
        sortOrder = 0
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
    if (conversation.title == "New chat") {
      dao.updateConversationTitle(conversationId, cleanText.take(40), now)
    } else {
      dao.upsertConversation(conversation.copy(updatedAt = now))
    }
    streamAssistant(conversationId, provider, assistantMessage.id)
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
      dao.updateMessage(
        assistantMessageId,
        output,
        MessageStatus.FAILED.name,
        System.currentTimeMillis(),
        error.message ?: "Provider request failed"
      )
    }
  }

  private fun defaultProviders(): List<ProviderEntity> {
    return listOf(
      ProviderEntity(
        id = "tokenhub-proxy",
        displayName = "TokenHub Proxy",
        type = ProviderType.TOKENHUB_PROXY.name,
        baseUrl = "http://127.0.0.1:8787/v1",
        defaultModel = "glm-5.1",
        enabled = true,
        supportsStreaming = true,
        extraHeadersJson = "",
        secretRef = "provider_tokenhub-proxy",
        sortOrder = 0
      ),
      ProviderEntity(
        id = "openai-responses",
        displayName = "OpenAI Responses",
        type = ProviderType.OPENAI_RESPONSES.name,
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4.1-mini",
        enabled = false,
        supportsStreaming = true,
        extraHeadersJson = "",
        secretRef = "provider_openai-responses",
        sortOrder = 1
      ),
      ProviderEntity(
        id = "openai-compatible",
        displayName = "OpenAI Compatible",
        type = ProviderType.OPENAI_COMPATIBLE_CHAT.name,
        baseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        enabled = false,
        supportsStreaming = true,
        extraHeadersJson = "",
        secretRef = "provider_openai-compatible",
        sortOrder = 2
      )
    )
  }

  private fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
}
