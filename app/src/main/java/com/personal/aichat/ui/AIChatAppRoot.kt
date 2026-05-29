package com.personal.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.ChatBackgroundPreset
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ConversationType
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.FavoriteSnippetMessage
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.GroupTurnTrigger
import com.personal.aichat.domain.ImageGenerationApiMode
import com.personal.aichat.domain.ImageGenerationBackground
import com.personal.aichat.domain.ImageGenerationOutputFormat
import com.personal.aichat.domain.ImageGenerationQuality
import com.personal.aichat.domain.ImageGenerationSize
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.WebSearchMode
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import java.util.Locale
import java.util.TimeZone

private data class GroupChatDialogDraft(
  val title: String,
  val topic: String,
  val selectedBotIds: Set<String>,
  val mode: GroupChatDialogMode
)

private enum class GroupChatDialogMode {
  CREATE,
  EDIT,
  COPY
}

private data class BotBubblePalette(
  val key: String,
  val label: String,
  val lightContainer: Color,
  val lightContent: Color,
  val lightAccent: Color,
  val darkContainer: Color,
  val darkContent: Color,
  val darkAccent: Color
)

private data class BotBubbleColors(
  val key: String,
  val label: String,
  val container: Color,
  val content: Color,
  val accent: Color
)

private data class MarkdownRenderColors(
  val content: Color,
  val muted: Color,
  val blockContainer: Color,
  val blockHeader: Color,
  val border: Color,
  val divider: Color
)

private data class UserBubbleColors(
  val container: Color,
  val content: Color,
  val metadata: Color
)

internal data class LongBubbleNavTarget(
  val index: Int,
  val showUp: Boolean,
  val showDown: Boolean,
  val bottomOffset: Int,
  val messageId: String? = null,
  val showActions: Boolean = false
)

internal data class VisibleListItemBounds(
  val index: Int,
  val offset: Int,
  val size: Int,
  val messageId: String? = null,
  val supportsActions: Boolean = false
)

internal sealed interface GroupMessageListItem {
  val key: String
  val messageIds: List<String>

  data class Message(val message: GroupChatMessage) : GroupMessageListItem {
    override val key: String = message.id
    override val messageIds: List<String> = listOf(message.id)
  }

  data class ToolGroup(val messages: List<GroupChatMessage>) : GroupMessageListItem {
    override val key: String = "tool_group_${messages.firstOrNull()?.id.orEmpty()}"
    override val messageIds: List<String> = messages.map { it.id }
  }
}

internal sealed interface ChatMessageListItem {
  val key: String
  val messageIds: List<String>

  data class Message(val message: ChatMessage) : ChatMessageListItem {
    override val key: String = message.id
    override val messageIds: List<String> = listOf(message.id)
  }

  data class ToolGroup(val messages: List<ChatMessage>) : ChatMessageListItem {
    override val key: String = "tool_group_${messages.firstOrNull()?.id.orEmpty()}"
    override val messageIds: List<String> = messages.map { it.id }
  }
}

private val BotBubblePalettes = listOf(
  BotBubblePalette("TEAL", "青绿", Color(0xFFD2F4EA), Color(0xFF073B32), Color(0xFF00866E), Color(0xFF163731), Color(0xFFE0FFF6), Color(0xFF46D3BA)),
  BotBubblePalette("BLUE", "蓝", Color(0xFFD8E9FF), Color(0xFF062B55), Color(0xFF1E6FD9), Color(0xFF162D47), Color(0xFFE5F1FF), Color(0xFF6EA8FF)),
  BotBubblePalette("PURPLE", "紫", Color(0xFFE8DDFF), Color(0xFF32105D), Color(0xFF7B43D6), Color(0xFF322845), Color(0xFFF0E8FF), Color(0xFFB994FF)),
  BotBubblePalette("ROSE", "玫红", Color(0xFFFFD9E6), Color(0xFF5F0B2D), Color(0xFFD43D75), Color(0xFF442632), Color(0xFFFFE4EE), Color(0xFFFF8BB4)),
  BotBubblePalette("ORANGE", "橙", Color(0xFFFFE1C7), Color(0xFF542600), Color(0xFFD66A00), Color(0xFF443021), Color(0xFFFFE9D6), Color(0xFFFFA857)),
  BotBubblePalette("GOLD", "金", Color(0xFFFFEDB5), Color(0xFF4B3500), Color(0xFFB98700), Color(0xFF42371E), Color(0xFFFFF0BE), Color(0xFFFFD35D)),
  BotBubblePalette("INDIGO", "靛蓝", Color(0xFFDEE3FF), Color(0xFF161F61), Color(0xFF4C5DD9), Color(0xFF252A49), Color(0xFFE8EBFF), Color(0xFF8FA0FF)),
  BotBubblePalette("CYAN", "湖蓝", Color(0xFFCFF3FF), Color(0xFF003847), Color(0xFF0089A8), Color(0xFF173642), Color(0xFFE2F8FF), Color(0xFF54D8F4))
)

private val AutoBotBubblePalette = BotBubblePalette(
  key = "AUTO",
  label = "自动",
  lightContainer = Color(0xFFE6ECE9),
  lightContent = Color(0xFF1C2623),
  lightAccent = Color(0xFF5B6F68),
  darkContainer = Color(0xFF293230),
  darkContent = Color(0xFFE8F0ED),
  darkAccent = Color(0xFF9CB0AA)
)

internal fun resolvedBotBubbleColorKey(botId: String, requestedKey: String): String {
  val explicitKey = requestedKey.trim().uppercase(Locale.ROOT)
  if (explicitKey != "AUTO" && BotBubblePalettes.any { it.key == explicitKey }) return explicitKey
  if (BotBubblePalettes.isEmpty()) return "AUTO"
  val hash = botId.fold(0) { acc, char -> (acc * 31 + char.code) and Int.MAX_VALUE }
  return BotBubblePalettes[hash % BotBubblePalettes.size].key
}

@Composable
private fun botBubbleColors(bot: AiBot?): BotBubbleColors {
  val requestedKey = bot?.bubbleColorKey ?: "AUTO"
  val key = bot?.let { resolvedBotBubbleColorKey(it.id, requestedKey) } ?: "AUTO"
  val palette = BotBubblePalettes.firstOrNull { it.key == key } ?: AutoBotBubblePalette
  val dark = isDarkThemeColors()
  return BotBubbleColors(
    key = palette.key,
    label = palette.label,
    container = if (dark) palette.darkContainer else palette.lightContainer,
    content = if (dark) palette.darkContent else palette.lightContent,
    accent = if (dark) palette.darkAccent else palette.lightAccent
  )
}

private fun markdownColorsForBotBubble(colors: BotBubbleColors): MarkdownRenderColors {
  return MarkdownRenderColors(
    content = colors.content,
    muted = colors.content.copy(alpha = 0.72f),
    blockContainer = mixColors(colors.container, Color.Black, 0.12f),
    blockHeader = mixColors(colors.container, colors.accent, 0.18f),
    border = colors.accent.copy(alpha = 0.42f),
    divider = colors.accent.copy(alpha = 0.34f)
  )
}

private fun mixColors(base: Color, overlay: Color, overlayAlpha: Float): Color {
  val alpha = overlayAlpha.coerceIn(0f, 1f)
  val inverse = 1f - alpha
  return Color(
    red = base.red * inverse + overlay.red * alpha,
    green = base.green * inverse + overlay.green * alpha,
    blue = base.blue * inverse + overlay.blue * alpha,
    alpha = base.alpha
  )
}

private fun conversationGroupLabel(groupName: String): String =
  groupName.ifBlank { "未分组" }

@Composable
private fun userBubbleColors(): UserBubbleColors {
  val isDark = isDarkThemeColors()
  val content = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
  return UserBubbleColors(
    container = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
    content = content,
    metadata = content.copy(alpha = if (isDark) 0.72f else 0.78f)
  )
}

@Composable
private fun isDarkThemeColors(): Boolean {
  val onBackground = MaterialTheme.colorScheme.onBackground
  return onBackground.red > 0.75f &&
    onBackground.green > 0.75f &&
    onBackground.blue > 0.75f
}

@Composable
fun AIChatAppRoot(viewModel: ChatViewModel) {
  val state by viewModel.uiState.collectAsState()
  val editingProvider = state.editingProvider
  val context = LocalContext.current
  var drawerOpen by remember { mutableStateOf(false) }
  var previewImage by remember { mutableStateOf<ChatAttachment?>(null) }
  var favoriteDraftMessageIds by remember { mutableStateOf<Set<String>?>(null) }
  var favoriteDraftGroupMessageIds by remember { mutableStateOf<Set<String>?>(null) }
  var editingFavorite by remember { mutableStateOf<FavoriteSnippet?>(null) }
  var appendFavoritePickerOpen by remember { mutableStateOf(false) }
  var groupChatDialogDraft by remember { mutableStateOf<GroupChatDialogDraft?>(null) }
  val openAttachmentInApp: (ChatAttachment) -> Unit = { attachment ->
    if (attachment.isImage) {
      previewImage = attachment
    } else {
      openAttachment(context, attachment)
    }
  }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
    viewModel.addAttachments(uris)
  }
  val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    viewModel.addAttachments(uris)
  }
  val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    val uri = pendingCameraUri
    pendingCameraUri = null
    if (success && uri != null) {
      viewModel.addAttachments(listOf(uri))
    }
  }

  val groupChatDialogOpen = state.newGroupChatDialogOpen || state.editingGroupChatId != null || groupChatDialogDraft != null
  val incomingShareOpen = state.incomingShareDraft?.open == true
  val backHandled = previewImage != null ||
    state.providerRebindDeleteSourceId != null ||
    state.error != null ||
    state.deleteConfirmOpen ||
    appendFavoritePickerOpen ||
    editingFavorite != null ||
    favoriteDraftGroupMessageIds != null ||
    favoriteDraftMessageIds != null ||
    groupChatDialogOpen ||
    drawerOpen ||
    incomingShareOpen ||
    state.forkTargetMessageId != null ||
    state.newConversationPickerOpen ||
    state.botManagerOpen ||
    state.favoritePageOpen ||
    state.settingsOpen ||
    state.providerManagerOpen ||
    state.settingsPageOpen ||
    state.messageSelectionMode

  BackHandler(enabled = backHandled) {
    when {
      previewImage != null -> previewImage = null
      state.providerRebindDeleteSourceId != null -> viewModel.cancelProviderRebindDelete()
      state.error != null -> viewModel.clearError()
      state.deleteConfirmOpen -> viewModel.cancelDeleteConversation()
      appendFavoritePickerOpen -> appendFavoritePickerOpen = false
      editingFavorite != null -> editingFavorite = null
      favoriteDraftGroupMessageIds != null -> favoriteDraftGroupMessageIds = null
      favoriteDraftMessageIds != null -> favoriteDraftMessageIds = null
      groupChatDialogOpen -> {
        groupChatDialogDraft = null
        viewModel.closeNewGroupChatDialog()
      }
      drawerOpen -> drawerOpen = false
      incomingShareOpen -> viewModel.dismissIncomingShareDraft()
      state.forkTargetMessageId != null -> viewModel.closeForkProviderPicker()
      state.newConversationPickerOpen -> viewModel.closeNewConversationPicker()
      state.botManagerOpen -> viewModel.closeBotManager()
      state.favoritePageOpen -> viewModel.closeFavoritePage()
      state.settingsOpen -> viewModel.closeSettings()
      state.providerManagerOpen -> viewModel.closeProviderManager()
      state.settingsPageOpen -> viewModel.closeSettingsPage()
      state.messageSelectionMode -> viewModel.toggleMessageSelectionMode(false)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      TopBar(
        state = state,
        onOpenConversationDrawer = { drawerOpen = true },
        onNewChat = viewModel::openNewConversationPicker,
        onOpenSettings = viewModel::openSettingsPage,
        onTogglePin = viewModel::togglePinConversation,
        onArchive = viewModel::archiveConversation,
        onDelete = viewModel::deleteConversation,
        onRename = viewModel::updateConversationMeta,
        onToggleSelectionMode = viewModel::toggleMessageSelectionMode,
        onShareText = { viewModel.shareConversationText(state.selectedConversationId.orEmpty(), context) },
        onShareSelected = { viewModel.shareSelectedMessagesText(context) },
        onShareImage = { viewModel.shareConversationLongImage(context) },
        onShareSelectedImage = { viewModel.shareSelectedMessagesLongImage(context) },
        onShareMarkdown = { viewModel.shareConversationMarkdownFile(context) },
        onFavoriteSelected = {
          if (state.selectedMessageIds.isNotEmpty()) {
            favoriteDraftMessageIds = state.selectedMessageIds
          }
        },
        onAppendSelectedToFavorite = {
          if (state.selectedMessageIds.isNotEmpty()) {
            appendFavoritePickerOpen = true
          }
        }
      )
      MessageList(
        state = state,
        messages = state.messages,
        imageMode = state.selectedConversation?.type == ConversationType.IMAGE,
        selectedMessageIds = state.selectedMessageIds,
        selectionMode = state.messageSelectionMode,
        onToggleMessageSelected = viewModel::toggleMessageSelected,
        onSetMessagesSelected = viewModel::setMessagesSelected,
        onSelectRangeTo = viewModel::selectMessageRangeTo,
        onEditResend = viewModel::editAndResend,
        onShareMessageText = { viewModel.shareMessageText(it, context) },
        onShareMessageImage = { viewModel.shareMessageImage(it, context) },
        onFavoriteMessage = { favoriteDraftMessageIds = setOf(it) },
        onFavoriteMessages = { favoriteDraftMessageIds = it },
        onForkMessage = viewModel::openForkProviderPicker,
        onOpenAttachment = openAttachmentInApp,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
      Composer(
        input = state.input,
        attachments = state.pendingAttachments,
        attachmentsEnabled = if (state.selectedConversation?.type == ConversationType.IMAGE) {
          state.selectedProvider?.supportsImageGeneration == true
        } else {
          state.selectedProvider?.supportsAttachments == true
        },
        imageMode = state.selectedConversation?.type == ConversationType.IMAGE,
        imageOptions = state.imageGenerationOptions,
        onInput = viewModel::updateInput,
        onSend = viewModel::send,
        onRetry = viewModel::retryLast,
        onPickImages = { imagePickerLauncher.launch("image/*") },
        onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
        onTakePhoto = {
          val uri = createCameraCaptureUri(context)
          pendingCameraUri = uri
          cameraLauncher.launch(uri)
        },
        onRemoveAttachment = viewModel::removePendingAttachment,
        onOpenAttachment = openAttachmentInApp,
        isGenerating = state.isSelectedConversationStreaming,
        onStopGenerating = viewModel::stopGenerating,
        onImageSize = viewModel::setImageGenerationSize,
        onImageQuality = viewModel::setImageGenerationQuality,
        onImageCount = viewModel::setImageGenerationCount,
        onImageOutputFormat = viewModel::setImageGenerationOutputFormat,
        onImageBackground = viewModel::setImageGenerationBackground
      )
    }

    if (state.groupChatPageOpen) {
      GroupChatPage(
        state = state,
        onOpenDrawer = { drawerOpen = true },
        onClose = viewModel::closeGroupChatPage,
        onNewGroup = viewModel::openNewGroupChatDialog,
        onInput = viewModel::updateGroupInput,
        onSendUser = viewModel::sendGroupUserMessage,
        onPickImages = { imagePickerLauncher.launch("image/*") },
        onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
        onTakePhoto = {
          val uri = createCameraCaptureUri(context)
          pendingCameraUri = uri
          cameraLauncher.launch(uri)
        },
        onRemoveAttachment = viewModel::removePendingAttachment,
        onOpenAttachment = openAttachmentInApp,
        onBotTurn = { viewModel.sendGroupBotTurn(it, summarize = false) },
        onSummarize = { viewModel.sendGroupBotTurn(it, summarize = true) },
        onToggleAutoPlay = viewModel::toggleGroupAutoPlay,
        onStop = viewModel::stopGroupGenerating,
        onEditGroup = viewModel::openEditGroupChatDialog,
        onDeleteGroup = viewModel::deleteSelectedGroupChat,
        onFavoriteMessage = { favoriteDraftGroupMessageIds = setOf(it) },
        onFavoriteMessages = { favoriteDraftGroupMessageIds = it },
        selectedMessageIds = state.selectedMessageIds,
        selectionMode = state.messageSelectionMode,
        onToggleSelectionMode = viewModel::toggleMessageSelectionMode,
        onToggleMessageSelected = viewModel::toggleMessageSelected,
        onSetMessagesSelected = viewModel::setMessagesSelected,
        onSelectRangeTo = viewModel::selectMessageRangeTo,
        onShareText = { viewModel.shareGroupChatText(context) },
        onShareSelected = { viewModel.shareSelectedGroupMessagesText(context) },
        onShareImage = { viewModel.shareGroupChatLongImage(context) },
        onShareSelectedImage = { viewModel.shareSelectedGroupMessagesLongImage(context) },
        onShareMessageText = { viewModel.shareGroupMessageText(it, context) },
        onShareMessageImage = { viewModel.shareGroupMessageImage(it, context) },
        onFavoriteSelected = {
          if (state.selectedMessageIds.isNotEmpty()) {
            favoriteDraftGroupMessageIds = state.selectedMessageIds
          }
        },
        onAppendSelectedToFavorite = {
          if (state.selectedMessageIds.isNotEmpty()) {
            appendFavoritePickerOpen = true
          }
        },
        onCopyGroup = {
          state.selectedGroupChat?.let { group ->
            groupChatDialogDraft = GroupChatDialogDraft(
              title = "${group.title.ifBlank { "AI 群聊" }} 副本",
              topic = group.topic,
              selectedBotIds = state.groupMembers.map { it.botId }.toSet(),
              mode = GroupChatDialogMode.COPY
            )
          }
        }
      )
    }

    if (state.settingsOpen && editingProvider != null) {
      ProviderSettingsDialog(
        provider = editingProvider,
        hasSavedApiKey = state.editingProviderHasApiKey,
        onDismiss = viewModel::closeSettings,
        onSave = viewModel::saveProvider
      )
    }

    if (state.providerManagerOpen) {
      ProviderManagerDialog(
        state = state,
        onDismiss = viewModel::closeProviderManager,
        onSelectProvider = viewModel::selectProvider,
        onEditProvider = viewModel::openSettings,
        onCloneProvider = viewModel::cloneProvider,
        onDeleteProvider = viewModel::deleteProvider,
        onCreateProvider = viewModel::createProvider
      )
    }

    if (state.settingsPageOpen) {
      AppSettingsPage(
        state = state,
        onDismiss = viewModel::closeSettingsPage,
        onOpenProviderManager = viewModel::openProviderManager,
        onPalette = viewModel::setThemePalette,
        onThemeMode = viewModel::setThemeMode,
        onFontScale = viewModel::setFontScale,
        onDebugResponseLogging = viewModel::setDebugResponseLogging,
        onWebSearchMode = viewModel::setWebSearchMode,
        onAttachmentMaxFileMb = viewModel::setAttachmentMaxFileMb,
        onAttachmentMaxPendingMb = viewModel::setAttachmentMaxPendingMb,
        onAttachmentMaxImageSourceMb = viewModel::setAttachmentMaxImageSourceMb,
        onExportProviderConfigs = { viewModel.exportProviderConfigsText(context) },
        onImportProviderConfigs = viewModel::importProviderConfigsText,
        onOpenBotManager = viewModel::openBotManager,
        onSaveBackgroundPreset = viewModel::saveBackgroundPreset,
        onDeleteBackgroundPreset = viewModel::deleteBackgroundPreset,
        onMoveBackgroundPreset = viewModel::moveBackgroundPreset
      )
    }

    if (state.favoritePageOpen) {
      FavoriteSnippetsPage(
        favorites = state.favoriteSnippets,
        onDismiss = viewModel::closeFavoritePage,
        onOpenAttachment = openAttachmentInApp,
        onShareText = { viewModel.shareFavoriteSnippetText(it, context) },
        onShareImage = { viewModel.shareFavoriteSnippetLongImage(it, context) },
        onCopyText = { viewModel.copyFavoriteSnippetText(it, context) },
        onEdit = { editingFavorite = it },
        onDelete = viewModel::deleteFavoriteSnippet,
        onRemoveMessage = viewModel::removeMessageFromFavorite,
        onJumpToSource = viewModel::jumpToFavoriteSource
      )
    }

    if (state.botManagerOpen) {
      BotManagerPage(
        providers = state.providers,
        bots = state.aiBots,
        onDismiss = viewModel::closeBotManager,
        onCreate = viewModel::createAiBot,
        onUpdate = viewModel::updateAiBot,
        onToggleEnabled = viewModel::setAiBotEnabled,
        onDelete = viewModel::deleteAiBot
      )
    }

    if (state.newConversationPickerOpen) {
      NewConversationProviderDialog(
        providers = state.providers,
        selectedProviderId = state.selectedProviderId,
        onDismiss = viewModel::closeNewConversationPicker,
        onSelectProvider = viewModel::createConversationWithProvider,
        onSelectImageProvider = viewModel::createImageConversationWithProvider
      )
    }

    if (state.forkTargetMessageId != null) {
      ForkProviderDialog(
        providers = state.providers,
        selectedProviderId = state.selectedProviderId,
        onDismiss = viewModel::closeForkProviderPicker,
        onSelectProvider = viewModel::forkConversationAtMessage
      )
    }

    state.incomingShareDraft?.takeIf { it.open }?.let { draft ->
      IncomingShareTargetDialog(
        state = state,
        draft = draft,
        onDismiss = viewModel::dismissIncomingShareDraft,
        onSelectConversation = viewModel::applyIncomingShareToConversation,
        onSelectGroup = viewModel::applyIncomingShareToGroup,
        onCreateConversation = viewModel::createConversationForIncomingShare,
        onOpenAttachment = openAttachmentInApp
      )
    }

    if (drawerOpen) {
      ConversationDrawer(
        state = state,
        onDismiss = { drawerOpen = false },
        onOpenFavorites = {
          drawerOpen = false
          viewModel.openFavoritePage()
        },
        onOpenGroups = {
          drawerOpen = false
          viewModel.openGroupChatPage()
        },
        onNewGroup = {
          drawerOpen = false
          viewModel.openNewGroupChatDialog()
        },
        onSelectGroup = {
          viewModel.selectGroupChat(it)
          drawerOpen = false
        },
        onSelectConversation = {
          viewModel.selectConversation(it)
          drawerOpen = false
        },
        onTogglePin = viewModel::togglePinConversation,
        onArchive = viewModel::archiveConversation,
        onRestore = viewModel::restoreConversation,
        onDelete = viewModel::deleteConversation,
        onRename = viewModel::updateConversationMeta,
        onRenameGroup = viewModel::renameConversationGroup,
        onClearGroup = viewModel::clearConversationGroup
      )
    }

    if (groupChatDialogOpen) {
      val editingGroup = state.editingGroupChatId?.let { id -> state.groupChats.firstOrNull { it.id == id } }
      val draft = groupChatDialogDraft ?: editingGroup?.let { group ->
        GroupChatDialogDraft(
          title = group.title,
          topic = group.topic,
          selectedBotIds = state.groupMembers.filter { it.groupId == group.id }.map { it.botId }.toSet(),
          mode = GroupChatDialogMode.EDIT
        )
      }
      val mode = draft?.mode ?: GroupChatDialogMode.CREATE
      NewGroupChatDialog(
        bots = state.aiBots.filter { it.enabled },
        backgroundPresets = state.appSettings.backgroundPresets,
        title = when (mode) {
          GroupChatDialogMode.EDIT -> "编辑 AI 群聊"
          GroupChatDialogMode.COPY -> "复制 AI 群聊"
          GroupChatDialogMode.CREATE -> "新建 AI 群聊"
        },
        confirmText = if (mode == GroupChatDialogMode.EDIT) "保存" else if (mode == GroupChatDialogMode.COPY) "创建副本" else "创建",
        initialTitle = draft?.title.orEmpty(),
        initialTopic = draft?.topic.orEmpty(),
        initialSelectedBotIds = draft?.selectedBotIds.orEmpty(),
        onDismiss = {
          groupChatDialogDraft = null
          viewModel.closeNewGroupChatDialog()
        },
        onCreate = { title, topic, botIds ->
          groupChatDialogDraft = null
          if (mode == GroupChatDialogMode.EDIT && editingGroup != null) {
            viewModel.updateGroupChat(editingGroup.id, title, topic, botIds)
          } else {
            viewModel.createGroupChat(title, topic, botIds)
          }
        }
      )
    }

    favoriteDraftMessageIds?.let { messageIds ->
      FavoriteSnippetDialog(
        title = "收藏片段",
        messageCount = messageIds.size,
        initialTitle = defaultFavoriteTitle(state, messageIds),
        initialDescription = "",
        initialTags = "",
        onDismiss = { favoriteDraftMessageIds = null },
        onSave = { title, description, tags ->
          viewModel.saveFavoriteSnippet(messageIds, title, description, tags)
          favoriteDraftMessageIds = null
        }
      )
    }

    favoriteDraftGroupMessageIds?.let { messageIds ->
      FavoriteSnippetDialog(
        title = "收藏群聊片段",
        messageCount = messageIds.size,
        initialTitle = defaultGroupFavoriteTitle(state, messageIds),
        initialDescription = "",
        initialTags = "",
        onDismiss = { favoriteDraftGroupMessageIds = null },
        onSave = { title, description, tags ->
          viewModel.saveGroupFavoriteSnippet(messageIds, title, description, tags)
          favoriteDraftGroupMessageIds = null
        }
      )
    }

    editingFavorite?.let { favorite ->
      FavoriteSnippetDialog(
        title = "编辑收藏",
        messageCount = favorite.messageCount,
        initialTitle = favorite.title,
        initialDescription = favorite.description,
        initialTags = favorite.tags.joinToString("，"),
        onDismiss = { editingFavorite = null },
        onSave = { title, description, tags ->
          viewModel.updateFavoriteSnippet(favorite.id, title, description, tags)
          editingFavorite = null
        }
      )
    }

    if (appendFavoritePickerOpen) {
      val sourceId = if (state.groupChatPageOpen) state.selectedGroupChatId else state.selectedConversationId
      AppendToFavoriteDialog(
        favorites = state.favoriteSnippets.filter { it.sourceConversationId == sourceId },
        selectedCount = state.selectedMessageIds.size,
        onDismiss = { appendFavoritePickerOpen = false },
        onSelectFavorite = { favoriteId ->
          viewModel.appendSelectedMessagesToFavorite(favoriteId)
          appendFavoritePickerOpen = false
        }
      )
    }

    if (state.deleteConfirmOpen) {
      AlertDialog(
        onDismissRequest = viewModel::cancelDeleteConversation,
        title = { Text("删除对话") },
        text = { Text("确定要删除这条对话吗？删除后会从列表中隐藏。") },
        confirmButton = {
          Button(onClick = viewModel::confirmDeleteConversation) {
            Text("删除")
          }
        },
        dismissButton = {
          TextButton(onClick = viewModel::cancelDeleteConversation) {
            Text("取消")
          }
        }
      )
    }

    state.error?.let { message ->
      AlertDialog(
        onDismissRequest = viewModel::clearError,
        title = { Text("提示") },
        text = { Text(message) },
        confirmButton = {
          TextButton(onClick = viewModel::clearError) {
            Text("知道了")
          }
        }
      )
    }

    if (state.providerRebindDeleteSourceId != null) {
      ProviderRebindDeleteDialog(
        state = state,
        onDismiss = viewModel::cancelProviderRebindDelete,
        onRebindAndDelete = viewModel::rebindProviderBotsAndDelete
      )
    }

    previewImage?.let { attachment ->
      ImagePreviewDialog(
        attachment = attachment,
        onDismiss = { previewImage = null },
        onOpenExternal = { openAttachment(context, attachment) }
      )
    }
  }
}

@Composable
private fun ChatActionBar(
  state: ChatUiState,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit
) {
  val selectedConversation = state.selectedConversation
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = selectedConversation?.title ?: "未选择对话",
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = selectedConversation?.let { "${conversationGroupLabel(it.groupName)} / ${it.model}" } ?: "从左上角聊天列表选择",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (selectedConversation != null) {
      ConversationQuickActions(
        conversation = selectedConversation,
        onTogglePin = onTogglePin,
        onArchive = onArchive,
        onDelete = onDelete,
        onRename = onRename
      )
      ConversationShareMenu(
        selectionMode = state.messageSelectionMode,
        selectedCount = state.selectedMessageIds.size,
        onToggleSelectionMode = onToggleSelectionMode,
        onShareText = onShareText,
        onShareSelected = onShareSelected,
        onShareImage = onShareImage,
        onShareSelectedImage = onShareSelectedImage,
        onShareMarkdown = onShareMarkdown
      )
    }
  }
}

@Composable
private fun ConversationShareMenu(
  selectionMode: Boolean,
  selectedCount: Int,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { menuOpen = true }) {
      Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text(if (selectionMode) "退出多选" else "多选消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        onClick = {
          menuOpen = false
          onToggleSelectionMode(!selectionMode)
        }
      )
      DropdownMenuItem(
        text = { Text("分享全文文本") },
        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareText()
        }
      )
      DropdownMenuItem(
        text = { Text("分享选中消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("生成长图分享") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareImage()
        }
      )
      DropdownMenuItem(
        text = { Text("选中消息生成长图") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelectedImage()
        }
      )
      DropdownMenuItem(
        text = { Text("导出 Markdown 文件") },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareMarkdown()
        }
      )
    }
  }
}

@Composable
private fun ConversationDrawer(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onOpenFavorites: () -> Unit,
  onOpenGroups: () -> Unit,
  onNewGroup: () -> Unit,
  onSelectGroup: (String) -> Unit,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onRestore: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onRenameGroup: (String, String) -> Unit,
  onClearGroup: (String) -> Unit
) {
  var collapsedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.28f))
        .clickable(onClick = onDismiss)
    )
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.82f)
        .align(Alignment.CenterStart)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Folder, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("聊天列表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭")
          }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          item(key = "favorites-entry") {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFavorites)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Outlined.Bookmark, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("收藏夹", fontWeight = FontWeight.SemiBold)
                  Text(
                    "${state.favoriteSnippets.size} 个收藏片段",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
          item(key = "groups-entry") {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGroups)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Outlined.Groups, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("AI 群聊", fontWeight = FontWeight.SemiBold)
                  Text(
                    "${state.groupChats.size} 个群聊 · ${state.aiBots.size} 个机器人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                TextButton(onClick = onNewGroup) {
                  Text("新建")
                }
              }
            }
          }
          if (state.groupChats.isNotEmpty()) {
            item(key = "section-groups") {
              Text("群聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            items(state.groupChats, key = { "group-${it.id}" }) { group ->
              Surface(
                color = if (group.id == state.selectedGroupChatId && state.groupChatPageOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectGroup(group.id) }
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(group.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                  Text(group.topic.ifBlank { "未填写主题" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
          val folderedConversationIds = state.conversationGroups.flatMap { it.conversations }.map { it.id }.toSet()
          val visibleLooseConversations = state.conversations.filterNot { it.id in folderedConversationIds }
          val pinnedConversations = visibleLooseConversations.filter { it.isPinned }
          val normalConversations = visibleLooseConversations.filterNot { it.isPinned }
          val pinnedFolders = state.conversationGroups.filter { group -> group.conversations.any { it.isPinned } }
          val normalFolders = state.conversationGroups.filter { group -> group.conversations.none { it.isPinned } }
          drawerSection("置顶", pinnedConversations) { conversation ->
            ConversationDrawerRow(conversation, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          }
          drawerFolderSection("置顶文件夹", pinnedFolders, collapsedFolders, { key ->
            collapsedFolders = if (key in collapsedFolders) collapsedFolders - key else collapsedFolders + key
          }, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename, onRenameGroup, onClearGroup)
          drawerDatedConversationSections(normalConversations, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          drawerFolderSection("普通文件夹", normalFolders, collapsedFolders, { key ->
            collapsedFolders = if (key in collapsedFolders) collapsedFolders - key else collapsedFolders + key
          }, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename, onRenameGroup, onClearGroup)
          drawerArchivedSection("已归档", state.archivedConversations, onRestore, onDelete)
        }
      }
    }
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.drawerDatedConversationSections(
  conversations: List<ChatConversation>,
  selectedConversationId: String?,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  conversations.groupBy { conversationDateBucket(it.updatedAt) }.forEach { (bucket, items) ->
    drawerSection(bucket, items) { conversation ->
      ConversationDrawerRow(conversation, selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
    }

  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.drawerSection(
  title: String,
  conversations: List<ChatConversation>,
  row: @Composable (ChatConversation) -> Unit
) {
  if (conversations.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  items(conversations, key = { "$title-${it.id}" }) { conversation -> row(conversation) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.drawerFolderSection(
  title: String,
  groups: List<ChatConversationGroup>,
  collapsedFolders: Set<String>,
  onToggleFolder: (String) -> Unit,
  selectedConversationId: String?,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onRenameGroup: (String, String) -> Unit,
  onClearGroup: (String) -> Unit
) {
  if (groups.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  groups.forEach { group ->
    val folderKey = "$title:${group.name}"
    item(key = "folder-${title}-${group.name}") {
      DrawerFolderHeader(
        title = group.name,
        count = group.conversations.size,
        collapsed = folderKey in collapsedFolders,
        onToggle = { onToggleFolder(folderKey) },
        onRename = { newName -> onRenameGroup(group.name, newName) },
        onDelete = { onClearGroup(group.name) }
      )
    }
    if (folderKey !in collapsedFolders) {
      items(group.conversations, key = { "$title-${group.name}-${it.id}" }) { conversation ->
        ConversationDrawerRow(conversation, selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
      }
    }
  }
}

@Composable
private fun DrawerFolderHeader(
  title: String,
  count: Int,
  collapsed: Boolean,
  onToggle: () -> Unit,
  onRename: (String) -> Unit,
  onDelete: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var editing by remember(title) { mutableStateOf(false) }
  var deleteConfirmOpen by remember(title) { mutableStateOf(false) }
  var newName by remember(title) { mutableStateOf(title) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    TextButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
      Icon(
        if (collapsed) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
      Spacer(Modifier.width(6.dp))
      Text(
        "$title ($count)",
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Box {
      IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
        Icon(Icons.Outlined.MoreVert, contentDescription = "文件夹操作")
      }
      DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
          text = { Text("重命名文件夹") },
          leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
          onClick = {
            menuOpen = false
            editing = true
          }
        )
        DropdownMenuItem(
          text = { Text("删除文件夹") },
          leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
          onClick = {
            menuOpen = false
            deleteConfirmOpen = true
          }
        )
      }
    }
  }
  if (editing) {
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("重命名文件夹") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("文件夹名称") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(onClick = {
          onRename(newName.trim())
          editing = false
        }) {
          Text("保存")
        }
      },
      dismissButton = {
        TextButton(onClick = { editing = false }) {
          Text("取消")
        }
      }
    )
  }
  if (deleteConfirmOpen) {
    AlertDialog(
      onDismissRequest = { deleteConfirmOpen = false },
      title = { Text("删除文件夹") },
      text = { Text("将 $count 个聊天移出「$title」文件夹，不会删除聊天内容。") },
      confirmButton = {
        Button(onClick = {
          onDelete()
          deleteConfirmOpen = false
        }) {
          Text("移出并删除文件夹")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteConfirmOpen = false }) {
          Text("取消")
        }
      }
    )
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.drawerArchivedSection(
  title: String,
  conversations: List<ChatConversation>,
  onRestore: (String) -> Unit,
  onDelete: (String) -> Unit
) {
  if (conversations.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  items(conversations, key = { "archived-${it.id}" }) { conversation ->
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(conversationGroupLabel(conversation.groupName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onRestore(conversation.id) }) {
          Icon(Icons.Outlined.Unarchive, contentDescription = "恢复归档")
        }
        IconButton(onClick = { onDelete(conversation.id) }) {
          Icon(Icons.Outlined.Delete, contentDescription = "删除")
        }
      }
    }
  }
}

@Composable
private fun ConversationDrawerRow(
  conversation: ChatConversation,
  selectedConversationId: String?,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (conversation.id == selectedConversationId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = { onSelectConversation(conversation.id) }, modifier = Modifier.weight(1f)) {
        Column(horizontalAlignment = Alignment.Start) {
          val typePrefix = if (conversation.type == ConversationType.IMAGE) "生图 · " else ""
          Text(if (conversation.isPinned) "置顶 · $typePrefix${conversation.title}" else "$typePrefix${conversation.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(conversation.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "创建 ${formatConversationTime(conversation.createdAt)} · 最后 ${formatConversationTime(conversation.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (conversation.forkedFromConversationId != null) {
            Text("分叉对话", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
            leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
            onClick = {
              menuOpen = false
              onTogglePin(conversation.id, conversation.isPinned)
            }
          )
          DropdownMenuItem(
            text = { Text("移动到文件夹") },
            leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            onClick = {
              menuOpen = false
              moving = true
            }
          )
          DropdownMenuItem(
            text = { Text("归档") },
            leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
            onClick = {
              menuOpen = false
              onArchive(conversation.id)
            }
          )
          DropdownMenuItem(
            text = { Text("删除") },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            onClick = {
              menuOpen = false
              onDelete(conversation.id)
            }
          )
        }
      }
    }
  }
  if (moving) {
    MoveConversationDialog(
      conversationTitle = conversation.title,
      initialGroupName = conversation.groupName,
      onDismiss = { moving = false },
      onMove = { targetGroup ->
        onRename(conversation.id, conversation.title, targetGroup)
        moving = false
      }
    )
  }
}

@Composable
private fun TopBar(
  state: ChatUiState,
  onOpenConversationDrawer: () -> Unit,
  onNewChat: () -> Unit,
  onOpenSettings: () -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit
) {
  val selectedConversation = state.selectedConversation
  CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
    IconButton(onClick = onOpenConversationDrawer) {
      Icon(Icons.Outlined.Menu, contentDescription = "打开聊天列表")
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = selectedConversation?.title ?: "AI 聊天",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = selectedConversation?.let { conversation ->
          val providerName = state.providers.firstOrNull { it.id == conversation.providerId }?.displayName ?: conversation.providerId
          val streaming = if (state.isSelectedConversationStreaming) " · 输出中" else ""
          val mode = if (conversation.type == ConversationType.IMAGE) "生图 / " else ""
          "$mode${conversationGroupLabel(conversation.groupName)} / $providerName / ${conversation.model}$streaming"
        } ?: "未选择配置",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    IconButton(onClick = onNewChat) {
      Icon(Icons.Outlined.Add, contentDescription = "新建对话")
    }
    IconButton(onClick = onOpenSettings) {
      Icon(Icons.Outlined.Settings, contentDescription = "设置")
    }
    if (selectedConversation != null) {
      ConversationOverflowMenu(
        conversation = selectedConversation,
        selectionMode = state.messageSelectionMode,
        selectedCount = state.selectedMessageIds.size,
        onTogglePin = onTogglePin,
        onArchive = onArchive,
        onDelete = onDelete,
        onRename = onRename,
        onToggleSelectionMode = onToggleSelectionMode,
        onShareText = onShareText,
        onShareSelected = onShareSelected,
        onShareImage = onShareImage,
        onShareSelectedImage = onShareSelectedImage,
        onShareMarkdown = onShareMarkdown,
        onFavoriteSelected = onFavoriteSelected,
        onAppendSelectedToFavorite = onAppendSelectedToFavorite
      )
    }
    }
  }
}

@Composable
private fun ConversationOverflowMenu(
  conversation: ChatConversation,
  selectionMode: Boolean,
  selectedCount: Int,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMarkdown: () -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var editing by remember(conversation.id) { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  var title by remember(conversation.id) { mutableStateOf(conversation.title) }
  var groupName by remember(conversation.id) { mutableStateOf(conversation.groupName) }

  Box {
    IconButton(onClick = { menuOpen = true }) {
      Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text(if (selectionMode) "退出多选" else "多选消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        onClick = {
          menuOpen = false
          onToggleSelectionMode(!selectionMode)
        }
      )
      DropdownMenuItem(
        text = { Text("分享全文文本") },
        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareText()
        }
      )
      DropdownMenuItem(
        text = { Text("分享选中消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("收藏选中消息") },
        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onFavoriteSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("追加到已有收藏") },
        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onAppendSelectedToFavorite()
        }
      )
      DropdownMenuItem(
        text = { Text("生成长图分享") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareImage()
        }
      )
      DropdownMenuItem(
        text = { Text("选中消息生成长图") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelectedImage()
        }
      )
      DropdownMenuItem(
        text = { Text("导出 Markdown 文件") },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareMarkdown()
        }
      )
      DropdownMenuItem(
        text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
        onClick = {
          menuOpen = false
          onTogglePin(conversation.id, conversation.isPinned)
        }
      )
      DropdownMenuItem(
        text = { Text("编辑对话") },
        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        onClick = {
          menuOpen = false
          editing = true
        }
      )
      DropdownMenuItem(
        text = { Text("移动到文件夹") },
        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
        onClick = {
          menuOpen = false
          moving = true
        }
      )
      DropdownMenuItem(
        text = { Text("归档") },
        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
        onClick = {
          menuOpen = false
          onArchive(conversation.id)
        }
      )
      DropdownMenuItem(
        text = { Text("删除") },
        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        onClick = {
          menuOpen = false
          onDelete(conversation.id)
        }
      )
    }
  }

  if (editing) {
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("编辑对话") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("分组") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          onRename(conversation.id, title, groupName)
          editing = false
        }) {
          Text("保存")
        }
      },
      dismissButton = {
        TextButton(onClick = { editing = false }) {
          Text("取消")
        }
      }
    )
  }
  if (moving) {
    MoveConversationDialog(
      conversationTitle = conversation.title,
      initialGroupName = conversation.groupName,
      onDismiss = { moving = false },
      onMove = { targetGroup ->
        onRename(conversation.id, conversation.title, targetGroup)
        moving = false
      }
    )
  }
}

@Composable
private fun FavoriteSnippetDialog(
  title: String,
  messageCount: Int,
  initialTitle: String,
  initialDescription: String,
  initialTags: String,
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit
) {
  var snippetTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
  var description by remember(initialDescription) { mutableStateOf(initialDescription) }
  var tags by remember(initialTags) { mutableStateOf(initialTags) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("将保存 $messageCount 条消息为一个收藏片段。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = snippetTitle,
          onValueChange = { snippetTitle = it },
          label = { Text("标题") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = tags,
          onValueChange = { tags = it },
          label = { Text("标签，用逗号分隔") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("描述") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(snippetTitle, description, tags) },
        enabled = snippetTitle.isNotBlank()
      ) {
        Text("保存")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun AppendToFavoriteDialog(
  favorites: List<FavoriteSnippet>,
  selectedCount: Int,
  onDismiss: () -> Unit,
  onSelectFavorite: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("追加到已有收藏") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "将 $selectedCount 条选中消息追加到同一来源对话的收藏片段。",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (favorites.isEmpty()) {
          Text("当前对话还没有可追加的收藏。请先创建一个收藏片段。")
        } else {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 360.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            favorites.forEach { favorite ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectFavorite(favorite.id) }
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(favorite.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(
                    "${favorite.messageCount} 条消息 · ${favorite.tags.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun FavoriteSnippetsPage(
  favorites: List<FavoriteSnippet>,
  onDismiss: () -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onShareText: (String) -> Unit,
  onShareImage: (String) -> Unit,
  onCopyText: (String) -> Unit,
  onEdit: (FavoriteSnippet) -> Unit,
  onDelete: (String) -> Unit,
  onRemoveMessage: (String, String) -> Unit,
  onJumpToSource: (FavoriteSnippet) -> Unit
) {
  var query by remember { mutableStateOf("") }
  var tagFilter by remember { mutableStateOf<String?>(null) }
  var selectedFavoriteId by remember { mutableStateOf<String?>(null) }
  val selectedFavorite = favorites.firstOrNull { it.id == selectedFavoriteId }
  val allTags = remember(favorites) {
    favorites.flatMap { it.tags }.distinctBy { it.lowercase() }.sorted()
  }
  val normalizedQuery = query.trim().lowercase()
  val filtered = favorites.filter { favorite ->
    val matchesQuery = normalizedQuery.isBlank() || favorite.searchText.contains(normalizedQuery)
    val matchesTag = tagFilter == null || favorite.tags.any { it.equals(tagFilter, ignoreCase = true) }
    matchesQuery && matchesTag
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    if (selectedFavorite != null) {
      FavoriteSnippetDetail(
        favorite = selectedFavorite,
        onBack = { selectedFavoriteId = null },
        onDismiss = onDismiss,
        onOpenAttachment = onOpenAttachment,
        onShareText = { onShareText(selectedFavorite.id) },
        onShareImage = { onShareImage(selectedFavorite.id) },
        onCopyText = { onCopyText(selectedFavorite.id) },
        onEdit = { onEdit(selectedFavorite) },
        onDelete = {
          onDelete(selectedFavorite.id)
          selectedFavoriteId = null
        },
        onRemoveMessage = { messageId -> onRemoveMessage(selectedFavorite.id, messageId) },
        onJumpToSource = { onJumpToSource(selectedFavorite) }
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Bookmark, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("收藏夹", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭收藏夹")
          }
        }
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("搜索标题、描述、标签、来源或正文") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (allTags.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = tagFilter == null,
                onClick = { tagFilter = null },
                label = { Text("全部") }
              )
            }
            items(allTags, key = { it }) { tag ->
              FilterChip(
                selected = tagFilter.equals(tag, ignoreCase = true),
                onClick = { tagFilter = if (tagFilter.equals(tag, ignoreCase = true)) null else tag },
                label = { Text(tag) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(16.dp)) }
              )
            }
          }
        }
        if (filtered.isEmpty()) {
          Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              text = if (favorites.isEmpty()) "还没有收藏片段。可以在消息气泡或多选菜单里收藏。" else "没有匹配的收藏。",
              modifier = Modifier.padding(16.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { favorite ->
              FavoriteSnippetCard(
                favorite = favorite,
                onClick = { selectedFavoriteId = favorite.id }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FavoriteSnippetCard(favorite: FavoriteSnippet, onClick: () -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(favorite.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (favorite.description.isNotBlank()) {
        Text(
          favorite.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      if (favorite.tags.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(favorite.tags, key = { it }) { tag ->
            AssistChip(onClick = {}, label = { Text(tag) }, leadingIcon = {
              Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(14.dp))
            })
          }
        }
      }
      Text(
        "${favorite.messageCount} 条消息 · ${favorite.sourceConversationTitle} · ${favorite.sourceModel.orEmpty()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        "收藏于 ${formatMessageTime(favorite.createdAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun FavoriteSnippetDetail(
  favorite: FavoriteSnippet,
  onBack: () -> Unit,
  onDismiss: () -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onCopyText: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onRemoveMessage: (String) -> Unit,
  onJumpToSource: () -> Unit
) {
  var deleteConfirmOpen by remember(favorite.id) { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "返回收藏列表")
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(favorite.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${favorite.sourceConversationTitle} / ${favorite.sourceProviderName ?: favorite.sourceProviderId.orEmpty()} / ${favorite.sourceModel.orEmpty()}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      IconButton(onClick = onDismiss) {
        Icon(Icons.Outlined.Close, contentDescription = "关闭收藏夹")
      }
    }
    FavoriteDetailActions(
      onShareText = onShareText,
      onShareImage = onShareImage,
      onCopyText = onCopyText,
      onEdit = onEdit,
      onJumpToSource = onJumpToSource,
      onDelete = { deleteConfirmOpen = true }
    )
    FavoriteDetailMeta(favorite = favorite)
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      favorite.messages.forEach { message ->
        FavoriteMessageBubble(
          message = message,
          canRemove = favorite.messages.size > 1,
          onRemove = { onRemoveMessage(message.id) },
          onOpenAttachment = onOpenAttachment
        )
      }
    }
  }
  if (deleteConfirmOpen) {
    AlertDialog(
      onDismissRequest = { deleteConfirmOpen = false },
      title = { Text("删除收藏") },
      text = { Text("确定要删除这个收藏片段吗？原对话内容不会被删除。") },
      confirmButton = {
        Button(onClick = {
          deleteConfirmOpen = false
          onDelete()
        }) {
          Text("删除")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteConfirmOpen = false }) {
          Text("取消")
        }
      }
    )
  }
}

@Composable
private fun FavoriteDetailActions(
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onCopyText: () -> Unit,
  onEdit: () -> Unit,
  onJumpToSource: () -> Unit,
  onDelete: () -> Unit
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    item {
      CompactFavoriteAction("文本", Icons.Outlined.Share, onShareText)
    }
    item {
      CompactFavoriteAction("长图", Icons.Outlined.Image, onShareImage)
    }
    item {
      CompactFavoriteAction("复制", Icons.Outlined.ContentCopy, onCopyText)
    }
    item {
      CompactFavoriteAction("编辑", Icons.Outlined.Edit, onEdit)
    }
    item {
      CompactFavoriteAction("来源", Icons.AutoMirrored.Outlined.OpenInNew, onJumpToSource)
    }
    item {
      CompactFavoriteAction("删除", Icons.Outlined.Delete, onDelete)
    }
  }
}

@Composable
private fun CompactFavoriteAction(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  AssistChip(
    onClick = onClick,
    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
  )
}

@Composable
private fun FavoriteDetailMeta(favorite: FavoriteSnippet) {
  val parts = buildList {
    if (favorite.description.isNotBlank()) add(favorite.description)
    if (favorite.tags.isNotEmpty()) add(favorite.tags.joinToString("  ") { "#$it" })
  }
  if (parts.isEmpty()) return
  Text(
    text = parts.joinToString(" · "),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.fillMaxWidth()
  )
}

@Composable
private fun FavoriteMessageBubble(
  message: FavoriteSnippetMessage,
  canRemove: Boolean,
  onRemove: () -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit
) {
  val isUser = message.role == MessageRole.USER
  val userColors = userBubbleColors()
  var removeConfirmOpen by remember(message.id) { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = if (isUser) userColors.container else MaterialTheme.colorScheme.surface,
      contentColor = if (isUser) userColors.content else MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.fillMaxWidth(if (isUser) 0.84f else 0.92f)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          "${favoriteRoleLabel(message.role)} · ${formatMessageTime(message.createdAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = if (isUser) userColors.metadata else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isUser) {
          Text(message.content)
        } else {
          MarkdownPreview(message.content)
        }
        if (message.attachments.isNotEmpty()) {
          AttachmentStrip(
            attachments = message.attachments,
            onOpenAttachment = onOpenAttachment,
            onRemoveAttachment = null,
            compact = false
          )
        }
        if (message.status == MessageStatus.FAILED) {
          Text(
            text = message.errorMessage ?: "Request failed",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
        formatFavoriteMessageMetadata(message)?.let { metadata ->
          Text(
            text = metadata,
            style = MaterialTheme.typography.bodySmall,
            color = if (isUser) userColors.metadata else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        TextButton(
          onClick = { removeConfirmOpen = true },
          enabled = canRemove
        ) {
          Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (canRemove) "从收藏移除" else "至少保留一条消息")
        }
      }
    }
  }
  if (removeConfirmOpen) {
    AlertDialog(
      onDismissRequest = { removeConfirmOpen = false },
      title = { Text("移除消息") },
      text = { Text("确定要从这个收藏片段中移除这条消息吗？原对话内容不会被删除。") },
      confirmButton = {
        Button(onClick = {
          removeConfirmOpen = false
          onRemove()
        }) {
          Text("移除")
        }
      },
      dismissButton = {
        TextButton(onClick = { removeConfirmOpen = false }) {
          Text("取消")
        }
      }
    )
  }
}

private fun favoriteRoleLabel(role: MessageRole): String = when (role) {
  MessageRole.USER -> "我"
  MessageRole.ASSISTANT -> "AI"
  MessageRole.SYSTEM -> "系统"
  MessageRole.TOOL -> "工具"
}

private fun formatFavoriteMessageMetadata(message: FavoriteSnippetMessage): String? {
  if (message.role != MessageRole.ASSISTANT) return null
  val parts = mutableListOf<String>()
  message.firstTokenDurationMs?.let { parts += "首 token ${formatDuration(it)}" }
  message.totalDurationMs?.let { parts += "总耗时 ${formatDuration(it)}" }
  if (message.totalTokens != null) {
    val detail = when {
      message.promptTokens != null && message.completionTokens != null ->
        "输入 ${message.promptTokens} / 输出 ${message.completionTokens}"
      message.promptTokens != null -> "输入 ${message.promptTokens}"
      message.completionTokens != null -> "输出 ${message.completionTokens}"
      else -> null
    }
    parts += if (detail == null) {
      "${message.totalTokens} tokens"
    } else {
      "$detail / 总 ${message.totalTokens} tokens"
    }
  }
  return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun defaultFavoriteTitle(state: ChatUiState, messageIds: Set<String>): String {
  val selectedMessages = state.messages.filter { it.id in messageIds }.sortedBy { it.createdAt }
  return selectedMessages
    .firstOrNull { it.role == MessageRole.ASSISTANT && it.content.isNotBlank() }
    ?.content
    ?.lineSequence()
    ?.firstOrNull { it.isNotBlank() }
    ?.trim()
    ?.take(40)
    ?: "${state.selectedConversation?.title?.ifBlank { "对话" } ?: "对话"}（节选）"
}

private fun defaultGroupFavoriteTitle(state: ChatUiState, messageIds: Set<String>): String {
  val selectedMessages = state.groupMessages.filter { it.id in messageIds }.sortedBy { it.createdAt }
  return selectedMessages
    .firstOrNull { it.senderType == GroupMessageSenderType.BOT && it.content.isNotBlank() }
    ?.content
    ?.lineSequence()
    ?.firstOrNull { it.isNotBlank() }
    ?.trim()
    ?.take(40)
    ?: "${state.selectedGroupChat?.title?.ifBlank { "群聊" } ?: "群聊"}（节选）"
}

private enum class GroupBotPickerMode {
  SPEAK,
  SUMMARIZE
}

@Composable
private fun GroupChatPage(
  state: ChatUiState,
  onOpenDrawer: () -> Unit,
  onClose: () -> Unit,
  onNewGroup: () -> Unit,
  onInput: (TextFieldValue) -> Unit,
  onSendUser: () -> Unit,
  onPickImages: () -> Unit,
  onPickFiles: () -> Unit,
  onTakePhoto: () -> Unit,
  onRemoveAttachment: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onBotTurn: (String) -> Unit,
  onSummarize: (String) -> Unit,
  onToggleAutoPlay: () -> Unit,
  onStop: () -> Unit,
  onEditGroup: () -> Unit,
  onDeleteGroup: () -> Unit,
  onFavoriteMessage: (String) -> Unit,
  onFavoriteMessages: (Set<String>) -> Unit,
  selectedMessageIds: Set<String>,
  selectionMode: Boolean,
  onToggleSelectionMode: (Boolean) -> Unit,
  onToggleMessageSelected: (String) -> Unit,
  onSetMessagesSelected: (Set<String>, Boolean) -> Unit,
  onSelectRangeTo: (String) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onShareMessageText: (String) -> Unit,
  onShareMessageImage: (String) -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit,
  onCopyGroup: () -> Unit
) {
  var pickerMode by remember { mutableStateOf<GroupBotPickerMode?>(null) }
  val selectedGroup = state.selectedGroupChat
  val memberBotIds = state.groupMembers.map { it.botId }.toSet()
  val groupBots = state.aiBots
    .filter { it.enabled && (memberBotIds.isEmpty() || it.id in memberBotIds) }
    .sortedWith(compareBy<AiBot> { bot -> state.groupMembers.firstOrNull { it.botId == bot.id }?.sortOrder ?: Int.MAX_VALUE }.thenBy { it.name })

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
        IconButton(onClick = onOpenDrawer) {
          Icon(Icons.Outlined.Menu, contentDescription = "打开聊天列表")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = selectedGroup?.title?.ifBlank { "AI 群聊" } ?: "AI 群聊",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = selectedGroup?.topic?.ifBlank { "手动点名机器人轮流发言" } ?: "选择或新建一个群聊",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        IconButton(onClick = onNewGroup) {
          Icon(Icons.Outlined.Add, contentDescription = "新建群聊")
        }
        IconButton(
          onClick = onToggleAutoPlay,
          enabled = selectedGroup != null && groupBots.isNotEmpty()
        ) {
          Icon(
            imageVector = if (state.isSelectedGroupAutoPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (state.isSelectedGroupAutoPlaying) "暂停轮流" else "开始轮流",
            tint = if (state.isSelectedGroupAutoPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          )
        }
        if (selectedGroup != null) {
          GroupChatOverflowMenu(
            selectionMode = selectionMode,
            selectedCount = selectedMessageIds.size,
            onToggleSelectionMode = onToggleSelectionMode,
            onShareText = onShareText,
            onShareSelected = onShareSelected,
            onShareImage = onShareImage,
            onShareSelectedImage = onShareSelectedImage,
            onFavoriteSelected = onFavoriteSelected,
            onAppendSelectedToFavorite = onAppendSelectedToFavorite,
            onEditGroup = onEditGroup,
            onDeleteGroup = onDeleteGroup,
            onCopyGroup = onCopyGroup
          )
        }
        IconButton(onClick = onClose) {
          Icon(Icons.Outlined.Close, contentDescription = "关闭群聊")
        }
        }
      }

      if (selectedGroup == null) {
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("还没有选择群聊", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.height(10.dp))
          Button(onClick = onNewGroup) {
            Icon(Icons.Outlined.Groups, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("新建 AI 群聊")
          }
        }
      } else {
        if (selectedGroup.summary.isNotBlank() || groupBots.isNotEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (selectedGroup.summary.isNotBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "摘要：${selectedGroup.summary}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
              }
            }
            if (groupBots.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(groupBots, key = { it.id }) { bot ->
                  val colors = botBubbleColors(bot)
                  AssistChip(
                    onClick = { onBotTurn(bot.id) },
                    enabled = !state.isSelectedGroupStreaming,
                    label = { Text("${bot.name} · ${bot.model}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                      Box(
                        modifier = Modifier
                          .size(12.dp)
                          .background(colors.accent, RoundedCornerShape(999.dp))
                      )
                    }
                  )
                }
              }
            }
          }
        }

        GroupMessageList(
          messages = state.groupMessages,
          bots = state.aiBots,
          selectedMessageIds = selectedMessageIds,
          selectionMode = selectionMode,
          onToggleMessageSelected = onToggleMessageSelected,
          onSetMessagesSelected = onSetMessagesSelected,
          onSelectRangeTo = onSelectRangeTo,
          onShareMessageText = onShareMessageText,
          onShareMessageImage = onShareMessageImage,
          onOpenAttachment = onOpenAttachment,
          onFavoriteMessage = onFavoriteMessage,
          onFavoriteMessages = onFavoriteMessages,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isDarkThemeColors()) {
            OutlinedButton(
              onClick = { pickerMode = GroupBotPickerMode.SPEAK },
              enabled = groupBots.isNotEmpty() && !state.isSelectedGroupStreaming,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Outlined.Groups, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("点名发言")
            }
          } else {
            Button(
              onClick = { pickerMode = GroupBotPickerMode.SPEAK },
              enabled = groupBots.isNotEmpty() && !state.isSelectedGroupStreaming,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Outlined.Groups, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("点名发言")
            }
          }
          TextButton(
            onClick = { pickerMode = GroupBotPickerMode.SUMMARIZE },
            enabled = groupBots.isNotEmpty() && state.groupMessages.isNotEmpty() && !state.isSelectedGroupStreaming
          ) {
            Text("总结讨论")
          }
        }

        Composer(
          input = state.groupInput,
          attachments = state.pendingAttachments,
          attachmentsEnabled = true,
          onInput = onInput,
          onSend = onSendUser,
          onRetry = {},
          onPickImages = onPickImages,
          onPickFiles = onPickFiles,
          onTakePhoto = onTakePhoto,
          onRemoveAttachment = onRemoveAttachment,
          onOpenAttachment = onOpenAttachment,
          isGenerating = state.isSelectedGroupStreaming,
          onStopGenerating = onStop,
          showRetry = false
        )
      }
    }
  }

  pickerMode?.let { mode ->
    GroupBotPickerDialog(
      title = if (mode == GroupBotPickerMode.SPEAK) "选择发言机器人" else "选择总结机器人",
      bots = groupBots,
      onDismiss = { pickerMode = null },
      onSelect = { botId ->
        pickerMode = null
        if (mode == GroupBotPickerMode.SPEAK) {
          onBotTurn(botId)
        } else {
          onSummarize(botId)
        }
      }
    )
  }
}

@Composable
private fun GroupChatOverflowMenu(
  selectionMode: Boolean,
  selectedCount: Int,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareText: () -> Unit,
  onShareSelected: () -> Unit,
  onShareImage: () -> Unit,
  onShareSelectedImage: () -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit,
  onEditGroup: () -> Unit,
  onDeleteGroup: () -> Unit,
  onCopyGroup: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { menuOpen = true }) {
      Icon(Icons.Outlined.MoreVert, contentDescription = "群聊操作")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text(if (selectionMode) "退出多选" else "多选消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        onClick = {
          menuOpen = false
          onToggleSelectionMode(!selectionMode)
        }
      )
      DropdownMenuItem(
        text = { Text("分享全文文本") },
        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareText()
        }
      )
      DropdownMenuItem(
        text = { Text("分享选中消息") },
        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("收藏选中消息") },
        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onFavoriteSelected()
        }
      )
      DropdownMenuItem(
        text = { Text("追加到已有收藏") },
        leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onAppendSelectedToFavorite()
        }
      )
      DropdownMenuItem(
        text = { Text("生成长图分享") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareImage()
        }
      )
      DropdownMenuItem(
        text = { Text("选中消息生成长图") },
        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
        enabled = selectedCount > 0,
        onClick = {
          menuOpen = false
          onShareSelectedImage()
        }
      )
      DropdownMenuItem(
        text = { Text("编辑群聊") },
        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        onClick = {
          menuOpen = false
          onEditGroup()
        }
      )
      DropdownMenuItem(
        text = { Text("删除群聊") },
        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        onClick = {
          menuOpen = false
          onDeleteGroup()
        }
      )
      DropdownMenuItem(
        text = { Text("复制群聊") },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
          menuOpen = false
          onCopyGroup()
        }
      )
    }
  }
}

@Composable
private fun GroupMessageList(
  messages: List<GroupChatMessage>,
  bots: List<AiBot>,
  selectedMessageIds: Set<String>,
  selectionMode: Boolean,
  onToggleMessageSelected: (String) -> Unit,
  onSetMessagesSelected: (Set<String>, Boolean) -> Unit,
  onSelectRangeTo: (String) -> Unit,
  onShareMessageText: (String) -> Unit,
  onShareMessageImage: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onFavoriteMessage: (String) -> Unit,
  onFavoriteMessages: (Set<String>) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val groupId = messages.firstOrNull()?.groupId
  var autoFollow by remember(groupId) { mutableStateOf(true) }
  var expandedBotMessageIds by remember(groupId) { mutableStateOf<Set<String>>(emptySet()) }
  val hasStreaming = messages.any { it.status == MessageStatus.STREAMING }
  var lastAutoFollowAt by remember(groupId) { mutableStateOf(0L) }
  var scrollHintVisible by remember(groupId) { mutableStateOf(false) }
  var longBubbleActionsExpanded by remember(groupId) { mutableStateOf(false) }
  val latestBotMessageId = messages.lastOrNull { it.senderType == GroupMessageSenderType.BOT }?.id
  val botsById = remember(bots) { bots.associateBy { it.id } }
  val listItems = remember(messages) { groupMessageListItems(messages) }
  val bottomAnchorIndex = listItems.size
  val visibleRangeTargetId by remember(selectionMode, selectedMessageIds, messages) {
    derivedStateOf {
      if (!selectionMode || selectedMessageIds.isEmpty()) {
        null
      } else {
        listState.layoutInfo.visibleItemsInfo
          .mapNotNull { item -> listItems.getOrNull(item.index) }
          .flatMap { it.messageIds }
          .firstOrNull { it !in selectedMessageIds }
      }
    }
  }
  val showScrollToBottom by remember(messages.size) {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      val lastItem = listState.layoutInfo.totalItemsCount - 1
      lastItem > 0 && lastVisible < lastItem - 1
    }
  }
  val groupLongBubbleNavTarget by remember(scrollHintVisible, listItems, expandedBotMessageIds, latestBotMessageId) {
    derivedStateOf {
      if (!scrollHintVisible) {
        null
      } else {
        val visibleBotCandidates = listItems.mapIndexedNotNull { index, item ->
          val message = (item as? GroupMessageListItem.Message)?.message ?: return@mapIndexedNotNull null
          val isExpandedBot = message.senderType == GroupMessageSenderType.BOT &&
            (message.status == MessageStatus.STREAMING || message.id == latestBotMessageId || message.id in expandedBotMessageIds)
          if (isExpandedBot) index to message.id else null
        }.toMap()
        lazyListLongBubbleNavTarget(
          visibleItems = listState.layoutInfo.visibleItemsInfo,
          viewportStart = listState.layoutInfo.viewportStartOffset,
          viewportEnd = listState.layoutInfo.viewportEndOffset,
          candidates = visibleBotCandidates
        )
      }
    }
  }

  LaunchedEffect(listState, groupId) {
    snapshotFlow { listState.isScrollInProgress to listState.isAtBottom() }
      .collect { (scrolling, atBottom) ->
        if (scrolling) scrollHintVisible = true
        if (scrolling && !atBottom) autoFollow = false
        if (atBottom) autoFollow = true
      }
  }

  LaunchedEffect(scrollHintVisible, listState.isScrollInProgress, longBubbleActionsExpanded) {
    if (scrollHintVisible && !listState.isScrollInProgress && !longBubbleActionsExpanded) {
      delay(2_500)
      if (!listState.isScrollInProgress && !longBubbleActionsExpanded) scrollHintVisible = false
    }
  }

  LaunchedEffect(messages.size, messages.lastOrNull()?.content, hasStreaming, autoFollow) {
    if (autoFollow && messages.isNotEmpty()) {
      val now = System.currentTimeMillis()
      if (!hasStreaming || now - lastAutoFollowAt > 120L) {
        lastAutoFollowAt = now
        listState.scrollToItem(bottomAnchorIndex)
      }
    }
  }

  Box(modifier = modifier) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(listItems, key = { it.key }) { item ->
        when (item) {
          is GroupMessageListItem.Message -> {
            val message = item.message
            val isBotMessage = message.senderType == GroupMessageSenderType.BOT
            val forceExpanded = isBotMessage && (
              message.status == MessageStatus.STREAMING ||
                message.id == latestBotMessageId
              )
            val expanded = !isBotMessage ||
              forceExpanded ||
              message.id in expandedBotMessageIds
            GroupMessageBubble(
              message = message,
              bot = message.botId?.let { botsById[it] },
              expanded = expanded,
              canToggleExpanded = isBotMessage && !forceExpanded,
              onToggleExpanded = if (isBotMessage && !forceExpanded) {
                {
                  expandedBotMessageIds = if (message.id in expandedBotMessageIds) {
                    expandedBotMessageIds - message.id
                  } else {
                    expandedBotMessageIds + message.id
                  }
                }
              } else {
                null
              },
              selected = message.id in selectedMessageIds,
              selectionMode = selectionMode,
              canSelectRangeTo = selectionMode && selectedMessageIds.isNotEmpty() && message.id !in selectedMessageIds,
              onToggleSelected = { onToggleMessageSelected(message.id) },
              onSelectRangeTo = { onSelectRangeTo(message.id) },
              onShareText = { onShareMessageText(message.id) },
              onShareImage = { onShareMessageImage(message.id) },
              onOpenAttachment = onOpenAttachment,
              onFavorite = { onFavoriteMessage(message.id) }
            )
          }
          is GroupMessageListItem.ToolGroup -> {
            val first = item.messages.first()
            val forceExpanded = item.messages.any { it.status == MessageStatus.STREAMING } ||
              first.id == latestBotMessageId
            val expanded = forceExpanded || first.id in expandedBotMessageIds
            val toolIds = item.messages.map { it.id }.toSet()
            val selected = toolIds.all { it in selectedMessageIds }
            GroupToolMessageBubble(
              messages = item.messages,
              bot = first.botId?.let { botsById[it] },
              expanded = expanded,
              canToggleExpanded = !forceExpanded,
              onToggleExpanded = if (!forceExpanded) {
                {
                  expandedBotMessageIds = if (first.id in expandedBotMessageIds) {
                    expandedBotMessageIds - first.id
                  } else {
                    expandedBotMessageIds + first.id
                  }
                }
              } else {
                null
              },
              selected = selected,
              selectionMode = selectionMode,
              canSelectRangeTo = selectionMode && selectedMessageIds.isNotEmpty() && item.messages.any { it.id !in selectedMessageIds },
              onToggleSelected = { onSetMessagesSelected(toolIds, !selected) },
              onSelectRangeTo = { onSelectRangeTo(first.id) },
              onShareText = { shareText(context, item.messages.joinToString("\n\n") { it.content }, "分享工具调用") },
              onShareImage = { onShareMessageImage(first.id) },
              onFavorite = { onFavoriteMessages(toolIds) }
            )
          }
        }
      }
      item(key = "group-message-list-bottom-anchor") {
        Spacer(Modifier.height(1.dp))
      }
    }
    val rangeTargetId = visibleRangeTargetId
    if (rangeTargetId != null) {
      Button(
        onClick = { onSelectRangeTo(rangeTargetId) },
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 18.dp, top = 8.dp)
      ) {
        Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("选择到这里")
      }
    }
    MessageScrollIndicator(
      listState = listState,
      onDragProgress = { progress ->
        autoFollow = false
        scrollHintVisible = true
        scope.launch {
          val total = listState.layoutInfo.totalItemsCount
          if (total > 0) {
            listState.scrollToItem(
              listState.itemIndexForProgress(progress).coerceIn(0, total - 1)
            )
          }
        }
      },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 4.dp)
    )
    if (showScrollToBottom && scrollHintVisible) {
      Button(
        onClick = {
          scope.launch {
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) {
              listState.animateScrollToItem(last)
              autoFollow = true
            }
          }
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp)
      ) {
        Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("回到底部")
      }
    }
    LongBubbleNavOverlay(
      target = groupLongBubbleNavTarget,
      onJumpTop = { target ->
        autoFollow = false
        scrollHintVisible = true
        scope.launch { listState.animateScrollToItem(target.index) }
      },
      onJumpBottom = { target ->
        autoFollow = false
        scrollHintVisible = true
        scope.launch { listState.animateScrollToItem(target.index, target.bottomOffset) }
      },
      onCopy = { target ->
        target.messageId
          ?.let { id -> messages.firstOrNull { it.id == id } }
          ?.let { copyToClipboard(context, it.content) }
      },
      onShareText = { target ->
        target.messageId?.let(onShareMessageText)
      },
      onShareImage = { target ->
        target.messageId?.let(onShareMessageImage)
      },
      onFavorite = { target ->
        target.messageId?.let(onFavoriteMessage)
      },
      onActionsExpandedChange = { longBubbleActionsExpanded = it },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 22.dp)
    )
  }
}

internal fun groupMessageListItems(messages: List<GroupChatMessage>): List<GroupMessageListItem> {
  val result = mutableListOf<GroupMessageListItem>()
  val toolGroups = messages
    .filter { it.senderType == GroupMessageSenderType.TOOL }
    .groupBy { it.groupTurnKey() }
    .mapValues { (_, tools) -> tools.sortedBy { it.createdAt } }
  val emittedToolKeys = mutableSetOf<GroupTurnKey>()

  messages.forEach { message ->
    if (message.senderType == GroupMessageSenderType.TOOL) {
      val key = message.groupTurnKey()
      if (emittedToolKeys.add(key)) {
        result += GroupMessageListItem.ToolGroup(toolGroups.getValue(key))
      }
    } else {
      if (message.senderType == GroupMessageSenderType.BOT) {
        val key = message.groupTurnKey()
        val tools = toolGroups[key].orEmpty()
        if (tools.isNotEmpty() && emittedToolKeys.add(key)) {
          result += GroupMessageListItem.ToolGroup(tools)
        }
      }
      result += GroupMessageListItem.Message(message)
    }
  }
  return result
}

internal fun chatMessageListItems(messages: List<ChatMessage>): List<ChatMessageListItem> {
  val result = mutableListOf<ChatMessageListItem>()
  val pendingTools = mutableListOf<ChatMessage>()
  messages.forEach { message ->
    if (message.role == MessageRole.TOOL) {
      pendingTools += message
    } else {
      if (pendingTools.isNotEmpty()) {
        result += ChatMessageListItem.ToolGroup(pendingTools.toList())
        pendingTools.clear()
      }
      result += ChatMessageListItem.Message(message)
    }
  }
  if (pendingTools.isNotEmpty()) {
    result += ChatMessageListItem.ToolGroup(pendingTools.toList())
  }
  return result
}

private data class GroupTurnKey(
  val groupId: String,
  val botId: String?,
  val trigger: GroupTurnTrigger,
  val round: Int?,
  val index: Int?,
  val memberCount: Int?
)

private fun GroupChatMessage.groupTurnKey(): GroupTurnKey = GroupTurnKey(
  groupId = groupId,
  botId = botId,
  trigger = turnTrigger,
  round = turnRound,
  index = turnIndex,
  memberCount = turnMemberCount
)

private fun lazyListLongBubbleNavTarget(
  visibleItems: List<LazyListItemInfo>,
  viewportStart: Int,
  viewportEnd: Int,
  candidates: Map<Int, String>
): LongBubbleNavTarget? = longBubbleNavTarget(
  visibleItems = visibleItems.map { item ->
    VisibleListItemBounds(
      index = item.index,
      offset = item.offset,
      size = item.size,
      messageId = candidates[item.index],
      supportsActions = candidates.containsKey(item.index)
    )
  },
  viewportStart = viewportStart,
  viewportEnd = viewportEnd,
  candidateIndexes = candidates.keys
)

internal fun longBubbleNavTarget(
  visibleItems: List<VisibleListItemBounds>,
  viewportStart: Int,
  viewportEnd: Int,
  candidateIndexes: Set<Int>
): LongBubbleNavTarget? {
  if (candidateIndexes.isEmpty() || viewportEnd <= viewportStart) return null
  val viewportHeight = viewportEnd - viewportStart
  val viewportCenter = viewportStart + viewportHeight / 2
  return visibleItems
    .asSequence()
    .filter { it.index in candidateIndexes }
    .mapNotNull { item ->
      val itemTop = item.offset
      val itemBottom = item.offset + item.size
      val showUp = itemTop < viewportStart
      val showDown = itemBottom > viewportEnd
      val longEnough = item.size > viewportHeight
      if (!showUp && !showDown) return@mapNotNull null
      if (!longEnough && itemTop >= viewportStart && itemBottom <= viewportEnd) return@mapNotNull null
      val distanceToCenter = kotlin.math.abs((itemTop + item.size / 2) - viewportCenter)
      val bottomOffset = (item.size - viewportHeight).coerceAtLeast(0)
      distanceToCenter to LongBubbleNavTarget(
        index = item.index,
        showUp = showUp,
        showDown = showDown,
        bottomOffset = bottomOffset,
        messageId = item.messageId,
        showActions = showUp && item.supportsActions && item.messageId != null
      )
    }
    .minByOrNull { it.first }
    ?.second
}

@Composable
private fun LongBubbleNavOverlay(
  target: LongBubbleNavTarget?,
  onJumpTop: (LongBubbleNavTarget) -> Unit,
  onJumpBottom: (LongBubbleNavTarget) -> Unit,
  onCopy: (LongBubbleNavTarget) -> Unit,
  onShareText: (LongBubbleNavTarget) -> Unit,
  onShareImage: (LongBubbleNavTarget) -> Unit,
  onFavorite: (LongBubbleNavTarget) -> Unit,
  onActionsExpandedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  if (target == null || (!target.showUp && !target.showDown && !target.showActions)) return
  var actionsExpanded by remember(target.index, target.messageId) { mutableStateOf(false) }
  DisposableEffect(actionsExpanded) {
    onActionsExpandedChange(actionsExpanded)
    onDispose {
      if (actionsExpanded) onActionsExpandedChange(false)
    }
  }
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(999.dp),
    modifier = modifier
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
  ) {
    Column(
      modifier = Modifier.padding(vertical = 3.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (target.showUp) {
        IconButton(onClick = { onJumpTop(target) }, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "跳到气泡顶部")
        }
      }
      if (target.showActions) {
        Box {
          IconButton(onClick = { actionsExpanded = true }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "更多气泡操作")
          }
          DropdownMenu(
            expanded = actionsExpanded,
            onDismissRequest = { actionsExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("复制") },
              leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
              onClick = {
                actionsExpanded = false
                onCopy(target)
              }
            )
            DropdownMenuItem(
              text = { Text("分享文本") },
              leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
              onClick = {
                actionsExpanded = false
                onShareText(target)
              }
            )
            DropdownMenuItem(
              text = { Text("分享长图") },
              leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
              onClick = {
                actionsExpanded = false
                onShareImage(target)
              }
            )
            DropdownMenuItem(
              text = { Text("收藏") },
              leadingIcon = { Icon(Icons.Outlined.Bookmark, contentDescription = null) },
              onClick = {
                actionsExpanded = false
                onFavorite(target)
              }
            )
          }
        }
      }
      if (target.showDown) {
        IconButton(onClick = { onJumpBottom(target) }, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "跳到气泡底部")
        }
      }
    }
  }
}

private fun groupTurnLabel(message: GroupChatMessage): String? {
  return when (message.turnTrigger) {
    GroupTurnTrigger.AUTO -> {
      val round = message.turnRound ?: return "自动发言"
      val index = message.turnIndex ?: return "自动第 $round 轮"
      val total = message.turnMemberCount
      if (total != null && total > 0) {
        "自动第 $round 轮 · 第 $index/$total 个发言"
      } else {
        "自动第 $round 轮 · 第 $index 个发言"
      }
    }
    GroupTurnTrigger.MANUAL -> message.turnIndex?.let { "点名第 $it 次发言" } ?: "点名发言"
    GroupTurnTrigger.SUMMARY -> message.turnIndex?.let { "总结第 $it 次" } ?: "总结发言"
    GroupTurnTrigger.UNKNOWN -> null
  }
}

@Composable
private fun GroupMessageBubble(
  message: GroupChatMessage,
  bot: AiBot?,
  expanded: Boolean,
  canToggleExpanded: Boolean,
  onToggleExpanded: (() -> Unit)?,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onFavorite: () -> Unit
) {
  val context = LocalContext.current
  var shareMenuOpen by remember(message.id) { mutableStateOf(false) }
  if (message.senderType == GroupMessageSenderType.TOOL) {
    ToolCallItem(
      message = message.toChatMessage(),
      selected = false,
      selectionMode = false,
      canSelectRangeTo = false,
      onToggleSelected = {},
      onSelectRangeTo = {},
      onCopy = { copyToClipboard(context, message.content) },
      onFavorite = onFavorite
    )
    return
  }
  val isUser = message.senderType == GroupMessageSenderType.USER
  val isBot = message.senderType == GroupMessageSenderType.BOT
  val botColors = botBubbleColors(bot)
  val useBotMarkdownColors = isBot && isDarkThemeColors()
  val markdownColors = remember(botColors, useBotMarkdownColors) {
    if (useBotMarkdownColors) markdownColorsForBotBubble(botColors) else null
  }
  val userColors = userBubbleColors()
  val contentColor = when {
    isUser -> userColors.content
    isBot -> botColors.content
    else -> MaterialTheme.colorScheme.onSurface
  }
  val metadataColor = when {
    isUser -> userColors.metadata
    isBot -> botColors.content.copy(alpha = 0.74f)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        isUser -> userColors.container
        message.status == MessageStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        isBot -> botColors.container
        else -> MaterialTheme.colorScheme.surface
      },
      contentColor = contentColor,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.84f else 0.92f)
        .then(
          if (isBot) {
            Modifier.border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else botColors.accent, RoundedCornerShape(8.dp))
          } else if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
          } else {
            Modifier
          }
        )
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isBot) {
            Box(
              modifier = Modifier
                .size(12.dp)
                .background(botColors.accent, RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.width(7.dp))
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = message.senderName.ifBlank { if (isUser) "用户" else "AI" },
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            groupTurnLabel(message)?.let { label ->
              Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = metadataColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = listOfNotNull(message.model, formatMessageTime(message.createdAt)).joinToString(" · "),
              style = MaterialTheme.typography.bodySmall,
              color = metadataColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(onClick = { copyToClipboard(context, message.content) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "复制群消息")
          }
          Box {
            IconButton(onClick = { shareMenuOpen = true }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Outlined.Share, contentDescription = "分享群消息")
            }
            DropdownMenu(expanded = shareMenuOpen, onDismissRequest = { shareMenuOpen = false }) {
              DropdownMenuItem(
                text = { Text("分享文本") },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                  shareMenuOpen = false
                  onShareText()
                }
              )
              DropdownMenuItem(
                text = { Text("分享长图") },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                onClick = {
                  shareMenuOpen = false
                  onShareImage()
                }
              )
            }
          }
          IconButton(onClick = onFavorite, enabled = message.status != MessageStatus.STREAMING, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Bookmark, contentDescription = "收藏群消息")
          }
        }
        if (isUser) {
          Text(message.content)
        } else if (isBot && !expanded) {
          Text(
            text = collapsedGroupMessageSummary(message),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
          )
        } else {
          SelectionContainer {
            MarkdownPreview(
              content = message.content.ifBlank { if (message.status == MessageStatus.STREAMING) "..." else "" },
              colors = markdownColors
            )
          }
        }
        if (message.attachments.isNotEmpty()) {
          AttachmentStrip(
            attachments = message.attachments,
            onOpenAttachment = onOpenAttachment,
            onRemoveAttachment = null,
            compact = false
          )
        }
        if (message.status == MessageStatus.FAILED) {
          Text(
            text = message.errorMessage ?: "请求失败",
            color = if (message.errorMessage == "已停止") metadataColor else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
        formatGroupMessageMetadata(message)?.let { metadata ->
          Text(
            text = metadata,
            style = MaterialTheme.typography.bodySmall,
            color = metadataColor
          )
        }
        if (selectionMode) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
            if (canSelectRangeTo) {
              TextButton(onClick = onSelectRangeTo) {
                Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("选择到这里")
              }
            }
          }
        }
        if (isBot && canToggleExpanded && onToggleExpanded != null) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CompactExpandToggle(expanded = expanded, onClick = onToggleExpanded)
          }
        }
      }
    }
  }
}

@Composable
private fun GroupToolMessageBubble(
  messages: List<GroupChatMessage>,
  bot: AiBot?,
  expanded: Boolean,
  canToggleExpanded: Boolean,
  onToggleExpanded: (() -> Unit)?,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onFavorite: () -> Unit
) {
  if (messages.isEmpty()) return
  val context = LocalContext.current
  val first = messages.first()
  val colors = botBubbleColors(bot)
  ToolCallGroupBubble(
    messages = messages.map { it.toChatMessage() },
    title = "${first.senderName.ifBlank { bot?.name ?: "AI" }} · 工具调用",
    metadata = listOfNotNull(
      groupTurnLabel(first),
      first.model,
      formatMessageTime(first.createdAt)
    ).joinToString(" · "),
    selected = selected,
    selectionMode = selectionMode,
    canSelectRangeTo = canSelectRangeTo,
    onToggleSelected = onToggleSelected,
    onSelectRangeTo = onSelectRangeTo,
    onCopy = { copyToClipboard(context, messages.joinToString("\n\n") { it.content }) },
    onShareText = onShareText,
    onShareImage = onShareImage,
    onFavorite = onFavorite,
    colors = defaultToolCallBubbleColors(colors),
    expanded = expanded,
    canToggleExpanded = canToggleExpanded,
    onToggleExpanded = onToggleExpanded
  )
}

private fun GroupChatMessage.toChatMessage(): ChatMessage {
  return ChatMessage(
    id = id,
    conversationId = groupId,
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
}

internal fun collapsedGroupMessageSummary(message: GroupChatMessage): String {
  if (message.content.isBlank()) {
    return if (message.status == MessageStatus.STREAMING) "输出中..." else "无内容"
  }
  val firstLine = message.content
    .lineSequence()
    .map { it.trim() }
    .firstOrNull { it.isNotBlank() }
    .orEmpty()
  val compact = firstLine.replace(Regex("\\s+"), " ")
  return if (compact.length <= 30) compact else compact.take(30) + "..."
}

private fun groupToolSummary(details: List<ToolCallDetails>, count: Int, streaming: Boolean): String {
  val names = details.map { it.name }.distinct().joinToString("、").ifBlank { "tool" }
  val summary = details.asSequence()
    .mapNotNull { it.summary }
    .firstOrNull { it.isNotBlank() }
  return buildString {
    append(if (streaming) "正在调用" else "已调用")
    append(" $count 次工具")
    append("：")
    append(names)
    summary?.let {
      append(" · ")
      append(it)
    }
  }
}

private fun formatGroupMessageMetadata(message: GroupChatMessage): String? {
  val parts = mutableListOf<String>()
  if (message.status == MessageStatus.STREAMING) parts += "输出中"
  message.firstTokenDurationMs?.let { parts += "首 token ${formatDuration(it)}" }
  message.totalDurationMs?.let { parts += "耗时 ${formatDuration(it)}" }
  if (message.totalTokens != null) {
    val detail = when {
      message.promptTokens != null && message.completionTokens != null ->
        "输入 ${message.promptTokens} / 输出 ${message.completionTokens}"
      message.promptTokens != null -> "输入 ${message.promptTokens}"
      message.completionTokens != null -> "输出 ${message.completionTokens}"
      else -> null
    }
    parts += if (detail == null) "${message.totalTokens} tokens" else "$detail / 总 ${message.totalTokens} tokens"
  }
  return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun GroupBotPickerDialog(
  title: String,
  bots: List<AiBot>,
  onDismiss: () -> Unit,
  onSelect: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      if (bots.isEmpty()) {
        Text("当前群聊没有可用机器人。")
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          bots.forEach { bot ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(bot.id) }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(bot.name, fontWeight = FontWeight.SemiBold)
                Text(bot.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun BotManagerPage(
  providers: List<ChatProviderConfig>,
  bots: List<AiBot>,
  onDismiss: () -> Unit,
  onCreate: (String, String, String, String, String) -> Unit,
  onUpdate: (String, String, String, String, String, String) -> Unit,
  onToggleEnabled: (String, Boolean) -> Unit,
  onDelete: (String) -> Unit
) {
  var editingBot by remember { mutableStateOf<AiBot?>(null) }
  var creating by remember { mutableStateOf(false) }
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "关闭机器人管理")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("AI 机器人", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          Text("常驻群聊成员，绑定固定 Provider/model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { creating = true }, enabled = providers.isNotEmpty()) {
          Icon(Icons.Outlined.Add, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("新建")
        }
      }
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (bots.isEmpty()) {
          item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
              Text("还没有机器人。先从已有 API 配置创建一个机器人，再把它加入群聊。", modifier = Modifier.padding(14.dp))
            }
          }
        }
        items(bots, key = { it.id }) { bot ->
          val providerName = providers.firstOrNull { it.id == bot.providerId }?.displayName ?: bot.providerId
          val colors = botBubbleColors(bot)
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(18.dp)
                    .background(colors.accent, RoundedCornerShape(999.dp))
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(bot.name, fontWeight = FontWeight.SemiBold)
                  Text(
                    "$providerName · ${bot.model} · ${colors.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Switch(checked = bot.enabled, onCheckedChange = { onToggleEnabled(bot.id, it) })
              }
              if (bot.systemPrompt.isNotBlank()) {
                Text(bot.systemPrompt, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { editingBot = bot }) {
                  Icon(Icons.Outlined.Edit, contentDescription = null)
                  Spacer(Modifier.width(4.dp))
                  Text("编辑")
                }
                TextButton(onClick = { onDelete(bot.id) }) {
                  Icon(Icons.Outlined.Delete, contentDescription = null)
                  Spacer(Modifier.width(4.dp))
                  Text("删除")
                }
              }
            }
          }
        }
      }
    }
  }
  if (creating) {
    BotEditorDialog(
      title = "新建机器人",
      providers = providers,
      bot = null,
      onDismiss = { creating = false },
      onSave = { name, providerId, model, prompt, colorKey ->
        onCreate(name, providerId, model, prompt, colorKey)
        creating = false
      }
    )
  }
  editingBot?.let { bot ->
    BotEditorDialog(
      title = "编辑机器人",
      providers = providers,
      bot = bot,
      onDismiss = { editingBot = null },
      onSave = { name, providerId, model, prompt, colorKey ->
        onUpdate(bot.id, name, providerId, model, prompt, colorKey)
        editingBot = null
      }
    )
  }
}

@Composable
private fun BotEditorDialog(
  title: String,
  providers: List<ChatProviderConfig>,
  bot: AiBot?,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String) -> Unit
) {
  val initialProvider = providers.firstOrNull { it.id == bot?.providerId } ?: providers.firstOrNull()
  var name by remember(bot?.id) { mutableStateOf(bot?.name ?: "") }
  var providerId by remember(bot?.id, providers) { mutableStateOf(initialProvider?.id.orEmpty()) }
  var model by remember(bot?.id, providers) { mutableStateOf(bot?.model ?: initialProvider?.defaultModel.orEmpty()) }
  var prompt by remember(bot?.id) { mutableStateOf(bot?.systemPrompt ?: "") }
  var bubbleColorKey by remember(bot?.id) { mutableStateOf(bot?.bubbleColorKey ?: "AUTO") }
  val selectedProvider = providers.firstOrNull { it.id == providerId }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 520.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("机器人名称") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Text("绑定 Provider", fontWeight = FontWeight.SemiBold)
        providers.forEach { provider ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                providerId = provider.id
                if (model.isBlank() || model == selectedProvider?.defaultModel) {
                  model = provider.defaultModel
                }
              }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(selected = provider.id == providerId, onClick = {
              providerId = provider.id
              if (model.isBlank() || model == selectedProvider?.defaultModel) {
                model = provider.defaultModel
              }
            })
            Column(modifier = Modifier.weight(1f)) {
              Text(provider.displayName)
              Text(provider.defaultModel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
        OutlinedTextField(
          value = model,
          onValueChange = { model = it },
          label = { Text("模型名") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Text("气泡颜色", fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item(key = "AUTO") {
            BotColorChoice(
              label = "自动",
              selected = bubbleColorKey == "AUTO",
              colors = botBubbleColors(bot?.copy(bubbleColorKey = "AUTO") ?: AiBot(
                id = "preview_auto",
                name = name,
                providerId = providerId,
                model = model,
                systemPrompt = prompt,
                bubbleColorKey = "AUTO",
                enabled = true,
                createdAt = 0,
                updatedAt = 0
              )),
              onClick = { bubbleColorKey = "AUTO" }
            )
          }
          items(BotBubblePalettes, key = { it.key }) { palette ->
            BotColorChoice(
              label = palette.label,
              selected = bubbleColorKey == palette.key,
              colors = botBubbleColors(bot?.copy(bubbleColorKey = palette.key) ?: AiBot(
                id = "preview_${palette.key}",
                name = name,
                providerId = providerId,
                model = model,
                systemPrompt = prompt,
                bubbleColorKey = palette.key,
                enabled = true,
                createdAt = 0,
                updatedAt = 0
              )),
              onClick = { bubbleColorKey = palette.key }
            )
          }
        }
        OutlinedTextField(
          value = prompt,
          onValueChange = { prompt = it },
          label = { Text("角色提示词") },
          minLines = 4,
          maxLines = 8,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name.trim(), providerId, model.trim(), prompt.trim(), bubbleColorKey) },
        enabled = name.isNotBlank() && providerId.isNotBlank() && model.isNotBlank()
      ) {
        Text("保存")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun BotColorChoice(
  label: String,
  selected: Boolean,
  colors: BotBubbleColors,
  onClick: () -> Unit
) {
  Surface(
    color = if (selected) colors.container else MaterialTheme.colorScheme.surfaceVariant,
    contentColor = if (selected) colors.content else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
      .width(74.dp)
      .border(
        width = if (selected) 2.dp else 1.dp,
        color = if (selected) colors.accent else MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Box(
        modifier = Modifier
          .size(18.dp)
          .background(colors.accent, RoundedCornerShape(999.dp))
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun NewGroupChatDialog(
  bots: List<AiBot>,
  backgroundPresets: List<ChatBackgroundPreset>,
  title: String = "新建 AI 群聊",
  confirmText: String = "创建",
  initialTitle: String = "",
  initialTopic: String = "",
  initialSelectedBotIds: Set<String> = emptySet(),
  onDismiss: () -> Unit,
  onCreate: (String, String, List<String>) -> Unit
) {
  var groupTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
  var topic by remember(initialTopic) { mutableStateOf(initialTopic) }
  var selectedBotIds by remember(initialSelectedBotIds) { mutableStateOf(initialSelectedBotIds) }
  var presetPickerOpen by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 520.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = groupTitle,
          onValueChange = { groupTitle = it },
          label = { Text("群聊标题") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("讨论主题") },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth()
        )
        TextButton(
          onClick = { presetPickerOpen = true },
          enabled = backgroundPresets.isNotEmpty()
        ) {
          Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("插入背景预设")
        }
        Text("选择机器人", fontWeight = FontWeight.SemiBold)
        if (bots.isEmpty()) {
          Text("还没有启用的机器人，请先到设置里创建。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          bots.forEach { bot ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedBotIds = if (bot.id in selectedBotIds) selectedBotIds - bot.id else selectedBotIds + bot.id
                }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = bot.id in selectedBotIds,
                onCheckedChange = { checked ->
                  selectedBotIds = if (checked) selectedBotIds + bot.id else selectedBotIds - bot.id
                }
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(bot.name)
                Text(bot.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalTitle = groupTitle.trim().ifBlank { topic.trim().lineSequence().firstOrNull()?.take(24) ?: "AI 群聊" }
          onCreate(finalTitle, topic.trim(), selectedBotIds.toList())
        },
        enabled = selectedBotIds.isNotEmpty()
      ) {
        Text(confirmText)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
  if (presetPickerOpen) {
    BackgroundPresetPickerDialog(
      presets = backgroundPresets,
      onDismiss = { presetPickerOpen = false },
      onSelect = { preset ->
        topic = appendPresetText(topic, preset.content)
        presetPickerOpen = false
      }
    )
  }
}

@Composable
private fun BackgroundPresetPickerDialog(
  presets: List<ChatBackgroundPreset>,
  onDismiss: () -> Unit,
  onSelect: (ChatBackgroundPreset) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("插入背景预设") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        presets.sortedBy { it.sortOrder }.forEach { preset ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(preset) }
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(preset.title, fontWeight = FontWeight.SemiBold)
              Text(
                preset.content,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

private fun appendPresetText(current: String, presetContent: String): String {
  val clean = presetContent.trim()
  if (clean.isBlank()) return current
  return if (current.isBlank()) clean else "${current.trimEnd()}\n\n$clean"
}

private fun shareText(context: Context, text: String, title: String) {
  if (text.isBlank()) return
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, text)
  }
  context.startActivity(Intent.createChooser(intent, title))
}

@Composable
private fun AppSettingsPage(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onOpenProviderManager: () -> Unit,
  onPalette: (AppThemePalette) -> Unit,
  onThemeMode: (AppThemeMode) -> Unit,
  onFontScale: (Float) -> Unit,
  onDebugResponseLogging: (Boolean) -> Unit,
  onWebSearchMode: (WebSearchMode) -> Unit,
  onAttachmentMaxFileMb: (Int) -> Unit,
  onAttachmentMaxPendingMb: (Int) -> Unit,
  onAttachmentMaxImageSourceMb: (Int) -> Unit,
  onExportProviderConfigs: () -> Unit,
  onImportProviderConfigs: (String) -> Unit,
  onOpenBotManager: () -> Unit,
  onSaveBackgroundPreset: (ChatBackgroundPreset?, String, String) -> Unit,
  onDeleteBackgroundPreset: (String) -> Unit,
  onMoveBackgroundPreset: (String, Int) -> Unit
) {
  var importDialogOpen by remember { mutableStateOf(false) }
  var editingPreset by remember { mutableStateOf<ChatBackgroundPreset?>(null) }
  var creatingPreset by remember { mutableStateOf(false) }
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "关闭设置")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
          Text("主题、显示和 API 配置", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        SettingsSection(title = "外观") {
          Text("主题色", fontWeight = FontWeight.SemiBold)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = AppThemePalette.values().toList(), key = { palette: AppThemePalette -> palette.name }) { palette: AppThemePalette ->
              FilterChip(
                selected = state.appSettings.palette == palette,
                onClick = { onPalette(palette) },
                label = { Text(palette.label) },
                leadingIcon = {
                  Box(
                    modifier = Modifier
                      .size(14.dp)
                      .background(palette.previewColor(), RoundedCornerShape(999.dp))
                  )
                }
              )
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("夜间模式", fontWeight = FontWeight.SemiBold)
              Text("使用深色背景和匹配的气泡颜色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = state.appSettings.themeMode == AppThemeMode.DARK,
              onCheckedChange = { enabled: Boolean -> onThemeMode(if (enabled) AppThemeMode.DARK else AppThemeMode.LIGHT) }
            )
          }
          Text("字体大小 ${(state.appSettings.fontScale * 100).roundToInt()}%", fontWeight = FontWeight.SemiBold)
          Slider(
            value = state.appSettings.fontScale,
            onValueChange = onFontScale,
            valueRange = 0.85f..1.25f,
            steps = 7
          )
        }

        SettingsSection(title = "附件限制") {
          Text(
            "限制按实际上传给 Provider 的附件体积计算；图片会先保留原图预览，再生成较小的发送副本。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          AttachmentLimitSlider(
            title = "单个附件上传上限",
            description = "普通文件按原文件计算；图片按压缩后的发送副本计算。",
            valueMb = state.appSettings.attachmentMaxFileMb,
            range = 1..100,
            onValueChange = onAttachmentMaxFileMb
          )
          AttachmentLimitSlider(
            title = "待发送附件总量上限",
            description = "同一条消息里所有待发送附件的上传体积总和。",
            valueMb = state.appSettings.attachmentMaxPendingMb.coerceAtLeast(state.appSettings.attachmentMaxFileMb),
            range = state.appSettings.attachmentMaxFileMb..300,
            onValueChange = onAttachmentMaxPendingMb
          )
          AttachmentLimitSlider(
            title = "图片原图导入上限",
            description = "超过该大小的原图不会导入；已导入原图仍用于本地预览。",
            valueMb = state.appSettings.attachmentMaxImageSourceMb.coerceAtLeast(state.appSettings.attachmentMaxFileMb),
            range = state.appSettings.attachmentMaxFileMb..300,
            onValueChange = onAttachmentMaxImageSourceMb
          )
        }

        SettingsSection(title = "模型与 API") {
          Text("Provider 配置用于新建对话和分叉对比；已有对话保持创建时的模型。", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("网络搜索", fontWeight = FontWeight.SemiBold)
          Text(
            "OpenAI Responses 使用官方托管 web_search；DeepSeek 等 Chat Completions 兼容接口使用函数调用，由 App 执行 web_search 后回传结果。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = WebSearchMode.values().toList(), key = { mode: WebSearchMode -> mode.name }) { mode: WebSearchMode ->
              FilterChip(
                selected = state.appSettings.webSearchMode == mode,
                onClick = { onWebSearchMode(mode) },
                label = { Text(mode.label) }
              )
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("AI 原始响应日志", fontWeight = FontWeight.SemiBold)
              Text(
                "保存流式响应事件，便于排查不同 Provider 的元数据格式。日志会限制长度。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = state.appSettings.debugResponseLogging,
              onCheckedChange = onDebugResponseLogging
            )
          }
          Button(onClick = onOpenProviderManager) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("管理 API 配置")
          }
          Button(onClick = onOpenBotManager) {
            Icon(Icons.Outlined.Groups, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("管理 AI 机器人")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onExportProviderConfigs) {
              Text("导出配置文本")
            }
            TextButton(onClick = { importDialogOpen = true }) {
              Text("导入配置文本")
            }
          }
          Text(
            "导出的文本包含 API Key，请只通过可信渠道保存或分享。二维码/扫码导入导出涉及相机权限、扫码库和长文本压缩，第一版先不启用。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        SettingsSection(title = "聊天背景预设") {
          Text(
            "常用背景可在新建或编辑群聊时快速插入到讨论主题里。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Button(onClick = { creatingPreset = true }) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("新增背景预设")
          }
          state.appSettings.backgroundPresets.sortedBy { it.sortOrder }.forEachIndexed { index, preset ->
            BackgroundPresetSettingsRow(
              preset = preset,
              canMoveUp = index > 0,
              canMoveDown = index < state.appSettings.backgroundPresets.lastIndex,
              onEdit = { editingPreset = preset },
              onDelete = { onDeleteBackgroundPreset(preset.id) },
              onMoveUp = { onMoveBackgroundPreset(preset.id, -1) },
              onMoveDown = { onMoveBackgroundPreset(preset.id, 1) }
            )
          }
        }
      }
    }
  }
  if (creatingPreset || editingPreset != null) {
    BackgroundPresetEditorDialog(
      preset = editingPreset,
      onDismiss = {
        creatingPreset = false
        editingPreset = null
      },
      onSave = { title, content ->
        onSaveBackgroundPreset(editingPreset, title, content)
        creatingPreset = false
        editingPreset = null
      }
    )
  }
  if (importDialogOpen) {
    ProviderConfigImportDialog(
      onDismiss = { importDialogOpen = false },
      onImport = { text ->
        onImportProviderConfigs(text)
        importDialogOpen = false
      }
    )
  }
}

@Composable
private fun AttachmentLimitSlider(
  title: String,
  description: String,
  valueMb: Int,
  range: IntRange,
  onValueChange: (Int) -> Unit
) {
  val cleanRange = if (range.first <= range.last) range else range.last..range.first
  val cleanValue = valueMb.coerceIn(cleanRange.first, cleanRange.last)
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
          description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text("${cleanValue}MB", fontWeight = FontWeight.SemiBold)
    }
    Slider(
      value = cleanValue.toFloat(),
      onValueChange = { next -> onValueChange(next.roundToInt().coerceIn(cleanRange.first, cleanRange.last)) },
      valueRange = cleanRange.first.toFloat()..cleanRange.last.toFloat(),
      steps = (cleanRange.last - cleanRange.first - 1).coerceAtLeast(0)
    )
  }
}

@Composable
private fun ProviderConfigImportDialog(
  onDismiss: () -> Unit,
  onImport: (String) -> Unit
) {
  var text by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("导入 API 配置文本") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "粘贴从本 App 导出的 JSON 配置文本。导入会新增 Provider；如果 ID 冲突会自动生成新 ID。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("配置 JSON") },
          minLines = 8,
          maxLines = 12,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = { onImport(text) }, enabled = text.isNotBlank()) {
        Text("导入")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun BackgroundPresetSettingsRow(
  preset: ChatBackgroundPreset,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(preset.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            preset.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onEdit) {
          Icon(Icons.Outlined.Edit, contentDescription = "编辑背景预设")
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Outlined.Delete, contentDescription = "删除背景预设")
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onMoveUp, enabled = canMoveUp) {
          Text("上移")
        }
        TextButton(onClick = onMoveDown, enabled = canMoveDown) {
          Text("下移")
        }
      }
    }
  }
}

@Composable
private fun BackgroundPresetEditorDialog(
  preset: ChatBackgroundPreset?,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit
) {
  var title by remember(preset?.id) { mutableStateOf(preset?.title.orEmpty()) }
  var content by remember(preset?.id) { mutableStateOf(preset?.content.orEmpty()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (preset == null) "新增背景预设" else "编辑背景预设") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("名称") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("背景内容") },
          minLines = 5,
          maxLines = 10,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, content) },
        enabled = content.isNotBlank()
      ) {
        Text("保存")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      content()
    }
  }
}

private fun AppThemePalette.previewColor(): Color = when (this) {
  AppThemePalette.MOSS -> Color(0xFF2F5E47)
  AppThemePalette.OCEAN -> Color(0xFF1F6D8C)
  AppThemePalette.SAKURA -> Color(0xFF9D3F68)
  AppThemePalette.AMBER -> Color(0xFF7A4B12)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderMenu(
  state: ChatUiState,
  onSelectProvider: (String) -> Unit,
  onOpenProviderManager: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
  ) {
    AssistChip(
      onClick = { expanded = true },
      label = {
        Text(
          text = state.selectedProvider?.displayName ?: "Provider",
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      },
      modifier = Modifier.menuAnchor()
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      state.providers.forEach { provider ->
        DropdownMenuItem(
          text = {
            Column {
              Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                provider.defaultModel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          },
          onClick = {
            onSelectProvider(provider.id)
            expanded = false
          }
        )
      }
      DropdownMenuItem(
        text = { Text("管理 API 配置") },
        onClick = {
          expanded = false
          onOpenProviderManager()
        }
      )
    }
  }
}

@Composable
private fun NewConversationProviderDialog(
  providers: List<ChatProviderConfig>,
  selectedProviderId: String?,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit,
  onSelectImageProvider: (String) -> Unit
) {
  val imageProviders = providers.filter { it.supportsImageGeneration }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("新对话模型") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("普通对话", fontWeight = FontWeight.SemiBold)
        ProviderChoiceList(
          providers = providers,
          selectedProviderId = selectedProviderId,
          onSelectProvider = onSelectProvider
        )
        Text("生图对话", fontWeight = FontWeight.SemiBold)
        if (imageProviders.isEmpty()) {
          Text(
            "当前没有开启生图能力的 GPT / OpenAI Responses 配置。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          ProviderChoiceList(
            providers = imageProviders,
            selectedProviderId = selectedProviderId,
            onSelectProvider = onSelectImageProvider
          )
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun ForkProviderDialog(
  providers: List<ChatProviderConfig>,
  selectedProviderId: String?,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("用其他模型分叉") },
    text = {
      ProviderChoiceList(
        providers = providers,
        selectedProviderId = selectedProviderId,
        onSelectProvider = onSelectProvider
      )
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun IncomingShareTargetDialog(
  state: ChatUiState,
  draft: IncomingShareDraft,
  onDismiss: () -> Unit,
  onSelectConversation: (String) -> Unit,
  onSelectGroup: (String) -> Unit,
  onCreateConversation: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text("选择发送到", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
              incomingShareSummary(draft),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          TextButton(onClick = onDismiss) {
            Text("取消")
          }
        }
        if (draft.failedCount > 0) {
          Text(
            "${draft.failedCount} 个文件导入失败，其余内容仍可发送",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        if (draft.text.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              draft.text,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
        if (draft.attachments.isNotEmpty()) {
          AttachmentStrip(
            attachments = draft.attachments,
            onOpenAttachment = onOpenAttachment,
            onRemoveAttachment = null,
            compact = true
          )
        }
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item(key = "new-conversation-section") {
            Text("新建单聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
          val creatableProviders = state.providers.filter { !draft.hasAttachments || it.supportsAttachments }
          if (creatableProviders.isEmpty()) {
            item(key = "new-conversation-empty") {
              Text(
                "没有支持这些附件的单聊模型配置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            items(creatableProviders, key = { "new-provider-${it.id}" }) { provider ->
              IncomingShareProviderRow(
                provider = provider,
                onClick = { onCreateConversation(provider.id) }
              )
            }
          }
          if (state.conversations.isNotEmpty()) {
            item(key = "conversation-section") {
              Text("已有单聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            }
            items(state.conversations, key = { "conversation-${it.id}" }) { conversation ->
              val provider = state.providers.firstOrNull { it.id == conversation.providerId }
              val enabled = !draft.hasAttachments || provider?.supportsAttachments == true
              IncomingShareConversationRow(
                conversation = conversation,
                provider = provider,
                enabled = enabled,
                disabledReason = if (!enabled) "该模型不支持附件" else null,
                onClick = { onSelectConversation(conversation.id) }
              )
            }
          }
          if (state.groupChats.isNotEmpty()) {
            item(key = "group-section") {
              Text("已有群聊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            }
            items(state.groupChats, key = { "group-${it.id}" }) { group ->
              IncomingShareGroupRow(
                group = group,
                memberCount = state.groupMembers.count { it.groupId == group.id },
                onClick = { onSelectGroup(group.id) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun IncomingShareProviderRow(
  provider: ChatProviderConfig,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Outlined.Add, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
          provider.defaultModel,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun IncomingShareConversationRow(
  conversation: ChatConversation,
  provider: ChatProviderConfig?,
  enabled: Boolean,
  disabledReason: String?,
  onClick: () -> Unit
) {
  Surface(
    color = if (enabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
      Text(
        "${provider?.displayName ?: conversation.providerId} / ${conversation.model}",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        disabledReason ?: "最后 ${formatConversationTime(conversation.updatedAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
      )
    }
  }
}

@Composable
private fun IncomingShareGroupRow(
  group: GroupChatRoom,
  memberCount: Int,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Groups, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(group.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
          group.topic.ifBlank { "未填写主题" },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          "$memberCount 个成员 · 最后 ${formatConversationTime(group.updatedAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private fun incomingShareSummary(draft: IncomingShareDraft): String {
  val parts = mutableListOf<String>()
  if (draft.attachments.isNotEmpty()) parts += "${draft.attachments.size} 个附件"
  if (draft.text.isNotBlank()) parts += "包含文本"
  return parts.ifEmpty { listOf("分享内容") }.joinToString(" · ")
}

@Composable
private fun ProviderChoiceList(
  providers: List<ChatProviderConfig>,
  selectedProviderId: String?,
  onSelectProvider: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    providers.forEach { provider ->
      TextButton(
        onClick = { onSelectProvider(provider.id) },
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          RadioButton(
            selected = provider.id == selectedProviderId,
            onClick = { onSelectProvider(provider.id) }
          )
          Spacer(Modifier.width(8.dp))
          Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
            Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              provider.defaultModel,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ConversationStrip(
  state: ChatUiState,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var listExpanded by remember { mutableStateOf(false) }
  val selectedConversation = state.selectedConversation
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Button(
        onClick = { listExpanded = !listExpanded },
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
          Text("对话列表", fontWeight = FontWeight.SemiBold)
          Text(
            text = selectedConversation?.let {
              "${conversationGroupLabel(it.groupName)} / ${it.title}"
            } ?: "点击切换历史对话",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          if (listExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
      }
      if (selectedConversation != null) {
        ConversationQuickActions(
          conversation = selectedConversation,
          onTogglePin = onTogglePin,
          onArchive = onArchive,
          onDelete = onDelete,
          onRename = onRename
        )
      }
    }

    if (listExpanded) {
      ConversationPicker(
        state = state,
        onSelectConversation = {
          onSelectConversation(it)
          listExpanded = false
        },
        onTogglePin = onTogglePin,
        onArchive = onArchive,
        onDelete = onDelete,
        onRename = onRename
      )
    }
  }
}

@Composable
private fun ConversationQuickActions(
  conversation: com.personal.aichat.domain.ChatConversation,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var editing by remember(conversation.id) { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  var title by remember(conversation.id) { mutableStateOf(conversation.title) }
  var groupName by remember(conversation.id) { mutableStateOf(conversation.groupName) }

  Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(
      onClick = { onTogglePin(conversation.id, conversation.isPinned) },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.PushPin, contentDescription = "置顶")
    }
    IconButton(
      onClick = { editing = true },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Edit, contentDescription = "编辑标题和分组")
    }
    IconButton(
      onClick = { moving = true },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Folder, contentDescription = "移动到文件夹")
    }
    IconButton(
      onClick = { onArchive(conversation.id) },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Archive, contentDescription = "归档")
    }
    IconButton(
      onClick = { onDelete(conversation.id) },
      modifier = Modifier.size(32.dp)
    ) {
      Icon(Icons.Outlined.Delete, contentDescription = "删除")
    }
  }

  if (editing) {
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("编辑对话") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("分组") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          onRename(conversation.id, title, groupName)
          editing = false
        }) {
          Text("保存")
        }
      },
      dismissButton = {
        TextButton(onClick = { editing = false }) {
          Text("取消")
        }
      }
    )
  }
  if (moving) {
    MoveConversationDialog(
      conversationTitle = conversation.title,
      initialGroupName = conversation.groupName,
      onDismiss = { moving = false },
      onMove = { targetGroup ->
        onRename(conversation.id, conversation.title, targetGroup)
        moving = false
      }
    )
  }
}

@Composable
private fun MoveConversationDialog(
  conversationTitle: String,
  initialGroupName: String,
  onDismiss: () -> Unit,
  onMove: (String) -> Unit
) {
  var targetGroup by remember(conversationTitle) { mutableStateOf(initialGroupName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("移动到文件夹") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(conversationTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        OutlinedTextField(
          value = targetGroup,
          onValueChange = { targetGroup = it },
          label = { Text("文件夹名称") },
          placeholder = { Text("留空则不放入文件夹") },
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          "文件夹用于收拢有共同归属的对话。加入文件夹后，该对话不再显示在普通列表里；留空可移出文件夹。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(onClick = { onMove(targetGroup.trim()) }) {
        Text("移动")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun ConversationPicker(
  state: ChatUiState,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var expandedGroups by remember(state.conversationGroups) {
    mutableStateOf(state.conversationGroups.associate { it.name to true }.toMutableMap())
  }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = 260.dp)
      .padding(top = 6.dp)
  ) {
    LazyColumn(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      state.conversationGroups.forEach { group ->
        item(key = "group-${group.name}") {
          TextButton(onClick = {
            expandedGroups[group.name] = expandedGroups[group.name] == false
          }) {
            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "${if (expandedGroups[group.name] == false) "+" else "-"} ${group.name} (${group.conversations.size})",
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        if (expandedGroups[group.name] != false) {
          item(key = "row-${group.name}") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              itemsIndexed(group.conversations, key = { _, conversation -> conversation.id }) { _, conversation ->
                ConversationChipRow(
                  conversation = conversation,
                  selected = conversation.id == state.selectedConversationId,
                  onSelectConversation = onSelectConversation,
                  onTogglePin = onTogglePin,
                  onArchive = onArchive,
                  onDelete = onDelete,
                  onRename = onRename
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConversationChipRow(
  conversation: com.personal.aichat.domain.ChatConversation,
  selected: Boolean,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  var moving by remember(conversation.id) { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.width(220.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f)
      ) {
        TextButton(onClick = { onSelectConversation(conversation.id) }) {
          Text(
            text = if (conversation.isPinned) "置顶 · ${conversation.title}" else conversation.title,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Box {
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "对话操作")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
            leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
            onClick = {
              menuOpen = false
              onTogglePin(conversation.id, conversation.isPinned)
            }
          )
          DropdownMenuItem(
            text = { Text("移动到文件夹") },
            leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            onClick = {
              menuOpen = false
              moving = true
            }
          )
          DropdownMenuItem(
            text = { Text("归档") },
            leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
            onClick = {
              menuOpen = false
              onArchive(conversation.id)
            }
          )
          DropdownMenuItem(
            text = { Text("删除") },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            onClick = {
              menuOpen = false
              onDelete(conversation.id)
            }
          )
        }
      }
    }
  }
  if (moving) {
    MoveConversationDialog(
      conversationTitle = conversation.title,
      initialGroupName = conversation.groupName,
      onDismiss = { moving = false },
      onMove = { targetGroup ->
        onRename(conversation.id, conversation.title, targetGroup)
        moving = false
      }
    )
  }
}

@Composable
private fun MessageList(
  state: ChatUiState,
  messages: List<ChatMessage>,
  imageMode: Boolean,
  selectedMessageIds: Set<String>,
  selectionMode: Boolean,
  onToggleMessageSelected: (String) -> Unit,
  onSetMessagesSelected: (Set<String>, Boolean) -> Unit,
  onSelectRangeTo: (String) -> Unit,
  onEditResend: (String) -> Unit,
  onShareMessageText: (String) -> Unit,
  onShareMessageImage: (String) -> Unit,
  onFavoriteMessage: (String) -> Unit,
  onFavoriteMessages: (Set<String>) -> Unit,
  onForkMessage: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  var autoFollow by remember(state.selectedConversationId) { mutableStateOf(true) }
  val hasStreaming = messages.any { it.status == MessageStatus.STREAMING }
  val listItems = remember(messages) { chatMessageListItems(messages) }
  val bottomAnchorIndex = listItems.size
  var lastAutoFollowAt by remember(state.selectedConversationId) { mutableStateOf(0L) }
  var scrollHintVisible by remember(state.selectedConversationId) { mutableStateOf(false) }
  var longBubbleActionsExpanded by remember(state.selectedConversationId) { mutableStateOf(false) }
  val visibleRangeTargetId by remember(selectionMode, selectedMessageIds, listItems) {
    derivedStateOf {
      if (!selectionMode || selectedMessageIds.isEmpty()) {
        null
      } else {
        listState.layoutInfo.visibleItemsInfo
          .mapNotNull { item -> listItems.getOrNull(item.index) }
          .flatMap { it.messageIds }
          .firstOrNull { it !in selectedMessageIds }
      }
    }
  }
  val showScrollToBottom by remember(messages.size) {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      val lastItem = listState.layoutInfo.totalItemsCount - 1
      lastItem > 0 && lastVisible < lastItem - 1
    }
  }
  val messageLongBubbleNavTarget by remember(scrollHintVisible, listItems) {
    derivedStateOf {
      if (!scrollHintVisible) {
        null
      } else {
        val assistantCandidates = listItems.mapIndexedNotNull { index, item ->
          val message = (item as? ChatMessageListItem.Message)?.message ?: return@mapIndexedNotNull null
          if (message.role == MessageRole.ASSISTANT) index to message.id else null
        }.toMap()
        lazyListLongBubbleNavTarget(
          visibleItems = listState.layoutInfo.visibleItemsInfo,
          viewportStart = listState.layoutInfo.viewportStartOffset,
          viewportEnd = listState.layoutInfo.viewportEndOffset,
          candidates = assistantCandidates
        )
      }
    }
  }
  LaunchedEffect(listState, state.selectedConversationId) {
    snapshotFlow { listState.isScrollInProgress to listState.isAtBottom() }
      .collect { (scrolling, atBottom) ->
        if (scrolling) scrollHintVisible = true
        if (scrolling && !atBottom) autoFollow = false
        if (atBottom) autoFollow = true
      }
  }
  LaunchedEffect(scrollHintVisible, listState.isScrollInProgress, longBubbleActionsExpanded) {
    if (scrollHintVisible && !listState.isScrollInProgress && !longBubbleActionsExpanded) {
      delay(2_500)
      if (!listState.isScrollInProgress && !longBubbleActionsExpanded) scrollHintVisible = false
    }
  }
  LaunchedEffect(messages.size, messages.lastOrNull()?.content, hasStreaming, autoFollow) {
    if (autoFollow && messages.isNotEmpty()) {
      val now = System.currentTimeMillis()
      if (!hasStreaming || now - lastAutoFollowAt > 120L) {
        lastAutoFollowAt = now
        listState.scrollToItem(bottomAnchorIndex)
      }
    }
  }
  Box(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (messages.isEmpty()) {
        item {
          EmptyState()
        }
      }
      items(listItems, key = { it.key }) { item ->
        when (item) {
          is ChatMessageListItem.ToolGroup -> {
            val toolIds = item.messages.map { it.id }.toSet()
            val selected = toolIds.all { it in selectedMessageIds }
            ToolCallGroupBubble(
              messages = item.messages,
              title = "工具调用",
              metadata = formatMessageTime(item.messages.first().createdAt),
              selected = selected,
              selectionMode = selectionMode,
              canSelectRangeTo = selectionMode && selectedMessageIds.isNotEmpty() && item.messages.any { it.id !in selectedMessageIds },
              onToggleSelected = { onSetMessagesSelected(toolIds, !selected) },
              onSelectRangeTo = { onSelectRangeTo(item.messages.first().id) },
              onCopy = { copyToClipboard(context, item.messages.joinToString("\n\n") { it.content }) },
              onShareText = { shareText(context, item.messages.joinToString("\n\n") { it.content }, "分享工具调用") },
              onShareImage = { onShareMessageImage(item.messages.first().id) },
              onFavorite = { onFavoriteMessages(toolIds) }
            )
          }
          is ChatMessageListItem.Message -> {
            val message = item.message
            MessageBubble(
              message = message,
              selected = message.id in selectedMessageIds,
              selectionMode = selectionMode,
              canSelectRangeTo = selectionMode && selectedMessageIds.isNotEmpty() && message.id !in selectedMessageIds,
              onToggleSelected = { onToggleMessageSelected(message.id) },
              onSelectRangeTo = { onSelectRangeTo(message.id) },
              onCopy = { copyToClipboard(context, message.content) },
              onCopyRawLog = { message.rawResponseLog?.let { copyToClipboard(context, it) } },
              onShareText = { onShareMessageText(message.id) },
              onShareImage = { onShareMessageImage(message.id) },
              onFavorite = { onFavoriteMessage(message.id) },
              onEditResend = { onEditResend(message.content) },
              onOpenAttachment = onOpenAttachment,
              onFork = { onForkMessage(message.id) },
              imageMode = imageMode
            )
          }
        }
      }
      item(key = "message-list-bottom-anchor") {
        Spacer(Modifier.height(1.dp))
      }
    }
    }
    val rangeTargetId = visibleRangeTargetId
    if (rangeTargetId != null) {
      Button(
        onClick = { onSelectRangeTo(rangeTargetId) },
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 18.dp, top = 8.dp)
      ) {
        Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("选择到这里")
      }
    }
    MessageScrollIndicator(
      listState = listState,
      onDragProgress = { progress ->
        scrollHintVisible = true
        scope.launch {
          val total = listState.layoutInfo.totalItemsCount
          if (total > 0) {
            listState.scrollToItem(
              listState.itemIndexForProgress(progress).coerceIn(0, total - 1)
            )
          }
        }
      },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 4.dp)
    )
    if (showScrollToBottom && scrollHintVisible) {
      Button(
        onClick = {
          scope.launch {
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.animateScrollToItem(last)
          }
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
        .padding(bottom = 12.dp)
      ) {
        Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("回到底部")
      }
    }
    LongBubbleNavOverlay(
      target = messageLongBubbleNavTarget,
      onJumpTop = { target ->
        autoFollow = false
        scrollHintVisible = true
        scope.launch { listState.animateScrollToItem(target.index) }
      },
      onJumpBottom = { target ->
        autoFollow = false
        scrollHintVisible = true
        scope.launch { listState.animateScrollToItem(target.index, target.bottomOffset) }
      },
      onCopy = { target ->
        target.messageId
          ?.let { id -> messages.firstOrNull { it.id == id } }
          ?.let { copyToClipboard(context, it.content) }
      },
      onShareText = { target ->
        target.messageId?.let(onShareMessageText)
      },
      onShareImage = { target ->
        target.messageId?.let(onShareMessageImage)
      },
      onFavorite = { target ->
        target.messageId?.let(onFavoriteMessage)
      },
      onActionsExpandedChange = { longBubbleActionsExpanded = it },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 18.dp)
    )
  }
}

@Composable
private fun MessageScrollIndicator(
  listState: androidx.compose.foundation.lazy.LazyListState,
  onDragProgress: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
  val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
  Box(
    modifier = modifier
      .width(5.dp)
      .fillMaxHeight(0.82f)
      .pointerInput(Unit) {
        detectDragGestures { change, _ ->
          val y = change.position.y.coerceIn(0f, size.height.toFloat())
          onDragProgress((y / size.height.toFloat()).coerceIn(0f, 1f))
        }
      }
      .drawWithContent {
        drawContent()
        val metrics = listState.scrollbarMetrics() ?: return@drawWithContent
        val radius = size.width / 2f
        val thumbHeightPx = 38.dp.toPx().coerceAtMost(size.height)
        val travel = (size.height - thumbHeightPx).coerceAtLeast(0f)
        val thumbTop = travel * metrics.progress
        drawRoundRect(
          color = trackColor,
          topLeft = Offset.Zero,
          size = Size(size.width, size.height),
          cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
          color = thumbColor,
          topLeft = Offset(x = 1.dp.toPx(), y = thumbTop),
          size = Size((size.width - 2.dp.toPx()).coerceAtLeast(1f), thumbHeightPx),
          cornerRadius = CornerRadius(radius, radius)
        )
      }
  )
}

private data class ScrollbarMetrics(val progress: Float)

private fun androidx.compose.foundation.lazy.LazyListState.scrollbarMetrics(): ScrollbarMetrics? {
  if (!canScroll()) return null
  return ScrollbarMetrics(scrollProgress())
}

private fun androidx.compose.foundation.lazy.LazyListState.scrollProgress(): Float {
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return 0f
  val first = visibleItems.first()
  val visibleCount = visibleItems.size.coerceAtLeast(1)
  val totalItems = layoutInfo.totalItemsCount.coerceAtLeast(visibleCount)
  val itemOffsetFraction = ((layoutInfo.viewportStartOffset - first.offset).toFloat() / first.size.coerceAtLeast(1))
    .coerceIn(0f, 1f)
  return ((first.index + itemOffsetFraction) / (totalItems - visibleCount + 1).coerceAtLeast(1)).coerceIn(0f, 1f)
}

private fun androidx.compose.foundation.lazy.LazyListState.itemIndexForProgress(progress: Float): Int {
  val total = layoutInfo.totalItemsCount
  if (total <= 0) return 0
  return (progress.coerceIn(0f, 1f) * (total - 1)).roundToInt()
}

private fun androidx.compose.foundation.lazy.LazyListState.canScroll(): Boolean {
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return false
  val first = visibleItems.first()
  val last = visibleItems.last()
  return first.index > 0 ||
    first.offset < layoutInfo.viewportStartOffset ||
    last.index < layoutInfo.totalItemsCount - 1 ||
    last.offset + last.size > layoutInfo.viewportEndOffset
}

private fun androidx.compose.foundation.lazy.LazyListState.isAtBottom(): Boolean {
  val visibleItems = layoutInfo.visibleItemsInfo
  if (visibleItems.isEmpty()) return true
  val last = visibleItems.last()
  return last.index >= layoutInfo.totalItemsCount - 1 &&
    last.offset + last.size <= layoutInfo.viewportEndOffset
}

@Composable
private fun ShareToolbar(
  conversationTitle: String,
  selectionMode: Boolean,
  selectedCount: Int,
  onToggleSelectionMode: (Boolean) -> Unit,
  onShareConversation: () -> Unit,
  onShareSelected: () -> Unit,
  onShareMarkdown: () -> Unit,
  onShareLongImage: () -> Unit,
  onShareSelectedLongImage: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuOpen by remember { mutableStateOf(false) }
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(conversationTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          text = if (selectionMode) "已选择 $selectedCount 条消息" else "可分享全文、Markdown 或长图",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      TextButton(onClick = { onToggleSelectionMode(!selectionMode) }) {
        Text(if (selectionMode) "取消多选" else "多选")
      }
      Box {
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "分享选项")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text("分享全文文本") },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
            onClick = {
              menuOpen = false
              onShareConversation()
            }
          )
          DropdownMenuItem(
            text = { Text("分享选中消息") },
            leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
            enabled = selectedCount > 0,
            onClick = {
              menuOpen = false
              onShareSelected()
            }
          )
          DropdownMenuItem(
            text = { Text("导出 Markdown 文件") },
            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
            onClick = {
              menuOpen = false
              onShareMarkdown()
            }
          )
          DropdownMenuItem(
            text = { Text("生成长图分享") },
            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
            onClick = {
              menuOpen = false
              onShareLongImage()
            }
          )
          DropdownMenuItem(
            text = { Text("选中消息生成长图") },
            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
            enabled = selectedCount > 0,
            onClick = {
              menuOpen = false
              onShareSelectedLongImage()
            }
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyState() {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 42.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text("先选择一组 API 配置，再开始对话。", fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(6.dp))
      Text(
        "已内置 GPT、DeepSeek 和 TokenHub 代理模板。进入 API 配置填入 Key 后即可发送。",
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

private data class ToolCallBubbleColors(
  val container: Color,
  val content: Color,
  val metadata: Color,
  val accent: Color,
  val detailContainer: Color
)

@Composable
private fun defaultToolCallBubbleColors(botColors: BotBubbleColors? = null): ToolCallBubbleColors {
  return if (botColors == null) {
    ToolCallBubbleColors(
      container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
      content = MaterialTheme.colorScheme.onSurface,
      metadata = MaterialTheme.colorScheme.onSurfaceVariant,
      accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
      detailContainer = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    )
  } else {
    ToolCallBubbleColors(
      container = botColors.container.copy(alpha = 0.74f),
      content = botColors.content,
      metadata = botColors.content.copy(alpha = 0.72f),
      accent = botColors.accent,
      detailContainer = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
    )
  }
}

@Composable
private fun ToolCallGroupBubble(
  messages: List<ChatMessage>,
  title: String,
  metadata: String?,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onCopy: () -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onFavorite: () -> Unit,
  modifier: Modifier = Modifier,
  colors: ToolCallBubbleColors? = null,
  expanded: Boolean? = null,
  canToggleExpanded: Boolean = true,
  onToggleExpanded: (() -> Unit)? = null
) {
  if (messages.isEmpty()) return
  val resolvedColors = colors ?: defaultToolCallBubbleColors()
  val details = remember(messages) { messages.map { parseToolCallDetails(it.content) } }
  val isStreaming = messages.any { it.status == MessageStatus.STREAMING }
  var localExpanded by remember(messages.first().id) { mutableStateOf(false) }
  val effectiveExpanded = isStreaming || (expanded ?: localExpanded)
  var shareMenuOpen by remember(messages.first().id) { mutableStateOf(false) }
  val toggleExpanded = {
    if (onToggleExpanded != null) {
      onToggleExpanded()
    } else {
      localExpanded = !localExpanded
    }
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = Arrangement.Start
  ) {
    Surface(
      color = if (selected) MaterialTheme.colorScheme.secondaryContainer else resolvedColors.container,
      contentColor = resolvedColors.content,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(0.86f)
        .height(IntrinsicSize.Min)
        .border(
          width = if (selected) 2.dp else 1.dp,
          color = if (selected) MaterialTheme.colorScheme.primary else resolvedColors.accent.copy(alpha = 0.55f),
          shape = RoundedCornerShape(8.dp)
        )
    ) {
      Box {
        Icon(
          Icons.Outlined.Search,
          contentDescription = null,
          tint = resolvedColors.accent.copy(alpha = 0.10f),
          modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = 18.dp, y = 12.dp)
            .size(88.dp)
        )
        Row {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(4.dp)
            .background(resolvedColors.accent)
        )
        Column(
          modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 5.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = resolvedColors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = resolvedColors.content
              )
              metadata?.takeIf { it.isNotBlank() }?.let {
                Text(
                  text = it,
                  style = MaterialTheme.typography.bodySmall,
                  color = resolvedColors.metadata,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Outlined.ContentCopy, contentDescription = "复制工具调用", tint = resolvedColors.content)
            }
            Box {
              IconButton(onClick = { shareMenuOpen = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Share, contentDescription = "分享工具调用", tint = resolvedColors.content)
              }
              DropdownMenu(expanded = shareMenuOpen, onDismissRequest = { shareMenuOpen = false }) {
                DropdownMenuItem(
                  text = { Text("分享文本") },
                  leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                  onClick = {
                    shareMenuOpen = false
                    onShareText()
                  }
                )
                DropdownMenuItem(
                  text = { Text("分享长图") },
                  leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                  onClick = {
                    shareMenuOpen = false
                    onShareImage()
                  }
                )
              }
            }
            IconButton(
              onClick = onFavorite,
              enabled = messages.none { it.status == MessageStatus.STREAMING },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Outlined.Bookmark, contentDescription = "收藏工具调用", tint = resolvedColors.content)
            }
          }

          Text(
            text = groupToolSummary(details, messages.size, isStreaming),
            maxLines = if (effectiveExpanded) 3 else 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = resolvedColors.content
          )

          if (effectiveExpanded) {
            details.forEachIndexed { index, item ->
              ToolCallDetail(
                index = index + 1,
                details = item,
                colors = resolvedColors
              )
            }
          }

          if (isStreaming) {
            Text("工具调用中", style = MaterialTheme.typography.bodySmall, color = resolvedColors.metadata)
          }

          if (selectionMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
              if (canSelectRangeTo) {
                TextButton(onClick = onSelectRangeTo) {
                  Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("选择到这里")
                }
              }
            }
          }

          if (canToggleExpanded && !isStreaming) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              CompactExpandToggle(expanded = effectiveExpanded, onClick = toggleExpanded)
            }
          }
        }
        }
      }
    }
  }
}

@Composable
private fun ToolCallDetail(
  index: Int,
  details: ToolCallDetails,
  colors: ToolCallBubbleColors
) {
  Surface(
    color = colors.detailContainer,
    contentColor = colors.content,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text("工具 $index · ${details.name}", fontWeight = FontWeight.SemiBold, color = colors.content)
      }
      details.query?.takeIf { it.isNotBlank() }?.let {
        ToolCallSection("查询词", it, labelColor = colors.accent, textColor = colors.metadata)
      }
      if (details.openedUrls.isNotEmpty()) {
        ToolCallSection("打开 URL", details.openedUrls.joinToString("\n"), labelColor = colors.accent, textColor = colors.metadata)
      }
      if (details.citations.isNotEmpty()) {
        ToolCallSection(
          "Citation URL",
          details.citations.joinToString("\n\n") { citation ->
            listOfNotNull(citation.title?.takeIf { it.isNotBlank() }, citation.url).joinToString("\n")
          },
          labelColor = colors.accent,
          textColor = colors.metadata
        )
      }
      details.input?.takeIf { it.isNotBlank() }?.let {
        ToolCallSection("原始输入", it, labelColor = colors.accent, textColor = colors.metadata)
      }
      details.output?.takeIf { it.isNotBlank() }?.let {
        ToolCallSection("原始输出", it, labelColor = colors.accent, textColor = colors.metadata)
      }
      if (details.input.isNullOrBlank() && details.output.isNullOrBlank()) {
        Text("暂无详情", style = MaterialTheme.typography.bodySmall, color = colors.metadata)
      }
    }
  }
}

@Composable
private fun CompactExpandToggle(
  expanded: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.primary,
    shape = RoundedCornerShape(999.dp),
    modifier = modifier
      .height(30.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        modifier = Modifier.size(17.dp)
      )
      Spacer(Modifier.width(3.dp))
      Text(
        if (expanded) "折叠" else "展开",
        style = MaterialTheme.typography.bodySmall
      )
    }
  }
}

@Composable
private fun ToolCallItem(
  message: ChatMessage,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onCopy: () -> Unit,
  onFavorite: () -> Unit
) {
  val details = remember(message.content) { parseToolCallDetails(message.content) }
  var expanded by remember(message.id) { mutableStateOf(false) }
  val isSearching = message.status == MessageStatus.STREAMING
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = Arrangement.Start
  ) {
    Surface(
      color = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
      },
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .then(
          if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
          } else {
            Modifier
          }
        )
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (details.name == "web_search") {
                if (isSearching) "Searching web" else "Searched web"
              } else {
                if (isSearching) "Running ${details.name}" else "Ran ${details.name}"
              },
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            details.summary?.takeIf { it.isNotBlank() }?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Icon(
            if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            contentDescription = if (expanded) "折叠工具调用" else "展开工具调用",
            modifier = Modifier.size(20.dp)
          )
        }
        if (expanded) {
          Spacer(Modifier.height(8.dp))
          ToolCallSection("工具", details.name)
          details.input?.takeIf { it.isNotBlank() }?.let { ToolCallSection("输入", it) }
          details.output?.takeIf { it.isNotBlank() }?.let { ToolCallSection("输出", it) }
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
              Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
              if (canSelectRangeTo) {
                TextButton(onClick = onSelectRangeTo) {
                  Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("选择到这里")
                }
              }
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Outlined.ContentCopy, contentDescription = "复制工具调用")
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Outlined.Bookmark, contentDescription = "收藏工具调用")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToolCallSection(
  label: String,
  text: String,
  labelColor: Color = MaterialTheme.colorScheme.primary,
  textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
  Text(label, style = MaterialTheme.typography.bodySmall, color = labelColor, fontWeight = FontWeight.SemiBold)
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = textColor
  )
  Spacer(Modifier.height(6.dp))
}

internal data class ToolCallCitation(
  val title: String?,
  val url: String
)

internal data class ToolCallDetails(
  val name: String,
  val input: String?,
  val output: String?,
  val query: String? = null,
  val openedUrls: List<String> = emptyList(),
  val citations: List<ToolCallCitation> = emptyList()
) {
  val summary: String?
    get() {
      query?.takeIf { it.isNotBlank() }?.let { return "查询：${it.take(100)}" }
      openedUrls.firstOrNull()?.let { return "打开：${it.take(100)}" }
      citations.firstOrNull()?.let { citation ->
        return listOfNotNull(citation.title?.takeIf { it.isNotBlank() }, citation.url)
          .joinToString(" · ")
          .take(120)
      }
      val outputLines = output?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.toList()
        .orEmpty()
      val outputUrl = outputLines.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
      val outputFirst = outputLines.firstOrNull()
      val inputFirst = input?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
      return (outputUrl ?: outputFirst ?: inputFirst)?.take(120)
    }
}

internal fun parseToolCallDetails(content: String): ToolCallDetails {
  val name = content.substringAfter("工具：", "").lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "tool" }
  val input = content.sectionAfter("输入：", "输出：")
  val output = content.sectionAfter("输出：", null)
  val inputUrls = extractPlainUrls(input.orEmpty())
  val outputUrls = extractPlainUrls(output.orEmpty())
  val query = extractToolQuery(input) ?: extractToolQuery(output) ?: extractQueryFromToolOutput(output)
  val openedUrls = when (name) {
    "open", "open_page", "web_fetch" -> (inputUrls + outputUrls.take(1)).distinct()
    else -> inputUrls
  }
  val citations = extractToolCitations(output.orEmpty(), openedUrls.toSet())
  return ToolCallDetails(
    name = name,
    input = input,
    output = output,
    query = query,
    openedUrls = openedUrls,
    citations = citations
  )
}

private fun extractToolQuery(text: String?): String? {
  if (text.isNullOrBlank()) return null
  Regex(""""(?:query|queries|q)"\s*:\s*(\[[^\]]*]|"[^"]*")""").find(text)?.let { match ->
    val raw = match.groupValues[1].trim()
    if (raw.startsWith("[")) {
      return Regex(""""([^"]+)"""").find(raw)?.groupValues?.getOrNull(1)?.jsonUnescape()
    }
    return raw.trim('"').jsonUnescape().takeIf { it.isNotBlank() }
  }
  Regex("""(?im)^(?:查询|搜索关键词|搜索|query)\s*[:：]\s*(.+)$""").find(text)?.let { match ->
    return match.groupValues[1].trim().takeIf { it.isNotBlank() }
  }
  return null
}

private fun extractQueryFromToolOutput(output: String?): String? {
  if (output.isNullOrBlank()) return null
  val lines = output.lineSequence().map { it.trim() }.toList()
  val labelIndex = lines.indexOfFirst {
    it.contains("查询") ||
      it.contains("搜索关键词") ||
      it.contains("鏌ヨ") ||
      it.contains("鎼滅储鍏抽敭")
  }
  if (labelIndex < 0) return null
  return lines.drop(labelIndex + 1)
    .firstOrNull { line ->
      line.isNotBlank() &&
        !line.startsWith("http://") &&
        !line.startsWith("https://") &&
        !line.contains("网址") &&
        !line.contains("缃戝潃")
    }
    ?.take(120)
}

private fun extractPlainUrls(text: String): List<String> {
  return PlainToolUrlRegex.findAll(text)
    .map { it.value.trimEnd('.', ',', ';', '，', '。', '；') }
    .distinct()
    .toList()
}

private fun extractToolCitations(output: String, openedUrls: Set<String>): List<ToolCallCitation> {
  if (output.isBlank()) return emptyList()
  val lines = output.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
  val citations = mutableListOf<ToolCallCitation>()
  lines.forEachIndexed { index, line ->
    extractPlainUrls(line).forEach { url ->
      if (url in openedUrls) return@forEach
      val title = nearestCitationTitle(lines, index)
      citations += ToolCallCitation(title = title, url = url)
    }
  }
  return citations.distinctBy { it.url }
}

private fun nearestCitationTitle(lines: List<String>, urlLineIndex: Int): String? {
  lines.take(urlLineIndex).takeLast(4).asReversed().forEach { line ->
    Regex("""^\d+[.)]\s+(.+)$""").matchEntire(line)?.let { match ->
      return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }
  }
  val previous = lines.take(urlLineIndex).asReversed().firstOrNull { line ->
    !line.startsWith("http://") &&
      !line.startsWith("https://") &&
      !line.contains("：") &&
      !line.contains(":") &&
      line.length <= 120
  } ?: return null
  return previous
    .replace(Regex("""^\d+[.)]\s*"""), "")
    .trim()
    .takeIf { it.isNotBlank() }
}

private fun String.jsonUnescape(): String {
  return replace("\\n", "\n")
    .replace("\\\"", "\"")
    .replace("\\/", "/")
    .replace("\\\\", "\\")
}

private val PlainToolUrlRegex = Regex("https?://[^\\s<>\"'`\\]\\)\\}]+")

private fun String.sectionAfter(label: String, until: String?): String? {
  val start = indexOf(label)
  if (start < 0) return null
  val contentStart = start + label.length
  val end = until?.let { marker ->
    indexOf(marker, startIndex = contentStart).takeIf { it >= 0 }
  } ?: length
  return substring(contentStart, end).trim().takeIf { it.isNotBlank() }
}

@Composable
private fun MessageBubble(
  message: ChatMessage,
  selected: Boolean,
  selectionMode: Boolean,
  canSelectRangeTo: Boolean,
  onToggleSelected: () -> Unit,
  onSelectRangeTo: () -> Unit,
  onCopy: () -> Unit,
  onCopyRawLog: () -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onFavorite: () -> Unit,
  onEditResend: () -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onFork: () -> Unit,
  imageMode: Boolean = false
) {
  val isUser = message.role == MessageRole.USER
  val userColors = userBubbleColors()
  var shareMenuOpen by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        isUser -> userColors.container
        else -> MaterialTheme.colorScheme.surface
      },
      contentColor = if (isUser) userColors.content else MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.84f else 0.92f)
        .then(
          if (selected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
          } else {
            Modifier
          }
        )
    ) {
      Column(modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp)) {
        Text(
          text = formatMessageTime(message.createdAt),
          style = MaterialTheme.typography.bodySmall,
          color = if (isUser) userColors.metadata else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        if (isUser) {
          Text(message.content.ifBlank { if (message.status == MessageStatus.STREAMING) "..." else "" })
        } else {
          SelectionContainer {
            MarkdownPreview(message.content.ifBlank { if (message.status == MessageStatus.STREAMING) "..." else "" })
          }
        }
        if (message.attachments.isNotEmpty()) {
          Spacer(Modifier.height(8.dp))
          if (imageMode && !isUser && message.attachments.any { it.isImage }) {
            GeneratedImageGrid(
              attachments = message.attachments.filter { it.isImage },
              onOpenAttachment = onOpenAttachment
            )
          } else {
            AttachmentStrip(
              attachments = message.attachments,
              onOpenAttachment = onOpenAttachment,
              onRemoveAttachment = null,
              compact = false
            )
          }
        }
        if (message.status == MessageStatus.FAILED) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = message.errorMessage ?: "Request failed",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
        formatMessageMetadata(message)?.let { metadata ->
          Spacer(Modifier.height(8.dp))
          Text(
            text = metadata,
            style = MaterialTheme.typography.bodySmall,
            color = if (isUser) {
              userColors.metadata
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }
        Spacer(Modifier.height(4.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (selectionMode) {
            Checkbox(
              checked = selected,
              onCheckedChange = { onToggleSelected() }
            )
            if (canSelectRangeTo) {
              TextButton(onClick = onSelectRangeTo) {
                Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("选择到这里")
              }
            }
          }
          IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "复制消息")
          }
          if (isUser) {
            IconButton(
              onClick = onEditResend,
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Outlined.Edit, contentDescription = "编辑重发")
            }
          }
          IconButton(
            onClick = onFork,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = "用其他模型分叉")
          }
          IconButton(
            onClick = onFavorite,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Outlined.Bookmark, contentDescription = "收藏此消息")
          }
          if (!isUser) {
            if (!message.rawResponseLog.isNullOrBlank()) {
              IconButton(
                onClick = onCopyRawLog,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "复制原始响应日志")
              }
            }
            Box {
              IconButton(
                onClick = { shareMenuOpen = true },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Outlined.Share, contentDescription = "分享消息")
              }
              DropdownMenu(expanded = shareMenuOpen, onDismissRequest = { shareMenuOpen = false }) {
                DropdownMenuItem(
                  text = { Text("分享文本") },
                  leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                  onClick = {
                    shareMenuOpen = false
                    onShareText()
                  }
                )
                DropdownMenuItem(
                  text = { Text("分享图片") },
                  leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                  onClick = {
                    shareMenuOpen = false
                    onShareImage()
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun formatMessageMetadata(message: ChatMessage): String? {
  if (message.role != MessageRole.ASSISTANT) return null
  val parts = mutableListOf<String>()
  message.firstTokenDurationMs?.let { parts += "首 token ${formatDuration(it)}" }
  message.totalDurationMs?.let { parts += "总耗时 ${formatDuration(it)}" }
  if (message.totalTokens != null) {
    val detail = when {
      message.promptTokens != null && message.completionTokens != null ->
        "输入 ${message.promptTokens} / 输出 ${message.completionTokens}"
      message.promptTokens != null -> "输入 ${message.promptTokens}"
      message.completionTokens != null -> "输出 ${message.completionTokens}"
      else -> null
    }
    parts += buildString {
      append(message.totalTokens)
      append(" tokens")
      if (detail != null) append(" (").append(detail).append(")")
    }
  } else if (message.promptTokens != null || message.completionTokens != null) {
    parts += "输入 ${message.promptTokens ?: "-"} / 输出 ${message.completionTokens ?: "-"}"
  }
  if (!message.rawResponseLog.isNullOrBlank()) {
    parts += "含原始日志"
  }
  return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun formatDuration(ms: Long): String {
  return if (ms < 1_000) {
    "${ms}ms"
  } else {
    String.format(Locale.getDefault(), "%.1fs", ms / 1_000f)
  }
}

@Composable
private fun MarkdownPreview(content: String, colors: MarkdownRenderColors? = null) {
  val blocks = remember(content) { parseMarkdownBlocks(content) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    blocks.forEach { block ->
      when (block) {
        is MarkdownBlock.Code -> Surface(
          color = colors?.blockContainer ?: MaterialTheme.colorScheme.background,
          contentColor = colors?.content ?: MaterialTheme.colorScheme.onSurface,
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = block.text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyMedium
          )
        }
        is MarkdownBlock.Heading -> Text(
          text = block.text,
          style = when (block.level) {
            1 -> MaterialTheme.typography.titleLarge
            2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
          },
          fontWeight = FontWeight.SemiBold,
          color = colors?.content ?: Color.Unspecified
        )
        is MarkdownBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(block.marker, color = colors?.muted ?: MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            renderInlineMarkdown(block.text),
            modifier = Modifier.weight(1f),
            color = colors?.content ?: Color.Unspecified
          )
        }
        is MarkdownBlock.Table -> MarkdownTable(block, colors)
        MarkdownBlock.Divider -> Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors?.divider ?: MaterialTheme.colorScheme.outlineVariant)
        )
        is MarkdownBlock.Paragraph -> Text(
          renderInlineMarkdown(block.text),
          color = colors?.content ?: Color.Unspecified
        )
      }
    }
  }
}

private sealed interface MarkdownBlock {
  data class Paragraph(val text: String) : MarkdownBlock
  data class Heading(val level: Int, val text: String) : MarkdownBlock
  data class ListItem(val marker: String, val text: String) : MarkdownBlock
  data class Code(val text: String) : MarkdownBlock
  data class Table(val rows: List<List<String>>) : MarkdownBlock
  data object Divider : MarkdownBlock
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table, colors: MarkdownRenderColors? = null) {
  if (table.rows.isEmpty()) return
  val columnCount = table.rows.maxOf { it.size }.coerceAtLeast(1)
  Surface(
    color = colors?.blockContainer ?: MaterialTheme.colorScheme.background,
    contentColor = colors?.content ?: MaterialTheme.colorScheme.onSurface,
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .padding(8.dp)
        .border(1.dp, colors?.border ?: MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
    ) {
      table.rows.forEachIndexed { rowIndex, row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(
              color = if (rowIndex == 0) colors?.blockHeader ?: MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
              shape = RoundedCornerShape(4.dp)
            )
            .border(0.5.dp, colors?.border ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            .padding(vertical = 0.dp)
        ) {
          repeat(columnCount) { column ->
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(0.5.dp, colors?.border?.copy(alpha = 0.75f) ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
              Text(
                text = renderInlineMarkdown(row.getOrNull(column).orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                color = colors?.content ?: Color.Unspecified
              )
            }
          }
        }
      }
    }
  }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
  val blocks = mutableListOf<MarkdownBlock>()
  val paragraph = StringBuilder()
  val code = StringBuilder()
  val tableRows = mutableListOf<List<String>>()
  var inCode = false

  fun flushParagraph() {
    val text = paragraph.toString().trim()
    if (text.isNotBlank()) blocks += MarkdownBlock.Paragraph(text)
    paragraph.clear()
  }

  fun flushTable() {
    if (tableRows.isNotEmpty()) {
      blocks += MarkdownBlock.Table(tableRows.toList())
      tableRows.clear()
    }
  }

  markdown.lines().forEach { rawLine ->
    val line = rawLine.trimEnd()
    if (line.trimStart().startsWith("```")) {
      if (inCode) {
        blocks += MarkdownBlock.Code(code.toString().trimEnd())
        code.clear()
        inCode = false
      } else {
        flushParagraph()
        inCode = true
      }
      return@forEach
    }

    if (inCode) {
      code.append(rawLine).append('\n')
      return@forEach
    }

    val trimmed = line.trim()
    if (trimmed.isBlank()) {
      flushTable()
      flushParagraph()
      return@forEach
    }

    if (isMarkdownDivider(trimmed)) {
      flushTable()
      flushParagraph()
      blocks += MarkdownBlock.Divider
      return@forEach
    }

    if (isMarkdownTableSeparator(trimmed)) {
      if (tableRows.isNotEmpty()) {
        flushParagraph()
      } else if (paragraph.isNotEmpty()) {
        paragraph.append('\n').append(trimmed)
      } else {
        paragraph.append(trimmed)
      }
      return@forEach
    }

    if (looksLikeMarkdownTableRow(trimmed)) {
      if (tableRows.isNotEmpty()) {
        tableRows += parseMarkdownTableRow(trimmed)
        return@forEach
      }
      val paragraphLines = paragraph.lines()
      if (paragraphLines.isNotEmpty() && isMarkdownTableSeparator(paragraphLines.last().trim())) {
        val header = paragraphLines.dropLast(1).lastOrNull()?.trim()
        if (header != null && looksLikeMarkdownTableRow(header)) {
          val body = paragraphLines.dropLast(2).joinToString("\n").trim()
          paragraph.clear()
          if (body.isNotBlank()) blocks += MarkdownBlock.Paragraph(body)
          tableRows += parseMarkdownTableRow(header)
          tableRows += parseMarkdownTableRow(trimmed)
          return@forEach
        }
      }
    }
    flushTable()

    val headingLevel = trimmed.takeWhile { it == '#' }.length
    if (headingLevel in 1..4 && trimmed.getOrNull(headingLevel) == ' ') {
      flushParagraph()
      blocks += MarkdownBlock.Heading(headingLevel, trimmed.drop(headingLevel).trim())
      return@forEach
    }

    val unordered = listOf("- ", "* ", "+ ").firstOrNull { trimmed.startsWith(it) }
    if (unordered != null) {
      flushParagraph()
      blocks += MarkdownBlock.ListItem("•", trimmed.drop(unordered.length).trim())
      return@forEach
    }

    val orderedMatch = Regex("^(\\d+)[.)]\\s+(.*)$").matchEntire(trimmed)
    if (orderedMatch != null) {
      flushParagraph()
      blocks += MarkdownBlock.ListItem("${orderedMatch.groupValues[1]}.", orderedMatch.groupValues[2])
      return@forEach
    }

    if (paragraph.isNotEmpty()) paragraph.append('\n')
    paragraph.append(trimmed)
  }

  if (inCode) blocks += MarkdownBlock.Code(code.toString().trimEnd())
  flushTable()
  flushParagraph()
  return blocks
}

private fun isMarkdownDivider(line: String): Boolean {
  val compact = line.filterNot { it.isWhitespace() }
  if (compact.length < 3) return false
  val marker = compact.first()
  return marker in listOf('-', '*', '_') && compact.all { it == marker }
}

private fun looksLikeMarkdownTableRow(line: String): Boolean {
  return line.count { it == '|' } >= 2
}

private fun isMarkdownTableSeparator(line: String): Boolean {
  val cells = parseMarkdownTableRow(line)
  return cells.isNotEmpty() && cells.all { cell ->
    cell.isNotBlank() && cell.all { it == '-' || it == ':' }
  }
}

private fun parseMarkdownTableRow(line: String): List<String> {
  return line
    .trim()
    .trim('|')
    .split('|')
    .map { it.trim() }
}

private fun renderInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
  var index = 0
  val pattern = Regex("(\\*\\*[^*]+\\*\\*)|(`[^`]+`)|(\\[[^]]+\\]\\([^)]+\\))")
  pattern.findAll(text).forEach { match ->
    append(text.substring(index, match.range.first))
    val value = match.value
    when {
      value.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(value.removePrefix("**").removeSuffix("**"))
      }
      value.startsWith("`") -> withStyle(
        SpanStyle(
          background = androidx.compose.ui.graphics.Color(0x1A2F5E47),
          fontWeight = FontWeight.Medium
        )
      ) {
        append(value.removePrefix("`").removeSuffix("`"))
      }
      value.startsWith("[") -> {
        val label = value.substringAfter("[").substringBefore("]")
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
          append(label)
        }
      }
    }
    index = match.range.last + 1
  }
  append(text.substring(index))
}

private fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("AI Chat message", text))
}

private fun createCameraCaptureUri(context: Context): Uri {
  val dir = File(context.cacheDir, "captured_images").apply { mkdirs() }
  val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun openAttachment(context: Context, attachment: ChatAttachment) {
  val file = File(attachment.localPath)
  if (!file.exists()) return
  runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, attachment.mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, attachment.displayName))
  }.onFailure {
    Toast.makeText(context, "无法用系统应用打开该文件", Toast.LENGTH_SHORT).show()
  }
}

private fun formatAttachmentSize(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024f
  if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
  val mb = kb / 1024f
  return String.format(Locale.getDefault(), "%.1f MB", mb)
}

private fun formatMessageTime(timestamp: Long): String {
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
  }.format(Date(timestamp))
}

private fun formatConversationTime(timestamp: Long): String {
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
  }.format(Date(timestamp))
}

private fun conversationDateBucket(timestamp: Long): String {
  val now = java.util.Calendar.getInstance()
  val target = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
  val todayStart = (now.clone() as java.util.Calendar).apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
  }
  val targetStart = (target.clone() as java.util.Calendar).apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
  }
  val days = ((todayStart.timeInMillis - targetStart.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()
  return when {
    days <= 0 -> "今天"
    days == 1 -> "昨天"
    days < 7 -> "最近 7 天"
    days < 30 -> "最近 30 天"
    else -> SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(timestamp))
  }
}

@Composable
private fun AttachmentStrip(
  attachments: List<ChatAttachment>,
  onOpenAttachment: (ChatAttachment) -> Unit,
  onRemoveAttachment: ((String) -> Unit)?,
  compact: Boolean
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    items(attachments, key = { it.id }) { attachment ->
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (compact) 0.88f else 0.64f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onOpenAttachment(attachment) }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          AttachmentIconOrThumbnail(attachment)
          Spacer(Modifier.width(6.dp))
          Column(modifier = Modifier.width(if (compact) 132.dp else 180.dp)) {
            Text(
              text = attachment.displayName,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = formatAttachmentSize(attachment.sizeBytes),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
              maxLines = 1
            )
          }
          if (onRemoveAttachment != null) {
            IconButton(
              onClick = { onRemoveAttachment(attachment.id) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "移除附件", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AttachmentIconOrThumbnail(attachment: ChatAttachment) {
  if (attachment.isImage) {
    val bitmap = remember(attachment.localPath) {
      BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
    }
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(42.dp)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
      )
      return
    }
  }
  Icon(
    if (attachment.isImage) Icons.Outlined.Image else Icons.AutoMirrored.Outlined.InsertDriveFile,
    contentDescription = null,
    modifier = Modifier.size(18.dp)
  )
}

@Composable
private fun GeneratedImageGrid(
  attachments: List<ChatAttachment>,
  onOpenAttachment: (ChatAttachment) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    attachments.forEach { attachment ->
      val bitmap = remember(attachment.localPath) {
        BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenAttachment(attachment) }
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = attachment.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 180.dp, max = 420.dp)
              .padding(4.dp)
          )
        } else {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Outlined.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(attachment.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }
}

@Composable
private fun ImagePreviewDialog(
  attachment: ChatAttachment,
  onDismiss: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val bitmap = remember(attachment.localPath) {
    BitmapFactory.decodeFile(attachment.localPath)?.asImageBitmap()
  }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = attachment.displayName,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭预览")
          }
        }
        Spacer(Modifier.height(8.dp))
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = attachment.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 180.dp, max = 620.dp)
          )
        } else {
          Text("无法在应用内预览这张图片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onOpenExternal) {
            Text("用系统应用打开")
          }
        }
      }
    }
  }
}

@Composable
private fun Composer(
  input: TextFieldValue,
  attachments: List<ChatAttachment>,
  attachmentsEnabled: Boolean,
  imageMode: Boolean = false,
  imageOptions: com.personal.aichat.domain.ImageGenerationOptions = com.personal.aichat.domain.ImageGenerationOptions(),
  onInput: (TextFieldValue) -> Unit,
  onSend: () -> Unit,
  onRetry: () -> Unit,
  onPickImages: () -> Unit,
  onPickFiles: () -> Unit,
  onTakePhoto: () -> Unit,
  onRemoveAttachment: (String) -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit,
  isGenerating: Boolean,
  onStopGenerating: () -> Unit,
  onImageSize: (ImageGenerationSize) -> Unit = {},
  onImageQuality: (ImageGenerationQuality) -> Unit = {},
  onImageCount: (Int) -> Unit = {},
  onImageOutputFormat: (ImageGenerationOutputFormat) -> Unit = {},
  onImageBackground: (ImageGenerationBackground) -> Unit = {},
  showRetry: Boolean = true
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .imePadding()
      .navigationBarsPadding()
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    if (attachments.isNotEmpty()) {
      AttachmentStrip(
        attachments = attachments,
        onOpenAttachment = onOpenAttachment,
        onRemoveAttachment = onRemoveAttachment,
        compact = true
      )
      Spacer(Modifier.height(6.dp))
    }
    if (imageMode) {
      ImageGenerationControls(
        options = imageOptions,
        onSize = onImageSize,
        onQuality = onImageQuality,
        onCount = onImageCount,
        onOutputFormat = onImageOutputFormat,
        onBackground = onImageBackground
      )
      Spacer(Modifier.height(6.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      var attachMenuOpen by remember { mutableStateOf(false) }
      if (attachmentsEnabled) {
        Box {
          IconButton(onClick = { attachMenuOpen = true }) {
            Icon(Icons.Outlined.AttachFile, contentDescription = "添加附件", tint = MaterialTheme.colorScheme.primary)
          }
          DropdownMenu(expanded = attachMenuOpen, onDismissRequest = { attachMenuOpen = false }) {
            DropdownMenuItem(
              text = { Text("选择图片") },
              leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
              onClick = {
                attachMenuOpen = false
                onPickImages()
              }
            )
            DropdownMenuItem(
              text = { Text("拍摄照片") },
              leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
              onClick = {
                attachMenuOpen = false
                onTakePhoto()
              }
            )
            DropdownMenuItem(
              text = { Text("选择文件") },
              leadingIcon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
              enabled = !imageMode,
              onClick = {
                attachMenuOpen = false
                onPickFiles()
              }
            )
          }
        }
      }
      OutlinedTextField(
        value = input,
        onValueChange = onInput,
        modifier = Modifier.weight(1f),
        minLines = 1,
        maxLines = 5,
        placeholder = { Text(if (imageMode) "描述要生成或编辑的图片" else "给当前模型发送消息") }
      )
      Spacer(Modifier.width(8.dp))
      if (!isGenerating && showRetry) {
        IconButton(onClick = onRetry) {
          Icon(Icons.Outlined.Refresh, contentDescription = "重试上一条", tint = MaterialTheme.colorScheme.primary)
        }
      }
      if (isGenerating) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.primary,
          tonalElevation = 1.dp
        ) {
          IconButton(onClick = onStopGenerating) {
            Icon(
              Icons.Outlined.Stop,
              contentDescription = "停止输出",
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
        }
      } else {
        val canSend = input.text.isNotBlank() || (attachmentsEnabled && attachments.isNotEmpty())
        IconButton(onClick = onSend, enabled = canSend) {
          Icon(
            Icons.AutoMirrored.Outlined.Send,
            contentDescription = "发送",
            tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun ImageGenerationControls(
  options: com.personal.aichat.domain.ImageGenerationOptions,
  onSize: (ImageGenerationSize) -> Unit,
  onQuality: (ImageGenerationQuality) -> Unit,
  onCount: (Int) -> Unit,
  onOutputFormat: (ImageGenerationOutputFormat) -> Unit,
  onBackground: (ImageGenerationBackground) -> Unit
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    item {
      ImageOptionMenu(
        label = "尺寸 ${imageSizeLabel(options.size)}",
        values = ImageGenerationSize.entries,
        selected = options.size,
        valueLabel = ::imageSizeLabel,
        onSelect = onSize
      )
    }
    item {
      ImageOptionMenu(
        label = "质量 ${imageQualityLabel(options.quality)}",
        values = ImageGenerationQuality.entries,
        selected = options.quality,
        valueLabel = ::imageQualityLabel,
        onSelect = onQuality
      )
    }
    item {
      ImageCountMenu(count = options.count, onCount = onCount)
    }
    item {
      ImageOptionMenu(
        label = "格式 ${imageOutputFormatLabel(options.outputFormat)}",
        values = ImageGenerationOutputFormat.entries,
        selected = options.outputFormat,
        valueLabel = ::imageOutputFormatLabel,
        onSelect = onOutputFormat
      )
    }
    item {
      ImageOptionMenu(
        label = "背景 ${imageBackgroundLabel(options.background)}",
        values = ImageGenerationBackground.entries,
        selected = options.background,
        valueLabel = ::imageBackgroundLabel,
        onSelect = onBackground
      )
    }
  }
}

@Composable
private fun <T> ImageOptionMenu(
  label: String,
  values: List<T>,
  selected: T,
  valueLabel: (T) -> String,
  onSelect: (T) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    AssistChip(onClick = { expanded = true }, label = { Text(label) })
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      values.forEach { value ->
        DropdownMenuItem(
          text = { Text(valueLabel(value)) },
          leadingIcon = {
            if (value == selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
          },
          onClick = {
            expanded = false
            onSelect(value)
          }
        )
      }
    }
  }
}

@Composable
private fun ImageCountMenu(count: Int, onCount: (Int) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    AssistChip(onClick = { expanded = true }, label = { Text("数量 $count") })
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      (1..4).forEach { value ->
        DropdownMenuItem(
          text = { Text("$value 张") },
          leadingIcon = {
            if (value == count) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
          },
          onClick = {
            expanded = false
            onCount(value)
          }
        )
      }
    }
  }
}

private fun imageSizeLabel(size: ImageGenerationSize): String = when (size) {
  ImageGenerationSize.AUTO -> "自动"
  ImageGenerationSize.SQUARE -> "方图"
  ImageGenerationSize.LANDSCAPE -> "横图"
  ImageGenerationSize.PORTRAIT -> "竖图"
}

private fun imageQualityLabel(quality: ImageGenerationQuality): String = when (quality) {
  ImageGenerationQuality.AUTO -> "自动"
  ImageGenerationQuality.LOW -> "低"
  ImageGenerationQuality.MEDIUM -> "中"
  ImageGenerationQuality.HIGH -> "高"
}

private fun imageOutputFormatLabel(format: ImageGenerationOutputFormat): String = when (format) {
  ImageGenerationOutputFormat.PNG -> "PNG"
  ImageGenerationOutputFormat.JPEG -> "JPEG"
  ImageGenerationOutputFormat.WEBP -> "WEBP"
}

private fun imageBackgroundLabel(background: ImageGenerationBackground): String = when (background) {
  ImageGenerationBackground.AUTO -> "自动"
  ImageGenerationBackground.OPAQUE -> "不透明"
  ImageGenerationBackground.TRANSPARENT -> "透明"
}

@Composable
private fun ProviderManagerDialog(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit,
  onEditProvider: (ChatProviderConfig?) -> Unit,
  onCloneProvider: (String) -> Unit,
  onDeleteProvider: (String) -> Unit,
  onCreateProvider: (ProviderType) -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .padding(18.dp)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "API 配置",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "可保存多组 GPT、DeepSeek 或代理配置，并随时切换。被 AI 机器人使用的配置需先改绑或删除机器人后再删除。",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          state.providers.forEach { provider ->
            ProviderConfigRow(
              provider = provider,
              selected = provider.id == state.selectedProviderId,
              onSelect = { onSelectProvider(provider.id) },
              onEdit = { onEditProvider(provider) },
              onClone = { onCloneProvider(provider.id) },
              onDelete = { onDeleteProvider(provider.id) }
            )
          }
        }
        Spacer(Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TextButton(onClick = { onCreateProvider(ProviderType.OPENAI_RESPONSES) }) {
            Text("新增 GPT")
          }
          TextButton(onClick = { onCreateProvider(ProviderType.OPENAI_COMPATIBLE_CHAT) }) {
            Text("新增 DeepSeek")
          }
          TextButton(onClick = { onCreateProvider(ProviderType.TOKENHUB_PROXY) }) {
            Text("新增代理")
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("关闭")
          }
        }
      }
    }
  }
}

@Composable
private fun ProviderConfigRow(
  provider: ChatProviderConfig,
  selected: Boolean,
  onSelect: () -> Unit,
  onEdit: () -> Unit,
  onClone: () -> Unit,
  onDelete: () -> Unit
) {
  var confirmDelete by remember(provider.id) { mutableStateOf(false) }
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      RadioButton(selected = selected, onClick = onSelect)
      Column(modifier = Modifier.weight(1f)) {
        Text(provider.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "${provider.type.label} / ${provider.defaultModel}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          provider.baseUrl,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      IconButton(onClick = onClone) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = "克隆配置")
      }
      IconButton(onClick = onEdit) {
        Icon(Icons.Outlined.Edit, contentDescription = "编辑配置")
      }
      IconButton(onClick = { confirmDelete = true }) {
        Icon(Icons.Outlined.Delete, contentDescription = "删除配置", tint = MaterialTheme.colorScheme.error)
      }
    }
  }
  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("删除 API 配置") },
      text = {
        Text("确定要删除「${provider.displayName}」吗？如果有 AI 机器人正在使用它，删除会被阻止，并提示你先删除机器人或把机器人切换到其他 API 配置。")
      },
      confirmButton = {
        Button(
          onClick = {
            confirmDelete = false
            onDelete()
          }
        ) {
          Text("删除")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) {
          Text("取消")
        }
      }
    )
  }
}

@Composable
private fun ProviderRebindDeleteDialog(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onRebindAndDelete: (String) -> Unit
) {
  val sourceProvider = state.providers.firstOrNull { it.id == state.providerRebindDeleteSourceId }
  val bots = state.aiBots.filter { it.id in state.providerRebindDeleteBotIds }
  val targets = state.providers.filter { it.id != state.providerRebindDeleteSourceId }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("改绑机器人后删除") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("「${sourceProvider?.displayName ?: "此 API 配置"}」仍被以下机器人使用：")
        bots.forEach { bot ->
          Text("· ${bot.name} / ${bot.model}", style = MaterialTheme.typography.bodySmall)
        }
        Text(
          "选择一个现有 API 配置后，这些机器人会全部改绑到目标配置，并使用目标配置的默认模型，然后删除原 API 配置。",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall
        )
        if (targets.isEmpty()) {
          Text(
            "当前没有其他 API 配置可用于改绑。请先新增一个 API 配置，或删除这些机器人。",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        } else {
          targets.forEach { provider ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onRebindAndDelete(provider.id) }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(provider.displayName, fontWeight = FontWeight.SemiBold)
                Text(
                  "${provider.type.label} / ${provider.defaultModel}",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSettingsDialog(
  provider: ChatProviderConfig,
  hasSavedApiKey: Boolean,
  onDismiss: () -> Unit,
  onSave: (ChatProviderConfig, String?) -> Unit
) {
  var displayName by remember(provider.id) { mutableStateOf(provider.displayName) }
  var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
  var model by remember(provider.id) { mutableStateOf(provider.defaultModel) }
  var reasoningEffort by remember(provider.id) { mutableStateOf(provider.reasoningEffort) }
  var supportsAttachments by remember(provider.id) { mutableStateOf(provider.supportsAttachments) }
  var supportsImageGeneration by remember(provider.id) { mutableStateOf(provider.supportsImageGeneration) }
  var imageGenerationApiMode by remember(provider.id) { mutableStateOf(provider.imageGenerationApiMode) }
  var imageGenerationModel by remember(provider.id) { mutableStateOf(provider.imageGenerationModel) }
  var apiKey by remember(provider.id) { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 640.dp)
        .padding(18.dp)
    ) {
      Column(
        modifier = Modifier.padding(18.dp)
      ) {
        Text(
          text = "编辑 API 配置",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(14.dp))
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .defaultMinSize(minHeight = 1.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = provider.type.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("API 类型") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("显示名称") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("默认模型") },
            modifier = Modifier.fillMaxWidth()
          )
          if (provider.type == ProviderType.OPENAI_RESPONSES) {
            ReasoningEffortSelector(
              value = reasoningEffort,
              onValueChange = { reasoningEffort = it }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("允许图片/文件附件", fontWeight = FontWeight.Medium)
              Text(
                text = "仅在供应商 API 支持多模态输入时开启。DeepSeek 当前不支持。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = supportsAttachments,
              onCheckedChange = { supportsAttachments = it }
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("允许生图模式", fontWeight = FontWeight.Medium)
              Text(
                text = "第一版仅建议 GPT / OpenAI Responses 配置开启。DeepSeek 当前不支持图片生成。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = supportsImageGeneration,
              enabled = provider.type == ProviderType.OPENAI_RESPONSES,
              onCheckedChange = { supportsImageGeneration = it }
            )
          }
          if (provider.type == ProviderType.OPENAI_RESPONSES) {
            ImageGenerationApiModeSelector(
              value = imageGenerationApiMode,
              enabled = supportsImageGeneration,
              onValueChange = { imageGenerationApiMode = it }
            )
            OutlinedTextField(
              value = imageGenerationModel,
              onValueChange = { imageGenerationModel = it },
              enabled = supportsImageGeneration && imageGenerationApiMode == ImageGenerationApiMode.IMAGES_API,
              label = { Text("生图模型名") },
              placeholder = { Text("例如 gpt-image-2 或 gpt-image-1") },
              supportingText = {
                Text("Images API 模式使用此模型；Responses 工具模式继续使用上方默认模型。")
              },
              modifier = Modifier.fillMaxWidth()
            )
          }
          OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key 或代理 Key") },
            placeholder = {
              Text(if (hasSavedApiKey) "已保存 Key；留空则继续使用原 Key" else "请输入 API Key")
            },
            modifier = Modifier.fillMaxWidth()
          )
          if (hasSavedApiKey) {
            Text(
              text = "当前配置已有加密保存的 Key。留空则继续使用原 Key；输入新 Key 后保存会替换。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Text(
            text = provider.type.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Key 会通过 Android Keystore 加密保存，不写入聊天数据库。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("取消")
          }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              onSave(
                provider.copy(
                  displayName = displayName.trim(),
                  baseUrl = baseUrl.trim().trimEnd('/'),
                  defaultModel = model.trim(),
                  enabled = true,
                  supportsAttachments = supportsAttachments,
                  supportsImageGeneration = supportsImageGeneration && provider.type == ProviderType.OPENAI_RESPONSES,
                  imageGenerationApiMode = imageGenerationApiMode,
                  imageGenerationModel = imageGenerationModel.trim(),
                  reasoningEffort = reasoningEffort
                ),
                apiKey.takeIf { it.isNotBlank() }
              )
            }
          ) {
            Text("保存")
          }
        }
      }
    }
  }
}

@Composable
private fun ReasoningEffortSelector(
  value: ReasoningEffort,
  onValueChange: (ReasoningEffort) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxWidth()) {
    AssistChip(
      onClick = { expanded = true },
      label = { Text("推理强度：${value.label}") },
      trailingIcon = {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      ReasoningEffort.entries.forEach { effort ->
        DropdownMenuItem(
          text = {
            Column {
              Text(effort.label)
              Text(
                effort.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = {
            onValueChange(effort)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun ImageGenerationApiModeSelector(
  value: ImageGenerationApiMode,
  enabled: Boolean,
  onValueChange: (ImageGenerationApiMode) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxWidth()) {
    AssistChip(
      enabled = enabled,
      onClick = { expanded = true },
      label = { Text("生图接口：${value.label}") },
      trailingIcon = {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      ImageGenerationApiMode.entries.forEach { mode ->
        DropdownMenuItem(
          text = {
            Column {
              Text(mode.label)
              Text(
                mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          onClick = {
            onValueChange(mode)
            expanded = false
          }
        )
      }
    }
  }
}

private val com.personal.aichat.domain.ProviderType.label: String
  get() = when (this) {
    com.personal.aichat.domain.ProviderType.OPENAI_RESPONSES -> "GPT / OpenAI Responses"
    com.personal.aichat.domain.ProviderType.OPENAI_COMPATIBLE_CHAT -> "DeepSeek / OpenAI-compatible Chat Completions"
    com.personal.aichat.domain.ProviderType.TOKENHUB_PROXY -> "TokenHub 代理 / Responses 兼容"
    com.personal.aichat.domain.ProviderType.ANTHROPIC_MESSAGES -> "Anthropic Messages"
    com.personal.aichat.domain.ProviderType.GEMINI_GENERATE_CONTENT -> "Gemini GenerateContent"
  }

private val ImageGenerationApiMode.label: String
  get() = when (this) {
    ImageGenerationApiMode.RESPONSES_TOOL -> "Responses 工具模式"
    ImageGenerationApiMode.IMAGES_API -> "Images API 模式"
  }

private val ImageGenerationApiMode.description: String
  get() = when (this) {
    ImageGenerationApiMode.RESPONSES_TOOL -> "请求 /responses + image_generation，适合官方 OpenAI Responses。"
    ImageGenerationApiMode.IMAGES_API -> "请求 /images/generations 或 /images/edits，适合只支持 generations 的中转。"
  }

private val ReasoningEffort.label: String
  get() = when (this) {
    ReasoningEffort.AUTO -> "智能"
    ReasoningEffort.LOW -> "低"
    ReasoningEffort.MEDIUM -> "中"
    ReasoningEffort.HIGH -> "高"
    ReasoningEffort.XHIGH -> "超高"
  }

private val ReasoningEffort.description: String
  get() = when (this) {
    ReasoningEffort.AUTO -> "不显式发送 effort，由模型和服务端自动决定。"
    ReasoningEffort.LOW -> "更快、成本更低，适合普通问答。"
    ReasoningEffort.MEDIUM -> "平衡速度和推理质量。"
    ReasoningEffort.HIGH -> "更深入推理，适合复杂任务。"
    ReasoningEffort.XHIGH -> "尽可能高强度推理；仅部分模型支持。"
  }

private val com.personal.aichat.domain.ProviderType.description: String
  get() = when (this) {
    com.personal.aichat.domain.ProviderType.OPENAI_RESPONSES ->
      "用于 GPT / OpenAI 官方 Responses API。Base URL 示例：https://api.openai.com/v1"
    com.personal.aichat.domain.ProviderType.OPENAI_COMPATIBLE_CHAT ->
      "用于 DeepSeek 以及其他 OpenAI-compatible Chat Completions 服务。DeepSeek 示例：https://api.deepseek.com"
    com.personal.aichat.domain.ProviderType.TOKENHUB_PROXY ->
      "用于本机或局域网中暴露 Responses-compatible /v1 API 的代理。"
    com.personal.aichat.domain.ProviderType.ANTHROPIC_MESSAGES ->
      "Reserved for a future Anthropic Messages adapter."
    com.personal.aichat.domain.ProviderType.GEMINI_GENERATE_CONTENT ->
      "Reserved for a future Gemini GenerateContent adapter."
  }
