package com.personal.aichat

import com.personal.aichat.data.remote.OpenAiCompatibleChatAdapter
import com.personal.aichat.data.remote.SseParser
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderAdapterTest {
  @Test
  fun sseParserCollectsDataLines() {
    val parser = SseParser()
    assertEquals(null, parser.accept("event: response.output_text.delta"))
    assertEquals(null, parser.accept("data: {\"delta\":\"hi\"}"))
    val frame = parser.accept("")
    assertEquals("response.output_text.delta", frame?.event)
    assertEquals("{\"delta\":\"hi\"}", frame?.data)
  }

  @Test
  fun openAiCompatibleAdapterReadsStreamingDeltas() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
          "data: {\"choices\":[{\"delta\":{\"content\":\"hel\"}}]}\n\n" +
            "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\n\n" +
            "data: [DONE]\n\n"
        )
    )
    server.start()
    try {
      val adapter = OpenAiCompatibleChatAdapter()
      val events = adapter.streamChat(
        config = ChatProviderConfig(
          id = "test",
          displayName = "Test",
          type = ProviderType.OPENAI_COMPATIBLE_CHAT,
          baseUrl = server.url("/v1").toString().trimEnd('/'),
          defaultModel = "test-model",
          enabled = true,
          supportsStreaming = true,
          extraHeadersJson = "",
          secretRef = "provider_test",
          reasoningEffort = ReasoningEffort.AUTO
        ),
        apiKey = "test-key",
        messages = listOf(
          ChatMessage(
            id = "msg_1",
            conversationId = "conv_1",
            role = MessageRole.USER,
            content = "hello",
            status = MessageStatus.COMPLETE,
            providerId = "test",
            model = "test-model",
            createdAt = 1,
            updatedAt = 1,
            errorMessage = null
          )
        ),
        options = ChatCompletionOptions(model = "test-model")
      ).toList()

      assertEquals(ChatStreamEvent.Started, events[0])
      assertEquals(ChatStreamEvent.TextDelta("hel"), events[1])
      assertEquals(ChatStreamEvent.TextDelta("lo"), events[2])
      assertEquals(ChatStreamEvent.Completed, events[3])
    } finally {
      server.shutdown()
    }
  }
}
