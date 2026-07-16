package com.personal.aichat

import android.graphics.Bitmap
import com.personal.aichat.data.ConversationExport
import com.personal.aichat.data.ConversationExportMessage
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.ui.ConversationShareRenderer
import com.personal.aichat.ui.ConversationShareRenderer.ImageInlineStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConversationShareRendererTest {
  @Test
  fun imageExportPagesKeepContentInOnePageWhenExactLayoutFits() {
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

    assertEquals(1, pages.size)
    assertEquals(messages.map { it.content }, pages.flatMap { it.messages }.map { it.content })
    assertEquals("Long chat", pages.single().title)
  }

  @Test
  fun imageExportPagesSplitOneOversizedMessageWithoutDroppingText() {
    val content = List(320) { index -> "oversized line $index" }.joinToString("\n\n")
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
    assertEquals(
      content.replace("\n", ""),
      pages.flatMap { it.messages }.joinToString("") { it.content.replace("\n", "") }
    )
    assertTrue(pages.map { it.title }.all { it.contains("/") })
  }

  @Test
  fun imageExportPlanAllowsSafeSingleImageForMediumLongContent() {
    val plan = ConversationShareRenderer.imageExportPlan(
      ConversationExport(
        "Medium long chat",
        null,
        null,
        listOf(message(List(220) { index -> "medium line $index" }.joinToString("\n\n")))
      )
    )

    assertTrue(plan.standardHeightPx > 12_000)
    assertTrue(plan.standardHeightPx <= 24_000)
    assertTrue(plan.pageCount > 1)
    assertTrue(plan.singleImageAllowed)
  }

  @Test
  fun imageExportPlanRejectsSingleImageBeyondSafeHeight() {
    val plan = ConversationShareRenderer.imageExportPlan(
      ConversationExport(
        "Very long chat",
        null,
        null,
        listOf(message(List(450) { index -> "very long line $index" }.joinToString("\n\n")))
      )
    )

    assertTrue(plan.standardHeightPx > 24_000)
    assertTrue(plan.pageCount > 1)
    assertTrue(!plan.singleImageAllowed)
  }

  @Test
  fun imageExportModesKeepBitmapMemoryWithinConfiguredBudgets() {
    assertEquals(
      Bitmap.Config.RGB_565,
      ConversationShareRenderer.imageBitmapConfig(ConversationShareRenderer.ImageExportMode.SINGLE)
    )
    assertEquals(
      24_000,
      ConversationShareRenderer.imageMaxHeight(ConversationShareRenderer.ImageExportMode.SINGLE)
    )
    assertEquals(
      Bitmap.Config.ARGB_8888,
      ConversationShareRenderer.imageBitmapConfig(ConversationShareRenderer.ImageExportMode.PAGED)
    )
    assertEquals(
      12_000,
      ConversationShareRenderer.imageMaxHeight(ConversationShareRenderer.ImageExportMode.PAGED)
    )
  }

  @Test
  fun imageInlineMarkdownSpansPreserveRichStylesAndVisibleTextOrder() {
    val text = "Normal **bold** with `code` and [OpenAI](https://openai.com), plus help.openai.com."

    val spans = ConversationShareRenderer.imageInlineMarkdownSpans(text)

    assertEquals(
      "Normal bold with code and OpenAI, plus help.openai.com.",
      spans.joinToString("") { it.text }
    )
    assertTrue(spans.any { it.text == "bold" && it.style == ImageInlineStyle.BOLD })
    assertTrue(spans.any { it.text == "code" && it.style == ImageInlineStyle.INLINE_CODE })
    assertTrue(spans.any { it.text == "OpenAI" && it.style == ImageInlineStyle.LINK })
    assertTrue(spans.any { it.text == "help.openai.com" && it.style == ImageInlineStyle.LINK })
  }

  private fun message(content: String) = ConversationExportMessage(
    id = "message",
    role = MessageRole.ASSISTANT,
    content = content,
    status = MessageStatus.COMPLETE,
    errorMessage = null,
    createdAt = 1L
  )
}
