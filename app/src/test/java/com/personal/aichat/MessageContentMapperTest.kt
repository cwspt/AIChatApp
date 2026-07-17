package com.personal.aichat

import com.personal.aichat.data.local.MessageEntity
import com.personal.aichat.data.local.formatMessageContentDocument
import com.personal.aichat.data.local.toDomain
import com.personal.aichat.domain.MessageContentDocument
import com.personal.aichat.domain.MessageContentPart
import com.personal.aichat.domain.MessageContentPartStatus
import com.personal.aichat.domain.MessageContentPartType
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageContentMapperTest {
  @Test
  fun emptyOrDamagedDocumentFallsBackToLegacyTextPart() {
    listOf("", "not-json").forEach { json ->
      val message = entity(content = "legacy **markdown**", contentPartsJson = json).toDomain()

      assertEquals("legacy **markdown**", message.contentParts.single().text)
      assertEquals(MessageContentPartType.TEXT, message.contentParts.single().type)
      assertTrue(!message.inlineImagesRequested)
    }
  }

  @Test
  fun completedMessageRecoversGeneratingImageAsInterruptedFailure() {
    val json = formatMessageContentDocument(
      MessageContentDocument(
        inlineImagesRequested = true,
        parts = listOf(
          MessageContentPart("text", MessageContentPartType.TEXT, text = "before"),
          MessageContentPart(
            id = "image",
            type = MessageContentPartType.IMAGE,
            prompt = "city map",
            status = MessageContentPartStatus.GENERATING
          )
        )
      )
    )

    val message = entity(content = "before", contentPartsJson = json).toDomain()
    val image = message.contentParts.last()

    assertTrue(message.inlineImagesRequested)
    assertEquals(MessageContentPartStatus.FAILED, image.status)
    assertEquals("生成中断，可重试", image.errorMessage)
  }

  private fun entity(content: String, contentPartsJson: String) = MessageEntity(
    id = "message",
    conversationId = "conversation",
    role = MessageRole.ASSISTANT.name,
    content = content,
    status = MessageStatus.COMPLETE.name,
    providerId = "provider",
    model = "model",
    createdAt = 1,
    updatedAt = 2,
    errorMessage = null,
    contentPartsJson = contentPartsJson
  )
}
