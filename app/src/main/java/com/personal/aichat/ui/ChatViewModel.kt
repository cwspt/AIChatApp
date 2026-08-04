package com.personal.aichat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personal.aichat.AppForegroundTracker
import com.personal.aichat.ChatGenerationService
import com.personal.aichat.data.ChatRepository
import com.personal.aichat.data.ChatSelectionStore
import com.personal.aichat.data.ConversationExport
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.AppSettings
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.ChatBackgroundPreset
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ConversationType
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.GroupChatMember
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.GroupAutoPlayPreference
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.GroupTurnTrigger
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.domain.ImageGenerationBackground
import com.personal.aichat.domain.ImageGenerationOutputFormat
import com.personal.aichat.domain.ImageGenerationQuality
import com.personal.aichat.domain.ImageGenerationSize
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.StreamingBubbleMotion
import com.personal.aichat.domain.WebSearchMode
import com.personal.aichat.domain.groupAutoPlayPreference
import com.personal.aichat.domain.supportsAttachmentsForModel
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import java.util.UUID

private data class BackgroundPresetExportPayload(
  val version: Int = 1,
  val presets: List<ChatBackgroundPreset> = emptyList()
)

private data class GroupAutoPlaySession(
  val completedTurns: Int = 0,
  val retriedBotId: String? = null
)

private data class PendingImageExportPayload(
  val export: ConversationExport,
  val chooserTitle: String
)

class ChatViewModel(
  private val repository: ChatRepository,
  private val preferencesRepository: ChatSelectionStore,
  private val appContext: Context? = null
) : ViewModel() {
  private val localState = MutableStateFlow(ChatUiState())
  private val sendJobsByConversationId = mutableMapOf<String, Job>()
  private val groupJobsByGroupId = mutableMapOf<String, Job>()
  private val lastGroupTurnCompletedByGroupId = mutableMapOf<String, Boolean>()
  private val groupAutoPlaySessionsByGroupId = mutableMapOf<String, GroupAutoPlaySession>()
  private var pendingDeleteConversationId: String? = null
  private var pendingImageExport: PendingImageExportPayload? = null
  private var imageExportJob: Job? = null

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
        val memberOrder = members.associate { it.botId to it.sortOrder }
        val bots = state.aiBots
          .filter { it.enabled && it.id in memberOrder }
          .sortedWith(compareBy<AiBot> { bot -> memberOrder[bot.id] ?: Int.MAX_VALUE }.thenBy { it.name })
        val estimateBotId = nextGroupAutoPlayBotId(bots, groupMessages) ?: bots.firstOrNull()?.id
        state.copy(
          groupMessages = groupMessages,
          groupMembers = members,
          selectedGroupContextCapacity = repository.estimateGroupContextCapacity(groupId, estimateBotId),
          selectedContextCapacity = null
        )
      }
    } else if (conversationId == null) {
      flowOf(state.copy(groupMessages = emptyList(), groupMembers = emptyList(), selectedContextCapacity = null, selectedGroupContextCapacity = null))
    } else {
      repository.observeMessages(conversationId).combine(flowOf(state)) { messages, current ->
        current.copy(
          messages = messages,
          groupMessages = emptyList(),
          groupMembers = emptyList(),
          selectedContextCapacity = repository.estimateConversationContextCapacity(conversationId),
          selectedGroupContextCapacity = null
        )
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
    val state = uiState.value
    if (!state.groupChatPageOpen &&
      state.selectedProvider?.supportsAttachmentsForModel(state.selectedConversation?.model.orEmpty()) != true
    ) return
    viewModelScope.launch {
      val settings = uiState.value.appSettings
      val attempts = uris.map { uri -> importAttachment(context, uri, settings) }
      val imported = attempts.mapNotNull { it.attachment }
      val importErrorCount = attempts.count { it.attachment == null }
      val result = appendAttachmentsWithinLimit(uiState.value.pendingAttachments, imported, settings)
      localState.update {
        it.copy(
          pendingAttachments = result.attachments,
          error = attachmentImportMessage(importErrorCount + result.skippedCount, uris.size, settings)
        )
      }
    }
  }

  fun removePendingAttachment(id: String) {
    localState.update { state ->
      state.pendingAttachments.firstOrNull { it.id == id }?.let { attachment ->
        deleteAttachmentFiles(attachment)
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

  fun handleIncomingShareIntent(intent: Intent?) {
    val context = appContext ?: return
    val payload = intent?.toIncomingSharePayload() ?: return
    viewModelScope.launch {
      val settings = uiState.value.appSettings
      val attempts = payload.uris.map { uri -> importAttachment(context, uri, settings) }
      val imported = attempts.mapNotNull { it.attachment }
      val appendResult = appendAttachmentsWithinLimit(emptyList(), imported, settings)
      val failedCount = attempts.count { it.attachment == null } + appendResult.skippedCount
      if (payload.text.isBlank() && appendResult.attachments.isEmpty()) {
        if (failedCount > 0) {
          localState.update { it.copy(error = "分享文件过大或导入失败") }
        }
        return@launch
      }
      localState.update {
        it.copy(
          incomingShareDraft = IncomingShareDraft(
            text = payload.text,
            attachments = appendResult.attachments,
            failedCount = failedCount,
            open = true
          ),
          error = attachmentImportMessage(failedCount, payload.uris.size, settings),
          favoritePageOpen = false,
          settingsPageOpen = false,
          providerManagerOpen = false,
          newConversationPickerOpen = false,
          forkTargetMessageId = null
        )
      }
    }
  }

  fun dismissIncomingShareDraft() {
    localState.update { state ->
      state.incomingShareDraft?.attachments.orEmpty().forEach { attachment ->
        deleteAttachmentFiles(attachment)
      }
      state.copy(incomingShareDraft = null)
    }
  }

  fun applyIncomingShareToConversation(conversationId: String) {
    val state = uiState.value
    val draft = state.incomingShareDraft?.takeIf { it.hasContent } ?: return
    val conversation = state.conversations.firstOrNull { it.id == conversationId } ?: return
    val provider = state.providers.firstOrNull { it.id == conversation.providerId }
    if (draft.hasAttachments && provider?.supportsAttachmentsForModel(conversation.model) != true) {
      localState.update { it.copy(error = "该对话的模型不支持附件") }
      return
    }
    viewModelScope.launch {
      preferencesRepository.setSelectedConversation(conversation.id)
      preferencesRepository.setSelectedProvider(conversation.providerId)
      localState.update {
        it.copy(
          incomingShareDraft = null,
          groupChatPageOpen = false,
          selectedGroupChatId = null,
          input = appendDraftText(it.input, draft.text),
          pendingAttachments = it.pendingAttachments + draft.attachments
        )
      }
    }
  }

  fun applyIncomingShareToGroup(groupId: String) {
    val draft = uiState.value.incomingShareDraft?.takeIf { it.hasContent } ?: return
    localState.update {
      it.copy(
        incomingShareDraft = null,
        groupChatPageOpen = true,
        selectedGroupChatId = groupId,
        groupInput = appendDraftText(it.groupInput, draft.text),
        pendingAttachments = it.pendingAttachments + draft.attachments
      )
    }
  }

  fun createConversationForIncomingShare(providerId: String) {
    val draft = uiState.value.incomingShareDraft?.takeIf { it.hasContent } ?: return
    val provider = uiState.value.providers.firstOrNull { it.id == providerId } ?: return
    if (draft.hasAttachments && !provider.supportsAttachmentsForModel(provider.defaultModel)) {
      localState.update { it.copy(error = "该模型不支持附件") }
      return
    }
    viewModelScope.launch {
      val conversation = repository.createConversation(provider.id, provider.defaultModel)
      preferencesRepository.setSelectedProvider(provider.id)
      preferencesRepository.setSelectedConversation(conversation.id)
      localState.update {
        it.copy(
          incomingShareDraft = null,
          groupChatPageOpen = false,
          selectedGroupChatId = null,
          input = appendDraftText(it.input, draft.text),
          pendingAttachments = it.pendingAttachments + draft.attachments
        )
      }
    }
  }

  fun setMessagesSelected(messageIds: Set<String>, selected: Boolean) {
    if (messageIds.isEmpty()) return
    localState.update { state ->
      val next = state.selectedMessageIds.toMutableSet()
      if (selected) {
        next += messageIds
      } else {
        next -= messageIds
      }
      state.copy(
        messageSelectionMode = next.isNotEmpty() || state.messageSelectionMode,
        selectedMessageIds = next
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
    val messageIds = if (state.groupChatPageOpen) {
      state.groupMessages.map { it.id }
    } else {
      state.messages.map { it.id }
    }
    val targetIndex = messageIds.indexOf(messageId)
    if (targetIndex < 0) return
    val anchorIndex = messageIds.indexOfLast { it in selectedIds }.takeIf { it >= 0 } ?: targetIndex
    val range = if (anchorIndex <= targetIndex) anchorIndex..targetIndex else targetIndex..anchorIndex
    val rangeIds = range.map { messageIds[it] }
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
    val isImageConversation = state.selectedConversation?.type == ConversationType.IMAGE
    val selectedProvider = state.selectedProvider
    val inlineImagesRequested = !isImageConversation &&
      state.inlineImagesAllowedForNextSend &&
      selectedProvider?.type == com.personal.aichat.domain.ProviderType.OPENAI_RESPONSES &&
      selectedProvider.supportsImageGeneration &&
      selectedProvider.imageGenerationApiMode == com.personal.aichat.domain.ImageGenerationApiMode.RESPONSES_TOOL
    val attachments = when {
      isImageConversation -> state.pendingAttachments.filter { it.isImage }
      selectedProvider?.supportsAttachmentsForModel(state.selectedConversation?.model.orEmpty()) == true -> state.pendingAttachments
      else -> emptyList()
    }
    if ((text.isBlank() && attachments.isEmpty()) || sendJobsByConversationId[conversationId]?.isActive == true) return
    localState.update {
      it.copy(
        input = TextFieldValue(""),
        pendingAttachments = emptyList(),
        inlineImagesAllowedForNextSend = false
      )
    }
    launchStreamingJob(conversationId) {
      if (isImageConversation) {
        repository.sendImageMessage(conversationId, text, attachments, state.imageGenerationOptions)
      } else {
        repository.sendMessage(
          conversationId,
          text,
          attachments,
          inlineImagesRequested = inlineImagesRequested
        )
      }
    }
  }

  fun setInlineImagesAllowedForNextSend(enabled: Boolean) {
    localState.update { it.copy(inlineImagesAllowedForNextSend = enabled) }
  }

  fun setImageGenerationSize(size: ImageGenerationSize) {
    localState.update { it.copy(imageGenerationOptions = it.imageGenerationOptions.copy(size = size)) }
  }

  fun setImageGenerationQuality(quality: ImageGenerationQuality) {
    localState.update { it.copy(imageGenerationOptions = it.imageGenerationOptions.copy(quality = quality)) }
  }

  fun setImageGenerationCount(count: Int) {
    localState.update { it.copy(imageGenerationOptions = it.imageGenerationOptions.copy(count = count.coerceIn(1, 4))) }
  }

  fun setImageGenerationOutputFormat(format: ImageGenerationOutputFormat) {
    localState.update { it.copy(imageGenerationOptions = it.imageGenerationOptions.copy(outputFormat = format)) }
  }

  fun setImageGenerationBackground(background: ImageGenerationBackground) {
    localState.update { it.copy(imageGenerationOptions = it.imageGenerationOptions.copy(background = background)) }
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

  fun compressSelectedConversationContext() {
    val conversationId = uiState.value.selectedConversationId ?: return
    if (conversationId in uiState.value.compressingConversationIds) return
    localState.update { it.copy(compressingConversationIds = it.compressingConversationIds + conversationId) }
    viewModelScope.launch {
      runCatching {
        repository.compressConversationContext(conversationId)
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "压缩上下文失败") }
      }
      localState.update { it.copy(compressingConversationIds = it.compressingConversationIds - conversationId) }
    }
  }

  fun stopGroupGenerating() {
    val groupId = uiState.value.selectedGroupChatId ?: return
    groupJobsByGroupId[groupId]?.cancel()
  }

  fun compressSelectedGroupContext() {
    val state = uiState.value
    val groupId = state.selectedGroupChatId ?: return
    if (groupId in state.compressingGroupIds) return
    val botId = nextAutoPlayBotId(groupId) ?: enabledGroupBotsForCurrentState(groupId).firstOrNull()?.id
    localState.update { it.copy(compressingGroupIds = it.compressingGroupIds + groupId) }
    viewModelScope.launch {
      runCatching {
        repository.compressGroupContext(groupId, botId)
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "压缩群聊上下文失败") }
      }
      localState.update { it.copy(compressingGroupIds = it.compressingGroupIds - groupId) }
    }
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
    groupAutoPlaySessionsByGroupId[targetGroupId] = GroupAutoPlaySession()
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
    groupAutoPlaySessionsByGroupId.remove(targetGroupId)
    localState.update { it.copy(autoPlayingGroupIds = it.autoPlayingGroupIds - targetGroupId) }
  }

  fun setGroupAutoPlayPreference(groupId: String, preference: GroupAutoPlayPreference) {
    viewModelScope.launch {
      preferencesRepository.setGroupAutoPlayPreference(groupId, preference)
    }
  }

  private fun importAttachment(context: Context, uri: Uri, settings: AppSettings): AttachmentImportAttempt {
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
      val resolvedMimeType = resolver.getType(uri).orEmpty()
      val guessedMimeType = guessMimeType(info.displayName)
      val mimeType = when {
        resolvedMimeType.isBlank() -> guessedMimeType ?: "application/octet-stream"
        resolvedMimeType == "application/octet-stream" && guessedMimeType?.startsWith("image/") == true -> guessedMimeType
        else -> resolvedMimeType
      }
      val id = "att_${UUID.randomUUID().toString().replace("-", "")}"
      val displayName = info.displayName?.takeIf { it.isNotBlank() } ?: "$id.${mimeType.substringAfter('/', "bin")}"
      val isImageAttachment = isImageAttachment(mimeType, displayName)
      val maxAttachmentBytes = settings.maxAttachmentBytes()
      val maxSourceBytes = if (isImageAttachment) settings.maxImageSourceBytes() else maxAttachmentBytes
      if ((info.sizeBytes ?: 0L) > maxSourceBytes) {
        return@runCatching AttachmentImportAttempt(errorMessage = "${displayName} 超过 ${formatImportLimit(maxSourceBytes)}")
      }
      val dir = File(context.filesDir, "chat_attachments").apply { mkdirs() }
      val safeName = displayName.replace(Regex("""[\\/:*?"<>|]"""), "_")
      val target = File(dir, "${id}_$safeName")
      resolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
      } ?: return@runCatching AttachmentImportAttempt(errorMessage = "${displayName} 导入失败")
      val actualSize = target.length()
      if (actualSize > maxSourceBytes) {
        runCatching { target.delete() }
        return@runCatching AttachmentImportAttempt(errorMessage = "${displayName} 超过 ${formatImportLimit(maxSourceBytes)}")
      }
      val compressedPayload = if (isImageAttachment) {
        compressedImagePayload(target, id, maxAttachmentBytes)
      } else {
        null
      }
      val payloadSize = compressedPayload?.sizeBytes ?: actualSize
      if (payloadSize > maxAttachmentBytes) {
        runCatching { target.delete() }
        compressedPayload?.let { payload -> runCatching { payload.file.delete() } }
        return@runCatching AttachmentImportAttempt(errorMessage = "${displayName} 超过 ${formatImportLimit(maxAttachmentBytes)}")
      }
      AttachmentImportAttempt(
        attachment = ChatAttachment(
          id = id,
          displayName = displayName,
          mimeType = mimeType,
          sizeBytes = actualSize,
          localPath = target.absolutePath,
          transmitLocalPath = compressedPayload?.file?.absolutePath,
          transmitMimeType = compressedPayload?.mimeType,
          transmitSizeBytes = compressedPayload?.sizeBytes
        )
      )
    }.getOrDefault(AttachmentImportAttempt(errorMessage = "附件导入失败"))
  }

  private fun compressedImagePayload(source: File, id: String, maxAttachmentBytes: Long): CompressedImagePayload? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.absolutePath, bounds)
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null
    val longestEdge = max(width, height)
    val preferredUploadBytes = minOf(PreferredImageUploadBytes, maxAttachmentBytes)
    val shouldCompress = longestEdge > MaxImageUploadEdgePx || source.length() > preferredUploadBytes
    if (!shouldCompress) return null

    val sampleSize = imageSampleSize(width, height, MaxImageUploadEdgePx)
    val decoded = BitmapFactory.decodeFile(
      source.absolutePath,
      BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return null
    val scaled = if (max(decoded.width, decoded.height) > MaxImageUploadEdgePx) {
      val scale = MaxImageUploadEdgePx.toFloat() / max(decoded.width, decoded.height).toFloat()
      Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).roundToInt().coerceAtLeast(1),
        (decoded.height * scale).roundToInt().coerceAtLeast(1),
        true
      )
    } else {
      decoded
    }

    val hasAlpha = scaled.hasAlpha()
    val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    val outputMimeType = if (hasAlpha) "image/png" else "image/jpeg"
    val extension = if (hasAlpha) "png" else "jpg"
    val output = File(source.parentFile, "${id}_upload.$extension")
    val qualities = if (format == Bitmap.CompressFormat.JPEG) listOf(86, 78, 70, 62) else listOf(100)
    qualities.forEach { quality ->
      output.outputStream().use { stream ->
        scaled.compress(format, quality, stream)
      }
      if (output.length() <= maxAttachmentBytes || quality == qualities.last()) {
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return if (output.length() < source.length() || source.length() > maxAttachmentBytes) {
          CompressedImagePayload(output, outputMimeType, output.length())
        } else {
          runCatching { output.delete() }
          null
        }
      }
    }
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()
    runCatching { output.delete() }
    return null
  }

  private fun imageSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sampleSize = 1
    while (max(width / sampleSize, height / sampleSize) > maxEdge * 2) {
      sampleSize *= 2
    }
    return sampleSize
  }

  private fun isImageAttachment(mimeType: String, displayName: String): Boolean {
    if (mimeType.startsWith("image/")) return true
    val lowerName = displayName.lowercase()
    return lowerName.endsWith(".jpg") ||
      lowerName.endsWith(".jpeg") ||
      lowerName.endsWith(".png") ||
      lowerName.endsWith(".webp") ||
      lowerName.endsWith(".heic") ||
      lowerName.endsWith(".heif")
  }

  private fun guessMimeType(displayName: String?): String? {
    val lowerName = displayName?.lowercase().orEmpty()
    return when {
      lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> "image/jpeg"
      lowerName.endsWith(".png") -> "image/png"
      lowerName.endsWith(".webp") -> "image/webp"
      lowerName.endsWith(".heic") -> "image/heic"
      lowerName.endsWith(".heif") -> "image/heif"
      lowerName.endsWith(".pdf") -> "application/pdf"
      lowerName.endsWith(".txt") -> "text/plain"
      lowerName.endsWith(".md") -> "text/markdown"
      else -> null
    }
  }

  private fun deleteAttachmentFiles(attachment: ChatAttachment) {
    runCatching { File(attachment.localPath).delete() }
    attachment.transmitLocalPath
      ?.takeIf { it.isNotBlank() && it != attachment.localPath }
      ?.let { path -> runCatching { File(path).delete() } }
  }

  private fun appendAttachmentsWithinLimit(
    existing: List<ChatAttachment>,
    incoming: List<ChatAttachment>,
    settings: AppSettings
  ): AttachmentAppendResult {
    val maxPendingAttachmentBytes = settings.maxPendingAttachmentBytes()
    var totalSize = existing.sumOf { it.payloadSizeBytes }
    val accepted = existing.toMutableList()
    var skipped = 0
    incoming.forEach { attachment ->
      if (totalSize + attachment.payloadSizeBytes <= maxPendingAttachmentBytes) {
        accepted += attachment
        totalSize += attachment.payloadSizeBytes
      } else {
        skipped += 1
        deleteAttachmentFiles(attachment)
      }
    }
    return AttachmentAppendResult(accepted, skipped)
  }

  private fun attachmentImportMessage(failedCount: Int, totalCount: Int, settings: AppSettings): String? {
    if (failedCount <= 0) return null
    val maxAttachmentBytes = settings.maxAttachmentBytes()
    val maxImageSourceBytes = settings.maxImageSourceBytes()
    val maxPendingAttachmentBytes = settings.maxPendingAttachmentBytes()
    return if (failedCount == totalCount) {
      "附件过大或导入失败。单个附件上传上限 ${formatImportLimit(maxAttachmentBytes)}，图片原图导入上限 ${formatImportLimit(maxImageSourceBytes)}，待发送总量上限 ${formatImportLimit(maxPendingAttachmentBytes)}。"
    } else {
      "已跳过 $failedCount 个过大或导入失败的附件。单个附件上传上限 ${formatImportLimit(maxAttachmentBytes)}，图片原图导入上限 ${formatImportLimit(maxImageSourceBytes)}。"
    }
  }

  private fun formatImportLimit(bytes: Long): String {
    val mb = bytes / 1024L / 1024L
    return "${mb}MB"
  }

  private fun AppSettings.maxAttachmentBytes(): Long {
    return attachmentMaxFileMb.coerceIn(MinAttachmentFileMb, MaxAttachmentFileMb).mbToBytes()
  }

  private fun AppSettings.maxPendingAttachmentBytes(): Long {
    val fileMb = attachmentMaxFileMb.coerceIn(MinAttachmentFileMb, MaxAttachmentFileMb)
    return attachmentMaxPendingMb.coerceIn(fileMb, MaxAttachmentPendingMb).mbToBytes()
  }

  private fun AppSettings.maxImageSourceBytes(): Long {
    val fileMb = attachmentMaxFileMb.coerceIn(MinAttachmentFileMb, MaxAttachmentFileMb)
    return attachmentMaxImageSourceMb.coerceIn(fileMb, MaxAttachmentImageSourceMb).mbToBytes()
  }

  private fun Int.mbToBytes(): Long = this.toLong() * 1024L * 1024L

  private fun launchStreamingJob(conversationId: String, block: suspend () -> Unit) {
    val wasIdle = sendJobsByConversationId.isEmpty() && groupJobsByGroupId.isEmpty()
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
        if (sendJobsByConversationId.isEmpty() && groupJobsByGroupId.isEmpty()) {
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

  private data class AttachmentImportAttempt(
    val attachment: ChatAttachment? = null,
    val errorMessage: String? = null
  )

  private data class CompressedImagePayload(
    val file: File,
    val mimeType: String,
    val sizeBytes: Long
  )

  private data class AttachmentAppendResult(
    val attachments: List<ChatAttachment>,
    val skippedCount: Int
  )

  private data class IncomingSharePayload(
    val text: String,
    val uris: List<Uri>
  )

  private fun Intent.toIncomingSharePayload(): IncomingSharePayload? {
    val action = action ?: return null
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
    val uris = linkedSetOf<Uri>()
    clipData?.let { data ->
      for (index in 0 until data.itemCount) {
        data.getItemAt(index).uri?.let { uris += it }
      }
    }
    streamUriExtra()?.let { uris += it }
    streamUriListExtra()?.let { uris += it }
    val subject = getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
    val extraText = getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
    val text = listOf(subject, extraText)
      .filter { it.isNotBlank() }
      .distinct()
      .joinToString("\n\n")
    if (text.isBlank() && uris.isEmpty()) return null
    return IncomingSharePayload(text = text, uris = uris.toList())
  }

  private fun appendDraftText(current: TextFieldValue, incoming: String): TextFieldValue {
    if (incoming.isBlank()) return current
    val nextText = if (current.text.isBlank()) {
      incoming
    } else {
      "${current.text.trimEnd()}\n\n$incoming"
    }
    return TextFieldValue(nextText, selection = TextRange(nextText.length))
  }

  private fun Intent.streamUriExtra(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
      @Suppress("DEPRECATION")
      getParcelableExtra(Intent.EXTRA_STREAM)
    }
  }

  private fun Intent.streamUriListExtra(): ArrayList<Uri>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
      @Suppress("DEPRECATION")
      getParcelableArrayListExtra(Intent.EXTRA_STREAM)
    }
  }

  fun selectConversation(id: String) {
    localState.update {
      it.copy(
        groupChatPageOpen = false,
        selectedGroupChatId = null,
        inlineImagesAllowedForNextSend = false
      )
    }
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
    requestImageExport(context, "分享长图") {
      repository.conversationExport(conversationId)
    }
  }

  fun retryInlineImage(messageId: String, partId: String) {
    val conversationId = uiState.value.selectedConversationId ?: return
    if (sendJobsByConversationId[conversationId]?.isActive == true) return
    launchStreamingJob(conversationId) {
      repository.retryInlineImage(conversationId, messageId, partId)
    }
  }

  fun shareSelectedMessagesLongImage(context: Context) {
    val conversationId = uiState.value.selectedConversationId ?: return
    val selectedIds = uiState.value.selectedMessageIds
    if (selectedIds.isEmpty()) return
    requestImageExport(context, "分享选中消息长图") {
      val export = repository.conversationExport(conversationId) ?: return@requestImageExport null
      val selectedMessages = export.messages.filter { it.id in selectedIds }
      if (selectedMessages.isEmpty()) return@requestImageExport null
      export.copy(title = "${export.title}（节选）", messages = selectedMessages)
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
    requestImageExport(context, "分享消息图片") {
      repository.messageExport(conversationId, messageId)
    }
  }

  fun shareGroupChatText(context: Context) {
    val groupId = uiState.value.selectedGroupChatId ?: return
    viewModelScope.launch {
      val shareText = repository.groupChatShareText(groupId)
      if (shareText.isBlank()) return@launch
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享群聊文本"))
    }
  }

  fun shareSelectedGroupMessagesText(context: Context) {
    val state = uiState.value
    val groupId = state.selectedGroupChatId ?: return
    val selectedIds = state.selectedMessageIds
    if (selectedIds.isEmpty()) return
    viewModelScope.launch {
      val shareText = repository.groupChatShareText(groupId, selectedIds)
      if (shareText.isBlank()) return@launch
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享选中群消息"))
    }
  }

  fun shareGroupChatMarkdownFile(context: Context) {
    val state = uiState.value
    val groupId = state.selectedGroupChatId ?: return
    val selectedIds = state.selectedMessageIds
    viewModelScope.launch {
      val shareText = if (selectedIds.isEmpty()) {
        repository.groupChatShareText(groupId)
      } else {
        repository.groupChatShareText(groupId, selectedIds)
      }
      if (shareText.isBlank()) return@launch
      val title = state.selectedGroupChat?.title ?: "AIGroupChat"
      val exportTitle = if (selectedIds.isEmpty()) title else "${title}（节选）"
      val uri = ConversationShareRenderer.writeTextExport(context, exportTitle, shareText)
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享群聊 Markdown 文件"))
    }
  }

  fun shareGroupChatLongImage(context: Context) {
    val groupId = uiState.value.selectedGroupChatId ?: return
    requestImageExport(context, "分享群聊长图") {
      repository.groupChatExport(groupId)
    }
  }

  fun shareSelectedGroupMessagesLongImage(context: Context) {
    val state = uiState.value
    val groupId = state.selectedGroupChatId ?: return
    val selectedIds = state.selectedMessageIds
    if (selectedIds.isEmpty()) return
    requestImageExport(context, "分享选中群消息长图") {
      val export = repository.groupChatExport(groupId) ?: return@requestImageExport null
      val selectedMessages = export.messages.filter { it.id in selectedIds }
      if (selectedMessages.isEmpty()) return@requestImageExport null
      export.copy(title = "${export.title}（节选）", messages = selectedMessages)
    }
  }

  fun shareGroupMessageText(messageId: String, context: Context) {
    val groupId = uiState.value.selectedGroupChatId ?: return
    viewModelScope.launch {
      val shareText = repository.groupMessageShareText(groupId, messageId)
      if (shareText.isBlank()) return@launch
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
      context.startActivity(Intent.createChooser(sendIntent, "分享群消息文本"))
    }
  }

  fun shareGroupMessageImage(messageId: String, context: Context) {
    val groupId = uiState.value.selectedGroupChatId ?: return
    requestImageExport(context, "分享群消息图片") {
      repository.groupMessageExport(groupId, messageId)
    }
  }

  private fun requestImageExport(
    context: Context,
    chooserTitle: String,
    exportProvider: suspend () -> ConversationExport?
  ) {
    imageExportJob?.cancel()
    pendingImageExport = null
    localState.update { it.copy(pendingImageExportChoice = null) }
    imageExportJob = viewModelScope.launch {
      val export = exportProvider() ?: return@launch
      val plan = withContext(Dispatchers.Default) {
        ConversationShareRenderer.imageExportPlan(export)
      }
      if (plan.pageCount <= 1) {
        shareImageExport(context, export, chooserTitle, ConversationShareRenderer.ImageExportMode.PAGED)
      } else {
        pendingImageExport = PendingImageExportPayload(export, chooserTitle)
        localState.update {
          it.copy(
            pendingImageExportChoice = ImageExportChoiceState(
              pageCount = plan.pageCount,
              singleImageAllowed = plan.singleImageAllowed
            )
          )
        }
      }
    }
  }

  internal fun confirmImageExport(
    mode: ConversationShareRenderer.ImageExportMode,
    context: Context
  ) {
    val payload = pendingImageExport ?: return
    val choice = localState.value.pendingImageExportChoice ?: return
    if (mode == ConversationShareRenderer.ImageExportMode.SINGLE && !choice.singleImageAllowed) return
    imageExportJob?.cancel()
    pendingImageExport = null
    localState.update { it.copy(pendingImageExportChoice = null) }
    imageExportJob = viewModelScope.launch {
      shareImageExport(context, payload.export, payload.chooserTitle, mode)
    }
  }

  internal fun dismissImageExportChoice() {
    pendingImageExport = null
    localState.update { it.copy(pendingImageExportChoice = null) }
  }

  private suspend fun shareImageExport(
    context: Context,
    export: ConversationExport,
    chooserTitle: String,
    mode: ConversationShareRenderer.ImageExportMode
  ) {
    val uris = withContext(Dispatchers.IO) {
      runCatching {
        ConversationShareRenderer.saveImageExports(context, export, mode)
          ?: ConversationShareRenderer.writeImageExports(context, export, mode)
      }.getOrElse { error ->
        if (error is CancellationException) throw error
        emptyList()
      }
    }
    if (uris.isEmpty()) {
      localState.update {
        it.copy(
          error = if (mode == ConversationShareRenderer.ImageExportMode.SINGLE) {
            "单张长图导出失败，请改用分图"
          } else {
            "图片导出失败"
          }
        )
      }
      return
    }
    val sendIntent = Intent(
      if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
    ).apply {
      type = "image/png"
      if (uris.size == 1) {
        putExtra(Intent.EXTRA_STREAM, uris.first())
      } else {
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
      }
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
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
    localState.update { it.copy(newGroupChatDialogOpen = false, editingGroupChatId = null) }
  }

  fun openEditGroupChatDialog() {
    val groupId = uiState.value.selectedGroupChatId ?: return
    localState.update { it.copy(editingGroupChatId = groupId) }
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
        repository.sendGroupBotTurn(
          groupId = groupId,
          botId = botId,
          summarize = summarize,
          trigger = if (continueAutoPlay) GroupTurnTrigger.AUTO else GroupTurnTrigger.MANUAL
        ) == MessageStatus.COMPLETE
    }
  }

  fun updateGroupChat(groupId: String, title: String, topic: String, botIds: List<String>) {
    viewModelScope.launch {
      runCatching {
        repository.updateGroupChat(groupId, title, topic, botIds)
      }.onSuccess {
        localState.update { it.copy(editingGroupChatId = null) }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "保存群聊失败") }
      }
    }
  }

  fun deleteSelectedGroupChat() {
    val groupId = uiState.value.selectedGroupChatId ?: return
    viewModelScope.launch {
      runCatching {
        pauseGroupAutoPlay(groupId)
        repository.deleteGroupChat(groupId)
      }.onSuccess {
        val fallbackId = uiState.value.groupChats.firstOrNull { it.id != groupId }?.id
        localState.update {
          it.copy(
            selectedGroupChatId = fallbackId,
            groupChatPageOpen = fallbackId != null,
            groupMessages = emptyList(),
            groupMembers = emptyList(),
            selectedMessageIds = emptySet(),
            messageSelectionMode = false
          )
        }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "删除群聊失败") }
      }
    }
  }

  private suspend fun continueGroupAutoPlayIfNeeded(groupId: String, completed: Boolean, lastBotId: String) {
    val state = uiState.value
    if (groupId !in state.autoPlayingGroupIds) return
    val preference = state.appSettings.groupAutoPlayPreference(groupId)
    val bots = enabledGroupBotsForCurrentState(groupId)
    if (bots.isEmpty()) {
      pauseGroupAutoPlay(groupId)
      return
    }
    val session = groupAutoPlaySessionsByGroupId[groupId] ?: GroupAutoPlaySession()
    if (!completed) {
      if (preference.retryFailedTurn && session.retriedBotId != lastBotId) {
        groupAutoPlaySessionsByGroupId[groupId] = session.copy(retriedBotId = lastBotId)
        delayGroupAutoPlay(preference)
        if (groupId in uiState.value.autoPlayingGroupIds && groupJobsByGroupId[groupId]?.isActive != true) {
          launchGroupBotTurn(groupId, lastBotId, summarize = false, continueAutoPlay = true)
        }
      } else {
        pauseGroupAutoPlay(groupId)
      }
      return
    }
    val completedTurns = session.completedTurns + 1
    val maxTurns = preference.maxRounds.takeIf { it > 0 }?.let { it * bots.size }
    if (maxTurns != null && completedTurns >= maxTurns) {
      groupAutoPlaySessionsByGroupId[groupId] = session.copy(completedTurns = completedTurns, retriedBotId = null)
      pauseGroupAutoPlay(groupId)
      localState.update { it.copy(error = "已完成自动轮流 ${preference.maxRounds} 轮") }
      return
    }
    val nextBotId = nextAutoPlayBotIdAfter(groupId, lastBotId)
    if (nextBotId == null) {
      pauseGroupAutoPlay(groupId)
      return
    }
    groupAutoPlaySessionsByGroupId[groupId] = session.copy(completedTurns = completedTurns, retriedBotId = null)
    delayGroupAutoPlay(preference)
    if (groupId !in uiState.value.autoPlayingGroupIds || groupJobsByGroupId[groupId]?.isActive == true) return
    launchGroupBotTurn(groupId, nextBotId, summarize = false, continueAutoPlay = true)
  }

  private suspend fun delayGroupAutoPlay(preference: GroupAutoPlayPreference) {
    val intervalMs = preference.intervalSeconds.coerceAtLeast(0) * 1_000L
    if (intervalMs > 0) delay(intervalMs)
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
    localState.update { it.copy(botManagerOpen = true) }
  }

  fun closeBotManager() {
    localState.update { it.copy(botManagerOpen = false) }
  }

  fun createAiBot(name: String, providerId: String, model: String, systemPrompt: String, bubbleColorKey: String = "AUTO") {
    viewModelScope.launch {
      runCatching {
        repository.createAiBot(name, providerId, model, systemPrompt, bubbleColorKey)
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "创建机器人失败") }
      }
    }
  }

  fun updateAiBot(botId: String, name: String, providerId: String, model: String, systemPrompt: String, bubbleColorKey: String = "AUTO") {
    viewModelScope.launch {
      runCatching {
        repository.updateAiBot(botId, name, providerId, model, systemPrompt, bubbleColorKey)
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
    val selectedIds = state.selectedMessageIds
    viewModelScope.launch {
      runCatching {
        if (state.groupChatPageOpen) {
          val groupId = state.selectedGroupChatId ?: error("请先选择群聊")
          repository.appendGroupMessagesToFavoriteSnippet(favoriteId, groupId, selectedIds)
        } else {
          val conversationId = state.selectedConversationId ?: error("请先选择对话")
          repository.appendMessagesToFavoriteSnippet(favoriteId, conversationId, selectedIds)
        }
      }.onSuccess {
        localState.update { current -> current.copy(error = "已追加到收藏") }
      }.onFailure { error ->
        localState.update { current -> current.copy(error = error.message ?: "追加收藏失败") }
      }
    }
  }

  fun removeMessageFromFavorite(favoriteId: String, messageId: String) {
    removeMessagesFromFavorite(favoriteId, setOf(messageId))
  }

  fun removeMessagesFromFavorite(favoriteId: String, messageIds: Set<String>) {
    viewModelScope.launch {
      runCatching {
        repository.removeMessagesFromFavoriteSnippet(favoriteId, messageIds)
      }.onSuccess { updated ->
        localState.update { current ->
          current.copy(
            error = if (updated == null) {
              "收藏不存在"
            } else {
              "已从收藏移除 ${messageIds.size} 条消息"
            }
          )
        }
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

  fun addTagsToFavoriteSnippets(favoriteIds: Set<String>, tags: String) {
    viewModelScope.launch {
      runCatching {
        repository.addTagsToFavoriteSnippets(favoriteIds, tags)
      }.onSuccess { count ->
        localState.update { it.copy(error = if (count > 0) "已为 $count 个收藏添加标签" else "收藏标签无需更新") }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "批量添加标签失败") }
      }
    }
  }

  fun renameFavoriteTag(oldTag: String, newTag: String) {
    viewModelScope.launch {
      runCatching {
        repository.renameFavoriteTag(oldTag, newTag)
      }.onSuccess { count ->
        localState.update { it.copy(error = if (count > 0) "标签已更新" else "没有收藏使用该标签") }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "重命名标签失败") }
      }
    }
  }

  fun deleteFavoriteTag(tag: String) {
    viewModelScope.launch {
      runCatching {
        repository.deleteFavoriteTag(tag)
      }.onSuccess { count ->
        localState.update { it.copy(error = if (count > 0) "标签已删除" else "没有收藏使用该标签") }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "删除标签失败") }
      }
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
    requestImageExport(context, "分享收藏长图") {
      repository.favoriteSnippetExport(favoriteId)
    }
  }

  fun exportFavoriteSnippetsJson(context: Context) {
    viewModelScope.launch {
      val text = repository.favoriteSnippetsExportJson()
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText("AIChat 收藏 JSON", text))
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, text)
      }
      context.startActivity(Intent.createChooser(sendIntent, "导出收藏 JSON"))
      localState.update { it.copy(error = "收藏 JSON 已复制") }
    }
  }

  fun exportFavoriteSnippetsMarkdown(context: Context) {
    viewModelScope.launch {
      val text = repository.favoriteSnippetsExportMarkdown()
      if (text.isBlank()) {
        localState.update { it.copy(error = "没有可导出的收藏") }
        return@launch
      }
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_TEXT, text)
      }
      context.startActivity(Intent.createChooser(sendIntent, "导出收藏 Markdown"))
    }
  }

  fun importFavoriteSnippetsJson(text: String) {
    viewModelScope.launch {
      runCatching {
        repository.importFavoriteSnippetsJson(text)
      }.onSuccess { count ->
        localState.update { it.copy(error = "已导入 $count 个收藏") }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "导入收藏失败") }
      }
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

  fun clearConversationGroup(groupName: String) {
    viewModelScope.launch {
      repository.clearConversationGroup(groupName)
    }
  }

  fun selectProvider(id: String) {
    localState.update { it.copy(inlineImagesAllowedForNextSend = false) }
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

  fun cleanupHistoricalDsmlToolMarkup() {
    viewModelScope.launch {
      runCatching {
        repository.cleanupHistoricalDsmlToolMarkup()
      }.onSuccess { result ->
        localState.update {
          it.copy(
            error = if (result.totalMessages > 0) {
              "已清理 ${result.totalMessages} 条历史工具标记"
            } else {
              "没有发现需要清理的历史工具标记"
            }
          )
        }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "清理历史工具标记失败") }
      }
    }
  }

  fun setWebSearchMode(mode: WebSearchMode) {
    viewModelScope.launch {
      preferencesRepository.setWebSearchMode(mode)
    }
  }

  fun setStreamingBubbleMotion(motion: StreamingBubbleMotion) {
    viewModelScope.launch {
      preferencesRepository.setStreamingBubbleMotion(motion)
    }
  }

  fun setKeepScreenOnWhileGenerating(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setKeepScreenOnWhileGenerating(enabled)
    }
  }

  fun setAttachmentMaxFileMb(value: Int) {
    val current = uiState.value.appSettings
    viewModelScope.launch {
      preferencesRepository.setAttachmentLimits(
        maxFileMb = value,
        maxPendingMb = current.attachmentMaxPendingMb.coerceAtLeast(value),
        maxImageSourceMb = current.attachmentMaxImageSourceMb.coerceAtLeast(value)
      )
    }
  }

  fun setAttachmentMaxPendingMb(value: Int) {
    val current = uiState.value.appSettings
    viewModelScope.launch {
      preferencesRepository.setAttachmentLimits(
        maxFileMb = current.attachmentMaxFileMb,
        maxPendingMb = value,
        maxImageSourceMb = current.attachmentMaxImageSourceMb
      )
    }
  }

  fun setAttachmentMaxImageSourceMb(value: Int) {
    val current = uiState.value.appSettings
    viewModelScope.launch {
      preferencesRepository.setAttachmentLimits(
        maxFileMb = current.attachmentMaxFileMb,
        maxPendingMb = current.attachmentMaxPendingMb,
        maxImageSourceMb = value
      )
    }
  }

  fun createImageConversationWithProvider(providerId: String) {
    val provider = uiState.value.providers.firstOrNull { it.id == providerId && it.supportsImageGeneration } ?: return
    localState.update { it.copy(newConversationPickerOpen = false, groupChatPageOpen = false, selectedGroupChatId = null) }
    viewModelScope.launch {
      val conversation = repository.createImageConversation(provider.id, provider.defaultModel)
      preferencesRepository.setSelectedProvider(provider.id)
      preferencesRepository.setSelectedConversation(conversation.id)
    }
  }

  fun saveBackgroundPreset(preset: ChatBackgroundPreset?, title: String, content: String, category: String) {
    val cleanTitle = title.trim().ifBlank { "未命名背景" }
    val cleanContent = content.trim()
    val cleanCategory = category.trim().takeIf { it.isNotBlank() }
    if (cleanContent.isBlank()) {
      localState.update { it.copy(error = "背景内容不能为空") }
      return
    }
    viewModelScope.launch {
      val current = uiState.value.appSettings.backgroundPresets
      val now = System.currentTimeMillis()
      val next = if (preset == null) {
        current + ChatBackgroundPreset(
          id = "bg_${UUID.randomUUID().toString().replace("-", "")}",
          title = cleanTitle,
          content = cleanContent,
          sortOrder = current.size,
          createdAt = now,
          updatedAt = now,
          category = cleanCategory
        )
      } else {
        current.map {
          if (it.id == preset.id) {
            it.copy(title = cleanTitle, content = cleanContent, category = cleanCategory, updatedAt = now)
          } else {
            it
          }
        }
      }
      preferencesRepository.setBackgroundPresets(next)
    }
  }

  fun deleteBackgroundPreset(presetId: String) {
    viewModelScope.launch {
      preferencesRepository.setBackgroundPresets(uiState.value.appSettings.backgroundPresets.filterNot { it.id == presetId })
    }
  }

  fun moveBackgroundPreset(presetId: String, direction: Int) {
    val current = uiState.value.appSettings.backgroundPresets.sortedBy { it.sortOrder }
    val index = current.indexOfFirst { it.id == presetId }
    val target = (index + direction).coerceIn(0, current.lastIndex)
    if (index < 0 || target == index) return
    val mutable = current.toMutableList()
    val item = mutable.removeAt(index)
    mutable.add(target, item)
    viewModelScope.launch {
      preferencesRepository.setBackgroundPresets(mutable.mapIndexed { order, preset -> preset.copy(sortOrder = order) })
    }
  }

  fun exportBackgroundPresetsText(context: Context) {
    viewModelScope.launch {
      val presets = uiState.value.appSettings.backgroundPresets.sortedBy { it.sortOrder }
      val text = Gson().toJson(BackgroundPresetExportPayload(presets = presets))
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText("AIChat 背景预设", text))
      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, text)
      }
      context.startActivity(Intent.createChooser(sendIntent, "导出背景预设 JSON"))
    }
  }

  fun importBackgroundPresetsText(text: String) {
    viewModelScope.launch {
      runCatching {
        val imported = parseBackgroundPresetImportText(text)
        val current = uiState.value.appSettings.backgroundPresets.sortedBy { it.sortOrder }
        val usedIds = current.map { it.id }.toMutableSet()
        val now = System.currentTimeMillis()
        val cleanImported = imported.mapNotNull { preset ->
          val cleanContent = preset.content.trim()
          if (cleanContent.isBlank()) {
            null
          } else {
            preset.copy(
              id = uniqueBackgroundPresetId(preset.id, usedIds),
              title = preset.title.trim().ifBlank { "未命名背景" },
              content = cleanContent,
              category = preset.category?.trim()?.takeIf { it.isNotBlank() },
              createdAt = preset.createdAt.takeIf { it > 0L } ?: now,
              updatedAt = now
            )
          }
        }
        require(cleanImported.isNotEmpty()) { "没有可导入的背景预设" }
        preferencesRepository.setBackgroundPresets(
          (current + cleanImported).mapIndexed { index, preset -> preset.copy(sortOrder = index) }
        )
        cleanImported.size
      }.onSuccess { count ->
        localState.update { it.copy(error = "已导入 $count 个背景预设") }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "导入背景预设失败") }
      }
    }
  }

  fun saveGroupBackgroundPresetCombination(groupId: String, presetIds: List<String>) {
    viewModelScope.launch {
      preferencesRepository.setGroupBackgroundPresetCombination(groupId, presetIds)
      localState.update {
        it.copy(error = if (presetIds.isEmpty()) "已清除本群常用背景组合" else "已保存本群常用背景组合")
      }
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

  fun exportProviderConfigsQrText(onReady: (String) -> Unit) {
    viewModelScope.launch {
      runCatching {
        repository.exportProvidersQrText(includeApiKeys = true)
      }.onSuccess { text ->
        onReady(text)
        localState.update { it.copy(error = "已生成 API 配置二维码") }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "生成 API 配置二维码失败") }
      }
    }
  }

  fun importProviderConfigsText(text: String) {
    viewModelScope.launch {
      runCatching {
        repository.importProvidersText(text)
      }.onSuccess { count ->
        localState.update {
          it.copy(error = if (count > 0) "已导入 $count 个 API 配置" else "没有可导入的 API 配置")
        }
      }.onFailure { error ->
        localState.update { it.copy(error = error.message ?: "导入 API 配置失败") }
      }
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
        if (result.wouldLeaveNoProvider) {
          localState.update {
            it.copy(error = "至少保留一个 API 配置。请先新增或导入其他配置后再删除。")
          }
          return@launch
        }
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
    private const val MinAttachmentFileMb = 1
    private const val MaxAttachmentFileMb = 100
    private const val MaxAttachmentPendingMb = 300
    private const val MaxAttachmentImageSourceMb = 300
    private const val PreferredImageUploadBytes = 4L * 1024L * 1024L
    private const val MaxImageUploadEdgePx = 2048

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

private fun parseBackgroundPresetImportText(text: String): List<ChatBackgroundPreset> {
  val gson = Gson()
  val cleanText = text.trim()
  require(cleanText.isNotBlank()) { "请粘贴背景预设 JSON" }
  val payload = runCatching {
    gson.fromJson(cleanText, BackgroundPresetExportPayload::class.java)
  }.getOrNull()
  if (payload != null && payload.presets.isNotEmpty()) return payload.presets
  val listType = object : TypeToken<List<ChatBackgroundPreset>>() {}.type
  return runCatching {
    gson.fromJson<List<ChatBackgroundPreset>>(cleanText, listType)
  }.getOrNull()
    ?: error("无法识别背景预设 JSON")
}

private fun uniqueBackgroundPresetId(rawId: String, usedIds: MutableSet<String>): String {
  val clean = rawId
    .trim()
    .takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) }
  var candidate = clean ?: newBackgroundPresetId()
  while (!usedIds.add(candidate)) {
    candidate = newBackgroundPresetId()
  }
  return candidate
}

private fun newBackgroundPresetId(): String = "bg_${UUID.randomUUID().toString().replace("-", "")}"

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
