package com.personal.aichat

import com.personal.aichat.data.remote.OpenAiCompatibleChatAdapter
import com.personal.aichat.data.remote.OpenAiResponsesAdapter
import com.personal.aichat.data.remote.SseParser
import com.personal.aichat.data.remote.WebSearchClient
import com.personal.aichat.data.remote.WebSearchResponse
import com.personal.aichat.data.remote.WebSearchResult
import com.personal.aichat.data.remote.extractCompatibleToolCalls
import com.personal.aichat.data.remote.extractTokenUsage
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.MessageStatus
import com.personal.aichat.domain.ProviderType
import com.personal.aichat.domain.ReasoningEffort
import com.personal.aichat.domain.WebSearchMode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

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
      val requestBody = server.takeRequest().body.readUtf8()
      assertEquals(true, requestBody.contains("\"stream_options\":{\"include_usage\":true}"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun extractsTokenUsageFromOpenAiCompatibleChunk() {
    val usage = extractTokenUsage(
      """
      {"choices":[],"usage":{"prompt_tokens":12,"completion_tokens":34,"total_tokens":46}}
      """.trimIndent()
    )

    assertEquals(12, usage?.promptTokens)
    assertEquals(34, usage?.completionTokens)
    assertEquals(46, usage?.totalTokens)
  }

  @Test
  fun openAiResponsesAdapterSendsHostedWebSearchTool() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody("event: response.completed\ndata: {\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}\n\n")
    )
    server.start()
    try {
      val adapter = OpenAiResponsesAdapter()
      adapter.streamChat(
        config = providerConfig(
          type = ProviderType.OPENAI_RESPONSES,
          baseUrl = server.url("/v1").toString().trimEnd('/')
        ),
        apiKey = "test-key",
        messages = listOf(userMessage("current news")),
        options = ChatCompletionOptions(
          model = "gpt-test",
          webSearchMode = WebSearchMode.AUTO
        )
      ).toList()

      val requestBody = server.takeRequest().body.readUtf8()
      assertEquals(true, requestBody.contains("\"tools\":[{\"type\":\"web_search\"}]"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun openAiResponsesAdapterSendsImageAndFileAttachments() = runTest {
    val imageFile = Files.createTempFile("aichat-image", ".png").toFile().apply {
      writeBytes(byteArrayOf(1, 2, 3, 4))
      deleteOnExit()
    }
    val textFile = Files.createTempFile("aichat-file", ".txt").toFile().apply {
      writeText("hello")
      deleteOnExit()
    }
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody("event: response.completed\ndata: {\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}\n\n")
    )
    server.start()
    try {
      val adapter = OpenAiResponsesAdapter()
      adapter.streamChat(
        config = providerConfig(
          type = ProviderType.OPENAI_RESPONSES,
          baseUrl = server.url("/v1").toString().trimEnd('/')
        ),
        apiKey = "test-key",
        messages = listOf(
          userMessage("describe these").copy(
            attachments = listOf(
              ChatAttachment("att_image", "image.png", "image/png", imageFile.length(), imageFile.absolutePath),
              ChatAttachment("att_file", "note.txt", "text/plain", textFile.length(), textFile.absolutePath)
            )
          )
        ),
        options = ChatCompletionOptions(model = "gpt-test")
      ).toList()

      val requestBody = server.takeRequest().body.readUtf8()
      assertEquals(true, requestBody.contains("\"type\":\"input_text\""))
      assertEquals(true, requestBody.contains("\"type\":\"input_image\""))
      assertEquals(true, requestBody.contains("\"image_url\":\"data:image/png;base64,AQIDBA"))
      assertEquals(true, requestBody.contains("\"type\":\"input_file\""))
      assertEquals(true, requestBody.contains("\"filename\":\"note.txt\""))
      assertEquals(true, requestBody.contains("\"file_data\":\"data:text/plain;base64,aGVsbG8"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun openAiResponsesAdapterEmitsHostedWebSearchToolEvents() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
          "event: response.web_search_call.searching\n" +
            "data: {\"item_id\":\"ws_1\",\"type\":\"web_search_call\",\"status\":\"searching\"}\n\n" +
            "event: response.web_search_call.completed\n" +
            "data: {\"item_id\":\"ws_1\",\"type\":\"web_search_call\",\"status\":\"completed\"}\n\n" +
            "event: response.output_text.annotation.added\n" +
            "data: {\"annotation\":{\"type\":\"url_citation\",\"title\":\"DeepSeek Pricing\",\"url\":\"https://api-docs.deepseek.com/quick_start/pricing\"}}\n\n" +
            "event: response.output_text.delta\n" +
            "data: {\"delta\":\"answer\"}\n\n" +
            "event: response.completed\n" +
            "data: {\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}\n\n"
        )
    )
    server.start()
    try {
      val adapter = OpenAiResponsesAdapter()
      val events = adapter.streamChat(
        config = providerConfig(
          type = ProviderType.OPENAI_RESPONSES,
          baseUrl = server.url("/v1").toString().trimEnd('/')
        ),
        apiKey = "test-key",
        messages = listOf(userMessage("current news")),
        options = ChatCompletionOptions(
          model = "gpt-test",
          webSearchMode = WebSearchMode.AUTO
        )
      ).toList()

      val toolEvents = events.filterIsInstance<ChatStreamEvent.ToolCall>()
      assertEquals(2, toolEvents.size)
      assertEquals("openai-web-search", toolEvents.first().id)
      assertEquals(null, toolEvents.first().output)
      assertEquals("openai-web-search", toolEvents.last().id)
      assertEquals(true, toolEvents.last().output?.contains("https://api-docs.deepseek.com/quick_start/pricing"))
      assertEquals(ChatStreamEvent.TextDelta("answer"), events.filterIsInstance<ChatStreamEvent.TextDelta>().single())
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun openAiResponsesAdapterExtractsWebSearchUrlsFromAssistantText() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
          "event: response.web_search_call.searching\n" +
            "data: {\"item_id\":\"ws_1\",\"type\":\"web_search_call\",\"status\":\"searching\"}\n\n" +
            "event: response.web_search_call.completed\n" +
            "data: {\"item_id\":\"ws_1\",\"type\":\"web_search_call\",\"status\":\"completed\"}\n\n" +
            "event: response.output_text.delta\n" +
            "data: {\"delta\":\"DeepSeek pricing: https://api-docs.deepseek.com/zh-cn/quick_start/pricing/.\"}\n\n" +
            "event: response.completed\n" +
            "data: {\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}\n\n"
        )
    )
    server.start()
    try {
      val adapter = OpenAiResponsesAdapter()
      val events = adapter.streamChat(
        config = providerConfig(
          type = ProviderType.OPENAI_RESPONSES,
          baseUrl = server.url("/v1").toString().trimEnd('/')
        ),
        apiKey = "test-key",
        messages = listOf(userMessage("deepseek pricing")),
        options = ChatCompletionOptions(
          model = "gpt-test",
          webSearchMode = WebSearchMode.AUTO
        )
      ).toList()

      val toolEvents = events.filterIsInstance<ChatStreamEvent.ToolCall>()
      assertEquals(2, toolEvents.size)
      assertEquals(true, toolEvents.last().output?.contains("https://api-docs.deepseek.com/zh-cn/quick_start/pricing/"))
      assertEquals(false, toolEvents.last().output?.contains("pricing/."))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun openAiResponsesAdapterMergesHostedWebSearchActionsIntoOneToolCard() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
          "event: response.output_item.added\n" +
            "data: {\"item\":{\"id\":\"ws_1\",\"type\":\"web_search_call\",\"status\":\"in_progress\"}}\n\n" +
            "event: response.output_item.done\n" +
            "data: {\"item\":{\"id\":\"ws_1\",\"type\":\"web_search_call\",\"status\":\"completed\",\"action\":{\"type\":\"search\",\"queries\":[\"DeepSeek API pricing official\",\"DeepSeek API price docs\"]}}}\n\n" +
            "event: response.output_item.added\n" +
            "data: {\"item\":{\"id\":\"ws_2\",\"type\":\"web_search_call\",\"status\":\"in_progress\"}}\n\n" +
            "event: response.output_item.done\n" +
            "data: {\"item\":{\"id\":\"ws_2\",\"type\":\"web_search_call\",\"status\":\"completed\",\"action\":{\"type\":\"open_page\",\"url\":\"https://api-docs.deepseek.com/quick_start/pricing/\"}}}\n\n" +
            "event: response.completed\n" +
            "data: {\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3}}\n\n"
        )
    )
    server.start()
    try {
      val adapter = OpenAiResponsesAdapter()
      val events = adapter.streamChat(
        config = providerConfig(
          type = ProviderType.OPENAI_RESPONSES,
          baseUrl = server.url("/v1").toString().trimEnd('/')
        ),
        apiKey = "test-key",
        messages = listOf(userMessage("deepseek pricing")),
        options = ChatCompletionOptions(
          model = "gpt-test",
          webSearchMode = WebSearchMode.AUTO
        )
      ).toList()

      val toolEvents = events.filterIsInstance<ChatStreamEvent.ToolCall>()
      assertEquals(3, toolEvents.size)
      assertEquals(true, toolEvents.all { it.id == "openai-web-search" })
      assertEquals(true, toolEvents.last().output?.contains("DeepSeek API pricing official"))
      assertEquals(true, toolEvents.last().output?.contains("https://api-docs.deepseek.com/quick_start/pricing/"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun openAiCompatibleAdapterRunsWebSearchToolAndStreamsFinalAnswer() = runTest {
    val server = MockWebServer()
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(
          """
          {"choices":[{"message":{"tool_calls":[{"id":"call_1","type":"function","function":{"name":"web_search","arguments":"{\"query\":\"latest kotlin\"}"}}]}}]}
          """.trimIndent()
        )
    )
    server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
          "data: {\"choices\":[{\"delta\":{\"content\":\"done\"}}]}\n\n" +
            "data: [DONE]\n\n"
        )
    )
    server.start()
    try {
      val adapter = OpenAiCompatibleChatAdapter(
        webSearchClient = object : WebSearchClient {
          override suspend fun search(query: String): WebSearchResponse {
            return WebSearchResponse(
              query = query,
              results = listOf(WebSearchResult("Kotlin", "https://kotlinlang.org", "Kotlin news"))
            )
          }
        }
      )
      val events = adapter.streamChat(
        config = providerConfig(
          type = ProviderType.OPENAI_COMPATIBLE_CHAT,
          baseUrl = server.url("/v1").toString().trimEnd('/')
        ),
        apiKey = "test-key",
        messages = listOf(userMessage("search")),
        options = ChatCompletionOptions(
          model = "deepseek-test",
          webSearchMode = WebSearchMode.AUTO
        )
      ).toList()

      assertEquals(ChatStreamEvent.Started, events[0])
      assertEquals(true, events.any { it is ChatStreamEvent.ToolCall })
      assertEquals(ChatStreamEvent.TextDelta("done"), events.filterIsInstance<ChatStreamEvent.TextDelta>().single())
      val toolRequestBody = server.takeRequest().body.readUtf8()
      val finalRequestBody = server.takeRequest().body.readUtf8()
      assertEquals(true, toolRequestBody.contains("\"tools\""))
      assertEquals(true, finalRequestBody.contains("\"role\":\"tool\""))
      assertEquals(true, finalRequestBody.contains("https://kotlinlang.org"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun extractsCompatibleToolCalls() {
    val calls = extractCompatibleToolCalls(
      """
      {"choices":[{"message":{"tool_calls":[{"id":"call_1","type":"function","function":{"name":"web_search","arguments":"{\"query\":\"deepseek\"}"}}]}}]}
      """.trimIndent()
    )

    assertEquals(1, calls.size)
    assertEquals("call_1", calls[0].id)
    assertEquals("web_search", calls[0].name)
  }

  private fun providerConfig(type: ProviderType, baseUrl: String): ChatProviderConfig {
    return ChatProviderConfig(
      id = "test",
      displayName = "Test",
      type = type,
      baseUrl = baseUrl,
      defaultModel = "test-model",
      enabled = true,
      supportsStreaming = true,
      extraHeadersJson = "",
      secretRef = "provider_test",
      reasoningEffort = ReasoningEffort.AUTO
    )
  }

  private fun userMessage(content: String): ChatMessage {
    return ChatMessage(
      id = "msg_1",
      conversationId = "conv_1",
      role = MessageRole.USER,
      content = content,
      status = MessageStatus.COMPLETE,
      providerId = "test",
      model = "test-model",
      createdAt = 1,
      updatedAt = 1,
      errorMessage = null
    )
  }
}
