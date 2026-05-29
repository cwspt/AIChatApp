package com.personal.aichat

import com.personal.aichat.data.ChatRepository
import com.personal.aichat.data.ChatSelectionStore
import com.personal.aichat.data.local.AiBotEntity
import com.personal.aichat.data.local.ChatDao
import com.personal.aichat.data.local.ConversationEntity
import com.personal.aichat.data.local.FavoriteSnippetEntity
import com.personal.aichat.data.local.GroupChatMemberEntity
import com.personal.aichat.data.local.GroupChatRoomEntity
import com.personal.aichat.data.local.GroupMessageEntity
import com.personal.aichat.data.local.MessageEntity
import com.personal.aichat.data.local.ProviderEntity
import com.personal.aichat.data.local.toDomain
import com.personal.aichat.data.security.ApiKeyStore
import com.personal.aichat.domain.AppSettings
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.ChatBackgroundPreset
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.GroupTurnTrigger
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderAdapter
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import com.personal.aichat.domain.WebSearchMode
import com.personal.aichat.ui.collapsedGroupMessageSummary
import com.personal.aichat.ui.ChatMessageListItem
import com.personal.aichat.ui.GroupMessageListItem
import com.personal.aichat.ui.LongBubbleNavTarget
import com.personal.aichat.ui.ToolCallCitation
import com.personal.aichat.ui.VisibleListItemBounds
import com.personal.aichat.ui.chatMessageListItems
import com.personal.aichat.ui.groupMessageListItems
import com.personal.aichat.ui.longBubbleNavTarget
import com.personal.aichat.ui.nextGroupAutoPlayBotId
import com.personal.aichat.ui.parseToolCallDetails
import com.personal.aichat.ui.resolvedBotBubbleColorKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

  @Test
  fun deleteProviderDeletesProviderAndApiKeyWhenNoBotsDependOnIt() = runTest {
    val dao = FakeChatDao()
    val keyStore = FakeApiKeyStore()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = keyStore,
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("remove-me", "old-model").copy(secretRef = "provider_remove-me"))
    dao.upsertProvider(provider("fallback", "fallback-model"))

    val result = repository.deleteProvider("remove-me")

    assertEquals(true, result.deleted)
    assertNull(dao.providerById("remove-me"))
    assertEquals(listOf("provider_remove-me"), keyStore.deletedSecretRefs)
  }

  @Test
  fun deleteProviderIsBlockedWhenAiBotsDependOnIt() = runTest {
    val dao = FakeChatDao()
    val keyStore = FakeApiKeyStore()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = keyStore,
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a").copy(secretRef = "provider_provider"))
    val bot = repository.createAiBot("Reviewer", "provider", "bot-model", "")

    val result = repository.deleteProvider("provider")

    assertEquals(false, result.deleted)
    assertEquals(listOf(bot.id), result.blockingBots.map { it.id })
    assertNotNull(dao.providerById("provider"))
    assertEquals(emptyList<String>(), keyStore.deletedSecretRefs)
  }

  @Test
  fun rebindProviderBotsAndDeleteMovesBotsToTargetProviderAndDeletesSource() = runTest {
    val dao = FakeChatDao()
    val keyStore = FakeApiKeyStore()
    val selection = FakeSelectionStore()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = selection,
      apiKeyStore = keyStore,
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("source", "source-model").copy(secretRef = "provider_source"))
    dao.upsertProvider(provider("target", "target-default"))
    val first = repository.createAiBot("Reviewer", "source", "custom-a", "")
    val second = repository.createAiBot("Writer", "source", "custom-b", "")

    val result = repository.rebindProviderBotsAndDelete("source", "target")

    assertEquals(true, result.deleted)
    assertNull(dao.providerById("source"))
    assertEquals(listOf("provider_source"), keyStore.deletedSecretRefs)
    assertEquals("target", selection.selectedProviderId.value)
    assertEquals("target", dao.aiBotById(first.id)?.providerId)
    assertEquals("target-default", dao.aiBotById(first.id)?.model)
    assertEquals("target", dao.aiBotById(second.id)?.providerId)
    assertEquals("target-default", dao.aiBotById(second.id)?.model)
  }

  @Test
  fun createAndUpdateAiBotPersistBubbleColorKey() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "provider-default"))

    val created = repository.createAiBot("Reviewer", "provider", "bot-model", "", "ROSE")
    val updated = repository.updateAiBot(created.id, "Reviewer", "provider", "bot-model", "", "CYAN")

    assertEquals("ROSE", created.bubbleColorKey)
    assertEquals("CYAN", updated?.bubbleColorKey)
    assertEquals("CYAN", dao.aiBotById(created.id)?.bubbleColorKey)
  }

  @Test
  fun createFavoriteSnippetStoresSnapshotSourceAndNormalizedTags() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "model-a"))
    dao.upsertMessage(message("u1", "conv", MessageRole.USER, "question", "provider", "model-a", 1))
    dao.upsertMessage(message("a1", "conv", MessageRole.ASSISTANT, "valuable answer", "provider", "model-a", 2))

    val favorite = repository.createFavoriteSnippet(
      conversationId = "conv",
      messageIds = setOf("a1", "u1"),
      title = "",
      description = "useful result",
      tagsInput = "#Work， kotlin,Work"
    )

    assertEquals("valuable answer", favorite.title)
    assertEquals(listOf("Work", "kotlin"), favorite.tags)
    assertEquals("conv", favorite.sourceConversationId)
    assertEquals("u1", favorite.sourceFirstMessageId)
    assertEquals("a1", favorite.sourceLastMessageId)
    assertEquals(2, favorite.messageCount)
    assertEquals(listOf("question", "valuable answer"), favorite.messages.map { it.content })
    assertTrue(favorite.searchText.contains("useful result"))
    assertTrue(favorite.searchText.contains("valuable answer"))
  }

  @Test
  fun favoriteSnippetExportKeepsSnapshotAfterSourceChanges() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "model-a"))
    dao.upsertMessage(message("a1", "conv", MessageRole.ASSISTANT, "original answer", "provider", "model-a", 1))

    val favorite = repository.createFavoriteSnippet("conv", setOf("a1"), "Saved", "", "")
    dao.upsertMessage(message("a1", "conv", MessageRole.ASSISTANT, "changed answer", "provider", "model-a", 1))

    val shareText = repository.favoriteSnippetShareText(favorite.id)

    assertTrue(shareText.contains("original answer"))
    assertTrue(!shareText.contains("changed answer"))
  }

  @Test
  fun createFavoriteSnippetRejectsStreamingMessages() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "model-a"))
    dao.upsertMessage(
      message("a1", "conv", MessageRole.ASSISTANT, "partial", "provider", "model-a", 1)
        .copy(status = MessageStatus.STREAMING.name)
    )

    val result = runCatching {
      repository.createFavoriteSnippet("conv", setOf("a1"), "Saved", "", "")
    }

    assertTrue(result.isFailure)
    assertEquals(0, dao.observeFavoriteSnippets().first().size)
  }

  @Test
  fun favoriteTagManagementRenamesMergesAndDeletesTags() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "model-a"))
    dao.upsertMessage(message("a1", "conv", MessageRole.ASSISTANT, "answer one", "provider", "model-a", 1))
    dao.upsertMessage(message("a2", "conv", MessageRole.ASSISTANT, "answer two", "provider", "model-a", 2))
    val first = repository.createFavoriteSnippet("conv", setOf("a1"), "Saved one", "", "Work kotlin")
    val second = repository.createFavoriteSnippet("conv", setOf("a2"), "Saved two", "", "Archive")

    val renamed = repository.renameFavoriteTag("work", "Archive")
    val firstAfterRename = repository.favoriteSnippetById(first.id)!!
    val secondAfterRename = repository.favoriteSnippetById(second.id)!!

    assertEquals(1, renamed)
    assertEquals(listOf("Archive", "kotlin"), firstAfterRename.tags)
    assertEquals(listOf("Archive"), secondAfterRename.tags)
    assertTrue(firstAfterRename.searchText.contains("archive"))
    assertTrue(!firstAfterRename.searchText.contains("work"))

    val deleted = repository.deleteFavoriteTag("archive")

    assertEquals(2, deleted)
    assertEquals(listOf("kotlin"), repository.favoriteSnippetById(first.id)!!.tags)
    assertEquals(emptyList<String>(), repository.favoriteSnippetById(second.id)!!.tags)
  }

  @Test
  fun appendMessagesToFavoriteMergesSortsAndDeduplicatesSnapshots() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "model-a"))
    dao.upsertMessage(message("u1", "conv", MessageRole.USER, "question", "provider", "model-a", 1))
    dao.upsertMessage(message("a1", "conv", MessageRole.ASSISTANT, "answer", "provider", "model-a", 2))
    dao.upsertMessage(message("u2", "conv", MessageRole.USER, "follow up", "provider", "model-a", 3))
    val favorite = repository.createFavoriteSnippet("conv", setOf("a1"), "Saved", "desc", "tag")

    val updated = repository.appendMessagesToFavoriteSnippet(favorite.id, "conv", setOf("u2", "u1", "a1"))

    assertNotNull(updated)
    assertEquals(listOf("u1", "a1", "u2"), updated!!.messages.map { it.id })
    assertEquals(3, updated.messageCount)
    assertEquals("u1", updated.sourceFirstMessageId)
    assertEquals("u2", updated.sourceLastMessageId)
    assertTrue(updated.searchText.contains("follow up"))
  }

  @Test
  fun appendMessagesToFavoriteRejectsDifferentSourceConversation() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv-a", providerId = "provider", model = "model-a"))
    dao.upsertConversation(conversation("conv-b", providerId = "provider", model = "model-a"))
    dao.upsertMessage(message("a1", "conv-a", MessageRole.ASSISTANT, "answer", "provider", "model-a", 1))
    dao.upsertMessage(message("b1", "conv-b", MessageRole.USER, "other", "provider", "model-a", 1))
    val favorite = repository.createFavoriteSnippet("conv-a", setOf("a1"), "Saved", "", "")

    val result = runCatching {
      repository.appendMessagesToFavoriteSnippet(favorite.id, "conv-b", setOf("b1"))
    }

    assertTrue(result.isFailure)
    assertEquals(listOf("a1"), repository.favoriteSnippetById(favorite.id)!!.messages.map { it.id })
  }

  @Test
  fun removeMessagesFromFavoriteUpdatesSnapshotButDoesNotAllowEmptyFavorite() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "model-a"))
    dao.upsertConversation(conversation("conv", providerId = "provider", model = "model-a"))
    dao.upsertMessage(message("u1", "conv", MessageRole.USER, "question", "provider", "model-a", 1))
    dao.upsertMessage(message("a1", "conv", MessageRole.ASSISTANT, "answer", "provider", "model-a", 2))
    val favorite = repository.createFavoriteSnippet("conv", setOf("u1", "a1"), "Saved", "", "")

    val updated = repository.removeMessagesFromFavoriteSnippet(favorite.id, setOf("u1"))
    val emptyResult = runCatching {
      repository.removeMessagesFromFavoriteSnippet(favorite.id, setOf("a1"))
    }

    assertNotNull(updated)
    assertEquals(listOf("a1"), updated!!.messages.map { it.id })
    assertEquals(1, updated.messageCount)
    assertEquals("a1", updated.sourceFirstMessageId)
    assertEquals("a1", updated.sourceLastMessageId)
    assertTrue(emptyResult.isFailure)
    assertEquals(listOf("a1"), repository.favoriteSnippetById(favorite.id)!!.messages.map { it.id })
  }

  @Test
  fun createGroupChatStoresBotsAndUserMessageDoesNotAutoTriggerAi() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("GPT 研究员", "provider", "bot-fixed-model", "优先给出事实依据")

    val group = repository.createGroupChat("选型讨论", "比较两个方案", listOf(bot.id))
    repository.sendGroupUserMessage(group.id, "先看方案 A", emptyList())

    assertEquals("选型讨论", group.title)
    assertEquals(listOf(bot.id), dao.groupChatMembers(group.id).map { it.botId })
    assertEquals(listOf("先看方案 A"), dao.groupMessages(group.id).map { it.content })
    assertNull(adapter.lastOptions)
  }

  @Test
  fun updateGroupChatUpdatesMetaMembersAndKeepsRemovedMemberHistory() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val first = repository.createAiBot("first", "provider", "model-a", "")
    val second = repository.createAiBot("second", "provider", "model-b", "")
    val third = repository.createAiBot("third", "provider", "model-c", "")
    val group = repository.createGroupChat("old", "old topic", listOf(first.id, second.id))
    repository.sendGroupUserMessage(group.id, "history", emptyList())

    repository.updateGroupChat(group.id, "new", "new topic", listOf(second.id, third.id))

    val room = dao.groupChatRoomById(group.id)!!
    assertEquals("new", room.title)
    assertEquals("new topic", room.topic)
    assertEquals(listOf(second.id, third.id), dao.groupChatMembers(group.id).map { it.botId })
    assertEquals(false, dao.allGroupChatMembers(group.id).first { it.botId == first.id }.enabled)
    assertEquals(listOf("history"), dao.groupMessages(group.id).map { it.content })
  }

  @Test
  fun deleteGroupChatSoftDeletesRoom() = runTest {
    val dao = FakeChatDao()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = emptyMap()
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("bot", "provider", "model", "")
    val group = repository.createGroupChat("group", "topic", listOf(bot.id))

    repository.deleteGroupChat(group.id)

    assertEquals(true, dao.groupChatRoomById(group.id)?.isDeleted)
    assertTrue(dao.observeGroupChatRooms().first().isEmpty())
  }

  @Test
  fun groupBotTurnUsesBotModelAndBuildsGroupContext() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("DeepSeek 审稿人", "provider", "deepseek-fixed", "专注审阅风险")
    val group = repository.createGroupChat("架构评审", "评审离线缓存方案", listOf(bot.id))
    repository.sendGroupUserMessage(group.id, "请先提出风险点", emptyList())

    repository.sendGroupBotTurn(group.id, bot.id)

    assertEquals("deepseek-fixed", adapter.lastOptions?.model)
    assertTrue(adapter.lastMessages.first().content.contains("多 AI 回合制群聊"))
    assertTrue(adapter.lastMessages.first().content.contains("DeepSeek 审稿人"))
    assertTrue(adapter.lastMessages.first().content.contains("评审离线缓存方案"))
    assertTrue(adapter.lastMessages.any { it.content.contains("[用户] 请先提出风险点") })
    assertEquals(listOf("请先提出风险点", "fake response"), dao.groupMessages(group.id).map { it.content })
    val botMessage = dao.groupMessages(group.id).last()
    assertEquals(GroupTurnTrigger.MANUAL.name, botMessage.turnTrigger)
    assertEquals(1, botMessage.turnIndex)
  }

  @Test
  fun groupBotTurnStoresAutoRoundAndSummaryTurnLabels() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val first = repository.createAiBot("GPT", "provider", "gpt-fixed", "")
    val second = repository.createAiBot("Deep", "provider", "deep-fixed", "")
    val group = repository.createGroupChat("评审", "轮流讨论", listOf(first.id, second.id))

    repository.sendGroupBotTurn(group.id, first.id, trigger = GroupTurnTrigger.AUTO)
    repository.sendGroupBotTurn(group.id, second.id, trigger = GroupTurnTrigger.AUTO)
    repository.sendGroupBotTurn(group.id, first.id, trigger = GroupTurnTrigger.AUTO)
    repository.sendGroupBotTurn(group.id, first.id, summarize = true)

    val botMessages = dao.groupMessages(group.id).filter { it.senderType == GroupMessageSenderType.BOT.name }
    assertEquals(listOf(1, 1, 2, null), botMessages.map { it.turnRound })
    assertEquals(listOf(1, 2, 1, 1), botMessages.map { it.turnIndex })
    assertEquals(listOf(2, 2, 2, null), botMessages.map { it.turnMemberCount })
    assertEquals(GroupTurnTrigger.SUMMARY.name, botMessages.last().turnTrigger)
  }

  @Test
  fun groupToolMessagesInheritTurnInfoAndAreGroupedForUi() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter(
      events = listOf(
        ChatStreamEvent.Started,
        ChatStreamEvent.ToolCall(id = "search-1", name = "web_search", input = "苏州天气", output = "https://example.com/weather"),
        ChatStreamEvent.ToolCall(id = "search-2", name = "web_search", input = "苏州景点", output = "https://example.com/travel"),
        ChatStreamEvent.TextDelta("fake response"),
        ChatStreamEvent.Completed
      )
    )
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("GPT", "provider", "gpt-fixed", "")
    val group = repository.createGroupChat("出游计划", "查资料", listOf(bot.id))

    repository.sendGroupBotTurn(group.id, bot.id, trigger = GroupTurnTrigger.AUTO)

    val messages = dao.groupMessages(group.id).map { it.toDomain() }
    val toolMessages = messages.filter { it.senderType == GroupMessageSenderType.TOOL }
    assertEquals(2, toolMessages.size)
    assertTrue(toolMessages.all { it.turnTrigger == GroupTurnTrigger.AUTO })
    assertTrue(toolMessages.all { it.turnRound == 1 && it.turnIndex == 1 && it.turnMemberCount == 1 })
    val items = groupMessageListItems(messages)
    assertEquals(2, items.size)
    assertTrue(items.first() is GroupMessageListItem.ToolGroup)
    val export = repository.groupChatExport(group.id)!!
    assertEquals(listOf(MessageRole.TOOL, MessageRole.TOOL, MessageRole.ASSISTANT), export.messages.map { it.role })
    assertTrue(repository.groupChatShareText(group.id).contains("工具"))
    assertTrue(repository.groupChatShareText(group.id, setOf(toolMessages.first().id)).contains("苏州天气"))
  }

  @Test
  fun groupToolListItemAppearsBeforeBotMessageForSameTurnEvenIfInsertedLater() {
    val bot = testGroupMessage("bot", GroupMessageSenderType.BOT, "bot-a", "A").copy(
      turnTrigger = GroupTurnTrigger.AUTO,
      turnRound = 1,
      turnIndex = 1,
      turnMemberCount = 1,
      createdAt = 1000
    )
    val tool = testGroupMessage("tool", GroupMessageSenderType.TOOL, "bot-a", "A").copy(
      role = MessageRole.TOOL,
      turnTrigger = GroupTurnTrigger.AUTO,
      turnRound = 1,
      turnIndex = 1,
      turnMemberCount = 1,
      createdAt = 1500
    )

    val items = groupMessageListItems(listOf(bot, tool))

    assertTrue(items[0] is GroupMessageListItem.ToolGroup)
    assertTrue(items[1] is GroupMessageListItem.Message)
  }

  @Test
  fun chatToolMessagesAreGroupedBeforeAssistantMessage() {
    val messages = listOf(
      testChatMessage("user", MessageRole.USER, createdAt = 1),
      testChatMessage("tool-1", MessageRole.TOOL, createdAt = 2),
      testChatMessage("tool-2", MessageRole.TOOL, createdAt = 3),
      testChatMessage("assistant", MessageRole.ASSISTANT, createdAt = 4)
    )

    val items = chatMessageListItems(messages)

    assertEquals(3, items.size)
    assertTrue(items[0] is ChatMessageListItem.Message)
    val toolGroup = items[1] as ChatMessageListItem.ToolGroup
    assertEquals(listOf("tool-1", "tool-2"), toolGroup.messageIds)
    assertEquals("assistant", (items[2] as ChatMessageListItem.Message).message.id)
  }

  @Test
  fun chatToolMessagesDoNotCrossUserMessages() {
    val messages = listOf(
      testChatMessage("tool-1", MessageRole.TOOL, createdAt = 1),
      testChatMessage("user", MessageRole.USER, createdAt = 2),
      testChatMessage("tool-2", MessageRole.TOOL, createdAt = 3),
      testChatMessage("assistant", MessageRole.ASSISTANT, createdAt = 4)
    )

    val items = chatMessageListItems(messages)

    assertEquals(4, items.size)
    assertEquals(listOf("tool-1"), (items[0] as ChatMessageListItem.ToolGroup).messageIds)
    assertEquals("user", (items[1] as ChatMessageListItem.Message).message.id)
    assertEquals(listOf("tool-2"), (items[2] as ChatMessageListItem.ToolGroup).messageIds)
    assertEquals("assistant", (items[3] as ChatMessageListItem.Message).message.id)
  }

  @Test
  fun chatOrphanToolMessagesRemainVisibleAsToolGroup() {
    val messages = listOf(
      testChatMessage("user", MessageRole.USER, createdAt = 1),
      testChatMessage("tool", MessageRole.TOOL, createdAt = 2)
    )

    val items = chatMessageListItems(messages)

    assertEquals(2, items.size)
    assertEquals("user", (items[0] as ChatMessageListItem.Message).message.id)
    assertEquals(listOf("tool"), (items[1] as ChatMessageListItem.ToolGroup).messageIds)
  }

  @Test
  fun toolCallDetailsExtractQueryAndCitationUrls() {
    val details = parseToolCallDetails(
      """
      工具：web_search
      输入：
      {"query":"DeepSeek API pricing"}
      输出：
      搜索关键词：
      DeepSeek API pricing

      1. DeepSeek Pricing
      Official pricing page
      https://api-docs.deepseek.com/quick_start/pricing/
      """.trimIndent()
    )

    assertEquals("web_search", details.name)
    assertEquals("DeepSeek API pricing", details.query)
    assertEquals(emptyList<String>(), details.openedUrls)
    assertEquals(1, details.citations.size)
    assertEquals("DeepSeek Pricing", details.citations.first().title)
    assertEquals("https://api-docs.deepseek.com/quick_start/pricing/", details.citations.first().url)
    assertTrue(details.summary?.contains("DeepSeek API pricing") == true)
  }

  @Test
  fun toolCallDetailsExtractOpenedUrlForOpenPage() {
    val details = parseToolCallDetails(
      """
      工具：open_page
      输入：
      {"url":"https://example.com/report"}
      输出：
      打开网页：https://example.com/report
      标题：Report
      """.trimIndent()
    )

    assertEquals(listOf("https://example.com/report"), details.openedUrls)
    assertEquals(emptyList<ToolCallCitation>(), details.citations)
    assertTrue(details.summary?.contains("https://example.com/report") == true)
  }

  @Test
  fun imageConversationGeneratesAssistantImageAttachment() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val keyStore = FakeApiKeyStore()
    val imageDir = File(System.getProperty("java.io.tmpdir"), "aichat_image_test_${System.nanoTime()}")
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = keyStore,
      adapters = mapOf(ProviderType.OPENAI_RESPONSES to adapter),
      generatedImageDir = imageDir
    )
    dao.upsertProvider(provider("provider", "gpt-image").copy(
      type = ProviderType.OPENAI_RESPONSES.name,
      supportsImageGeneration = true,
      secretRef = "provider_provider"
    ))
    keyStore.write("provider_provider", "key")
    val conversation = repository.createImageConversation("provider", "gpt-image")

    repository.sendImageMessage(conversation.id, "画一只猫", emptyList(), ImageGenerationOptions(count = 2))

    val messages = dao.messagesForConversation(conversation.id).map { it.toDomain() }
    assertEquals(listOf(MessageRole.USER, MessageRole.ASSISTANT), messages.map { it.role })
    val assistant = messages.last()
    assertEquals(MessageStatus.COMPLETE, assistant.status)
    assertEquals(2, assistant.attachments.size)
    assertTrue(assistant.attachments.all { it.isImage && File(it.localPath).exists() })
    assertEquals(1, adapter.lastImageOptions?.count)
  }

  @Test
  fun imageConversationCapturesRawResponseLogWhenDebugLoggingEnabled() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val preferences = FakeSelectionStore()
    preferences.setDebugResponseLogging(true)
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = preferences,
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.OPENAI_RESPONSES to adapter),
      generatedImageDir = File(System.getProperty("java.io.tmpdir"), "aichat_image_log_test_${System.nanoTime()}")
    )
    dao.upsertProvider(provider("provider", "gpt-image").copy(
      type = ProviderType.OPENAI_RESPONSES.name,
      supportsImageGeneration = true,
      secretRef = "provider_provider"
    ))
    val conversation = repository.createImageConversation("provider", "gpt-image")

    repository.sendImageMessage(conversation.id, "生成海报", emptyList(), ImageGenerationOptions())

    val assistant = dao.messagesForConversation(conversation.id).map { it.toDomain() }.last()
    assertEquals(MessageStatus.COMPLETE, assistant.status)
    assertTrue(adapter.lastImageOptions?.captureRawResponseLog == true)
    assertTrue(assistant.rawResponseLog?.contains("\"image_generation_call\"") == true)
  }

  @Test
  fun groupBotTurnSendsPriorBotMessagesAsUserContextForResponsesCompatibility() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val first = repository.createAiBot("GPT", "provider", "gpt-fixed", "")
    val second = repository.createAiBot("Deep", "provider", "deep-fixed", "")
    val group = repository.createGroupChat("出游计划", "制定计划", listOf(first.id, second.id))
    repository.sendGroupUserMessage(group.id, "先查天气", emptyList())
    repository.sendGroupBotTurn(group.id, first.id)

    repository.sendGroupBotTurn(group.id, second.id)

    val priorBotContext = adapter.lastMessages.firstOrNull { it.content.contains("[GPT] fake response") }
    assertNotNull(priorBotContext)
    assertEquals(MessageRole.USER, priorBotContext?.role)
  }

  @Test
  fun firstGroupBotTurnIncludesSyntheticUserTaskForResponsesWebSearch() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("GPT", "provider", "gpt-fixed", "")
    val group = repository.createGroupChat("带娃出游计划", "江苏苏州一岁半女童家庭，如何制定带娃出游计划", listOf(bot.id))

    repository.sendGroupBotTurn(group.id, bot.id)

    val task = adapter.lastMessages.firstOrNull { it.id.startsWith("group-initial-task-") }
    assertNotNull(task)
    assertEquals(MessageRole.USER, task?.role)
    assertTrue(task?.content?.contains("请以「GPT」的身份开始这个群聊") == true)
    assertTrue(task?.content?.contains("如果需要最新信息，可以先进行网页搜索") == true)
  }

  @Test
  fun groupSummaryTurnUpdatesRoomSummary() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("总结员", "provider", "summary-model", "")
    val group = repository.createGroupChat("复盘", "整理结论", listOf(bot.id))
    repository.sendGroupUserMessage(group.id, "结论一", emptyList())

    repository.sendGroupBotTurn(group.id, bot.id, summarize = true)

    assertEquals("fake response", dao.groupChatRoomById(group.id)?.summary)
    assertTrue(adapter.lastMessages.first().content.contains("本轮任务是总结当前讨论"))
  }

  @Test
  fun createFavoriteSnippetFromGroupMessageStoresGroupSnapshot() = runTest {
    val dao = FakeChatDao()
    val adapter = RecordingAdapter()
    val repository = ChatRepository(
      dao = dao,
      preferencesRepository = FakeSelectionStore(),
      apiKeyStore = FakeApiKeyStore(),
      adapters = mapOf(ProviderType.TOKENHUB_PROXY to adapter)
    )
    dao.upsertProvider(provider("provider", "provider-default"))
    val bot = repository.createAiBot("GPT 研究员", "provider", "gpt-fixed", "")
    val group = repository.createGroupChat("资料评审", "讨论资料", listOf(bot.id))
    repository.sendGroupUserMessage(group.id, "用户观点", emptyList())
    repository.sendGroupBotTurn(group.id, bot.id)
    val botMessage = dao.groupMessages(group.id).last()

    val favorite = repository.createFavoriteSnippetFromGroupMessages(group.id, setOf(botMessage.id), "", "群聊结果", "group")

    assertEquals("资料评审", favorite.sourceConversationTitle)
    assertEquals("AI 群聊", favorite.sourceGroupName)
    assertEquals("gpt-fixed", favorite.sourceModel)
    assertEquals(1, favorite.messageCount)
    assertTrue(favorite.messages.first().content.contains("GPT 研究员"))
    assertTrue(favorite.searchText.contains("群聊结果"))
  }

  @Test
  fun longBubbleNavTargetShowsOnlyMissingEdges() {
    val topHidden = longBubbleNavTarget(
      visibleItems = listOf(VisibleListItemBounds(index = 2, offset = -220, size = 520)),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(2)
    )
    val bottomHidden = longBubbleNavTarget(
      visibleItems = listOf(VisibleListItemBounds(index = 3, offset = 120, size = 520)),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(3)
    )
    val bothHidden = longBubbleNavTarget(
      visibleItems = listOf(VisibleListItemBounds(index = 4, offset = -120, size = 720)),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(4)
    )
    val fullyVisible = longBubbleNavTarget(
      visibleItems = listOf(VisibleListItemBounds(index = 5, offset = 40, size = 180)),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(5)
    )

    assertEquals(LongBubbleNavTarget(index = 2, showUp = true, showDown = false, bottomOffset = 120), topHidden)
    assertEquals(LongBubbleNavTarget(index = 3, showUp = false, showDown = true, bottomOffset = 120), bottomHidden)
    assertEquals(LongBubbleNavTarget(index = 4, showUp = true, showDown = true, bottomOffset = 320), bothHidden)
    assertNull(fullyVisible)
  }

  @Test
  fun longBubbleNavTargetShowsActionsOnlyWhenTopIsHidden() {
    val topHidden = longBubbleNavTarget(
      visibleItems = listOf(
        VisibleListItemBounds(index = 2, offset = -220, size = 520, messageId = "message-2", supportsActions = true)
      ),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(2)
    )
    val bottomHidden = longBubbleNavTarget(
      visibleItems = listOf(
        VisibleListItemBounds(index = 3, offset = 120, size = 520, messageId = "message-3", supportsActions = true)
      ),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(3)
    )

    assertEquals("message-2", topHidden?.messageId)
    assertEquals(true, topHidden?.showActions)
    assertEquals("message-3", bottomHidden?.messageId)
    assertEquals(false, bottomHidden?.showActions)
  }

  @Test
  fun longBubbleNavTargetPrefersVisibleCandidateNearestViewportCenter() {
    val target = longBubbleNavTarget(
      visibleItems = listOf(
        VisibleListItemBounds(index = 1, offset = -380, size = 500, messageId = "message-1", supportsActions = true),
        VisibleListItemBounds(index = 2, offset = 120, size = 500, messageId = "message-2", supportsActions = true)
      ),
      viewportStart = 0,
      viewportEnd = 400,
      candidateIndexes = setOf(1, 2)
    )

    assertEquals(2, target?.index)
    assertEquals("message-2", target?.messageId)
    assertEquals(false, target?.showUp)
    assertEquals(true, target?.showDown)
  }

  @Test
  fun nextGroupAutoPlayBotStartsAtFirstBotWhenNoBotHasSpoken() {
    val bots = listOf(testBot("bot-a", "A"), testBot("bot-b", "B"))

    assertEquals("bot-a", nextGroupAutoPlayBotId(bots, emptyList()))
  }

  @Test
  fun nextGroupAutoPlayBotCyclesAfterMostRecentBotMessage() {
    val bots = listOf(testBot("bot-a", "A"), testBot("bot-b", "B"))
    val messages = listOf(
      testGroupMessage("m1", GroupMessageSenderType.BOT, "bot-a", "A"),
      testGroupMessage("m2", GroupMessageSenderType.USER, null, "我"),
      testGroupMessage("m3", GroupMessageSenderType.BOT, "bot-b", "B")
    )

    assertEquals("bot-a", nextGroupAutoPlayBotId(bots, messages))
  }

  @Test
  fun nextGroupAutoPlayBotSkipsBotThatIsNoLongerEnabled() {
    val bots = listOf(testBot("bot-a", "A"), testBot("bot-c", "C"))
    val messages = listOf(testGroupMessage("m1", GroupMessageSenderType.BOT, "bot-b", "B"))

    assertEquals("bot-a", nextGroupAutoPlayBotId(bots, messages))
  }

  @Test
  fun autoBotBubbleColorKeyIsStableAndUsesPalette() {
    val first = resolvedBotBubbleColorKey("bot-a", "AUTO")
    val second = resolvedBotBubbleColorKey("bot-a", "AUTO")

    assertEquals(first, second)
    assertNotEquals("AUTO", first)
    assertEquals("ROSE", resolvedBotBubbleColorKey("bot-a", "ROSE"))
  }

  @Test
  fun collapsedGroupMessageSummaryUsesFirstNonBlankLineAndLimit() {
    val longMessage = testGroupMessage("m1", GroupMessageSenderType.BOT, "bot-a", "A").copy(
      content = "\n\n这是一个很长很长很长很长很长很长很长的回答正文和后续解释，需要继续截断\n第二行"
    )
    val emptyMessage = testGroupMessage("m2", GroupMessageSenderType.BOT, "bot-a", "A").copy(content = "")

    assertEquals("这是一个很长很长很长很长很长很长很长的回答正文和后续解释，需...", collapsedGroupMessageSummary(longMessage))
    assertEquals("无内容", collapsedGroupMessageSummary(emptyMessage))
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

  private fun testChatMessage(
    id: String,
    role: MessageRole,
    createdAt: Long
  ): ChatMessage = ChatMessage(
    id = id,
    conversationId = "conversation",
    role = role,
    content = "content",
    status = MessageStatus.COMPLETE,
    providerId = "provider",
    model = "model",
    createdAt = createdAt,
    updatedAt = createdAt,
    errorMessage = null
  )

  private fun testBot(id: String, name: String): AiBot = AiBot(
    id = id,
    name = name,
    providerId = "provider",
    model = "model",
    systemPrompt = "",
    bubbleColorKey = "AUTO",
    enabled = true,
    createdAt = 1,
    updatedAt = 1
  )

  private fun testGroupMessage(
    id: String,
    senderType: GroupMessageSenderType,
    botId: String?,
    senderName: String
  ): GroupChatMessage = GroupChatMessage(
    id = id,
    groupId = "group",
    senderType = senderType,
    botId = botId,
    senderName = senderName,
    role = if (senderType == GroupMessageSenderType.USER) MessageRole.USER else MessageRole.ASSISTANT,
    content = "content",
    status = MessageStatus.COMPLETE,
    providerId = "provider",
    model = "model",
    createdAt = 1,
    updatedAt = 1,
    errorMessage = null
  )
}

private class RecordingAdapter(
  private val events: List<ChatStreamEvent> = listOf(
    ChatStreamEvent.Started,
    ChatStreamEvent.TextDelta("fake response"),
    ChatStreamEvent.Completed
  )
) : ProviderAdapter {
  var lastOptions: ChatCompletionOptions? = null
  var lastImageOptions: ImageGenerationOptions? = null
  var lastMessages: List<ChatMessage> = emptyList()

  override fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent> = flow {
    lastMessages = messages
    lastOptions = options
    events.forEach { emit(it) }
  }

  override fun generateImages(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ImageGenerationOptions
  ): Flow<ChatStreamEvent> = flow {
    lastMessages = messages
    lastImageOptions = options
    emit(ChatStreamEvent.Started)
    if (options.captureRawResponseLog) {
      emit(ChatStreamEvent.RawFrame("response", """{"type":"image_generation_call","result":"aGVsbG8="}"""))
    }
    emit(ChatStreamEvent.ImageGenerated(base64Data = "aGVsbG8=", mimeType = "image/png", revisedPrompt = "revised"))
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

  override suspend fun setAttachmentLimits(maxFileMb: Int, maxPendingMb: Int, maxImageSourceMb: Int) {
    appSettings.value = appSettings.value.copy(
      attachmentMaxFileMb = maxFileMb,
      attachmentMaxPendingMb = maxPendingMb,
      attachmentMaxImageSourceMb = maxImageSourceMb
    )
  }

  override suspend fun setBackgroundPresets(presets: List<ChatBackgroundPreset>) {
    appSettings.value = appSettings.value.copy(backgroundPresets = presets)
  }
}

private class FakeApiKeyStore : ApiKeyStore {
  val deletedSecretRefs = mutableListOf<String>()
  override fun read(secretRef: String?): String? = "test-key"
  override fun exists(secretRef: String?): Boolean = true
  override fun write(secretRef: String, apiKey: String) = Unit
  override fun delete(secretRef: String) {
    deletedSecretRefs += secretRef
  }
}

private class FakeChatDao : ChatDao {
  private val providers = linkedMapOf<String, ProviderEntity>()
  private val conversations = linkedMapOf<String, ConversationEntity>()
  private val messages = linkedMapOf<String, MessageEntity>()
  private val favorites = linkedMapOf<String, FavoriteSnippetEntity>()
  private val aiBots = linkedMapOf<String, AiBotEntity>()
  private val groupRooms = linkedMapOf<String, GroupChatRoomEntity>()
  private val groupMembers = linkedMapOf<Pair<String, String>, GroupChatMemberEntity>()
  private val groupMessages = linkedMapOf<String, GroupMessageEntity>()

  override fun observeProviders(): Flow<List<ProviderEntity>> = flowOf(providers.values.toList())

  override suspend fun providerById(id: String): ProviderEntity? = providers[id]

  override suspend fun upsertProvider(provider: ProviderEntity) {
    providers[provider.id] = provider
  }

  override suspend fun setProviderEnabled(id: String, enabled: Boolean) {
    providers[id]?.let { providers[id] = it.copy(enabled = enabled) }
  }

  override suspend fun providerCount(): Int = providers.size

  override suspend fun deleteProvider(id: String) {
    providers.remove(id)
  }

  override suspend fun aiBotsByProviderId(providerId: String): List<AiBotEntity> =
    aiBots.values.filter { it.providerId == providerId }

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

  override suspend fun clearConversationGroup(groupName: String, updatedAt: Long) {
    conversations.replaceAll { _, conversation ->
      if (conversation.groupName == groupName && !conversation.isDeleted) {
        conversation.copy(groupName = "", updatedAt = updatedAt)
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

  override suspend fun updateMessageAttachments(id: String, attachmentsJson: String, updatedAt: Long) {
    messages[id]?.let {
      messages[id] = it.copy(attachmentsJson = attachmentsJson, updatedAt = updatedAt)
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

  override fun observeFavoriteSnippets(): Flow<List<FavoriteSnippetEntity>> = flowOf(
    favorites.values.sortedByDescending { it.updatedAt }
  )

  override suspend fun favoriteSnippetById(id: String): FavoriteSnippetEntity? = favorites[id]

  override suspend fun upsertFavoriteSnippet(favorite: FavoriteSnippetEntity) {
    favorites[favorite.id] = favorite
  }

  override suspend fun deleteFavoriteSnippet(id: String) {
    favorites.remove(id)
  }

  override fun observeAiBots(): Flow<List<AiBotEntity>> = flowOf(
    aiBots.values.sortedWith(compareByDescending<AiBotEntity> { it.enabled }.thenByDescending { it.updatedAt }.thenBy { it.name })
  )

  override suspend fun aiBotById(id: String): AiBotEntity? = aiBots[id]

  override suspend fun upsertAiBot(bot: AiBotEntity) {
    aiBots[bot.id] = bot
  }

  override suspend fun setAiBotEnabled(id: String, enabled: Boolean, updatedAt: Long) {
    aiBots[id]?.let { aiBots[id] = it.copy(enabled = enabled, updatedAt = updatedAt) }
  }

  override suspend fun deleteAiBot(id: String) {
    aiBots.remove(id)
  }

  override fun observeGroupChatRooms(): Flow<List<GroupChatRoomEntity>> = flowOf(
    groupRooms.values.filter { !it.isDeleted && !it.isArchived }.sortedByDescending { it.updatedAt }
  )

  override suspend fun groupChatRoomById(id: String): GroupChatRoomEntity? = groupRooms[id]

  override suspend fun upsertGroupChatRoom(room: GroupChatRoomEntity) {
    groupRooms[room.id] = room
  }

  override suspend fun updateGroupChatRoomMeta(id: String, title: String, topic: String, updatedAt: Long) {
    groupRooms[id]?.let { groupRooms[id] = it.copy(title = title, topic = topic, updatedAt = updatedAt) }
  }

  override suspend fun updateGroupChatSummary(id: String, summary: String, updatedAt: Long) {
    groupRooms[id]?.let { groupRooms[id] = it.copy(summary = summary, updatedAt = updatedAt) }
  }

  override suspend fun touchGroupChatRoom(id: String, updatedAt: Long) {
    groupRooms[id]?.let { groupRooms[id] = it.copy(updatedAt = updatedAt) }
  }

  override suspend fun archiveGroupChatRoom(id: String, updatedAt: Long) {
    groupRooms[id]?.let { groupRooms[id] = it.copy(isArchived = true, updatedAt = updatedAt) }
  }

  override suspend fun deleteGroupChatRoom(id: String, updatedAt: Long) {
    groupRooms[id]?.let { groupRooms[id] = it.copy(isDeleted = true, updatedAt = updatedAt) }
  }

  override fun observeGroupChatMembers(groupId: String): Flow<List<GroupChatMemberEntity>> = flowOf(
    groupChatMembersInternal(groupId)
  )

  override suspend fun groupChatMembers(groupId: String): List<GroupChatMemberEntity> =
    groupChatMembersInternal(groupId)

  override suspend fun allGroupChatMembers(groupId: String): List<GroupChatMemberEntity> =
    groupMembers.values.filter { it.groupId == groupId }.sortedBy { it.sortOrder }

  override suspend fun upsertGroupChatMember(member: GroupChatMemberEntity) {
    groupMembers[member.groupId to member.botId] = member
  }

  override suspend fun removeGroupChatMember(groupId: String, botId: String, updatedAt: Long) {
    groupMembers[groupId to botId]?.let { groupMembers[groupId to botId] = it.copy(enabled = false, updatedAt = updatedAt) }
  }

  override fun observeGroupMessages(groupId: String): Flow<List<GroupMessageEntity>> = flowOf(
    groupMessagesInternal(groupId)
  )

  override suspend fun groupMessages(groupId: String): List<GroupMessageEntity> =
    groupMessagesInternal(groupId)

  override suspend fun upsertGroupMessage(message: GroupMessageEntity) {
    groupMessages[message.id] = message
  }

  override suspend fun updateGroupMessageWithMetadata(
    id: String,
    content: String,
    status: String,
    updatedAt: Long,
    errorMessage: String?,
    totalDurationMs: Long?,
    firstTokenDurationMs: Long?,
    promptTokens: Int?,
    completionTokens: Int?,
    totalTokens: Int?
  ) {
    groupMessages[id]?.let {
      groupMessages[id] = it.copy(
        content = content,
        status = status,
        updatedAt = updatedAt,
        errorMessage = errorMessage,
        totalDurationMs = totalDurationMs,
        firstTokenDurationMs = firstTokenDurationMs,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
      )
    }
  }

  private fun messagesForConversationInternal(conversationId: String): List<MessageEntity> =
    messages.values.filter { it.conversationId == conversationId }.sortedBy { it.createdAt }

  private fun groupChatMembersInternal(groupId: String): List<GroupChatMemberEntity> =
    groupMembers.values.filter { it.groupId == groupId && it.enabled }.sortedBy { it.sortOrder }

  private fun groupMessagesInternal(groupId: String): List<GroupMessageEntity> =
    groupMessages.values.filter { it.groupId == groupId }.sortedBy { it.createdAt }
}
