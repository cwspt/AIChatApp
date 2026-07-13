package com.personal.aichat

import com.personal.aichat.data.ConversationExport
import com.personal.aichat.data.ConversationExportMessage
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.ui.ConversationShareRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationShareRendererTest {
  @Test
  fun imageExportPagesKeepEveryLongMessageAcrossSeparatePages() {
    val messages = List(3) { index ->
      ConversationExportMessage(
        id = "message-$index",
        role = MessageRole.ASSISTANT,
        content = "message-$index " + "content ".repeat(250),
        status = MessageStatus.COMPLETE,
        errorMessage = null,
        createdAt = index.toLong()
      )
    }

    val pages = ConversationShareRenderer.imageExportPages(
      ConversationExport("Long chat", null, null, messages)
    )

    assertEquals(3, pages.size)
    assertEquals(messages.map { it.content }, pages.flatMap { it.messages }.map { it.content })
    assertTrue(pages.map { it.title }.containsAll(listOf("Long chat (1/3)", "Long chat (2/3)", "Long chat (3/3)")))
  }

  @Test
  fun imageExportPagesSplitOneOversizedMessageWithoutDroppingText() {
    val content = "x".repeat(8_000)
    val message = ConversationExportMessage(
      id = "large-message",
      role = MessageRole.ASSISTANT,
      content = content,
      status = MessageStatus.COMPLETE,
      errorMessage = null,
      createdAt = 1L
    )

    val pages = ConversationShareRenderer.imageExportPages(
      ConversationExport("Oversized", null, null, listOf(message))
    )

    assertTrue(pages.size > 1)
    assertEquals(content, pages.flatMap { it.messages }.joinToString("") { it.content.replace("\n", "") })
  }
}
