package com.personal.aichat.data

import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ChatAttachment

data class ConversationExport(
  val title: String,
  val groupName: String?,
  val modelLabel: String?,
  val messages: List<ConversationExportMessage>
)

data class ConversationExportMessage(
  val id: String,
  val role: MessageRole,
  val content: String,
  val status: MessageStatus,
  val errorMessage: String?,
  val createdAt: Long,
  val attachments: List<ChatAttachment> = emptyList()
)

fun ConversationExport.withoutToolMessages(): ConversationExport = copy(
  messages = messages.filterNot { it.role == MessageRole.TOOL }
)
