package com.personal.aichat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.personal.aichat.AppForegroundTracker
import com.personal.aichat.ChatGenerationService
import com.personal.aichat.data.ChatRepository
import com.personal.aichat.data.ChatSelectionStore
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.GroupChatMember
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.WebSearchMode
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel(
  private val repository: ChatRepository,
  private val preferencesRepository: ChatSelectionStore,
  private val appContext: Context? = null
) : ViewModel() {
  private val localState = MutableStateFlow(ChatUiState())
  private val sendJobsByConversationId = mutableMapOf<String, Job>()
  private val groupJobsByGroupId = mutableMapOf<String, Job>()
  private val lastGroupTurnCompletedByGroupId = mutableMapOf<String, Boolean>()
  private var pendingDeleteConversationId: String? = null

  private val conversationLists = combine(
    repository.conversations,
    repository.archivedConversations,
    repository.favoriteSnippets,
    repository.aiBots,
    repository.groupChatRooms
  ) { conversations, archivedConversations, favoriteSnippets, aiBots, groupChats ->
    ConversationLists(conversations, archivedConversations, favoriteSnippets, aiBots, groupChats)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private val baseUiState = combine(
    repository.providers,
    conversationLists,
    preferencesRepository.selectedConversationId,
    preferencesRepository.selectedProviderId,
    localState
  ) { providers, lists, selectedConversationId, selectedProviderId, local ->
    val conversations = lists.active
    val effectiveConversationId = selectedConversationId ?: conversations.firstOrNull()?.id
    val effectiveProviderId = selectedProviderId
      ?: conversations.firstOrNull { it.id == effectiveConversationId }?.providerId
      ?: providers.firstOrNull()?.id
    local.copy(
      providers = providers,
      conversations = conversations,
      archivedConversations = lists.archived,
      conversationGroups = conversations.toConversationGroups(),
      favoriteSnippets = lists.favorites,
      aiBots = lists.aiBots,
      groupChats = lists.groupChats,
      selectedConversationId = effectiveConversationId,
      selectedProviderId = effectiveProviderId
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val uiState = baseUiState.combine(preferencesRepository.appSettings) { state, appSettings ->
    state.copy(appSettings = appSettings)
  }.flatMapLatest { state ->
    val groupId = state.selectedGroupChatId.takeIf { state.groupChatPageOpen }
    val conversationId = state.selectedConversationId
    if (groupId != null) {
      repository.observeGroupMessages(groupId).combine(repository.observeGroupMembers(groupId)) { groupMessages, members ->
        state.copy(groupMessages = groupMessages, groupMembers = members)
      }
    } else if (conversationId == null) {
      flowOf(state.copy(groupMessages = emptyList(), groupMembers = emptyList()))
    } else {
      repository.observeMessages(conversationId).combine(flowOf(state)) { messages, current ->
        current.copy(messages = messages, groupMessages = emptyList(), groupMembers = emptyList())
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

  init {
    viewModelScope.launch {
      repository.bootstrapDefaults()
      val conversation = repository.ensureConversation()
      preferencesRepository.setSelectedConversation(conversation.id)
      preferencesRepository.setSelectedProvider(conversation.providerId)
    }
  }

  fun updateInput(value: TextFieldValue) {
    localState.update { it.copy(input = value) }
  }

  fun updateGroupInput(value: TextFieldValue) {
    localState.update { it.copy(groupInput = value) }
  }

  fun addAttachments(uris: List<Uri>) {
    val context = appContext ?: return
    if (!uiState.value.groupChatPageOpen && uiState.value.selectedProvider?.supportsAttachments != true) return
    viewModelScope.launch {
      val imported = uris.mapNotNull { uri -> importAttachment(context, uri) }
      if (imported.isNotEmpty()) {
        localState.update { it.copy(pendingAttachments = it.pendingAttachments + imported) }
      }
    }
  }

  fun removePendingAttachment(id: String) {
    localState.update { state ->
      state.pendingAttachments.firstOrNull { it.id == id }?.let { attachment ->
        runCatching { File(attachment.localPath).delete() }
      }
      state.copy(pendingAttachments = state.pendingAttachments.filterNot { it.id == id })
    }
  }

  fun editAndResend(messageText: String) {
    localState.update {
      it.copy(input = TextFieldValue(messageText, selection = TextRange(messageText.length)))
    }
  }

  fun toggleMessageSelectionMode(enabled: Boolean) {
    localState.update {
      it.copy(
        messageSelectionMode = enabled,
        selectedMessageIds = if (enabled) it.selectedMessageIds else emptySet()
      )
    }
  }

  fun toggleMessageSelected(messageId: String) {
    localState.update { state ->
      val selected = state.selectedMessageIds.toMutableSet()
      if (!selected.add(messageId)) selected.remove(messageId)
      state.copy(
        messageSelectionMode = selected.isNotEmpty() || state.messageSelectionMode,
        selectedMessageIds = selected
      )
    }
  }

  fun selectMessageRangeTo(messageId: String) {
    val state = uiState.value
    val selectedIds = state.selectedMessageIds
    if (selectedIds.isEmpty()) {
      toggleMessageSelected(messageId)
      return
    }
    val messages = state.messages
    val targetIndex = messages.indexOfFirst { it.id == messageId }
    if (targetIndex < 0) return
    val anchorIndex = messages.indexOfLast { it.id in selectedIds }.takeIf { it >= 0 } ?: targetIndex
    val range = if (anchorIndex <= targetIndex) anchorIndex..targetIndex else targetIndex..anchorIndex
    val rangeIds = range.map { messages[it].id }
    localState.update {
      it.copy(
        messageSelectionMode = true,
        selectedMessageIds = it.selectedMessageIds + rangeIds
      )
    }
  }

  fun send() {
    val state = uiState.value
    val conversationId = state.selectedConversationId ?: return
    val text = state.input.text
    val attachments = if (state.selectedProvider?.supportsAttachments == true) state.pendingAttachments else emptyList()
    if ((text.isBlank() && attachments.isEmpty()) || sendJobsByConversationId[conversationId]?.isActive == true) return
    localState.update { it.copy(input = TextFieldValue(""), pendingAttachments = emptyList()) }
    launchStreamingJob(conversationId) {
      repository.sendMessage(conversationId, text, attachments)
    }
  }

  fun retryLast() {
    val conversationId = uiState.value.selectedConversationId ?: return
    if (sendJobsByConversationId[conversationId]?.isActive == true) return
    launchStreamingJob(conversationId) {
      repository.retryLast(conversationId)
    }
  }

  fun stopGenerating() {
    val conversationId = uiState.value.selectedConversationId ?: return
    sendJobsByConversationId[conversationId]?.cancel()
  }

  fun stopGroupGenerating() {
    val groupId = uiState.value.selectedGroupChatId ?: return
    groupJobsByGroupId[groupId]?.cancel()
  }

  fun toggleGroupAutoPlay() {
    val state = uiState.value
    val groupId = state.selectedGroupChatId ?: return
    if (groupId in state.autoPlayingGroupIds) {
      pauseGroupAutoPlay(groupId)
    } else {
      startGroupAutoPlay(groupId)
    }
  }

  fun startGroupAutoPlay(groupId: String? = null) {
    val targetGroupId = groupId ?: uiState.value.selectedGroupChatId ?: return
    localState.update { it.copy(autoPlayingGroupIds = it.autoPlayingGroupIds + targetGroupId) }
    if (groupJobsByGroupId[targetGroupId]?.isActive == true) return
    val nextBotId = nextAutoPlayBotId(targetGroupId)
    if (nextBotId == null) {
      pauseGroupAutoPlay(targetGroupId)
      return
    }
    launchGroupBotTurn(targetGroupId, nextBotId, summarize = false, continueAutoPlay = true)
  }

  fun pauseGroupAutoPlay(groupId: String? = null) {
    val targetGroupId = groupId ?: uiState.value.selectedGroupChatId ?: return
    localState.update { it.copy(autoPlayingGroupIds = it.autoPlayingGroupIds - targetGroupId) }
  }

  private fun importAttachment(context: Context, uri: Uri): ChatAttachment? {
    return runCatching {
      val resolver = context.contentResolver
      val info = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          AttachmentImportInfo(
            displayName = nameIndex.takeIf { it >= 0 }?.let { cursor.getString(it) },
            sizeBytes = sizeIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) }
          )
        } else {
          AttachmentImportInfo(null, null)
        }
      } ?: AttachmentImportInfo(null, null)
      val mimeType = resolver.getType(uri) ?: "application/octet-stream"
      val id = "att_${UUID.randomUUID().toString().replace("-", "")}"
      val displayName = info.displayName?.takeIf { it.isNotBlank() } ?: "$id.${mimeType.substringAfter('/', "bin")}"
      val dir = File(context.filesDir, "chat_attachments").apply { mkdirs() }
      val safeName = displayName.replace(Regex("""[\\/:*?"<>|]"""), "_")
      val target = File(dir, "${id}_$safeName")
      resolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
      } ?: return@runCatching null
      ChatAttachment(
        id = id,
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = info.sizeBytes ?: target.length(),
        localPath = target.absolutePath
      )
    }.getOrNull()
  }

  private fun launchStreamingJob(conversationId: String, block: suspend () -> Unit) {
    val wasIdle = sendJobsByConversationId.isEmpty()
    if (wasIdle) {
      appContext?.let { context ->
        runCatching { ChatGenerationService.start(context) }
      }
    }
    localState.update { it.copy(streamingConversationIds = it.streamingConversationIds + conversationId) }
    val job = viewModelScope.launch {
      var completed = false
      try {
        block()
        completed = true
      } catch (error: CancellationException) {
        throw error
      } finally {
        sendJobsByConversationId.remove(conversationId)
        localState.update { it.copy(streamingConversationIds = it.streamingConversationIds - conversationId) }
        if (sendJobsByConversationId.isEmpty()) {
          appContext?.let { context ->
            if (completed && !AppForegroundTracker.isForeground) {
              runCatching { ChatGenerationService.complete(context) }
            } else {
              runCatching { ChatGenerationService.stop(context) }
            }
          }
        }
      }
    }
    sendJobsByConversationId[conversationId] = job
  }

  private fun launchGroupStreamingJob(
    groupId: String,
    onCompleted: suspend (Boolean) -> Unit = {},
    block: suspend () -> Unit
  ) {
    val wasIdle = groupJobsByGroupId.isEmpty() && sendJobsByConversationId.isEmpty()
    if (wasIdle) {
      appContext?.let { context -> runCatching { ChatGenerationService.start(context) } }
    }
    localState.update { it.copy(streamingGroupIds = it.streamingGroupIds + groupId) }
    val job = viewModelScope.launch {
      var completed = false
      try {
        block()
        completed = true
      } catch (error: CancellationException) {
        throw error
      } finally {
        groupJobsByGroupId.remove(groupId)
        localState.update { it.copy(streamingGroupIds = it.streamingGroupIds - groupId) }
        onCompleted(completed)
        if (groupJobsByGroupId.isEmpty() && sendJobsByConversationId.isEmpty()) {
          appContext?.let { context ->
            if (completed && !AppForegroundTracker.isForeground) {
              runCatching { ChatGenerationService.complete(context) }
            } else {
              runCatching { ChatGenerationService.stop(context) }
            }
          }
        }
      }
    }
    groupJobsByGroupId[groupId] = job
  }

  private data class AttachmentImportInfo(
    val displayName: String?,
    val sizeBytes: Long?
  )

  fun selectConversation(id: String) {
    viewModelScope.launch {
      preferencesRepository.setSelectedConversation(id)
    }
  }

  fun togglePinConversation(id: String, pinned: Boolean) {
    viewModelScope.launch {
      repository.setConversationPinned(id, !pinned)
    }
  }

  fun archiveConversation(id: String) {
    viewModelScope.launch {
      val fallback = repository.archiveConversation(id)
      preferencesRepository.setSelectedConversation(fallback.id)
      preferencesRepository.setSelectedProvider(fallback.providerId)
    }
  }

  fun restoreConversation(id: String) {
    viewModelScope.launch {
      val conversation = repository.restoreConversation(id)
      preferencesRepository.setSelectedConversation(conversation.id)
      preferencesRepository.setSelectedProvider(conversation.providerId)
    }
  }

  fun deleteConversation(id: String) {
    pendingDeleteConversationId = id
    localState.update {
      it.copy(
        deleteConfirmOpen = true,
        deleteTargetConversationId = id
      )
    }
  }

  fun confirmDeleteConversation() {
    val conversationId = pendingDeleteConversationId ?: return
    viewModelScope.launch {
      val fallback = repository.deleteConversation(conversationId)
      preferencesRepository.setSelectedConversation(fallback.id)
      preferencesRepository.setSelectedProvider(fallback.providerId)
      pendingDeleteConversationId = null
      localState.update {
        it.copy(
          deleteConfirmOpen = false,
          deleteTargetConversationId = null
        )
      }
    }
  }

  fun cancelDeleteConversation() {
    pendingDeleteConversationId = null
    localState.update {
      it.copy(
        deleteConfirmOpen = false,
        deleteTargetConversationId = null
      )
    }
  }

  fun renameConversation(conversationId: String, title: String, groupName: String) {
    viewModelScope.launch {
      repository.updateConversationMeta(conversationId, title, groupName)
    }
  }

  fun shareConversationText(conversationId: String, context: Context) {
    viewModelScope.launch {
      shareTextInternal(conversationId, emptySet(), context)
    }
  }

  fun shareSelectedMessagesText(context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    val selectedIds = uiState.value.selectedMessageIds
    viewModelScope.launch {
      shareTextInternal(conversationId, selectedIds, context)
    }
  }

  fun shareConversationMarkdownFile(context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    viewModelScope.launch {
      val shareText = repository.conversationShareText(conversationId)
      if (shareText.isBlank()) return@launch
      val title = uiState.value.selectedConversation?.title ?: "AIChat"
      val uri = ConversationShareRenderer.writeTextExport(context, title, shareText)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享 Markdown 文件"))
    }
  }

  fun shareConversationLongImage(context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    viewModelScope.launch {
      val export = repository.conversationExport(conversationId) ?: return@launch
      val uri = ConversationShareRenderer.saveImageToGallery(context, export)
        ?: ConversationShareRenderer.writeImageExport(context, export)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享长图"))
    }
  }

  fun shareSelectedMessagesLongImage(context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    val selectedIds = uiState.value.selectedMessageIds
    if (selectedIds.isEmpty()) return
    viewModelScope.launch {
      val export = repository.conversationExport(conversationId) ?: return@launch
      val selectedMessages = export.messages.filter { it.id in selectedIds }
      if (selectedMessages.isEmpty()) return@launch
      val selectedExport = export.copy(title = "${export.title}（节选）", messages = selectedMessages)
      val uri = ConversationShareRenderer.saveImageToGallery(context, selectedExport)
        ?: ConversationShareRenderer.writeImageExport(context, selectedExport)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享选中消息长图"))
    }
  }

  fun shareMessageText(messageId: String, context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    viewModelScope.launch {
      val shareText = repository.messageShareText(conversationId, messageId)
      if (shareText.isBlank()) return@launch
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享消息文本"))
    }
  }

  fun shareMessageImage(messageId: String, context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    viewModelScope.launch {
      val export = repository.messageExport(conversationId, messageId) ?: return@launch
      val uri = ConversationShareRenderer.saveImageToGallery(context, export)
        ?: ConversationShareRenderer.writeImageExport(context, export)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享消息图片"))
    }
  }

  fun openForkProviderPicker(messageId: String) {
    localState.update { it.copy(forkTargetMessageId = messageId) }
  }

  fun closeForkProviderPicker() {
    localState.update { it.copy(forkTargetMessageId = null) }
  }

  fun forkConversationAtMessage(providerId: String) {
    val conversationId = uiState.value.selectedConversationId ?: return
    val messageId = uiState.value.forkTargetMessageId ?: return
    localState.update { it.copy(forkTargetMessageId = null) }
    viewModelScope.launch {
      repository.forkConversationAtMessage(conversationId, messageId, providerId)
    }
  }

  fun openFavoritePage() {
    localState.update { it.copy(favoritePageOpen = true) }
  }

  fun closeFavoritePage() {
    localState.update { it.copy(favoritePageOpen = false) }
  }

  fun openGroupChatPage(groupId: String? = uiState.value.groupChats.firstOrNull()?.id) {
    localState.update {
      it.copy(
        groupChatPageOpen = true,
        selectedGroupChatId = groupId,
        favoritePageOpen = false,
        settingsPageOpen = false
      )
    }
  }

  fun closeGroupChatPage() {
    val groupId = uiState.value.selectedGroupChatId
    localState.update {
      it.copy(
        groupChatPageOpen = false,
        selectedGroupChatId = null,
        groupInput = TextFieldValue(""),
        autoPlayingGroupIds = groupId?.let { selected -> it.autoPlayingGroupIds - selected } ?: it.autoPlayingGroupIds
      )
    }
  }

  fun selectGroupChat(groupId: String) {
    localState.update { it.copy(selectedGroupChatId = groupId, groupChatPageOpen = true) }
  }

  fun openNewGroupChatDialog() {
    localState.update { it.copy(newGroupChatDialogOpen = true) }
  }

  fun closeNewGroupChatDialog() {
    localState.update { it.copy(newGroupChatDialogOpen = false) }
  }

  fun createGroupChat(title: String, topic: String, botIds: List<String>) {
    viewModelScope.launch {
      runCatching {
        repository.createGroupChat(title, topic, botIds)
      }.onSuccess { group ->
        localState.update {
          it.copy(
            newGroupChatDialogOpen = false,
            groupChatPageOpen = true,
            selectedGroupChatId = group.id
          )
        }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "创建群聊失败") }
      }
    }
  }

  fun sendGroupUserMessage() {
    val state = uiState.value
    val groupId = state.selectedGroupChatId ?: return
    val text = state.groupInput.text
    val attachments = state.pendingAttachments
    if (text.isBlank() && attachments.isEmpty()) return
    localState.update { it.copy(groupInput = TextFieldValue(""), pendingAttachments = emptyList()) }
    viewModelScope.launch {
      repository.sendGroupUserMessage(groupId, text, attachments)
    }
  }

  fun sendGroupBotTurn(botId: String, summarize: Boolean = false) {
    val groupId = uiState.value.selectedGroupChatId ?: return
    if (groupJobsByGroupId[groupId]?.isActive == true) return
    launchGroupBotTurn(groupId, botId, summarize, continueAutoPlay = false)
  }

  private fun launchGroupBotTurn(
    groupId: String,
    botId: String,
    summarize: Boolean,
    continueAutoPlay: Boolean
  ) {
    if (groupJobsByGroupId[groupId]?.isActive == true) return
    launchGroupStreamingJob(
      groupId = groupId,
      onCompleted = { completed ->
        if (continueAutoPlay) {
          val turnCompleted = completed && lastGroupTurnCompletedByGroupId.remove(groupId) == true
          continueGroupAutoPlayIfNeeded(groupId, turnCompleted, botId)
        } else {
          lastGroupTurnCompletedByGroupId.remove(groupId)
        }
      }
    ) {
      lastGroupTurnCompletedByGroupId[groupId] =
        repository.sendGroupBotTurn(groupId, botId, summarize) == MessageStatus.COMPLETE
    }
  }

  private fun continueGroupAutoPlayIfNeeded(groupId: String, completed: Boolean, lastBotId: String) {
    if (!completed) {
      pauseGroupAutoPlay(groupId)
      return
    }
    val state = uiState.value
    if (groupId !in state.autoPlayingGroupIds) return
    val nextBotId = nextAutoPlayBotIdAfter(groupId, lastBotId)
    if (nextBotId == null) {
      pauseGroupAutoPlay(groupId)
      return
    }
    launchGroupBotTurn(groupId, nextBotId, summarize = false, continueAutoPlay = true)
  }

  private fun enabledGroupBotsForCurrentState(groupId: String): List<AiBot> {
    val state = uiState.value
    val memberOrder = state.groupMembers
      .filter { it.groupId == groupId && it.enabled }
      .associate { it.botId to it.sortOrder }
    if (memberOrder.isEmpty()) return emptyList()
    return state.aiBots
      .filter { it.enabled && it.id in memberOrder }
      .sortedWith(compareBy<AiBot> { bot -> memberOrder[bot.id] ?: Int.MAX_VALUE }.thenBy { it.name })
  }

  private fun nextAutoPlayBotId(groupId: String): String? {
    return nextGroupAutoPlayBotId(
      bots = enabledGroupBotsForCurrentState(groupId),
      messages = uiState.value.groupMessages.filter { it.groupId == groupId }
    )
  }

  private fun nextAutoPlayBotIdAfter(groupId: String, lastBotId: String): String? {
    val bots = enabledGroupBotsForCurrentState(groupId)
    if (bots.isEmpty()) return null
    val lastIndex = bots.indexOfFirst { it.id == lastBotId }
    return bots[((lastIndex + 1).coerceAtLeast(0)) % bots.size].id
  }

  fun openBotManager() {
    localState.update { it.copy(botManagerOpen = true, settingsPageOpen = false) }
  }

  fun closeBotManager() {
    localState.update { it.copy(botManagerOpen = false) }
  }

  fun createAiBot(name: String, providerId: String, model: String, systemPrompt: String) {
    viewModelScope.launch {
      runCatching {
        repository.createAiBot(name, providerId, model, systemPrompt)
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "创建机器人失败") }
      }
    }
  }

  fun updateAiBot(botId: String, name: String, providerId: String, model: String, systemPrompt: String) {
    viewModelScope.launch {
      runCatching {
        repository.updateAiBot(botId, name, providerId, model, systemPrompt)
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "更新机器人失败") }
      }
    }
  }

  fun setAiBotEnabled(botId: String, enabled: Boolean) {
    viewModelScope.launch { repository.setAiBotEnabled(botId, enabled) }
  }

  fun deleteAiBot(botId: String) {
    viewModelScope.launch { repository.deleteAiBot(botId) }
  }

  fun saveFavoriteSnippet(messageIds: Set<String>, title: String, description: String, tags: String) {
    val conversationId = uiState.value.selectedConversationId ?: return
    viewModelScope.launch {
      runCatching {
        repository.createFavoriteSnippet(conversationId, messageIds, title, description, tags)
      }.onSuccess {
        localState.update { state -> state.copy(error = "已收藏到收藏夹") }
      }.onFailure { error ->
        localState.update { state -> state.copy(error = error.message ?: "收藏失败") }
      }
    }
  }

  fun saveGroupFavoriteSnippet(messageIds: Set<String>, title: String, description: String, tags: String) {
    val groupId = uiState.value.selectedGroupChatId ?: return
    viewModelScope.launch {
      runCatching {
        repository.createFavoriteSnippetFromGroupMessages(groupId, messageIds, title, description, tags)
      }.onSuccess {
        localState.update { state -> state.copy(error = "已收藏到收藏夹") }
      }.onFailure { error ->
        localState.update { state -> state.copy(error = error.message ?: "收藏失败") }
      }
    }
  }

  fun updateFavoriteSnippet(favoriteId: String, title: String, description: String, tags: String) {
    viewModelScope.launch {
      runCatching {
        repository.updateFavoriteSnippetMetadata(favoriteId, title, description, tags)
      }.onSuccess {
        localState.update { state -> state.copy(error = "收藏已更新") }
      }.onFailure { error ->
        localState.update { state -> state.copy(error = error.message ?: "更新收藏失败") }
      }
    }
  }

  fun appendSelectedMessagesToFavorite(favoriteId: String) {
    val state = uiState.value
    val conversationId = state.selectedConversationId ?: return
    val selectedIds = state.selectedMessageIds
    viewModelScope.launch {
      runCatching {
        repository.appendMessagesToFavoriteSnippet(favoriteId, conversationId, selectedIds)
      }.onSuccess {
        localState.update { current -> current.copy(error = "已追加到收藏") }
      }.onFailure { error ->
        localState.update { current -> current.copy(error = error.message ?: "追加收藏失败") }
      }
    }
  }

  fun removeMessageFromFavorite(favoriteId: String, messageId: String) {
    viewModelScope.launch {
      runCatching {
        repository.removeMessagesFromFavoriteSnippet(favoriteId, setOf(messageId))
      }.onSuccess {
        localState.update { current -> current.copy(error = "已从收藏移除") }
      }.onFailure { error ->
        localState.update { current -> current.copy(error = error.message ?: "移除收藏消息失败") }
      }
    }
  }

  fun deleteFavoriteSnippet(favoriteId: String) {
    viewModelScope.launch {
      repository.deleteFavoriteSnippet(favoriteId)
    }
  }

  fun copyFavoriteSnippetText(favoriteId: String, context: Context) {
    viewModelScope.launch {
      val text = repository.favoriteSnippetShareText(favoriteId)
      if (text.isBlank()) return@launch
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText("AIChat 收藏", text))
      localState.update { it.copy(error = "收藏内容已复制") }
    }
  }

  fun shareFavoriteSnippetText(favoriteId: String, context: Context) {
    viewModelScope.launch {
      val shareText = repository.favoriteSnippetShareText(favoriteId)
      if (shareText.isBlank()) return@launch
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享收藏文本"))
    }
  }

  fun shareFavoriteSnippetLongImage(favoriteId: String, context: Context) {
    viewModelScope.launch {
      val export = repository.favoriteSnippetExport(favoriteId) ?: return@launch
      val uri = ConversationShareRenderer.saveImageToGallery(context, export)
        ?: ConversationShareRenderer.writeImageExport(context, export)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享收藏长图"))
    }
  }

  fun jumpToFavoriteSource(favorite: FavoriteSnippet) {
    viewModelScope.launch {
      val source = repository.conversationById(favorite.sourceConversationId)
      if (source == null || source.isDeleted) {
        localState.update { it.copy(error = "来源对话不可用") }
        return@launch
      }
      val activeSource = if (source.isArchived) {
        repository.restoreConversation(source.id)
      } else {
        source
      }
      preferencesRepository.setSelectedConversation(activeSource.id)
      preferencesRepository.setSelectedProvider(activeSource.providerId)
      localState.update { it.copy(favoritePageOpen = false) }
    }
  }

  fun clearError() {
    localState.update { it.copy(error = null) }
  }

  private suspend fun shareTextInternal(conversationId: String, selectedIds: Set<String>, context: Context) {
    val shareText = if (selectedIds.isEmpty()) {
      repository.conversationShareText(conversationId)
    } else {
      repository.conversationShareText(conversationId, selectedIds)
    }
    if (shareText.isBlank()) return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(sendIntent, if (selectedIds.isEmpty()) "分享对话" else "分享选中消息"))
  }

  fun updateConversationMeta(conversationId: String, title: String, groupName: String) {
    viewModelScope.launch {
      repository.updateConversationMeta(conversationId, title, groupName)
    }
  }

  fun renameConversationGroup(oldGroupName: String, newGroupName: String) {
    viewModelScope.launch {
      repository.renameConversationGroup(oldGroupName, newGroupName)
    }
  }

  fun selectProvider(id: String) {
    viewModelScope.launch {
      repository.switchConversationProvider(uiState.value.selectedConversationId, id)
    }
  }

  fun newConversation() {
    val provider = uiState.value.selectedProvider ?: return
    viewModelScope.launch {
      val conversation = repository.createConversation(provider.id, provider.defaultModel)
      preferencesRepository.setSelectedProvider(provider.id)
      preferencesRepository.setSelectedConversation(conversation.id)
    }
  }

  fun openNewConversationPicker() {
    localState.update { it.copy(newConversationPickerOpen = true) }
  }

  fun closeNewConversationPicker() {
    localState.update { it.copy(newConversationPickerOpen = false) }
  }

  fun createConversationWithProvider(providerId: String) {
    val provider = uiState.value.providers.firstOrNull { it.id == providerId } ?: return
    localState.update { it.copy(newConversationPickerOpen = false) }
    viewModelScope.launch {
      val conversation = repository.createConversation(provider.id, provider.defaultModel)
      preferencesRepository.setSelectedProvider(provider.id)
      preferencesRepository.setSelectedConversation(conversation.id)
    }
  }

  fun openSettingsPage() {
    localState.update {
      it.copy(
        settingsPageOpen = true,
        providerManagerOpen = false,
        settingsOpen = false,
        editingProvider = null,
        editingProviderHasApiKey = false
      )
    }
  }

  fun closeSettingsPage() {
    localState.update { it.copy(settingsPageOpen = false) }
  }

  fun setThemePalette(palette: AppThemePalette) {
    viewModelScope.launch {
      preferencesRepository.setThemePalette(palette)
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch {
      preferencesRepository.setThemeMode(mode)
    }
  }

  fun setFontScale(scale: Float) {
    viewModelScope.launch {
      preferencesRepository.setFontScale(scale)
    }
  }

  fun setDebugResponseLogging(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setDebugResponseLogging(enabled)
    }
  }

  fun setWebSearchMode(mode: WebSearchMode) {
    viewModelScope.launch {
      preferencesRepository.setWebSearchMode(mode)
    }
  }

  fun exportProviderConfigsText(context: Context) {
    viewModelScope.launch {
      val text = repository.exportProvidersText(includeApiKeys = true)
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText("AIChat API 配置", text))
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, text)
      }
      context.startActivity(Intent.createChooser(sendIntent, "导出 API 配置文本"))
    }
  }

  fun importProviderConfigsText(text: String) {
    viewModelScope.launch {
      repository.importProvidersText(text)
    }
  }

  fun openProviderManager() {
    localState.update { it.copy(providerManagerOpen = true) }
  }

  fun closeProviderManager() {
    localState.update { it.copy(providerManagerOpen = false) }
  }

  fun openSettings(provider: ChatProviderConfig? = uiState.value.selectedProvider) {
    localState.update {
      it.copy(
        providerManagerOpen = false,
        settingsOpen = true,
        editingProvider = provider,
        editingProviderHasApiKey = repository.hasApiKey(provider)
      )
    }
  }

  fun closeSettings() {
    localState.update {
      it.copy(
        settingsOpen = false,
        editingProvider = null,
        editingProviderHasApiKey = false
      )
    }
  }

  fun createProvider(type: ProviderType) {
    val template = repository.providerTemplate(type)
    localState.update {
      it.copy(
        providerManagerOpen = false,
        settingsOpen = true,
        editingProvider = template,
        editingProviderHasApiKey = false
      )
    }
  }

  fun cloneProvider(providerId: String) {
    viewModelScope.launch {
      val clone = repository.cloneProvider(providerId) ?: return@launch
      preferencesRepository.setSelectedProvider(clone.id)
      localState.update {
        it.copy(
          providerManagerOpen = false,
          settingsOpen = true,
          editingProvider = clone,
          editingProviderHasApiKey = false
        )
      }
    }
  }

  fun deleteProvider(providerId: String) {
    viewModelScope.launch {
      val result = repository.deleteProvider(providerId)
      if (!result.deleted) {
        val botNames = result.blockingBots.joinToString("、") { it.name }
        localState.update {
          it.copy(
            providerRebindDeleteSourceId = providerId,
            providerRebindDeleteBotIds = result.blockingBots.map { bot -> bot.id },
            error = "无法直接删除这个 API 配置，因为机器人「$botNames」正在使用它。你可以先删除这些机器人，或选择一个现有 API 配置并批量改绑后删除。"
          )
        }
        return@launch
      }
      localState.update { it.copy(error = "API 配置已删除") }
    }
  }

  fun cancelProviderRebindDelete() {
    localState.update {
      it.copy(
        providerRebindDeleteSourceId = null,
        providerRebindDeleteBotIds = emptyList()
      )
    }
  }

  fun rebindProviderBotsAndDelete(targetProviderId: String) {
    val sourceProviderId = uiState.value.providerRebindDeleteSourceId ?: return
    viewModelScope.launch {
      runCatching {
        repository.rebindProviderBotsAndDelete(sourceProviderId, targetProviderId)
      }.onSuccess {
        localState.update {
          it.copy(
            providerRebindDeleteSourceId = null,
            providerRebindDeleteBotIds = emptyList(),
            error = "已改绑机器人并删除 API 配置"
          )
        }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "改绑并删除 API 配置失败") }
      }
    }
  }

  fun saveProvider(provider: ChatProviderConfig, apiKey: String?) {
    viewModelScope.launch {
      repository.saveProvider(provider, apiKey)
      preferencesRepository.setSelectedProvider(provider.id)
      closeSettings()
    }
  }

  companion object {
    fun factory(
      repository: ChatRepository,
      preferencesRepository: ChatSelectionStore,
      appContext: Context? = null
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(repository, preferencesRepository, appContext) as T
      }
    }
  }
}

private fun List<ChatConversation>.toConversationGroups(): List<ChatConversationGroup> {
  return groupBy { it.groupName.ifBlank { "默认" } }
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

private data class ConversationLists(
  val active: List<ChatConversation>,
  val archived: List<ChatConversation>,
  val favorites: List<FavoriteSnippet>,
  val aiBots: List<AiBot>,
  val groupChats: List<GroupChatRoom>
)

internal fun nextGroupAutoPlayBotId(
  bots: List<AiBot>,
  messages: List<GroupChatMessage>
): String? {
  if (bots.isEmpty()) return null
  val lastBotId = messages
    .asReversed()
    .firstOrNull { it.senderType == GroupMessageSenderType.BOT && it.botId != null }
    ?.botId
  val lastIndex = bots.indexOfFirst { it.id == lastBotId }
  return bots[((lastIndex + 1).coerceAtLeast(0)) % bots.size].id
}
