package com.personal.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.ImportExport
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.AiBot
import com.personal.aichat.domain.ChatBackgroundPreset
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ContextCapacity
import com.personal.aichat.domain.ContextCapacityStatus
import com.personal.aichat.domain.ConversationType
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.FavoriteSnippet
import com.personal.aichat.domain.FavoriteSnippetMessage
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupChatRoom
import com.personal.aichat.domain.GroupAutoPlayPreference
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
import com.personal.aichat.domain.StreamingBubbleMotion
import com.personal.aichat.domain.WebSearchMode
import com.personal.aichat.domain.groupAutoPlayPreference
import com.personal.aichat.domain.parseContextWindowTokensInput
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

@Composable
internal fun streamingPulse(enabled: Boolean): Float {
  if (!enabled) return 0f
  val transition = rememberInfiniteTransition()
  val pulse by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 950),
      repeatMode = RepeatMode.Reverse
    )
  )
  return pulse
}

@Composable
internal fun StreamingBubbleFrame(
  streaming: Boolean,
  motion: StreamingBubbleMotion,
  accent: Color,
  containerColor: Color,
  contentColor: Color,
  selected: Boolean,
  shape: Shape,
  modifier: Modifier = Modifier,
  baseBorderColor: Color? = null,
  baseBorderWidth: Dp = 1.dp,
  selectedBorderColor: Color = MaterialTheme.colorScheme.primary,
  content: @Composable () -> Unit
) {
  val animate = streaming && motion != StreamingBubbleMotion.OFF
  val pulse = streamingPulse(animate)
  val animatedContainer = if (animate && motion == StreamingBubbleMotion.STANDARD) {
    mixColors(containerColor, accent, 0.025f + 0.045f * pulse)
  } else {
    containerColor
  }
  val streamingBorderColor = when {
    !animate -> null
    motion == StreamingBubbleMotion.STANDARD -> accent.copy(alpha = 0.46f + 0.42f * pulse)
    else -> accent.copy(alpha = 0.26f + 0.24f * pulse)
  }
  val borderColor = when {
    selected -> selectedBorderColor
    streamingBorderColor != null -> streamingBorderColor
    else -> baseBorderColor
  }
  val borderWidth = when {
    selected -> 2.dp
    animate && motion == StreamingBubbleMotion.STANDARD -> 2.dp
    animate -> 1.dp
    else -> baseBorderWidth
  }
  Surface(
    color = animatedContainer,
    contentColor = contentColor,
    shape = shape,
    modifier = modifier.then(
      borderColor?.let { Modifier.border(borderWidth, it, shape) } ?: Modifier
    )
  ) {
    content()
  }
}

@Composable
internal fun StreamingStatusIndicator(
  text: String,
  accent: Color,
  textColor: Color,
  motion: StreamingBubbleMotion,
  modifier: Modifier = Modifier,
  animatedDots: Boolean = false
) {
  val animate = motion != StreamingBubbleMotion.OFF
  if (!animate) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = textColor,
      fontWeight = FontWeight.SemiBold,
      modifier = modifier
    )
    return
  }
  val pulse = streamingPulse(animate)
  val dots = if (animatedDots) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
      initialValue = 0f,
      targetValue = 3.99f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1_050),
        repeatMode = RepeatMode.Restart
      )
    )
    ".".repeat((phase.toInt() % 4).coerceAtLeast(1))
  } else {
    ""
  }
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Box(
      modifier = Modifier
        .size(if (animate) 7.dp + (2.dp * pulse) else 7.dp)
        .background(accent.copy(alpha = if (animate) 0.52f + 0.38f * pulse else 0.62f), RoundedCornerShape(999.dp))
    )
    Text(
      text = text + dots,
      style = MaterialTheme.typography.bodySmall,
      color = textColor,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
fun AIChatAppRoot(viewModel: ChatViewModel) {
  val state by viewModel.uiState.collectAsState()
  val editingProvider = state.editingProvider
  val context = LocalContext.current
  var drawerOpen by remember { mutableStateOf(false) }
  var previewImage by remember { mutableStateOf<ChatAttachment?>(null) }
  var previewAttachment by remember { mutableStateOf<ChatAttachment?>(null) }
  var favoriteDraftMessageIds by remember { mutableStateOf<Set<String>?>(null) }
  var favoriteDraftGroupMessageIds by remember { mutableStateOf<Set<String>?>(null) }
  var editingFavorite by remember { mutableStateOf<FavoriteSnippet?>(null) }
  var appendFavoritePickerOpen by remember { mutableStateOf(false) }
  var groupChatDialogDraft by remember { mutableStateOf<GroupChatDialogDraft?>(null) }
  val openAttachmentInApp: (ChatAttachment) -> Unit = { attachment ->
    if (attachment.isImage) {
      previewImage = attachment
    } else if (attachment.canPreviewInApp()) {
      previewAttachment = attachment
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
        onCompressContext = viewModel::compressSelectedConversationContext,
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
        onSaveAutoPlayPreference = viewModel::setGroupAutoPlayPreference,
        onCompressContext = viewModel::compressSelectedGroupContext,
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
        onShareMarkdown = { viewModel.shareGroupChatMarkdownFile(context) },
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
        onCleanupHistoricalDsmlToolMarkup = viewModel::cleanupHistoricalDsmlToolMarkup,
        onWebSearchMode = viewModel::setWebSearchMode,
        onStreamingBubbleMotion = viewModel::setStreamingBubbleMotion,
        onAttachmentMaxFileMb = viewModel::setAttachmentMaxFileMb,
        onAttachmentMaxPendingMb = viewModel::setAttachmentMaxPendingMb,
        onAttachmentMaxImageSourceMb = viewModel::setAttachmentMaxImageSourceMb,
        onExportProviderConfigs = { viewModel.exportProviderConfigsText(context) },
        onExportProviderConfigsQr = { onReady -> viewModel.exportProviderConfigsQrText(onReady) },
        onImportProviderConfigs = viewModel::importProviderConfigsText,
        onExportBackgroundPresets = { viewModel.exportBackgroundPresetsText(context) },
        onImportBackgroundPresets = viewModel::importBackgroundPresetsText,
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
        onExportJson = { viewModel.exportFavoriteSnippetsJson(context) },
        onExportMarkdown = { viewModel.exportFavoriteSnippetsMarkdown(context) },
        onImportJson = viewModel::importFavoriteSnippetsJson,
        onEdit = { editingFavorite = it },
        onDelete = viewModel::deleteFavoriteSnippet,
        onAddTagsToFavorites = viewModel::addTagsToFavoriteSnippets,
        onRenameTag = viewModel::renameFavoriteTag,
        onDeleteTag = viewModel::deleteFavoriteTag,
        onRemoveMessage = viewModel::removeMessagesFromFavorite,
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
      val editableGroupId = editingGroup?.id.takeIf { mode == GroupChatDialogMode.EDIT }
      NewGroupChatDialog(
        bots = state.aiBots.filter { it.enabled },
        backgroundPresets = state.appSettings.backgroundPresets,
        commonBackgroundPresetIds = editableGroupId?.let { state.appSettings.groupBackgroundPresetCombinations[it] }.orEmpty(),
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
        onManageBots = {
          groupChatDialogDraft = null
          viewModel.closeNewGroupChatDialog()
          viewModel.openBotManager()
        },
        onCreate = { title, topic, botIds ->
          groupChatDialogDraft = null
          if (mode == GroupChatDialogMode.EDIT && editingGroup != null) {
            viewModel.updateGroupChat(editingGroup.id, title, topic, botIds)
          } else {
            viewModel.createGroupChat(title, topic, botIds)
          }
        },
        onSaveBackgroundPresetCombination = editableGroupId?.let { groupId ->
          { presetIds -> viewModel.saveGroupBackgroundPresetCombination(groupId, presetIds) }
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
    previewAttachment?.let { attachment ->
      AttachmentPreviewDialog(
        attachment = attachment,
        onDismiss = { previewAttachment = null },
        onOpenExternal = { openAttachment(context, attachment) }
      )
    }
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
  onExportJson: () -> Unit,
  onExportMarkdown: () -> Unit,
  onImportJson: (String) -> Unit,
  onEdit: (FavoriteSnippet) -> Unit,
  onDelete: (String) -> Unit,
  onAddTagsToFavorites: (Set<String>, String) -> Unit,
  onRenameTag: (String, String) -> Unit,
  onDeleteTag: (String) -> Unit,
  onRemoveMessage: (String, Set<String>) -> Unit,
  onJumpToSource: (FavoriteSnippet) -> Unit
) {
  var query by remember { mutableStateOf("") }
  var tagFilter by remember { mutableStateOf<String?>(null) }
  var selectedFavoriteId by remember { mutableStateOf<String?>(null) }
  var batchMode by remember { mutableStateOf(false) }
  var sortMode by remember { mutableStateOf(FavoriteSortMode.UpdatedDesc) }
  var selectedFavoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var tagManagerOpen by remember { mutableStateOf(false) }
  var batchTagDialogOpen by remember { mutableStateOf(false) }
  var importExportOpen by remember { mutableStateOf(false) }
  val selectedFavorite = favorites.firstOrNull { it.id == selectedFavoriteId }
  val allTags = remember(favorites) {
    favorites.flatMap { it.tags }.distinctBy { it.lowercase() }.sorted()
  }
  val tagSummaries = remember(favorites, allTags) {
    allTags.map { tag ->
      FavoriteTagSummary(
        tag = tag,
        count = favorites.count { favorite -> favorite.tags.any { it.equals(tag, ignoreCase = true) } }
      )
    }
  }
  val normalizedQuery = query.trim().lowercase()
  val filtered = favorites.filter { favorite ->
    val matchesQuery = normalizedQuery.isBlank() || favorite.searchText.contains(normalizedQuery)
    val matchesTag = tagFilter == null || favorite.tags.any { it.equals(tagFilter, ignoreCase = true) }
    matchesQuery && matchesTag
  }
  val sortedFavorites = remember(filtered, sortMode) {
    when (sortMode) {
      FavoriteSortMode.UpdatedDesc -> filtered.sortedWith(
        compareByDescending<FavoriteSnippet> { it.updatedAt }.thenByDescending { it.createdAt }
      )
      FavoriteSortMode.CreatedDesc -> filtered.sortedWith(
        compareByDescending<FavoriteSnippet> { it.createdAt }.thenByDescending { it.updatedAt }
      )
      FavoriteSortMode.TitleAsc -> filtered.sortedWith(
        compareBy<FavoriteSnippet> { it.title.lowercase() }.thenByDescending { it.updatedAt }
      )
      FavoriteSortMode.TagAsc -> filtered.sortedWith(
        compareBy<FavoriteSnippet> { it.primaryTagSortKey() }
          .thenBy { it.title.lowercase() }
          .thenByDescending { it.updatedAt }
      )
    }
  }
  val filteredIds = filtered.map { it.id }.toSet()
  LaunchedEffect(favorites) {
    selectedFavoriteIds = selectedFavoriteIds.filterTo(mutableSetOf()) { id -> favorites.any { it.id == id } }
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
        onRemoveMessages = { messageIds -> onRemoveMessage(selectedFavorite.id, messageIds) },
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
          TextButton(onClick = {
            batchMode = !batchMode
            if (!batchMode) selectedFavoriteIds = emptySet()
          }) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (batchMode) "退出批量" else "批量")
          }
          TextButton(onClick = { importExportOpen = true }) {
            Icon(Icons.Outlined.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("导入导出")
          }
          if (allTags.isNotEmpty()) {
            TextButton(onClick = { tagManagerOpen = true }) {
              Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("标签")
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭收藏夹")
          }
        }
        if (batchMode) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("已选择 ${selectedFavoriteIds.size} 项", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              TextButton(
                onClick = {
                  selectedFavoriteIds = if (selectedFavoriteIds.containsAll(filteredIds)) {
                    selectedFavoriteIds - filteredIds
                  } else {
                    selectedFavoriteIds + filteredIds
                  }
                },
                enabled = filteredIds.isNotEmpty()
              ) {
                Text(if (filteredIds.isNotEmpty() && selectedFavoriteIds.containsAll(filteredIds)) "取消全选" else "全选当前")
              }
              TextButton(
                onClick = {
                  selectedFavoriteIds.forEach(onDelete)
                  selectedFavoriteIds = emptySet()
                  batchMode = false
                },
                enabled = selectedFavoriteIds.isNotEmpty()
              ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("删除")
              }
              TextButton(
                onClick = { batchTagDialogOpen = true },
                enabled = selectedFavoriteIds.isNotEmpty()
              ) {
                Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("打标签")
              }
            }
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FavoriteSortMenu(sortMode = sortMode, onSortModeChange = { sortMode = it })
          Text(
            "${sortedFavorites.size} 项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
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
        if (sortedFavorites.isEmpty()) {
          Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
              text = if (favorites.isEmpty()) "还没有收藏片段。可以在消息气泡或多选菜单里收藏。" else "没有匹配的收藏。",
              modifier = Modifier.padding(16.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(sortedFavorites, key = { it.id }) { favorite ->
              FavoriteSnippetCard(
                favorite = favorite,
                query = normalizedQuery,
                batchMode = batchMode,
                selected = favorite.id in selectedFavoriteIds,
                onToggleSelected = {
                  selectedFavoriteIds = if (favorite.id in selectedFavoriteIds) {
                    selectedFavoriteIds - favorite.id
                  } else {
                    selectedFavoriteIds + favorite.id
                  }
                },
                onClick = {
                  if (batchMode) {
                    selectedFavoriteIds = if (favorite.id in selectedFavoriteIds) {
                      selectedFavoriteIds - favorite.id
                    } else {
                      selectedFavoriteIds + favorite.id
                    }
                  } else {
                    selectedFavoriteId = favorite.id
                  }
                }
              )
            }
          }
        }
      }
    }
  }
  if (tagManagerOpen) {
    FavoriteTagManagerDialog(
      tags = tagSummaries,
      onDismiss = { tagManagerOpen = false },
      onRename = { oldTag, newTag ->
        onRenameTag(oldTag, newTag)
        if (tagFilter.equals(oldTag, ignoreCase = true)) {
          tagFilter = newTag.trim().trimStart('#').takeIf { it.isNotBlank() }
        }
      },
      onDelete = { tag ->
        onDeleteTag(tag)
        if (tagFilter.equals(tag, ignoreCase = true)) tagFilter = null
      }
    )
  }
  if (batchTagDialogOpen) {
    FavoriteBatchTagDialog(
      count = selectedFavoriteIds.size,
      onDismiss = { batchTagDialogOpen = false },
      onSave = { tags ->
        onAddTagsToFavorites(selectedFavoriteIds, tags)
        selectedFavoriteIds = emptySet()
        batchMode = false
        batchTagDialogOpen = false
      }
    )
  }
  if (importExportOpen) {
    FavoriteImportExportDialog(
      onDismiss = { importExportOpen = false },
      onExportJson = onExportJson,
      onExportMarkdown = onExportMarkdown,
      onImportJson = onImportJson
    )
  }
}

private enum class FavoriteSortMode {
  UpdatedDesc,
  CreatedDesc,
  TitleAsc,
  TagAsc
}

private fun favoriteSortModeLabel(sortMode: FavoriteSortMode): String = when (sortMode) {
  FavoriteSortMode.UpdatedDesc -> "最近更新"
  FavoriteSortMode.CreatedDesc -> "收藏时间"
  FavoriteSortMode.TitleAsc -> "标题"
  FavoriteSortMode.TagAsc -> "标签"
}

private fun FavoriteSnippet.primaryTagSortKey(): String {
  return tags.minOfOrNull { it.lowercase() } ?: "~"
}

@Composable
private fun FavoriteSortMenu(
  sortMode: FavoriteSortMode,
  onSortModeChange: (FavoriteSortMode) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    AssistChip(
      onClick = { expanded = true },
      label = { Text("排序 ${favoriteSortModeLabel(sortMode)}") },
      trailingIcon = {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      FavoriteSortMode.entries.forEach { mode ->
        DropdownMenuItem(
          text = { Text(favoriteSortModeLabel(mode)) },
          leadingIcon = {
            if (mode == sortMode) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
          },
          onClick = {
            expanded = false
            onSortModeChange(mode)
          }
        )
      }
    }
  }
}

@Composable
private fun FavoriteBatchTagDialog(
  count: Int,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit
) {
  var value by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("批量打标签") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "将为已选择的 $count 个收藏追加标签，已有标签会自动去重。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          label = { Text("标签，空格或逗号分隔") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSave(value) }, enabled = value.trim().trimStart('#').isNotBlank()) {
        Text("添加")
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
private fun FavoriteImportExportDialog(
  onDismiss: () -> Unit,
  onExportJson: () -> Unit,
  onExportMarkdown: () -> Unit,
  onImportJson: (String) -> Unit
) {
  var importText by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("导入导出收藏") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "JSON 可用于恢复收藏；Markdown 适合阅读和归档。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Button(onClick = onExportJson) {
            Text("导出 JSON")
          }
          TextButton(onClick = onExportMarkdown) {
            Text("导出 Markdown")
          }
        }
        OutlinedTextField(
          value = importText,
          onValueChange = { importText = it },
          label = { Text("粘贴收藏 JSON") },
          minLines = 6,
          maxLines = 10,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onImportJson(importText)
          importText = ""
          onDismiss()
        },
        enabled = importText.isNotBlank()
      ) {
        Text("导入")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("关闭")
      }
    }
  )
}

private data class FavoriteTagSummary(
  val tag: String,
  val count: Int
)

@Composable
private fun FavoriteTagManagerDialog(
  tags: List<FavoriteTagSummary>,
  onDismiss: () -> Unit,
  onRename: (String, String) -> Unit,
  onDelete: (String) -> Unit
) {
  var editingTag by remember { mutableStateOf<String?>(null) }
  var deletingTag by remember { mutableStateOf<String?>(null) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("管理收藏标签") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          "重命名为已有标签会自动合并；删除标签只会移除分类，不会删除收藏内容。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        tags.forEach { summary ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(summary.tag, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${summary.count} 个收藏", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              TextButton(onClick = { editingTag = summary.tag }) {
                Text("重命名")
              }
              TextButton(onClick = { deletingTag = summary.tag }) {
                Text("删除")
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("关闭")
      }
    }
  )
  editingTag?.let { tag ->
    FavoriteTagRenameDialog(
      tag = tag,
      onDismiss = { editingTag = null },
      onSave = { newTag ->
        onRename(tag, newTag)
        editingTag = null
      }
    )
  }
  deletingTag?.let { tag ->
    AlertDialog(
      onDismissRequest = { deletingTag = null },
      title = { Text("删除标签") },
      text = { Text("将从所有收藏中移除 #$tag。收藏内容不会被删除。") },
      confirmButton = {
        Button(onClick = {
          onDelete(tag)
          deletingTag = null
        }) {
          Text("删除")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingTag = null }) {
          Text("取消")
        }
      }
    )
  }
}

@Composable
private fun FavoriteTagRenameDialog(
  tag: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit
) {
  var value by remember(tag) { mutableStateOf(tag) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("重命名标签") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("填写已有标签名会合并到该标签。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          label = { Text("标签名") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSave(value) }, enabled = value.trim().trimStart('#').isNotBlank()) {
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
private fun FavoriteSnippetCard(
  favorite: FavoriteSnippet,
  query: String,
  batchMode: Boolean,
  selected: Boolean,
  onToggleSelected: () -> Unit,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (batchMode) {
        Checkbox(
          checked = selected,
          onCheckedChange = { onToggleSelected() },
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      val highlightStyle = SpanStyle(
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        highlightFavoriteMatch(favorite.title, query, highlightStyle),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (favorite.description.isNotBlank()) {
        Text(
          highlightFavoriteMatch(favorite.description, query, highlightStyle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      if (favorite.tags.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(favorite.tags, key = { it }) { tag ->
            AssistChip(onClick = {}, label = { Text(highlightFavoriteMatch(tag, query, highlightStyle)) }, leadingIcon = {
              Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, modifier = Modifier.size(14.dp))
            })
          }
        }
      }
      favorite.favoriteBodySearchSnippet(query)?.let { snippet ->
        Text(
          highlightFavoriteMatch("正文：$snippet", query, highlightStyle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        highlightFavoriteMatch(
          "${favorite.messageCount} 条消息 · ${favorite.sourceConversationTitle} · ${favorite.sourceModel.orEmpty()}",
          query,
          highlightStyle
        ),
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
}

private fun highlightFavoriteMatch(text: String, query: String, style: SpanStyle): AnnotatedString {
  if (query.isBlank() || text.isBlank()) return AnnotatedString(text)
  return buildAnnotatedString {
    var index = 0
    while (index < text.length) {
      val matchIndex = text.indexOf(query, startIndex = index, ignoreCase = true)
      if (matchIndex < 0) {
        append(text.substring(index))
        break
      }
      append(text.substring(index, matchIndex))
      withStyle(style) {
        append(text.substring(matchIndex, matchIndex + query.length))
      }
      index = matchIndex + query.length
    }
  }
}

private fun FavoriteSnippet.favoriteBodySearchSnippet(query: String): String? {
  if (query.isBlank()) return null
  messages.forEach { message ->
    val content = message.content.replace(Regex("\\s+"), " ").trim()
    val index = content.indexOf(query, ignoreCase = true)
    if (index >= 0) {
      val start = (index - 36).coerceAtLeast(0)
      val end = (index + query.length + 72).coerceAtMost(content.length)
      return buildString {
        if (start > 0) append("...")
        append(content.substring(start, end))
        if (end < content.length) append("...")
      }
    }
  }
  return null
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
  onRemoveMessages: (Set<String>) -> Unit,
  onJumpToSource: () -> Unit
) {
  var deleteConfirmOpen by remember(favorite.id) { mutableStateOf(false) }
  var messageBatchMode by remember(favorite.id) { mutableStateOf(false) }
  var selectedMessageIds by remember(favorite.id) { mutableStateOf<Set<String>>(emptySet()) }
  var batchRemoveConfirmOpen by remember(favorite.id) { mutableStateOf(false) }
  val selectedMessageCount = selectedMessageIds.size
  val canBatchRemove = selectedMessageCount in 1 until favorite.messages.size
  LaunchedEffect(favorite.id, favorite.messages) {
    selectedMessageIds = selectedMessageIds.filterTo(mutableSetOf()) { id -> favorite.messages.any { it.id == id } }
    if (favorite.messages.size <= 1) {
      selectedMessageIds = emptySet()
      messageBatchMode = false
    }
  }
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
      batchMode = messageBatchMode,
      canBatchMessages = favorite.messages.size > 1,
      onToggleBatchMode = {
        messageBatchMode = !messageBatchMode
        if (!messageBatchMode) selectedMessageIds = emptySet()
      },
      onDelete = { deleteConfirmOpen = true }
    )
    FavoriteDetailMeta(favorite = favorite)
    if (messageBatchMode) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("已选择 $selectedMessageCount 条", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
          TextButton(
            onClick = {
              selectedMessageIds = if (selectedMessageIds.size == favorite.messages.size) {
                emptySet()
              } else {
                favorite.messages.map { it.id }.toSet()
              }
            }
          ) {
            Text(if (selectedMessageIds.size == favorite.messages.size) "取消全选" else "全选")
          }
          TextButton(
            onClick = { batchRemoveConfirmOpen = true },
            enabled = canBatchRemove
          ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (selectedMessageCount >= favorite.messages.size) "至少保留一条" else "移除")
          }
        }
      }
    }
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
          batchMode = messageBatchMode,
          selected = message.id in selectedMessageIds,
          onToggleSelected = {
            selectedMessageIds = if (message.id in selectedMessageIds) {
              selectedMessageIds - message.id
            } else {
              selectedMessageIds + message.id
            }
          },
          onRemove = { onRemoveMessages(setOf(message.id)) },
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
  if (batchRemoveConfirmOpen) {
    AlertDialog(
      onDismissRequest = { batchRemoveConfirmOpen = false },
      title = { Text("批量移除消息") },
      text = { Text("确定要从这个收藏片段中移除选中的 $selectedMessageCount 条消息吗？原对话内容不会被删除。") },
      confirmButton = {
        Button(onClick = {
          batchRemoveConfirmOpen = false
          onRemoveMessages(selectedMessageIds)
          selectedMessageIds = emptySet()
          messageBatchMode = false
        }) {
          Text("移除")
        }
      },
      dismissButton = {
        TextButton(onClick = { batchRemoveConfirmOpen = false }) {
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
  batchMode: Boolean,
  canBatchMessages: Boolean,
  onToggleBatchMode: () -> Unit,
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
    if (canBatchMessages) {
      item {
        CompactFavoriteAction(if (batchMode) "退出批量" else "批量消息", Icons.Outlined.CheckCircle, onToggleBatchMode)
      }
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
  batchMode: Boolean,
  selected: Boolean,
  onToggleSelected: () -> Unit,
  onRemove: () -> Unit,
  onOpenAttachment: (ChatAttachment) -> Unit
) {
  val isUser = message.role == MessageRole.USER
  val userColors = userBubbleColors()
  var removeConfirmOpen by remember(message.id) { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    verticalAlignment = Alignment.Top
  ) {
    if (batchMode) {
      Checkbox(
        checked = selected,
        onCheckedChange = { onToggleSelected() },
        modifier = Modifier.padding(top = 8.dp)
      )
    }
    Surface(
      color = if (isUser) userColors.container else MaterialTheme.colorScheme.surface,
      contentColor = if (isUser) userColors.content else MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.84f else 0.92f)
        .then(if (batchMode) Modifier.clickable(onClick = onToggleSelected) else Modifier)
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
          MarkdownPreview(message.content, interactiveLinks = !batchMode)
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
        if (!batchMode) {
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

@Composable
private fun contextCapacityColor(capacity: ContextCapacity?): Color {
  return when (capacity?.status) {
    ContextCapacityStatus.WARNING -> MaterialTheme.colorScheme.tertiary
    ContextCapacityStatus.CRITICAL -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
}

internal fun formatTokenCount(value: Int): String {
  return when {
    value >= 1_000_000 -> "${(value / 100_000) / 10.0}M"
    value >= 1_000 -> "${(value / 100) / 10.0}k"
    else -> value.toString()
  }
}

private enum class GroupBotPickerMode {
  SPEAK,
  SUMMARIZE
}

private const val GroupSummaryInitialMessageThreshold = 8
private const val GroupSummaryRefreshMessageThreshold = 12

internal data class GroupSummaryRefreshHint(
  val message: String,
  val actionLabel: String,
  val staleMessageCount: Int
)

internal fun groupSummaryRefreshHint(
  room: GroupChatRoom,
  messages: List<GroupChatMessage>,
  initialThreshold: Int = GroupSummaryInitialMessageThreshold,
  refreshThreshold: Int = GroupSummaryRefreshMessageThreshold
): GroupSummaryRefreshHint? {
  val completedDiscussionMessages = messages.filter { message ->
    message.status == MessageStatus.COMPLETE &&
      message.content.isNotBlank() &&
      message.turnTrigger != GroupTurnTrigger.SUMMARY &&
      (message.senderType == GroupMessageSenderType.USER || message.senderType == GroupMessageSenderType.BOT)
  }
  if (room.summary.isBlank()) {
    val count = completedDiscussionMessages.size
    return if (count >= initialThreshold) {
      GroupSummaryRefreshHint(
        message = "已有 $count 条讨论消息，可以生成群聊摘要。",
        actionLabel = "生成摘要",
        staleMessageCount = count
      )
    } else {
      null
    }
  }

  val latestSummaryCreatedAt = messages.asSequence()
    .filter { message ->
      message.status == MessageStatus.COMPLETE &&
        message.content.isNotBlank() &&
        message.turnTrigger == GroupTurnTrigger.SUMMARY
    }
    .maxOfOrNull { it.createdAt }
  val staleCount = if (latestSummaryCreatedAt == null) {
    completedDiscussionMessages.size
  } else {
    completedDiscussionMessages.count { it.createdAt > latestSummaryCreatedAt }
  }
  return if (staleCount >= refreshThreshold) {
    GroupSummaryRefreshHint(
      message = "上次摘要后已有 $staleCount 条新消息，建议更新摘要。",
      actionLabel = "更新摘要",
      staleMessageCount = staleCount
    )
  } else {
    null
  }
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
  onSaveAutoPlayPreference: (String, GroupAutoPlayPreference) -> Unit,
  onCompressContext: () -> Unit,
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
  onShareMarkdown: () -> Unit,
  onShareMessageText: (String) -> Unit,
  onShareMessageImage: (String) -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit,
  onCopyGroup: () -> Unit
) {
  var pickerMode by remember { mutableStateOf<GroupBotPickerMode?>(null) }
  var autoPlaySettingsOpen by remember { mutableStateOf(false) }
  val selectedGroup = state.selectedGroupChat
  val autoPlayPreference = state.appSettings.groupAutoPlayPreference(selectedGroup?.id)
  val memberBotIds = state.groupMembers.map { it.botId }.toSet()
  val groupBots = state.aiBots
    .filter { it.enabled && (memberBotIds.isEmpty() || it.id in memberBotIds) }
    .sortedWith(compareBy<AiBot> { bot -> state.groupMembers.firstOrNull { it.botId == bot.id }?.sortOrder ?: Int.MAX_VALUE }.thenBy { it.name })
  val summaryRefreshHint = remember(selectedGroup?.id, selectedGroup?.summary, state.groupMessages) {
    selectedGroup?.let { groupSummaryRefreshHint(it, state.groupMessages) }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onOpenDrawer) {
              Icon(Icons.Outlined.Menu, contentDescription = "打开聊天列表")
            }
            Text(
              text = selectedGroup?.title?.ifBlank { "AI 群聊" } ?: "AI 群聊",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
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
                onShareMarkdown = onShareMarkdown,
                onFavoriteSelected = onFavoriteSelected,
                onAppendSelectedToFavorite = onAppendSelectedToFavorite,
                onOpenAutoPlaySettings = { autoPlaySettingsOpen = true },
                onCompressContext = onCompressContext,
                contextCompressionEnabled = selectedGroup.id !in state.compressingGroupIds,
                onEditGroup = onEditGroup,
                onDeleteGroup = onDeleteGroup,
                onCopyGroup = onCopyGroup
              )
            }
            IconButton(onClick = onClose) {
              Icon(Icons.Outlined.Close, contentDescription = "关闭群聊")
            }
          }
          TopMetadataStrip(
            items = groupMetadataItems(state, selectedGroup, groupBots.size),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
          )
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
        if (selectedGroup.summary.isNotBlank() || summaryRefreshHint != null || groupBots.isNotEmpty()) {
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
            summaryRefreshHint?.let { hint ->
              GroupSummaryRefreshPrompt(
                hint = hint,
                enabled = groupBots.isNotEmpty() && !state.isSelectedGroupStreaming,
                onClick = { pickerMode = GroupBotPickerMode.SUMMARIZE }
              )
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
          streamingBubbleMotion = state.appSettings.streamingBubbleMotion,
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

  if (autoPlaySettingsOpen && selectedGroup != null) {
    GroupAutoPlayPreferenceDialog(
      preference = autoPlayPreference,
      onDismiss = { autoPlaySettingsOpen = false },
      onSave = { preference ->
        onSaveAutoPlayPreference(selectedGroup.id, preference)
        autoPlaySettingsOpen = false
      }
    )
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
private fun GroupSummaryRefreshPrompt(
  hint: GroupSummaryRefreshHint,
  enabled: Boolean,
  onClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Outlined.Refresh,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(18.dp)
      )
      Text(
        text = hint.message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.weight(1f)
      )
      TextButton(
        onClick = onClick,
        enabled = enabled
      ) {
        Text(hint.actionLabel)
      }
    }
  }
}

@Composable
private fun GroupAutoPlayPreferenceDialog(
  preference: GroupAutoPlayPreference,
  onDismiss: () -> Unit,
  onSave: (GroupAutoPlayPreference) -> Unit
) {
  var maxRounds by remember(preference) { mutableStateOf(preference.maxRounds.coerceIn(0, 12)) }
  var intervalSeconds by remember(preference) { mutableStateOf(preference.intervalSeconds.coerceIn(0, 30)) }
  var retryFailedTurn by remember(preference) { mutableStateOf(preference.retryFailedTurn) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("播放器设置") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GroupAutoPlaySlider(
          title = "自动轮数",
          value = maxRounds,
          valueText = if (maxRounds == 0) "不限" else "$maxRounds 轮",
          range = 0..12,
          onValueChange = { maxRounds = it }
        )
        GroupAutoPlaySlider(
          title = "发言间隔",
          value = intervalSeconds,
          valueText = if (intervalSeconds == 0) "无间隔" else "$intervalSeconds 秒",
          range = 0..30,
          onValueChange = { intervalSeconds = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            Text("失败后重试一次", fontWeight = FontWeight.SemiBold)
            Text(
              "单个机器人发言失败时，播放器会先重试同一机器人一次，再暂停。",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(checked = retryFailedTurn, onCheckedChange = { retryFailedTurn = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            GroupAutoPlayPreference(
              maxRounds = maxRounds,
              intervalSeconds = intervalSeconds,
              retryFailedTurn = retryFailedTurn
            )
          )
        }
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
private fun GroupAutoPlaySlider(
  title: String,
  value: Int,
  valueText: String,
  range: IntRange,
  onValueChange: (Int) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
      Text(valueText, fontWeight = FontWeight.SemiBold)
    }
    Slider(
      value = value.toFloat(),
      onValueChange = { next -> onValueChange(next.roundToInt().coerceIn(range.first, range.last)) },
      valueRange = range.first.toFloat()..range.last.toFloat(),
      steps = (range.last - range.first - 1).coerceAtLeast(0)
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
  onShareMarkdown: () -> Unit,
  onFavoriteSelected: () -> Unit,
  onAppendSelectedToFavorite: () -> Unit,
  onOpenAutoPlaySettings: () -> Unit,
  onCompressContext: () -> Unit,
  contextCompressionEnabled: Boolean,
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
        text = { Text("导出 Markdown 文件") },
        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareMarkdown()
        }
      )
      DropdownMenuItem(
        text = { Text("播放器设置") },
        leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
        onClick = {
          menuOpen = false
          onOpenAutoPlaySettings()
        }
      )
      DropdownMenuItem(
        text = { Text("立即压缩上下文") },
        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
        enabled = contextCompressionEnabled,
        onClick = {
          menuOpen = false
          onCompressContext()
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
  streamingBubbleMotion: StreamingBubbleMotion,
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
  var isListScrolling by remember(groupId) { mutableStateOf(false) }
  var showScrollToBottom by remember(groupId) { mutableStateOf(false) }
  var groupLongBubbleNavTarget by remember(groupId) { mutableStateOf<LongBubbleNavTarget?>(null) }
  var longBubbleActionsExpanded by remember(groupId) { mutableStateOf(false) }
  val latestBotMessageId = messages.lastOrNull { it.senderType == GroupMessageSenderType.BOT }?.id
  val botsById = remember(bots) { bots.associateBy { it.id } }
  val listItems = remember(messages) { groupMessageListItems(messages) }
  val bottomAnchorIndex = listItems.size
  var visibleRangeTargetId by remember(groupId) { mutableStateOf<String?>(null) }
  val groupLongBubbleCandidates = remember(listItems, expandedBotMessageIds, latestBotMessageId) {
    listItems.mapIndexedNotNull { index, item ->
      val message = (item as? GroupMessageListItem.Message)?.message ?: return@mapIndexedNotNull null
      val isExpandedBot = message.senderType == GroupMessageSenderType.BOT &&
        (message.status == MessageStatus.STREAMING || message.id == latestBotMessageId || message.id in expandedBotMessageIds)
      if (isExpandedBot) index to message.id else null
    }.toMap()
  }
  LaunchedEffect(listState, groupId) {
    snapshotFlow { listState.isScrollInProgress }
      .distinctUntilChanged()
      .collect { scrolling ->
        isListScrolling = scrolling
        if (scrolling) scrollHintVisible = true
        if (scrolling && !listState.isAtBottom()) autoFollow = false
        if (!scrolling && listState.isAtBottom()) autoFollow = true
      }
  }

  LaunchedEffect(listState, groupId, selectionMode, selectedMessageIds, listItems) {
    if (!selectionMode || selectedMessageIds.isEmpty()) {
      visibleRangeTargetId = null
      return@LaunchedEffect
    }
    snapshotFlow {
      listState.layoutInfo.visibleItemsInfo
        .mapNotNull { item -> listItems.getOrNull(item.index) }
        .flatMap { it.messageIds }
        .firstOrNull { it !in selectedMessageIds }
    }
      .distinctUntilChanged()
      .collect { visibleRangeTargetId = it }
  }

  LaunchedEffect(listState, groupId, listItems.size) {
    snapshotFlow { listState.shouldShowScrollToBottom() }
      .distinctUntilChanged()
      .collect { showScrollToBottom = it }
  }

  LaunchedEffect(listState, groupId, scrollHintVisible, groupLongBubbleCandidates) {
    if (!scrollHintVisible || groupLongBubbleCandidates.isEmpty()) {
      groupLongBubbleNavTarget = null
      return@LaunchedEffect
    }
    snapshotFlow {
      lazyListLongBubbleNavTarget(
        visibleItems = listState.layoutInfo.visibleItemsInfo,
        viewportStart = listState.layoutInfo.viewportStartOffset,
        viewportEnd = listState.layoutInfo.viewportEndOffset,
        candidates = groupLongBubbleCandidates
      )
    }
      .distinctUntilChanged()
      .collect { groupLongBubbleNavTarget = it }
  }

  LaunchedEffect(scrollHintVisible, isListScrolling, longBubbleActionsExpanded) {
    if (scrollHintVisible && !isListScrolling && !longBubbleActionsExpanded) {
      delay(2_500)
      if (!isListScrolling && !longBubbleActionsExpanded) scrollHintVisible = false
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
              onFavorite = { onFavoriteMessage(message.id) },
              streamingBubbleMotion = streamingBubbleMotion
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
              onFavorite = { onFavoriteMessages(toolIds) },
              streamingBubbleMotion = streamingBubbleMotion
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
      FloatingActionPill(
        label = "选择到此",
        icon = { Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        onClick = { onSelectRangeTo(rangeTargetId) },
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 18.dp, top = 8.dp)
      )
    }
    MessageScrollIndicator(
      listState = listState,
      visible = scrollHintVisible || isListScrolling,
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
      FloatingActionPill(
        label = "回到底部",
        icon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
      )
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

private fun lazyListLongBubbleNavTarget(
  visibleItems: List<LazyListItemInfo>,
  viewportStart: Int,
  viewportEnd: Int,
  candidates: Map<Int, String>
): LongBubbleNavTarget? = longBubbleNavTarget(
  visibleItems = visibleItems.mapNotNull { item ->
    val messageId = candidates[item.index] ?: return@mapNotNull null
    VisibleListItemBounds(
      index = item.index,
      offset = item.offset,
      size = item.size,
      messageId = messageId,
      supportsActions = true
    )
  },
  viewportStart = viewportStart,
  viewportEnd = viewportEnd,
  candidateIndexes = candidates.keys
)

@Composable
private fun FloatingActionPill(
  label: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(999.dp)
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = shape,
    tonalElevation = 1.dp,
    modifier = modifier
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
      .clickable(onClick = onClick)
      .defaultMinSize(minHeight = 34.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      icon()
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SelectRangeToInlineChip(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(999.dp)
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = shape,
    modifier = modifier
      .defaultMinSize(minHeight = 30.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
      Text("选择到此", style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
  }
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
  onFavorite: () -> Unit,
  streamingBubbleMotion: StreamingBubbleMotion
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
  val senderDisplayName = message.senderName.ifBlank { if (isUser) "用户" else "AI" }
  val botDisplayName = bot?.name?.takeIf { it.isNotBlank() } ?: senderDisplayName
  val botIdentityText = if (isBot) {
    "身份 ${botIdentityCode(bot?.id ?: message.botId ?: message.senderName)} · ${botColors.label}"
  } else {
    null
  }
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
  val bubbleShape = RoundedCornerShape(8.dp)
  val bubbleContainerColor = when {
    selected -> MaterialTheme.colorScheme.secondaryContainer
    isUser -> userColors.container
    message.status == MessageStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
    isBot -> botColors.container
    else -> MaterialTheme.colorScheme.surface
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    StreamingBubbleFrame(
      streaming = isBot && message.status == MessageStatus.STREAMING,
      motion = streamingBubbleMotion,
      accent = botColors.accent,
      containerColor = bubbleContainerColor,
      contentColor = contentColor,
      selected = selected,
      shape = bubbleShape,
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.84f else 0.92f),
      baseBorderColor = if (isBot) botColors.accent else null
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isBot) {
            BotIdentityAvatar(
              label = botAvatarLabel(botDisplayName),
              colors = botColors
            )
            Spacer(Modifier.width(9.dp))
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = senderDisplayName,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            botIdentityText?.let { identity ->
              Text(
                text = identity,
                style = MaterialTheme.typography.bodySmall,
                color = metadataColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
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
          if (message.content.isBlank() && message.status == MessageStatus.STREAMING) {
            StreamingStatusIndicator(
              text = "正在输出",
              accent = botColors.accent,
              textColor = metadataColor,
              motion = streamingBubbleMotion,
              animatedDots = true
            )
          } else {
            SelectionContainer {
              MarkdownPreview(
                content = message.content,
                colors = markdownColors,
                interactiveLinks = !selectionMode
              )
            }
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
        if (isBot && message.status == MessageStatus.STREAMING && message.content.isNotBlank()) {
          StreamingStatusIndicator(
            text = "输出中",
            accent = botColors.accent,
            textColor = metadataColor,
            motion = streamingBubbleMotion
          )
        }
        formatGroupMessageMetadata(message, includeStreaming = false)?.let { metadata ->
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
              SelectRangeToInlineChip(onClick = onSelectRangeTo)
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
  onFavorite: () -> Unit,
  streamingBubbleMotion: StreamingBubbleMotion
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
    onToggleExpanded = onToggleExpanded,
    streamingBubbleMotion = streamingBubbleMotion
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

private fun formatGroupMessageMetadata(message: GroupChatMessage, includeStreaming: Boolean = true): String? {
  val parts = mutableListOf<String>()
  if (includeStreaming && message.status == MessageStatus.STREAMING) parts += "输出中"
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
            val colors = botBubbleColors(bot)
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(bot.id) }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                BotIdentityAvatar(
                  label = botAvatarLabel(bot.name),
                  colors = colors
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(bot.name, fontWeight = FontWeight.SemiBold)
                  Text(
                    "身份 ${botIdentityCode(bot.id)} · ${colors.label} · ${bot.model}",
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
                BotIdentityAvatar(
                  label = botAvatarLabel(bot.name),
                  colors = colors
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(bot.name, fontWeight = FontWeight.SemiBold)
                  Text(
                    "身份 ${botIdentityCode(bot.id)} · $providerName · ${bot.model} · ${colors.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
  commonBackgroundPresetIds: List<String> = emptyList(),
  title: String = "新建 AI 群聊",
  confirmText: String = "创建",
  initialTitle: String = "",
  initialTopic: String = "",
  initialSelectedBotIds: Set<String> = emptySet(),
  onDismiss: () -> Unit,
  onManageBots: () -> Unit,
  onCreate: (String, String, List<String>) -> Unit,
  onSaveBackgroundPresetCombination: ((List<String>) -> Unit)? = null
) {
  var groupTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
  var topic by remember(initialTopic) { mutableStateOf(initialTopic) }
  var selectedBotIds by remember(initialSelectedBotIds) { mutableStateOf(initialSelectedBotIds) }
  var presetPickerOpen by remember { mutableStateOf(false) }
  val commonBackgroundPresets = remember(backgroundPresets, commonBackgroundPresetIds) {
    val byId = backgroundPresets.associateBy { it.id }
    commonBackgroundPresetIds.mapNotNull { byId[it] }
  }
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
        if (commonBackgroundPresets.isNotEmpty()) {
          TextButton(onClick = { topic = appendPresetTexts(topic, commonBackgroundPresets.map { it.content }) }) {
            Icon(Icons.Outlined.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("插入本群常用组合")
          }
        }
        Text("选择机器人", fontWeight = FontWeight.SemiBold)
        if (bots.isEmpty()) {
          Text("还没有启用的机器人。", color = MaterialTheme.colorScheme.onSurfaceVariant)
          TextButton(onClick = onManageBots) {
            Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("管理机器人")
          }
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
      initialSelectedPresetIds = commonBackgroundPresetIds,
      onDismiss = { presetPickerOpen = false },
      onSelect = { selectedPresets ->
        topic = appendPresetTexts(topic, selectedPresets.map { it.content })
        presetPickerOpen = false
      },
      onSaveCombination = onSaveBackgroundPresetCombination?.let { saveCombination ->
        { selectedPresets ->
          saveCombination(selectedPresets.map { it.id })
          presetPickerOpen = false
        }
      }
    )
  }
}

@Composable
private fun BackgroundPresetPickerDialog(
  presets: List<ChatBackgroundPreset>,
  initialSelectedPresetIds: List<String> = emptyList(),
  onDismiss: () -> Unit,
  onSelect: (List<ChatBackgroundPreset>) -> Unit,
  onSaveCombination: ((List<ChatBackgroundPreset>) -> Unit)? = null
) {
  var query by remember { mutableStateOf("") }
  var categoryFilter by remember { mutableStateOf<String?>(null) }
  var selectedPresetIds by remember(initialSelectedPresetIds, presets) {
    mutableStateOf<Set<String>>(initialSelectedPresetIds.filter { id -> presets.any { it.id == id } }.toSet())
  }
  val sortedPresets = remember(presets) { presets.sortedBy { it.sortOrder } }
  val categories = remember(sortedPresets) {
    sortedPresets.mapNotNull { it.cleanCategory() }.distinctBy { it.lowercase() }.sorted()
  }
  val filteredPresets = remember(sortedPresets, query, categoryFilter) {
    sortedPresets.filter { preset ->
      preset.matchesBackgroundPresetQuery(query) &&
        (categoryFilter == null || preset.cleanCategory().equals(categoryFilter, ignoreCase = true))
    }
  }
  val selectedPresets = remember(sortedPresets, selectedPresetIds) {
    sortedPresets.filter { it.id in selectedPresetIds }
  }
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
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("搜索背景预设") },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          trailingIcon = {
            if (query.isNotBlank()) {
              IconButton(onClick = { query = "" }) {
                Icon(Icons.Outlined.Close, contentDescription = "清空搜索")
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (categories.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = categoryFilter == null,
                onClick = { categoryFilter = null },
                label = { Text("全部") }
              )
            }
            items(categories, key = { it }) { category ->
              FilterChip(
                selected = categoryFilter.equals(category, ignoreCase = true),
                onClick = { categoryFilter = if (categoryFilter.equals(category, ignoreCase = true)) null else category },
                label = { Text(category) }
              )
            }
          }
        }
        if (filteredPresets.isEmpty()) {
          Text(
            "没有匹配的背景预设",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }
        filteredPresets.forEach { preset ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(listOf(preset)) }
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(preset.title, fontWeight = FontWeight.SemiBold)
                preset.cleanCategory()?.let { category ->
                  Text("#$category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                  preset.content,
                  maxLines = 4,
                  overflow = TextOverflow.Ellipsis,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Checkbox(
                checked = preset.id in selectedPresetIds,
                onCheckedChange = { checked ->
                  selectedPresetIds = if (checked) selectedPresetIds + preset.id else selectedPresetIds - preset.id
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        onSaveCombination?.let { saveCombination ->
          TextButton(onClick = { saveCombination(selectedPresets) }, enabled = selectedPresets.isNotEmpty()) {
            Text("设为常用")
          }
        }
        Button(onClick = { onSelect(selectedPresets) }, enabled = selectedPresets.isNotEmpty()) {
          Text("插入选中")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    }
  )
}

private fun appendPresetTexts(current: String, presetContents: List<String>): String {
  val clean = presetContents.map { it.trim() }.filter { it.isNotBlank() }
  if (clean.isEmpty()) return current
  val appended = clean.joinToString("\n\n")
  return if (current.isBlank()) appended else "${current.trimEnd()}\n\n$appended"
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
  onCleanupHistoricalDsmlToolMarkup: () -> Unit,
  onWebSearchMode: (WebSearchMode) -> Unit,
  onStreamingBubbleMotion: (StreamingBubbleMotion) -> Unit,
  onAttachmentMaxFileMb: (Int) -> Unit,
  onAttachmentMaxPendingMb: (Int) -> Unit,
  onAttachmentMaxImageSourceMb: (Int) -> Unit,
  onExportProviderConfigs: () -> Unit,
  onExportProviderConfigsQr: ((String) -> Unit) -> Unit,
  onImportProviderConfigs: (String) -> Unit,
  onExportBackgroundPresets: () -> Unit,
  onImportBackgroundPresets: (String) -> Unit,
  onOpenBotManager: () -> Unit,
  onSaveBackgroundPreset: (ChatBackgroundPreset?, String, String, String) -> Unit,
  onDeleteBackgroundPreset: (String) -> Unit,
  onMoveBackgroundPreset: (String, Int) -> Unit
) {
  val context = LocalContext.current
  var importDialogOpen by remember { mutableStateOf(false) }
  var providerConfigQrText by remember { mutableStateOf<String?>(null) }
  var backgroundImportDialogOpen by remember { mutableStateOf(false) }
  var editingPreset by remember { mutableStateOf<ChatBackgroundPreset?>(null) }
  var creatingPreset by remember { mutableStateOf(false) }
  var backgroundPresetQuery by remember { mutableStateOf("") }
  var backgroundPresetCategoryFilter by remember { mutableStateOf<String?>(null) }
  val sortedBackgroundPresets = remember(state.appSettings.backgroundPresets) {
    state.appSettings.backgroundPresets.sortedBy { it.sortOrder }
  }
  val backgroundPresetCategories = remember(sortedBackgroundPresets) {
    sortedBackgroundPresets.mapNotNull { it.cleanCategory() }.distinctBy { it.lowercase() }.sorted()
  }
  val filteredBackgroundPresets = remember(sortedBackgroundPresets, backgroundPresetQuery, backgroundPresetCategoryFilter) {
    sortedBackgroundPresets.filter { preset ->
      preset.matchesBackgroundPresetQuery(backgroundPresetQuery) &&
        (backgroundPresetCategoryFilter == null || preset.cleanCategory().equals(backgroundPresetCategoryFilter, ignoreCase = true))
    }
  }
  val providerQrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
    val contents = result.contents
    if (contents.isNullOrBlank()) {
      Toast.makeText(context, "未读取到二维码内容", Toast.LENGTH_SHORT).show()
    } else {
      onImportProviderConfigs(contents)
    }
  }
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
          Text("输出中气泡动效", fontWeight = FontWeight.SemiBold)
          Text(
            "用于提示 AI 气泡仍在流式输出，卡顿时也能看出尚未完成。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = StreamingBubbleMotion.values().toList(), key = { motion: StreamingBubbleMotion -> motion.name }) { motion: StreamingBubbleMotion ->
              FilterChip(
                selected = state.appSettings.streamingBubbleMotion == motion,
                onClick = { onStreamingBubbleMotion(motion) },
                label = { Text(motion.label) }
              )
            }
          }
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
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("历史工具标记清理", fontWeight = FontWeight.SemiBold)
              Text(
                "清理旧版本可能写入正文的 DSML 工具调用标记，保留可读的工具调用摘要。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            OutlinedButton(onClick = onCleanupHistoricalDsmlToolMarkup) {
              Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("清理")
            }
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
              Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("导出配置文本")
            }
            TextButton(onClick = { importDialogOpen = true }) {
              Icon(Icons.Outlined.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("导入配置文本")
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onExportProviderConfigsQr { providerConfigQrText = it } }) {
              Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("生成二维码")
            }
            TextButton(
              onClick = {
                val options = ScanOptions().apply {
                  setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                  setPrompt("扫描 API 配置二维码")
                  setBeepEnabled(false)
                  setOrientationLocked(false)
                }
                providerQrScanLauncher.launch(options)
              }
            ) {
              Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("扫码导入")
            }
          }
          Text(
            "导出的文本和二维码都包含 API Key，请只通过可信渠道保存或分享。",
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
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
              onClick = onExportBackgroundPresets,
              enabled = state.appSettings.backgroundPresets.isNotEmpty()
            ) {
              Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("导出 JSON")
            }
            TextButton(onClick = { backgroundImportDialogOpen = true }) {
              Icon(Icons.Outlined.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("导入 JSON")
            }
          }
          OutlinedTextField(
            value = backgroundPresetQuery,
            onValueChange = { backgroundPresetQuery = it },
            label = { Text("搜索背景预设") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
              if (backgroundPresetQuery.isNotBlank()) {
                IconButton(onClick = { backgroundPresetQuery = "" }) {
                  Icon(Icons.Outlined.Close, contentDescription = "清空搜索")
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          if (backgroundPresetCategories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                FilterChip(
                  selected = backgroundPresetCategoryFilter == null,
                  onClick = { backgroundPresetCategoryFilter = null },
                  label = { Text("全部") }
                )
              }
              items(backgroundPresetCategories, key = { it }) { category ->
                FilterChip(
                  selected = backgroundPresetCategoryFilter.equals(category, ignoreCase = true),
                  onClick = {
                    backgroundPresetCategoryFilter =
                      if (backgroundPresetCategoryFilter.equals(category, ignoreCase = true)) null else category
                  },
                  label = { Text(category) }
                )
              }
            }
          }
          if (filteredBackgroundPresets.isEmpty()) {
            Text(
              "没有匹配的背景预设",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
          filteredBackgroundPresets.forEach { preset ->
            val sourceIndex = sortedBackgroundPresets.indexOfFirst { it.id == preset.id }
            BackgroundPresetSettingsRow(
              preset = preset,
              canMoveUp = sourceIndex > 0,
              canMoveDown = sourceIndex >= 0 && sourceIndex < sortedBackgroundPresets.lastIndex,
              onEdit = { editingPreset = preset },
              onDelete = { onDeleteBackgroundPreset(preset.id) },
              onMoveUp = { onMoveBackgroundPreset(preset.id, -1) },
              onMoveDown = { onMoveBackgroundPreset(preset.id, 1) }
            )
          }
        }

        AppChangelogSection()
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
      onSave = { title, content, category ->
        onSaveBackgroundPreset(editingPreset, title, content, category)
        creatingPreset = false
        editingPreset = null
      }
    )
  }
  if (backgroundImportDialogOpen) {
    BackgroundPresetImportDialog(
      onDismiss = { backgroundImportDialogOpen = false },
      onImport = { text ->
        onImportBackgroundPresets(text)
        backgroundImportDialogOpen = false
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
  providerConfigQrText?.let { qrText ->
    ProviderConfigQrDialog(
      qrText = qrText,
      onDismiss = { providerConfigQrText = null }
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
  var isListScrolling by remember(state.selectedConversationId) { mutableStateOf(false) }
  var showScrollToBottom by remember(state.selectedConversationId) { mutableStateOf(false) }
  var messageLongBubbleNavTarget by remember(state.selectedConversationId) { mutableStateOf<LongBubbleNavTarget?>(null) }
  var longBubbleActionsExpanded by remember(state.selectedConversationId) { mutableStateOf(false) }
  var visibleRangeTargetId by remember(state.selectedConversationId) { mutableStateOf<String?>(null) }
  val messageLongBubbleCandidates = remember(listItems) {
    listItems.mapIndexedNotNull { index, item ->
      val message = (item as? ChatMessageListItem.Message)?.message ?: return@mapIndexedNotNull null
      if (message.role == MessageRole.ASSISTANT) index to message.id else null
    }.toMap()
  }
  LaunchedEffect(listState, state.selectedConversationId) {
    snapshotFlow { listState.isScrollInProgress }
      .distinctUntilChanged()
      .collect { scrolling ->
        isListScrolling = scrolling
        if (scrolling) scrollHintVisible = true
        if (scrolling && !listState.isAtBottom()) autoFollow = false
        if (!scrolling && listState.isAtBottom()) autoFollow = true
      }
  }
  LaunchedEffect(listState, state.selectedConversationId, selectionMode, selectedMessageIds, listItems) {
    if (!selectionMode || selectedMessageIds.isEmpty()) {
      visibleRangeTargetId = null
      return@LaunchedEffect
    }
    snapshotFlow {
      listState.layoutInfo.visibleItemsInfo
        .mapNotNull { item -> listItems.getOrNull(item.index) }
        .flatMap { it.messageIds }
        .firstOrNull { it !in selectedMessageIds }
    }
      .distinctUntilChanged()
      .collect { visibleRangeTargetId = it }
  }
  LaunchedEffect(listState, state.selectedConversationId, listItems.size) {
    snapshotFlow { listState.shouldShowScrollToBottom() }
      .distinctUntilChanged()
      .collect { showScrollToBottom = it }
  }
  LaunchedEffect(listState, state.selectedConversationId, scrollHintVisible, messageLongBubbleCandidates) {
    if (!scrollHintVisible || messageLongBubbleCandidates.isEmpty()) {
      messageLongBubbleNavTarget = null
      return@LaunchedEffect
    }
    snapshotFlow {
      lazyListLongBubbleNavTarget(
        visibleItems = listState.layoutInfo.visibleItemsInfo,
        viewportStart = listState.layoutInfo.viewportStartOffset,
        viewportEnd = listState.layoutInfo.viewportEndOffset,
        candidates = messageLongBubbleCandidates
      )
    }
      .distinctUntilChanged()
      .collect { messageLongBubbleNavTarget = it }
  }
  LaunchedEffect(scrollHintVisible, isListScrolling, longBubbleActionsExpanded) {
    if (scrollHintVisible && !isListScrolling && !longBubbleActionsExpanded) {
      delay(2_500)
      if (!isListScrolling && !longBubbleActionsExpanded) scrollHintVisible = false
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
              onFavorite = { onFavoriteMessages(toolIds) },
              streamingBubbleMotion = state.appSettings.streamingBubbleMotion
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
              imageMode = imageMode,
              streamingBubbleMotion = state.appSettings.streamingBubbleMotion
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
      FloatingActionPill(
        label = "选择到此",
        icon = { Icon(Icons.Outlined.KeyboardDoubleArrowDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        onClick = { onSelectRangeTo(rangeTargetId) },
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 18.dp, top = 8.dp)
      )
    }
    MessageScrollIndicator(
      listState = listState,
      visible = scrollHintVisible || isListScrolling,
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
      FloatingActionPill(
        label = "回到底部",
        icon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp)) },
        onClick = {
          scope.launch {
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.animateScrollToItem(last)
          }
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp)
      )
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
  imageMode: Boolean = false,
  streamingBubbleMotion: StreamingBubbleMotion
) {
  val isUser = message.role == MessageRole.USER
  val userColors = userBubbleColors()
  var shareMenuOpen by remember { mutableStateOf(false) }
  val isStreamingAssistant = !isUser && message.status == MessageStatus.STREAMING
  val bubbleShape = RoundedCornerShape(8.dp)
  val bubbleContainerColor = when {
    selected -> MaterialTheme.colorScheme.secondaryContainer
    isUser -> userColors.container
    else -> MaterialTheme.colorScheme.surface
  }
  val assistantAccent = MaterialTheme.colorScheme.primary
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (selectionMode) Modifier.clickable(onClick = onToggleSelected) else Modifier),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    StreamingBubbleFrame(
      streaming = isStreamingAssistant,
      motion = streamingBubbleMotion,
      accent = assistantAccent,
      containerColor = bubbleContainerColor,
      contentColor = if (isUser) userColors.content else MaterialTheme.colorScheme.onSurface,
      selected = selected,
      shape = bubbleShape,
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.84f else 0.92f)
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
        } else if (message.content.isBlank() && message.status == MessageStatus.STREAMING) {
          StreamingStatusIndicator(
            text = "正在输出",
            accent = assistantAccent,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            motion = streamingBubbleMotion,
            animatedDots = true
          )
        } else {
          SelectionContainer {
            MarkdownPreview(message.content, interactiveLinks = !selectionMode)
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
        if (isStreamingAssistant && message.content.isNotBlank()) {
          Spacer(Modifier.height(8.dp))
          StreamingStatusIndicator(
            text = "输出中",
            accent = assistantAccent,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            motion = streamingBubbleMotion
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
              SelectRangeToInlineChip(onClick = onSelectRangeTo)
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

internal fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("AI Chat message", text))
}

private fun createCameraCaptureUri(context: Context): Uri {
  val dir = File(context.cacheDir, "captured_images").apply { mkdirs() }
  val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun formatMessageTime(timestamp: Long): String {
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
  }.format(Date(timestamp))
}
