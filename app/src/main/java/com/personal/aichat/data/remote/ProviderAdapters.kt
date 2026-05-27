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
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.conscrypt.Conscrypt
import java.io.IOException
import java.security.Security
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
    val requestBody = mutableMapOf<String, Any>(
        "model" to options.model,
        "stream" to options.stream,
        "input" to messages.map { message ->
          mapOf(
            "role" to message.role.apiRole,
            "content" to message.content
          )
        }
      )
    config.reasoningEffort.apiValue?.let { effort ->
      requestBody["reasoning"] = mapOf("effort" to effort)
    }
    val body = gson.toJson(requestBody)
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

fun defaultAiHttpClient(): OkHttpClient {
  ensureConscryptProvider()
  return OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
}

private fun ensureConscryptProvider() {
  if (Build.VERSION.SDK_INT <= 0) return
  if (Security.getProvider("Conscrypt") == null) {
    Security.insertProviderAt(Conscrypt.newProvider(), 1)
  }
}

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
  val requestHost = request.url.host
  client.newCall(request).execute().use { response ->
    if (!response.isSuccessful) {
      val errorBody = response.body?.string().orEmpty()
      throw IOException(
        buildString {
          append("Provider request failed with HTTP ${response.code}")
          append(" from ")
          append(requestHost)
          parseProviderErrorMessage(errorBody)?.let { append(": ").append(it) }
        }
      )
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

private fun parseProviderErrorMessage(body: String): String? {
  if (body.isBlank()) return null
  return runCatching {
    val root = JsonParser.parseString(body).asJsonObject
    root.getAsJsonObject("error")?.findString("message")
      ?: root.findString("message")
      ?: body.take(220)
  }.getOrDefault(body.take(220))
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
