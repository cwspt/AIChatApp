package com.personal.aichat.data

import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus

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
  val createdAt: Long
)
