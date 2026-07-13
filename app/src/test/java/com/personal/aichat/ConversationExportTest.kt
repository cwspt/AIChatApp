package com.personal.aichat

import com.personal.aichat.data.ConversationExport
import com.personal.aichat.data.ConversationExportMessage
import com.personal.aichat.data.withoutToolMessages
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationExportTest {
  @Test
  fun imageShareExportExcludesToolCallRecords() {
    val export = ConversationExport(
      title = "搜索结果",
      groupName = null,
      modelLabel = "GPT",
      messages = listOf(
        message("user", MessageRole.USER, "请搜索价格"),
        message("search", MessageRole.TOOL, "web_search: API pricing"),
        message("assistant", MessageRole.ASSISTANT, "官方价格如下")
      )
    )

    val shareable = export.withoutToolMessages()

    assertEquals(listOf("user", "assistant"), shareable.messages.map { it.id })
  }

  private fun message(id: String, role: MessageRole, content: String) = ConversationExportMessage(
    id = id,
    role = role,
    content = content,
    status = MessageStatus.COMPLETE,
    errorMessage = null,
    createdAt = 0L
  )
}
