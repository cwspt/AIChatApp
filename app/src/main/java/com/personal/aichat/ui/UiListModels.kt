package com.personal.aichat.ui

import com.personal.aichat.domain.ChatConversation
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.GroupChatMessage
import com.personal.aichat.domain.GroupMessageSenderType
import com.personal.aichat.domain.GroupTurnTrigger
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import kotlin.math.abs

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

private data class GroupTurnKey(
  val groupId: String,
  val botId: String?,
  val trigger: GroupTurnTrigger,
  val round: Int?,
  val index: Int?,
  val memberCount: Int?
)

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
      val distanceToCenter = abs((itemTop + item.size / 2) - viewportCenter)
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

internal fun conversationForkSourceLabel(
  conversation: ChatConversation,
  sourceConversations: List<ChatConversation>
): String? {
  val sourceId = conversation.forkedFromConversationId ?: return null
  val source = sourceConversations.firstOrNull { it.id == sourceId }
  return if (source == null) {
    "分叉来源不可用"
  } else {
    "分叉自 ${source.title.ifBlank { "未命名对话" }}"
  }
}

private fun GroupChatMessage.groupTurnKey(): GroupTurnKey = GroupTurnKey(
  groupId = groupId,
  botId = botId,
  trigger = turnTrigger,
  round = turnRound,
  index = turnIndex,
  memberCount = turnMemberCount
)
