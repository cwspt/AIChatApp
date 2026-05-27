package com.personal.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatConversationGroup
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AIChatAppRoot(viewModel: ChatViewModel) {
  val state by viewModel.uiState.collectAsState()
  val editingProvider = state.editingProvider
  val context = LocalContext.current
  var drawerOpen by remember { mutableStateOf(false) }

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
        onNewChat = viewModel::newConversation,
        onOpenProviderManager = viewModel::openProviderManager,
        onSelectProvider = viewModel::selectProvider
      )
      ChatActionBar(
        state = state,
        onTogglePin = viewModel::togglePinConversation,
        onArchive = viewModel::archiveConversation,
        onDelete = viewModel::deleteConversation,
        onRename = viewModel::updateConversationMeta,
        onShareText = { viewModel.shareConversationText(state.selectedConversationId.orEmpty(), context) },
        onShareImage = { viewModel.shareConversationLongImage(context) },
        onShareMarkdown = { viewModel.shareConversationMarkdownFile(context) }
      )
      MessageList(
        state = state,
        messages = state.messages,
        selectedMessageIds = state.selectedMessageIds,
        selectionMode = state.messageSelectionMode,
        onToggleSelectionMode = viewModel::toggleMessageSelectionMode,
        onToggleMessageSelected = viewModel::toggleMessageSelected,
        onSelectRangeTo = viewModel::selectMessageRangeTo,
        onEditResend = viewModel::editAndResend,
        onShareConversation = { viewModel.shareConversationText(state.selectedConversationId.orEmpty(), context) },
        onShareSelected = { viewModel.shareSelectedMessagesText(context) },
        onShareMarkdown = { viewModel.shareConversationMarkdownFile(context) },
        onShareLongImage = { viewModel.shareConversationLongImage(context) },
        onShareSelectedLongImage = { viewModel.shareSelectedMessagesLongImage(context) },
        onShareMessageText = { viewModel.shareMessageText(it, context) },
        onShareMessageImage = { viewModel.shareMessageImage(it, context) },
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
      Composer(
        input = state.input,
        onInput = viewModel::updateInput,
        onSend = viewModel::send,
        onRetry = viewModel::retryLast
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
        onCreateProvider = viewModel::createProvider
      )
    }

    if (drawerOpen) {
      ConversationDrawer(
        state = state,
        onDismiss = { drawerOpen = false },
        onSelectConversation = {
          viewModel.selectConversation(it)
          drawerOpen = false
        },
        onTogglePin = viewModel::togglePinConversation,
        onArchive = viewModel::archiveConversation,
        onRestore = viewModel::restoreConversation,
        onDelete = viewModel::deleteConversation,
        onRename = viewModel::updateConversationMeta
      )
    }

    if (state.deleteConfirmOpen) {
      AlertDialog(
        onDismissRequest = viewModel::cancelDeleteConversation,
        title = { Text("删除对话") },
        text = { Text("确定要删除这条对话吗？删除后会从列表中隐藏，聊天内容不会再出现在当前列表。") },
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
  }
}

@Composable
private fun ChatActionBar(
  state: ChatUiState,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit,
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = selectedConversation?.let { "${it.groupName.ifBlank { "默认" }} / ${it.model}" } ?: "从左上角聊天列表选择",
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
        onShareText = onShareText,
        onShareImage = onShareImage,
        onShareMarkdown = onShareMarkdown
      )
    }
  }
}

@Composable
private fun ConversationShareMenu(
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onShareMarkdown: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { menuOpen = true }) {
      Icon(Icons.Outlined.Share, contentDescription = "分享完整对话")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text("分享全文文本") },
        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
        onClick = {
          menuOpen = false
          onShareText()
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
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onRestore: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.28f))
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
          val pinnedConversations = state.conversations.filter { it.isPinned }
          val normalConversations = state.conversations.filterNot { it.isPinned }
          val pinnedFolders = state.conversationGroups.filter { group -> group.conversations.any { it.isPinned } }
          val normalFolders = state.conversationGroups.filter { group -> group.conversations.none { it.isPinned } }
          drawerSection("置顶聊天", pinnedConversations) { conversation ->
            ConversationDrawerRow(conversation, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          }
          drawerFolderSection("置顶文件夹", pinnedFolders, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          drawerSection("普通聊天", normalConversations) { conversation ->
            ConversationDrawerRow(conversation, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          }
          drawerFolderSection("普通文件夹", normalFolders, state.selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
          drawerArchivedSection("已归档", state.archivedConversations, onRestore, onDelete)
        }
      }
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
  selectedConversationId: String?,
  onSelectConversation: (String) -> Unit,
  onTogglePin: (String, Boolean) -> Unit,
  onArchive: (String) -> Unit,
  onDelete: (String) -> Unit,
  onRename: (String, String, String) -> Unit
) {
  if (groups.isEmpty()) return
  item(key = "section-$title") {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
  }
  groups.forEach { group ->
    item(key = "folder-${title}-${group.name}") {
      Text("▾ ${group.name}（${group.conversations.size}）", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(group.conversations, key = { "$title-${group.name}-${it.id}" }) { conversation ->
      ConversationDrawerRow(conversation, selectedConversationId, onSelectConversation, onTogglePin, onArchive, onDelete, onRename)
    }
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
          Text(conversation.groupName.ifBlank { "默认" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
          Text(if (conversation.isPinned) "置顶 · ${conversation.title}" else conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(conversation.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
  onOpenProviderManager: () -> Unit,
  onSelectProvider: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(onClick = onOpenConversationDrawer) {
      Icon(Icons.Outlined.Menu, contentDescription = "打开聊天列表")
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "AI 聊天",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = state.selectedProvider?.let { "${it.displayName} / ${it.defaultModel}" } ?: "未选择配置",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    ProviderMenu(
      state = state,
      onSelectProvider = onSelectProvider,
      onOpenProviderManager = onOpenProviderManager
    )
    Spacer(Modifier.width(4.dp))
    IconButton(onClick = onNewChat) {
      Icon(Icons.Outlined.Add, contentDescription = "新建对话")
    }
    IconButton(onClick = onOpenProviderManager) {
      Icon(Icons.Outlined.Settings, contentDescription = "API 配置")
    }
  }
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
              "${it.groupName.ifBlank { "默认" }} / ${it.title}"
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
  var targetGroup by remember(conversationTitle) { mutableStateOf(initialGroupName.ifBlank { "默认" }) }
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
          placeholder = { Text("输入新文件夹名，或填已有文件夹名") },
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          "当前版本的文件夹就是对话分组。输入同一个文件夹名称，就会把多个对话归到同一组。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(onClick = { onMove(targetGroup.trim().ifBlank { "默认" }) }) {
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
              text = "${if (expandedGroups[group.name] == false) "▸" else "▾"} ${group.name}（${group.conversations.size}）",
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
  selectedMessageIds: Set<String>,
  selectionMode: Boolean,
  onToggleSelectionMode: (Boolean) -> Unit,
  onToggleMessageSelected: (String) -> Unit,
  onSelectRangeTo: (String) -> Unit,
  onEditResend: (String) -> Unit,
  onShareConversation: () -> Unit,
  onShareSelected: () -> Unit,
  onShareMarkdown: () -> Unit,
  onShareLongImage: () -> Unit,
  onShareSelectedLongImage: () -> Unit,
  onShareMessageText: (String) -> Unit,
  onShareMessageImage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val showScrollToBottom by remember(messages.size) {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      val lastItem = listState.layoutInfo.totalItemsCount - 1
      lastItem > 0 && lastVisible < lastItem - 1
    }
  }
  Box(modifier = modifier) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (messages.isEmpty()) {
        item {
          EmptyState()
        }
      } else {
        item {
          ShareToolbar(
            conversationTitle = state.selectedConversation?.title ?: "当前对话",
            selectionMode = selectionMode,
            selectedCount = selectedMessageIds.size,
            onToggleSelectionMode = onToggleSelectionMode,
            onShareConversation = onShareConversation,
            onShareSelected = onShareSelected,
            onShareMarkdown = onShareMarkdown,
            onShareLongImage = onShareLongImage,
            onShareSelectedLongImage = onShareSelectedLongImage
          )
        }
      }
      items(messages, key = { it.id }) { message ->
        MessageBubble(
          message = message,
          selected = message.id in selectedMessageIds,
          selectionMode = selectionMode,
          canSelectRangeTo = selectionMode && selectedMessageIds.isNotEmpty() && message.id !in selectedMessageIds,
          onToggleSelected = { onToggleMessageSelected(message.id) },
          onSelectRangeTo = { onSelectRangeTo(message.id) },
          onCopy = { copyToClipboard(context, message.content) },
          onShareText = { onShareMessageText(message.id) },
          onShareImage = { onShareMessageImage(message.id) },
          onEditResend = { onEditResend(message.content) }
        )
      }
    }
    MessageScrollIndicator(
      progress = listState.scrollProgress(),
      visible = listState.layoutInfo.totalItemsCount > 8,
      onDragProgress = { progress ->
        scope.launch {
          val total = listState.layoutInfo.totalItemsCount
          if (total > 0) {
            listState.scrollToItem(((total - 1) * progress).roundToInt().coerceIn(0, total - 1))
          }
        }
      },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 4.dp)
    )
    if (showScrollToBottom) {
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
  }
}

@Composable
private fun MessageScrollIndicator(
  progress: Float,
  visible: Boolean,
  onDragProgress: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return
  BoxWithConstraints(
    modifier = modifier
      .width(5.dp)
      .fillMaxHeight(0.82f)
      .background(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        shape = RoundedCornerShape(999.dp)
      )
      .padding(vertical = 2.dp)
      .pointerInput(Unit) {
        detectDragGestures { change, _ ->
          val y = change.position.y.coerceIn(0f, size.height.toFloat())
          onDragProgress((y / size.height.toFloat()).coerceIn(0f, 1f))
        }
      }
  ) {
    val thumbHeight = 38.dp
    val travel = (maxHeight - thumbHeight).coerceAtLeast(0.dp)
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = travel * progress.coerceIn(0f, 1f))
        .width(3.dp)
        .height(thumbHeight)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
    )
  }
}

private fun androidx.compose.foundation.lazy.LazyListState.scrollProgress(): Float {
  val total = layoutInfo.totalItemsCount
  if (total <= 1) return 0f
  val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisibleItemIndex
  if (lastVisible >= total - 1) return 1f
  return (firstVisibleItemIndex.toFloat() / (total - 1)).coerceIn(0f, 1f)
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
  onShareSelectedLongImage: () -> Unit
) {
  var menuOpen by remember { mutableStateOf(false) }
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(conversationTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          text = if (selectionMode) "已选择 $selectedCount 条消息" else "可分享全文、Markdown 文件或长图",
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
  onShareText: () -> Unit,
  onShareImage: () -> Unit,
  onEditResend: () -> Unit
) {
  val isUser = message.role == MessageRole.USER
  var shareMenuOpen by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
      contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.fillMaxWidth(if (isUser) 0.84f else 0.92f)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = formatMessageTime(message.createdAt),
          style = MaterialTheme.typography.bodySmall,
          color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        if (isUser) {
          Text(message.content.ifBlank { if (message.status == MessageStatus.STREAMING) "..." else "" })
        } else {
          SelectionContainer {
            MarkdownPreview(message.content.ifBlank { if (message.status == MessageStatus.STREAMING) "..." else "" })
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
        Spacer(Modifier.height(8.dp))
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
          if (!isUser) {
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

@Composable
private fun MarkdownPreview(content: String) {
  val blocks = remember(content) { parseMarkdownBlocks(content) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    blocks.forEach { block ->
      when (block) {
        is MarkdownBlock.Code -> Surface(
          color = MaterialTheme.colorScheme.background,
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
          fontWeight = FontWeight.SemiBold
        )
        is MarkdownBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(block.marker, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(renderInlineMarkdown(block.text), modifier = Modifier.weight(1f))
        }
        is MarkdownBlock.Table -> MarkdownTable(block)
        is MarkdownBlock.Paragraph -> Text(renderInlineMarkdown(block.text))
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
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table) {
  if (table.rows.isEmpty()) return
  val columnCount = table.rows.maxOf { it.size }.coerceAtLeast(1)
  Surface(
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .padding(8.dp)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
    ) {
      table.rows.forEachIndexed { rowIndex, row ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              color = if (rowIndex == 0) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent,
              shape = RoundedCornerShape(4.dp)
            )
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            .padding(vertical = 0.dp)
        ) {
          repeat(columnCount) { column ->
            Box(
              modifier = Modifier
                .weight(1f)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
              Text(
                text = renderInlineMarkdown(row.getOrNull(column).orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal
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

    if (isMarkdownTableSeparator(trimmed)) {
      flushParagraph()
      return@forEach
    }

    if (looksLikeMarkdownTableRow(trimmed)) {
      flushParagraph()
      tableRows += parseMarkdownTableRow(trimmed)
      return@forEach
    } else {
      flushTable()
    }

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

private fun formatMessageTime(timestamp: Long): String {
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
  }.format(Date(timestamp))
}

@Composable
private fun Composer(
  input: String,
  onInput: (String) -> Unit,
  onSend: () -> Unit,
  onRetry: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .imePadding()
      .navigationBarsPadding()
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedTextField(
      value = input,
      onValueChange = onInput,
      modifier = Modifier.weight(1f),
      minLines = 1,
      maxLines = 5,
      placeholder = { Text("给当前模型发送消息") }
    )
    Spacer(Modifier.width(8.dp))
    IconButton(onClick = onRetry) {
      Icon(Icons.Outlined.Refresh, contentDescription = "重试上一条")
    }
    IconButton(onClick = onSend, enabled = input.isNotBlank()) {
      Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "发送")
    }
  }
}

@Composable
private fun ProviderManagerDialog(
  state: ChatUiState,
  onDismiss: () -> Unit,
  onSelectProvider: (String) -> Unit,
  onEditProvider: (ChatProviderConfig?) -> Unit,
  onCloneProvider: (String) -> Unit,
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
          text = "可保存多组 GPT、DeepSeek 或代理配置，并随时切换。",
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
              onClone = { onCloneProvider(provider.id) }
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
  onClone: () -> Unit
) {
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
    }
  }
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
              text = "当前配置已有已加密保存的 Key。出于安全考虑不会回显明文；如需更换，直接输入新的 Key 后保存。",
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

private val com.personal.aichat.domain.ProviderType.label: String
  get() = when (this) {
    com.personal.aichat.domain.ProviderType.OPENAI_RESPONSES -> "GPT / OpenAI Responses"
    com.personal.aichat.domain.ProviderType.OPENAI_COMPATIBLE_CHAT -> "DeepSeek / OpenAI-compatible Chat Completions"
    com.personal.aichat.domain.ProviderType.TOKENHUB_PROXY -> "TokenHub 代理 / Responses 兼容"
    com.personal.aichat.domain.ProviderType.ANTHROPIC_MESSAGES -> "Anthropic Messages"
    com.personal.aichat.domain.ProviderType.GEMINI_GENERATE_CONTENT -> "Gemini GenerateContent"
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
      "用于 DeepSeek 以及其它 OpenAI-compatible Chat Completions 服务。DeepSeek 示例：https://api.deepseek.com"
    com.personal.aichat.domain.ProviderType.TOKENHUB_PROXY ->
      "用于你本机或局域网中暴露 Responses-compatible /v1 入口的代理。"
    com.personal.aichat.domain.ProviderType.ANTHROPIC_MESSAGES ->
      "Reserved for a future Anthropic Messages adapter."
    com.personal.aichat.domain.ProviderType.GEMINI_GENERATE_CONTENT ->
      "Reserved for a future Gemini GenerateContent adapter."
  }
