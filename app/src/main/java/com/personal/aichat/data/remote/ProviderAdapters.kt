package com.personal.aichat.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.ProviderAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private val JsonMediaType = "application/json; charset=utf-8".toMediaType()

class OpenAiResponsesAdapter(
  private val client: OkHttpClient = defaultAiHttpClient(),
  private val gson: Gson = Gson()
) : ProviderAdapter {
  override fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent> = flow {
    emit(ChatStreamEvent.Started)
    val body = gson.toJson(
      mapOf(
        "model" to options.model,
        "stream" to options.stream,
        "input" to messages.map { message ->
          mapOf(
            "role" to message.role.apiRole,
            "content" to message.content
          )
        }
      )
    )
    val request = Request.Builder()
      .url(config.baseUrl.trimEnd('/') + "/responses")
      .headers(config.headersWithAuth(apiKey))
      .post(body.toRequestBody(JsonMediaType))
      .build()

    streamJsonLines(
      request = request,
      client = client,
      onFrame = { frame ->
        when (frame.event) {
          "response.output_text.delta" -> extractString(frame.data, "delta")
          "response.completed" -> null
          "error" -> throw IOException(extractString(frame.data, "message") ?: "Provider returned an error")
          else -> extractString(frame.data, "delta")
        }
      },
      onText = { emit(ChatStreamEvent.TextDelta(it)) }
    )
    emit(ChatStreamEvent.Completed)
  }.flowOn(Dispatchers.IO)
}

class OpenAiCompatibleChatAdapter(
  private val client: OkHttpClient = defaultAiHttpClient(),
  private val gson: Gson = Gson()
) : ProviderAdapter {
  override fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent> = flow {
    emit(ChatStreamEvent.Started)
    val body = gson.toJson(
      mapOf(
        "model" to options.model,
        "stream" to options.stream,
        "messages" to messages.map { message ->
          mapOf("role" to message.role.apiRole, "content" to message.content)
        }
      )
    )
    val request = Request.Builder()
      .url(config.baseUrl.trimEnd('/') + "/chat/completions")
      .headers(config.headersWithAuth(apiKey))
      .post(body.toRequestBody(JsonMediaType))
      .build()

    streamJsonLines(
      request = request,
      client = client,
      onFrame = { frame ->
        if (frame.data == "[DONE]") null else extractChatDelta(frame.data)
      },
      onText = { emit(ChatStreamEvent.TextDelta(it)) }
    )
    emit(ChatStreamEvent.Completed)
  }.flowOn(Dispatchers.IO)
}

class TokenHubProxyAdapter(
  private val delegate: OpenAiResponsesAdapter = OpenAiResponsesAdapter()
) : ProviderAdapter {
  override fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent> {
    return delegate.streamChat(config, apiKey, messages, options)
  }
}

fun defaultAiHttpClient(): OkHttpClient = OkHttpClient.Builder()
  .connectTimeout(30, TimeUnit.SECONDS)
  .readTimeout(0, TimeUnit.SECONDS)
  .writeTimeout(60, TimeUnit.SECONDS)
  .build()

private val MessageRole.apiRole: String
  get() = when (this) {
    MessageRole.USER -> "user"
    MessageRole.ASSISTANT -> "assistant"
    MessageRole.SYSTEM -> "system"
  }

private fun ChatProviderConfig.headersWithAuth(apiKey: String?): okhttp3.Headers {
  val builder = okhttp3.Headers.Builder()
    .add("Content-Type", "application/json")
  if (!apiKey.isNullOrBlank()) {
    builder.add("Authorization", "Bearer $apiKey")
  }
  parseExtraHeaders(extraHeadersJson).forEach { (name, value) ->
    if (!name.equals("authorization", ignoreCase = true)) {
      builder.set(name, value)
    }
  }
  return builder.build()
}

private fun parseExtraHeaders(extraHeadersJson: String): Map<String, String> {
  if (extraHeadersJson.isBlank()) return emptyMap()
  return runCatching {
    JsonParser.parseString(extraHeadersJson).asJsonObject.entrySet().associate { entry ->
      entry.key to entry.value.asString
    }
  }.getOrDefault(emptyMap())
}

private suspend fun streamJsonLines(
  request: Request,
  client: OkHttpClient,
  onFrame: (SseFrame) -> String?,
  onText: suspend (String) -> Unit
) {
  client.newCall(request).execute().use { response ->
    if (!response.isSuccessful) {
      throw IOException("Provider request failed with HTTP ${response.code}")
    }

    val body = response.body ?: throw IOException("Provider returned an empty response")
    val parser = SseParser()
    body.source().use { source ->
      while (!source.exhausted()) {
        val line = source.readUtf8Line() ?: break
        val frame = parser.accept(line) ?: continue
        val text = onFrame(frame)
        if (!text.isNullOrEmpty()) {
          onText(text)
        }
      }
    }
  }
}

fun extractString(json: String, name: String): String? {
  return runCatching {
    JsonParser.parseString(json).asJsonObject.findString(name)
  }.getOrNull()
}

fun extractChatDelta(json: String): String? {
  return runCatching {
    val root = JsonParser.parseString(json).asJsonObject
    root.getAsJsonArray("choices")
      ?.firstOrNull()
      ?.asJsonObject
      ?.getAsJsonObject("delta")
      ?.findString("content")
  }.getOrNull()
}

private fun JsonObject.findString(name: String): String? {
  if (has(name) && !get(name).isJsonNull) return get(name).asString
  entrySet().forEach { entry ->
    val value = entry.value
    if (value.isJsonObject) {
      val nested = value.asJsonObject.findString(name)
      if (nested != null) return nested
    }
    if (value.isJsonArray) {
      value.asJsonArray.forEach { item ->
        if (item.isJsonObject) {
          val nested = item.asJsonObject.findString(name)
          if (nested != null) return nested
        }
      }
    }
  }
  return null
}
