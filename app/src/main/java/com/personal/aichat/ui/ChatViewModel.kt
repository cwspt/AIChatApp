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
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.ChatProviderConfig
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
  private var pendingDeleteConversationId: String? = null

  private val conversationLists = combine(
    repository.conversations,
    repository.archivedConversations
  ) { conversations, archivedConversations ->
    ConversationLists(conversations, archivedConversations)
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
      selectedConversationId = effectiveConversationId,
      selectedProviderId = effectiveProviderId
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val uiState = baseUiState.combine(preferencesRepository.appSettings) { state, appSettings ->
    state.copy(appSettings = appSettings)
  }.flatMapLatest { state ->
    val conversationId = state.selectedConversationId
    if (conversationId == null) {
      flowOf(state)
    } else {
      repository.observeMessages(conversationId).combine(flowOf(state)) { messages, current ->
        current.copy(messages = messages)
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

  fun addAttachments(uris: List<Uri>) {
    val context = appContext ?: return
    if (uiState.value.selectedProvider?.supportsAttachments != true) return
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
  val archived: List<ChatConversation>
)
