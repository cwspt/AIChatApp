package com.personal.aichat

import com.personal.aichat.data.ChatRepository
import com.personal.aichat.data.ChatSelectionStore
import com.personal.aichat.data.local.ChatDao
import com.personal.aichat.data.local.ConversationEntity
import com.personal.aichat.data.local.MessageEntity
import com.personal.aichat.data.local.ProviderEntity
import com.personal.aichat.data.security.ApiKeyStore
import com.personal.aichat.domain.AppSettings
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderAdapter
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import com.personal.aichat.domain.WebSearchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRepositoryForkTest {
  @Test
  fun forkFromUserMessageCopiesContextAndAutoGeneratesWithTargetModel() = runTest {
    val dao = FakeChatDao()
    val selection = FakeSelectionStore()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = selection,
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("source", "source-model"))
    dao.upsertProvider(provider("target", "target-model"))
    dao.upsertConversation(conversation("source-conv", providerId = "source", model = "source-model"))
    val firstUser = message("u1", "source-conv", MessageRole.USER, "hello", "source", "source-model", 1)
    val firstAssistant = message("a1", "source-conv", MessageRole.ASSISTANT, "hi", "source", "source-model", 2)
    val secondUser = message("u2", "source-conv", MessageRole.USER, "compare", "source", "source-model", 3)
    dao.upsertMessage(firstUser)
    dao.upsertMessage(firstAssistant)
    dao.upsertMessage(secondUser)

    val forked = repository.forkConversationAtMessage("source-conv", "u2", "target")

    assertNotNull(forked)
    assertEquals("target", forked?.providerId)
    assertEquals("target-model", forked?.model)
    assertEquals("source-conv", forked?.forkedFromConversationId)
    assertEquals("u2", forked?.forkedFromMessageId)
    assertEquals(forked?.id, selection.selectedConversationId.value)
    assertEquals("target", selection.selectedProviderId.value)

    val forkMessages = dao.messagesForConversation(forked!!.id)
    assertEquals(4, forkMessages.size)
    assertEquals(listOf("hello", "hi", "compare", "fake response"), forkMessages.map { it.content })
    assertEquals(listOf(MessageRole.USER.name, MessageRole.ASSISTANT.name, MessageRole.USER.name, MessageRole.ASSISTANT.name), forkMessages.map { it.role })
    assertEquals("target", forkMessages.last().providerId)
    assertEquals("target-model", forkMessages.last().model)
    assertNotEquals("u1", forkMessages[0].id)
    assertNotEquals("a1", forkMessages[1].id)
    assertNotEquals("u2", forkMessages[2].id)
    assertEquals("target-model", adapter.lastOptions?.model)
    assertEquals(listOf("hello", "hi", "compare"), adapter.lastMessages.map { it.content })
  }

  @Test
  fun forkFromAssistantMessageCopiesThroughAssistantWithoutAutoGenerating() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("source", "source-model"))
    dao.upsertProvider(provider("target", "target-model"))
    dao.upsertConversation(conversation("source-conv", providerId = "source", model = "source-model"))
    dao.upsertMessage(message("u1", "source-conv", MessageRole.USER, "hello", "source", "source-model", 1))
    dao.upsertMessage(message("a1", "source-conv", MessageRole.ASSISTANT, "hi", "source", "source-model", 2))
    dao.upsertMessage(message("u2", "source-conv", MessageRole.USER, "later", "source", "source-model", 3))

    val forked = repository.forkConversationAtMessage("source-conv", "a1", "target")

    assertNotNull(forked)
    val forkMessages = dao.messagesForConversation(forked!!.id)
    assertEquals(2, forkMessages.size)
    assertEquals(listOf("hello", "hi"), forkMessages.map { it.content })
    assertNull(adapter.lastOptions)
  }

  @Test
  fun sendMessageUsesConversationModelInsteadOfProviderDefaultModel() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "conversation-fixed"))

    repository.sendMessage("conv", "hello")

    assertEquals("conversation-fixed", adapter.lastOptions?.model)
    val messages = dao.messagesForConversation("conv")
    assertEquals("conversation-fixed", messages[0].model)
    assertEquals("conversation-fixed", messages[1].model)
  }

  private fun provider(id: String, model: String): ProviderEntity = ProviderEntity(
    id = id,
    displayName = id,
    type = ProviderType.TOKENHUB_PROXY.name,
    baseUrl = "http://127.0.0.1:8787/v1",
    defaultModel = model,
    enabled = true,
    supportsStreaming = true,
    extraHeadersJson = "",
    reasoningEffort = ReasoningEffort.AUTO.name,
    secretRef = "provider_$id",
    sortOrder = 0
  )

  private fun conversation(id: String, providerId: String, model: String): ConversationEntity = ConversationEntity(
    id = id,
    title = "Source",
    providerId = providerId,
    model = model,
    groupName = "Group",
    createdAt = 1,
    updatedAt = 1
  )

  private fun message(
    id: String,
    conversationId: String,
    role: MessageRole,
    content: String,
    providerId: String,
    model: String,
    createdAt: Long
  ): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    status = MessageStatus.COMPLETE.name,
    providerId = providerId,
    model = model,
    createdAt = createdAt,
    updatedAt = createdAt,
    errorMessage = null
  )
}

private class RecordingAdapter : ProviderAdapter {
  var lastOptions: ChatCompletionOptions? = null
  var lastMessages: List<ChatMessage> = emptyList()

  override fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent> = flow {
    lastMessages = messages
    lastOptions = options
    emit(ChatStreamEvent.Started)
    emit(ChatStreamEvent.TextDelta("fake response"))
    emit(ChatStreamEvent.Completed)
  }
}

private class FakeSelectionStore : ChatSelectionStore {
  override val selectedProviderId = MutableStateFlow<String?>(null)
  override val selectedConversationId = MutableStateFlow<String?>(null)
  override val appSettings = MutableStateFlow(AppSettings())

  override suspend fun setSelectedProvider(id: String) {
    selectedProviderId.value = id
  }

  override suspend fun setSelectedConversation(id: String) {
    selectedConversationId.value = id
  }

  override suspend fun setThemePalette(palette: AppThemePalette) {
    appSettings.value = appSettings.value.copy(palette = palette)
  }

  override suspend fun setThemeMode(mode: AppThemeMode) {
    appSettings.value = appSettings.value.copy(themeMode = mode)
  }

  override suspend fun setFontScale(scale: Float) {
    appSettings.value = appSettings.value.copy(fontScale = scale)
  }

  override suspend fun setDebugResponseLogging(enabled: Boolean) {
    appSettings.value = appSettings.value.copy(debugResponseLogging = enabled)
  }

  override suspend fun setWebSearchMode(mode: WebSearchMode) {
    appSettings.value = appSettings.value.copy(webSearchMode = mode)
  }
}

private class FakeApiKeyStore : ApiKeyStore {
  override fun read(secretRef: String?): String? = "test-key"
  override fun exists(secretRef: String?): Boolean = true
  override fun write(secretRef: String, apiKey: String) = Unit
  override fun delete(secretRef: String) = Unit
}

private class FakeChatDao : ChatDao {
  private val providers = linkedMapOf<String, ProviderEntity>()
  private val conversations = linkedMapOf<String, ConversationEntity>()
  private val messages = linkedMapOf<String, MessageEntity>()

  override fun observeProviders(): Flow<List<ProviderEntity>> = flowOf(providers.values.toList())

  override suspend fun providerById(id: String): ProviderEntity? = providers[id]

  override suspend fun upsertProvider(provider: ProviderEntity) {
    providers[provider.id] = provider
  }

  override suspend fun setProviderEnabled(id: String, enabled: Boolean) {
    providers[id]?.let { providers[id] = it.copy(enabled = enabled) }
  }

  override suspend fun providerCount(): Int = providers.size

  override fun observeConversations(): Flow<List<ConversationEntity>> = flowOf(
    conversations.values.filter { !it.isDeleted && !it.isArchived }.sortedByDescending { it.updatedAt }
  )

  override fun observeArchivedConversations(): Flow<List<ConversationEntity>> = flowOf(
    conversations.values.filter { !it.isDeleted && it.isArchived }.sortedByDescending { it.updatedAt }
  )

  override suspend fun latestConversation(): ConversationEntity? =
    conversations.values.filter { !it.isDeleted && !it.isArchived }.maxByOrNull { it.updatedAt }

  override suspend fun conversationById(id: String): ConversationEntity? = conversations[id]

  override suspend fun upsertConversation(conversation: ConversationEntity) {
    conversations[conversation.id] = conversation
  }

  override suspend fun updateConversationTitle(id: String, title: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(title = title, updatedAt = updatedAt) }
  }

  override suspend fun updateConversationProvider(id: String, providerId: String, model: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(providerId = providerId, model = model, updatedAt = updatedAt) }
  }

  override suspend fun updateConversationMeta(id: String, title: String, groupName: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(title = title, groupName = groupName, updatedAt = updatedAt) }
  }

  override suspend fun renameConversationGroup(oldGroupName: String, newGroupName: String, updatedAt: Long) {
    conversations.replaceAll { _, conversation ->
      if (conversation.groupName == oldGroupName && !conversation.isDeleted) {
        conversation.copy(groupName = newGroupName, updatedAt = updatedAt)
      } else {
        conversation
      }
    }
  }

  override suspend fun touchConversation(id: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(updatedAt = updatedAt) }
  }

  override suspend fun setConversationPinned(id: String, isPinned: Boolean, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(isPinned = isPinned, updatedAt = updatedAt) }
  }

  override suspend fun archiveConversation(id: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(isArchived = true, updatedAt = updatedAt) }
  }

  override suspend fun restoreConversation(id: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(isArchived = false, updatedAt = updatedAt) }
  }

  override suspend fun deleteConversation(id: String, updatedAt: Long) {
    conversations[id]?.let { conversations[id] = it.copy(isDeleted = true, updatedAt = updatedAt) }
  }

  override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = flowOf(
    messagesForConversationInternal(conversationId)
  )

  override suspend fun messagesForConversation(conversationId: String): List<MessageEntity> =
    messagesForConversationInternal(conversationId)

  override suspend fun upsertMessage(message: MessageEntity) {
    messages[message.id] = message
  }

  override suspend fun updateMessage(id: String, content: String, status: String, updatedAt: Long, errorMessage: String?) {
    messages[id]?.let {
      messages[id] = it.copy(content = content, status = status, updatedAt = updatedAt, errorMessage = errorMessage)
    }
  }

  override suspend fun updateMessageWithMetadata(
    id: String,
    content: String,
    status: String,
    updatedAt: Long,
    errorMessage: String?,
    totalDurationMs: Long?,
    firstTokenDurationMs: Long?,
    promptTokens: Int?,
    completionTokens: Int?,
    totalTokens: Int?,
    rawResponseLog: String?
  ) {
    messages[id]?.let {
      messages[id] = it.copy(
        content = content,
        status = status,
        updatedAt = updatedAt,
        errorMessage = errorMessage,
        totalDurationMs = totalDurationMs,
        firstTokenDurationMs = firstTokenDurationMs,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        rawResponseLog = rawResponseLog
      )
    }
  }

  override suspend fun lastUserMessage(conversationId: String): MessageEntity? =
    messagesForConversationInternal(conversationId).lastOrNull { it.role == MessageRole.USER.name }

  private fun messagesForConversationInternal(conversationId: String): List<MessageEntity> =
    messages.values.filter { it.conversationId == conversationId }.sortedBy { it.createdAt }
}
