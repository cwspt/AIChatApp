package com.personal.aichat.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.personal.aichat.domain.ChatAttachment
import com.personal.aichat.domain.ChatCompletionOptions
import com.personal.aichat.domain.ChatMessage
import com.personal.aichat.domain.ChatProviderConfig
import com.personal.aichat.domain.ChatStreamEvent
import com.personal.aichat.domain.ImageGenerationApiMode
import com.personal.aichat.domain.ImageGenerationOptions
import com.personal.aichat.domain.MessageRole
import com.personal.aichat.domain.ProviderAdapter
import com.personal.aichat.domain.WebSearchMode
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.conscrypt.Conscrypt
import java.io.File
import java.io.EOFException
import java.io.IOException
import java.security.Security
import java.util.Base64
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
    val webSearchEnabled = options.webSearchMode != WebSearchMode.OFF
    var responseWebSearchId: String? = null
    val responseCitations = linkedMapOf<String, String>()
    val responseSearchQueries = linkedSetOf<String>()
    val responseText = StringBuilder()
    var lastEmittedCitationOutput: String? = null
    var emittedWebSearchStart = false

    suspend fun emitResponseCitations() {
      val output = formatWebSearchOutput(responseSearchQueries, responseCitations)
      if (output.isBlank()) return
      if (output == lastEmittedCitationOutput) return
      lastEmittedCitationOutput = output
      emittedWebSearchStart = true
      emit(
        ChatStreamEvent.ToolCall(
          id = responseWebSearchId ?: "openai-web-search",
          name = "web_search",
          input = "OpenAI Responses web search",
          output = output
        )
      )
    }

    val requestBody = mutableMapOf<String, Any>(
        "model" to options.model,
        "stream" to options.stream,
        "input" to messages.map(::toOpenAiResponseInputMessage)
      )
    config.reasoningEffort.apiValue?.let { effort ->
      requestBody["reasoning"] = mapOf("effort" to effort)
    }
    if (options.webSearchMode != WebSearchMode.OFF) {
      requestBody["tools"] = listOf(mapOf("type" to "web_search"))
      if (options.webSearchMode == WebSearchMode.REQUIRED) {
        requestBody["tool_choice"] = mapOf("type" to "web_search")
      }
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
        if (options.captureRawResponseLog) emit(ChatStreamEvent.RawFrame(frame.event, frame.data))
        when (frame.event) {
          "response.output_text.delta" -> {
            extractString(frame.data, "delta")?.let { delta ->
              responseText.append(delta)
              if (webSearchEnabled && emittedWebSearchStart) {
                extractUrlCitationsFromText(responseText.toString()).forEach { citation ->
                  responseCitations[citation.url] = citation.title
                }
                emitResponseCitations()
              }
              emit(ChatStreamEvent.TextDelta(delta))
            }
          }
          "response.completed" -> {
            extractOpenAiUrlCitations(frame.data).forEach { citation ->
              responseCitations[citation.url] = citation.title
            }
            if (webSearchEnabled && emittedWebSearchStart) {
              extractUrlCitationsFromText(responseText.toString()).forEach { citation ->
                responseCitations[citation.url] = citation.title
              }
            }
            emitResponseCitations()
            if (webSearchEnabled && emittedWebSearchStart && responseCitations.isEmpty() && lastEmittedCitationOutput == null) {
              emit(
                ChatStreamEvent.ToolCall(
                  id = responseWebSearchId ?: "openai-web-search",
                  name = "web_search",
                  input = "OpenAI Responses web search",
                  output = "未返回可展示网址。可在设置中开启调试日志后复制原始响应，用于补充解析规则。"
                )
              )
            }
            extractTokenUsage(frame.data)?.let {
              emit(
                ChatStreamEvent.Usage(
                  promptTokens = it.promptTokens,
                  completionTokens = it.completionTokens,
                  totalTokens = it.totalTokens,
                  raw = it.raw
                )
              )
            }
          }
          "error" -> throw IOException(extractString(frame.data, "message") ?: "Provider returned an error")
          else -> {
            extractOpenAiWebSearchUpdate(frame.event, frame.data)?.let { update ->
              responseWebSearchId = "openai-web-search"
              responseSearchQueries += update.queries
              update.urls.forEach { citation ->
                responseCitations[citation.url] = citation.title
              }
              if (update.queries.isNotEmpty() || update.urls.isNotEmpty()) {
                emitResponseCitations()
              } else if (!emittedWebSearchStart) {
                emittedWebSearchStart = true
                emit(
                  ChatStreamEvent.ToolCall(
                    id = responseWebSearchId,
                    name = "web_search",
                    input = "OpenAI Responses web search",
                    output = null
                  )
                )
              }
            }
            extractOpenAiUrlCitations(frame.data).forEach { citation ->
              responseCitations[citation.url] = citation.title
            }
            emitResponseCitations()
            extractString(frame.data, "delta")?.let { delta ->
              responseText.append(delta)
              if (webSearchEnabled && emittedWebSearchStart) {
                extractUrlCitationsFromText(responseText.toString()).forEach { citation ->
                  responseCitations[citation.url] = citation.title
                }
                emitResponseCitations()
              }
              emit(ChatStreamEvent.TextDelta(delta))
            }
          }
        }
      }
    )
    emit(ChatStreamEvent.Completed)
  }.flowOn(Dispatchers.IO)

  override fun generateImages(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ImageGenerationOptions
  ): Flow<ChatStreamEvent> = flow {
    emit(ChatStreamEvent.Started)
    if (config.imageGenerationApiMode == ImageGenerationApiMode.IMAGES_API) {
      emitImagesApiGeneration(config, apiKey, messages, options) { event -> emit(event) }
      emit(ChatStreamEvent.Completed)
      return@flow
    }
    val requestBody = mutableMapOf<String, Any>(
      "model" to config.defaultModel,
      "stream" to false,
      "input" to messages.takeLast(12).map(::toOpenAiImageInputMessage),
      "tools" to listOf(
        mapOf(
          "type" to "image_generation",
          "size" to options.size.apiValue,
          "quality" to options.quality.apiValue,
          "output_format" to options.outputFormat.apiValue,
          "background" to options.background.apiValue
        )
      ),
      "tool_choice" to mapOf("type" to "image_generation")
    )
    val request = Request.Builder()
      .url(config.baseUrl.trimEnd('/') + "/responses")
      .headers(config.headersWithAuth(apiKey))
      .post(gson.toJson(requestBody).toRequestBody(JsonMediaType))
      .build()
    client.newCall(request).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (options.captureRawResponseLog) {
        emit(ChatStreamEvent.RawFrame(if (response.isSuccessful) "response" else "http_error", body))
      }
      if (!response.isSuccessful) {
        throw IOException(parseProviderErrorMessage(body) ?: "Provider request failed with HTTP ${response.code}")
      }
      val images = extractGeneratedImages(body)
      if (images.isEmpty()) {
        emit(ChatStreamEvent.Failed("OpenAI 未返回可保存的图片数据"))
        return@flow
      }
      images.forEach { image ->
        emit(
          ChatStreamEvent.ImageGenerated(
            base64Data = image.base64Data,
            mimeType = image.mimeType,
            revisedPrompt = image.revisedPrompt
          )
        )
      }
      extractTokenUsage(body)?.let {
        emit(
          ChatStreamEvent.Usage(
            promptTokens = it.promptTokens,
            completionTokens = it.completionTokens,
            totalTokens = it.totalTokens,
            raw = it.raw
          )
        )
      }
    }
    emit(ChatStreamEvent.Completed)
  }.flowOn(Dispatchers.IO)

  private suspend fun emitImagesApiGeneration(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ImageGenerationOptions,
    emitEvent: suspend (ChatStreamEvent) -> Unit
  ) {
    val model = config.imageGenerationModel.trim()
    if (model.isBlank()) {
      emitEvent(ChatStreamEvent.Failed("请在 Provider 配置中填写生图模型名"))
      return
    }
    val latestUser = messages.lastOrNull { it.role == MessageRole.USER }
    val prompt = latestUser?.content?.trim().orEmpty().ifBlank { "根据参考图片生成或编辑图片" }
    val referenceImages = latestUser?.attachments.orEmpty().filter { it.isImage && File(it.localPath).isFile }
    val count = options.count.coerceIn(1, 4)
    val endpoint = if (referenceImages.isEmpty()) "/images/generations" else "/images/edits"
    val request = if (referenceImages.isEmpty()) {
      val requestBody = mapOf(
        "model" to model,
        "prompt" to prompt,
        "n" to count,
        "size" to options.size.apiValue,
        "quality" to options.quality.apiValue,
        "background" to options.background.apiValue,
        "output_format" to options.outputFormat.apiValue
      )
      Request.Builder()
        .url(config.baseUrl.trimEnd('/') + endpoint)
        .headers(config.headersWithAuth(apiKey))
        .post(gson.toJson(requestBody).toRequestBody(JsonMediaType))
        .build()
    } else {
      val multipart = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("model", model)
        .addFormDataPart("prompt", prompt)
        .addFormDataPart("n", count.toString())
        .addFormDataPart("size", options.size.apiValue)
        .addFormDataPart("quality", options.quality.apiValue)
        .addFormDataPart("background", options.background.apiValue)
        .addFormDataPart("output_format", options.outputFormat.apiValue)
      referenceImages.forEach { attachment ->
        val file = File(attachment.payloadLocalPath)
        multipart.addFormDataPart(
          "image[]",
          attachment.displayName,
          file.asRequestBody(attachment.payloadMimeType.toMediaTypeOrNull())
        )
      }
      Request.Builder()
        .url(config.baseUrl.trimEnd('/') + endpoint)
        .headers(config.headersWithAuth(apiKey, includeJsonContentType = false))
        .post(multipart.build())
        .build()
    }
    client.newCall(request).execute().use { response ->
      val body = response.body?.string().orEmpty()
      if (options.captureRawResponseLog) {
        emitEvent(ChatStreamEvent.RawFrame(if (response.isSuccessful) "response" else "http_error", body))
      }
      if (!response.isSuccessful) {
        throw IOException(parseProviderErrorMessage(body) ?: "Provider request failed with HTTP ${response.code}")
      }
      val images = extractImagesApiGeneratedImages(body, options.outputFormat.mimeType).toMutableList()
      extractImagesApiUrls(body).forEach { url ->
        downloadGeneratedImage(url, options.outputFormat.mimeType)?.let { images += it }
      }
      if (images.isEmpty()) {
        emitEvent(ChatStreamEvent.Failed("Images API 未返回可保存的图片数据"))
        return
      }
      images.distinctBy { it.base64Data.take(80) }.forEach { image ->
        emitEvent(
          ChatStreamEvent.ImageGenerated(
            base64Data = image.base64Data,
            mimeType = image.mimeType,
            revisedPrompt = image.revisedPrompt
          )
        )
      }
      extractTokenUsage(body)?.let {
        emitEvent(
          ChatStreamEvent.Usage(
            promptTokens = it.promptTokens,
            completionTokens = it.completionTokens,
            totalTokens = it.totalTokens,
            raw = it.raw
          )
        )
      }
    }
  }

  private fun downloadGeneratedImage(url: String, fallbackMimeType: String): GeneratedImagePayload? {
    val request = Request.Builder().url(url).get().build()
    return runCatching {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@runCatching null
        val bytes = response.body?.bytes() ?: return@runCatching null
        GeneratedImagePayload(
          base64Data = Base64.getEncoder().encodeToString(bytes),
          mimeType = response.body?.contentType()?.toString() ?: fallbackMimeType,
          revisedPrompt = null
        )
      }
    }.getOrNull()
  }
}

class OpenAiCompatibleChatAdapter(
  private val client: OkHttpClient = defaultAiHttpClient(),
  private val gson: Gson = Gson(),
  private val webSearchClient: WebSearchClient = CompositeWebSearchClient(),
  private val webPageClient: WebPageClient = SimpleWebPageClient()
) : ProviderAdapter {
  override fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent> = flow {
    emit(ChatStreamEvent.Started)
    if (options.webSearchMode != WebSearchMode.OFF) {
      streamChatWithTools(config, apiKey, messages, options) { emit(it) }
      return@flow
    }
    val requestBody = mutableMapOf<String, Any>(
        "model" to options.model,
        "stream" to options.stream,
        "messages" to messages.map { message ->
          mapOf("role" to message.role.apiRole, "content" to message.toCompatibleContent())
        }
      )
    if (options.stream) {
      requestBody["stream_options"] = mapOf("include_usage" to true)
    }
    val body = gson.toJson(requestBody)
    val request = Request.Builder()
      .url(config.baseUrl.trimEnd('/') + "/chat/completions")
      .headers(config.headersWithAuth(apiKey))
      .post(body.toRequestBody(JsonMediaType))
      .build()

    streamJsonLines(
      request = request,
      client = client,
      onFrame = { frame ->
        if (options.captureRawResponseLog) emit(ChatStreamEvent.RawFrame(frame.event, frame.data))
        if (frame.data != "[DONE]") {
          extractChatDelta(frame.data)?.let { emit(ChatStreamEvent.TextDelta(it)) }
          extractTokenUsage(frame.data)?.let {
            emit(
              ChatStreamEvent.Usage(
                promptTokens = it.promptTokens,
                completionTokens = it.completionTokens,
                totalTokens = it.totalTokens,
                raw = it.raw
              )
            )
          }
        }
      }
    )
    emit(ChatStreamEvent.Completed)
  }.flowOn(Dispatchers.IO)

  private suspend fun streamChatWithTools(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions,
    emitEvent: suspend (ChatStreamEvent) -> Unit
  ) {
    val toolMessages = messages.map { message ->
      mutableMapOf<String, Any>(
        "role" to message.role.apiRole,
        "content" to message.toCompatibleContent()
      )
    }.toMutableList()
    val decision = requestToolDecision(config, apiKey, toolMessages, options)
    if (decision.toolCalls.isEmpty()) {
      if (!decision.content.isNullOrBlank()) {
        emitEvent(ChatStreamEvent.TextDelta(decision.content))
        decision.usage?.let {
          emitEvent(
            ChatStreamEvent.Usage(
              promptTokens = it.promptTokens,
              completionTokens = it.completionTokens,
              totalTokens = it.totalTokens,
              raw = it.raw
            )
          )
        }
        emitEvent(ChatStreamEvent.Completed)
      } else {
        streamFinalChat(config, apiKey, toolMessages, options, emitEvent)
      }
      return
    }
    toolMessages += mutableMapOf<String, Any>(
      "role" to "assistant",
      "content" to "",
      "tool_calls" to decision.toolCalls.map { it.toRequestMap() }
    ).apply {
      decision.reasoningContent?.takeIf { it.isNotBlank() }?.let { reasoning ->
        put("reasoning_content", reasoning)
      }
    }
    decision.toolCalls.forEach { call ->
      executeCompatibleToolCall(call, toolMessages, emitEvent)
      /*
      emitEvent(ChatStreamEvent.ToolCall(id = call.id, name = call.name, input = call.arguments))
      val query = parseToolQuery(call.arguments)
      val output = if (query.isNullOrBlank()) {
        "工具调用缺少 query 参数。"
      } else {
        webSearchClient.search(query).toToolOutput()
      }
      emitEvent(ChatStreamEvent.ToolCall(id = call.id, name = call.name, input = call.arguments, output = output))
      toolMessages += mutableMapOf(
        "role" to "tool",
        "tool_call_id" to call.id,
        "content" to output
      )
      */
    }
    streamFinalChat(config, apiKey, toolMessages, options, emitEvent)
  }

  private suspend fun executeCompatibleToolCall(
    call: CompatibleToolCall,
    toolMessages: MutableList<MutableMap<String, Any>>,
    emitEvent: suspend (ChatStreamEvent) -> Unit
  ) {
    emitEvent(ChatStreamEvent.ToolCall(id = call.id, name = call.name, input = call.arguments))
    val output = when (call.name) {
      "web_search" -> {
        val query = parseToolQuery(call.arguments)
        if (query.isNullOrBlank()) {
          "工具调用缺少 query 参数。"
        } else {
          webSearchClient.search(query).toToolOutput()
        }
      }
      "open", "open_page", "web_fetch" -> {
        val url = parseToolUrl(call.arguments)
        if (url.isNullOrBlank()) {
          "工具调用缺少 url 参数。"
        } else {
          webPageClient.open(url).toToolOutput()
        }
      }
      else -> "不支持的工具：${call.name}"
    }
    emitEvent(ChatStreamEvent.ToolCall(id = call.id, name = call.name, input = call.arguments, output = output))
    toolMessages += mutableMapOf(
      "role" to "tool",
      "tool_call_id" to call.id,
      "content" to output
    )
  }

  private fun requestToolDecision(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: MutableList<MutableMap<String, Any>>,
    options: ChatCompletionOptions
  ): CompatibleToolDecision {
    val requestBody = mutableMapOf<String, Any>(
      "model" to options.model,
      "stream" to false,
      "messages" to messages,
      "tools" to compatibleToolDefinitions()
    )
    requestBody["tool_choice"] = if (options.webSearchMode == WebSearchMode.REQUIRED) {
      mapOf("type" to "function", "function" to mapOf("name" to "web_search"))
    } else {
      "auto"
    }
    val request = Request.Builder()
      .url(config.baseUrl.trimEnd('/') + "/chat/completions")
      .headers(config.headersWithAuth(apiKey))
      .post(gson.toJson(requestBody).toRequestBody(JsonMediaType))
      .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("Provider request failed with HTTP ${response.code} from ${request.url.host}: ${parseProviderErrorMessage(response.body?.string().orEmpty()).orEmpty()}")
      }
      return extractCompatibleToolDecision(response.body?.string().orEmpty())
    }
  }

  private suspend fun streamFinalChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: MutableList<MutableMap<String, Any>>,
    options: ChatCompletionOptions,
    emitEvent: suspend (ChatStreamEvent) -> Unit
  ) {
    val requestBody = mutableMapOf<String, Any>(
      "model" to options.model,
      "stream" to options.stream,
      "messages" to messages
    )
    if (options.stream) {
      requestBody["stream_options"] = mapOf("include_usage" to true)
    }
    var bufferedText = StringBuilder()
    val bufferedReasoning = StringBuilder()
    val request = Request.Builder()
      .url(config.baseUrl.trimEnd('/') + "/chat/completions")
      .headers(config.headersWithAuth(apiKey))
      .post(gson.toJson(requestBody).toRequestBody(JsonMediaType))
      .build()

    streamJsonLines(
      request = request,
      client = client,
      onFrame = { frame ->
        if (options.captureRawResponseLog) emitEvent(ChatStreamEvent.RawFrame(frame.event, frame.data))
        if (frame.data != "[DONE]") {
          extractChatReasoningDelta(frame.data)?.let { reasoning ->
            bufferedReasoning.append(reasoning)
          }
          extractChatDelta(frame.data)?.let { delta ->
            bufferedText.append(delta)
            if (!looksLikeCompatibleToolMarkup(bufferedText.toString())) {
              emitEvent(ChatStreamEvent.TextDelta(bufferedText.toString()))
              bufferedText = StringBuilder()
            }
          }
          extractTokenUsage(frame.data)?.let {
            emitEvent(
              ChatStreamEvent.Usage(
                promptTokens = it.promptTokens,
                completionTokens = it.completionTokens,
                totalTokens = it.totalTokens,
                raw = it.raw
              )
            )
          }
        }
      }
    )
    val trailingText = bufferedText.toString()
    val markupCalls = extractCompatibleMarkupToolCalls(trailingText)
    if (markupCalls.isNotEmpty()) {
      messages.add(mutableMapOf<String, Any>(
        "role" to "assistant",
        "content" to "",
        "tool_calls" to markupCalls.map { it.toRequestMap() }
      ).apply {
        bufferedReasoning.toString().takeIf { it.isNotBlank() }?.let { reasoning ->
          put("reasoning_content", reasoning)
        }
      })
      markupCalls.forEach { call ->
        executeCompatibleToolCall(call, messages, emitEvent)
      }
      streamFinalChat(config, apiKey, messages, options, emitEvent)
      return
    }
    if (trailingText.isNotBlank()) {
      emitEvent(ChatStreamEvent.TextDelta(trailingText))
    }
    emitEvent(ChatStreamEvent.Completed)
  }
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

private data class GeneratedImagePayload(
  val base64Data: String,
  val mimeType: String,
  val revisedPrompt: String?
)

private fun toOpenAiResponseInputMessage(message: ChatMessage): Map<String, Any> {
  val parts = mutableListOf<Map<String, Any>>()
  if (message.content.isNotBlank()) {
    parts += mapOf(
      "type" to if (message.role == MessageRole.ASSISTANT) "output_text" else "input_text",
      "text" to message.content
    )
  }
  if (message.role == MessageRole.USER) {
    message.attachments.forEach { attachment ->
      val dataUrl = attachment.toDataUrl() ?: return@forEach
      parts += if (attachment.isImage) {
        mapOf(
          "type" to "input_image",
          "image_url" to dataUrl
        )
      } else {
        mapOf(
          "type" to "input_file",
          "filename" to attachment.displayName,
          "file_data" to dataUrl
        )
      }
    }
  }
  val content: Any = parts.ifEmpty { return mapOf("role" to message.role.apiRole, "content" to message.content) }
  return mapOf(
    "role" to message.role.apiRole,
    "content" to content
  )
}

private fun toOpenAiImageInputMessage(message: ChatMessage): Map<String, Any> {
  val parts = mutableListOf<Map<String, Any>>()
  val prefix = when (message.role) {
    MessageRole.USER -> ""
    MessageRole.ASSISTANT -> "上一轮生成结果："
    MessageRole.SYSTEM -> "系统说明："
    MessageRole.TOOL -> "工具记录："
  }
  val text = (prefix + message.content).trim()
  if (text.isNotBlank()) {
    parts += mapOf("type" to "input_text", "text" to text)
  }
  message.attachments.filter { it.isImage }.forEach { attachment ->
    val dataUrl = attachment.toDataUrl() ?: return@forEach
    parts += mapOf("type" to "input_image", "image_url" to dataUrl)
  }
  if (parts.isEmpty()) {
    return mapOf("role" to "user", "content" to message.content)
  }
  return mapOf("role" to if (message.role == MessageRole.SYSTEM) "system" else "user", "content" to parts)
}

private fun extractGeneratedImages(json: String): List<GeneratedImagePayload> {
  return runCatching {
    val root = JsonParser.parseString(json)
    val results = mutableListOf<GeneratedImagePayload>()
    fun visit(element: JsonElement, revisedPrompt: String?) {
      when {
        element.isJsonObject -> {
          val obj = element.asJsonObject
          val type = obj.findString("type").orEmpty()
          val nextPrompt = obj.findString("revised_prompt") ?: obj.findString("revisedPrompt") ?: revisedPrompt
          val maybeImage = obj.findString("result")
            ?: obj.findString("b64_json")
            ?: obj.findString("image_base64")
          if (maybeImage != null && (type.contains("image", ignoreCase = true) || maybeImage.length > 500)) {
            results += GeneratedImagePayload(
              base64Data = maybeImage,
              mimeType = obj.findString("mime_type") ?: obj.findString("mimeType") ?: "image/png",
              revisedPrompt = nextPrompt
            )
          }
          obj.entrySet().forEach { visit(it.value, nextPrompt) }
        }
        element.isJsonArray -> element.asJsonArray.forEach { visit(it, revisedPrompt) }
      }
    }
    visit(root, null)
    results.distinctBy { it.base64Data.take(80) }
  }.getOrDefault(emptyList())
}

private fun extractImagesApiGeneratedImages(json: String, fallbackMimeType: String): List<GeneratedImagePayload> {
  return runCatching {
    val root = JsonParser.parseString(json)
    val data = root.asJsonObject.getAsJsonArray("data") ?: return@runCatching emptyList()
    data.mapNotNull { item ->
      val obj = item.asJsonObject
      val base64 = obj.findString("b64_json") ?: obj.findString("base64") ?: obj.findString("image_base64")
      base64?.let {
        GeneratedImagePayload(
          base64Data = it,
          mimeType = obj.findString("mime_type") ?: obj.findString("mimeType") ?: fallbackMimeType,
          revisedPrompt = obj.findString("revised_prompt") ?: obj.findString("revisedPrompt")
        )
      }
    }
  }.getOrDefault(emptyList())
}

private fun extractImagesApiUrls(json: String): List<String> {
  return runCatching {
    val root = JsonParser.parseString(json)
    val data = root.asJsonObject.getAsJsonArray("data") ?: return@runCatching emptyList()
    data.mapNotNull { item -> item.asJsonObject.findString("url") }
  }.getOrDefault(emptyList())
}

private fun ChatMessage.toCompatibleContent(): String {
  if (attachments.isEmpty()) return content
  val attachmentText = attachments.joinToString("\n") { attachment ->
    "- ${attachment.displayName} (${attachment.mimeType}, ${attachment.sizeBytes} bytes)"
  }
  return buildString {
    append(content)
    if (isNotBlank()) append("\n\n")
    append("Attachments are present but this provider adapter does not send binary attachment content yet:\n")
    append(attachmentText)
  }
}

private fun com.personal.aichat.domain.ChatAttachment.toDataUrl(): String? {
  val file = File(payloadLocalPath)
  if (!file.exists() || !file.isFile) return null
  val encoded = Base64.getEncoder().encodeToString(file.readBytes())
  return "data:$payloadMimeType;base64,$encoded"
}

fun defaultAiHttpClient(): OkHttpClient {
  ensureConscryptProvider()
  return OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .pingInterval(30, TimeUnit.SECONDS)
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
    MessageRole.TOOL -> "tool"
  }

private fun ChatProviderConfig.headersWithAuth(apiKey: String?, includeJsonContentType: Boolean = true): okhttp3.Headers {
  val builder = okhttp3.Headers.Builder()
  if (includeJsonContentType) {
    builder.add("Content-Type", "application/json")
  }
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
  onFrame: suspend (SseFrame) -> Unit
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
        onFrame(frame)
      }
    }
  }
}

internal fun Flow<ChatStreamEvent>.retrySilentTransportFailures(
  maxRetries: Long = 1,
  retryDelayMillis: Long = 750
): Flow<ChatStreamEvent> {
  var hasTextOutput = false
  return onEach { event ->
    if (event is ChatStreamEvent.TextDelta && event.text.isNotBlank()) {
      hasTextOutput = true
    }
  }.retryWhen { cause, attempt ->
    val shouldRetry = !hasTextOutput &&
      attempt < maxRetries &&
      cause.isRecoverableStreamingTransportFailure()
    if (shouldRetry && retryDelayMillis > 0) {
      delay(retryDelayMillis)
    }
    shouldRetry
  }
}

private fun Throwable.isRecoverableStreamingTransportFailure(): Boolean {
  val causes = generateSequence(this) { it.cause }.toList()
  if (causes.any { it is EOFException }) return true
  val details = causes.joinToString(" | ") { cause ->
    "${cause::class.java.simpleName}: ${cause.message.orEmpty()}"
  }
  return listOf(
    "broken pipe",
    "software caused connection abort",
    "connection reset",
    "socket closed",
    "unexpected end of stream",
    "stream was reset",
    "http/2 connection shutdown"
  ).any { marker -> details.contains(marker, ignoreCase = true) }
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

fun extractChatReasoningDelta(json: String): String? {
  return runCatching {
    val root = JsonParser.parseString(json).asJsonObject
    root.getAsJsonArray("choices")
      ?.firstOrNull()
      ?.asJsonObject
      ?.getAsJsonObject("delta")
      ?.let { delta ->
        delta.directString("reasoning_content")
          ?: delta.directString("reasoning")
      }
  }.getOrNull()
}

data class TokenUsage(
  val promptTokens: Int?,
  val completionTokens: Int?,
  val totalTokens: Int?,
  val raw: String
)

data class CompatibleToolCall(
  val id: String,
  val name: String,
  val arguments: String
) {
  fun toRequestMap(): Map<String, Any> = mapOf(
    "id" to id,
    "type" to "function",
    "function" to mapOf(
      "name" to name,
      "arguments" to arguments
    )
  )
}

data class CompatibleToolDecision(
  val toolCalls: List<CompatibleToolCall>,
  val content: String?,
  val usage: TokenUsage?,
  val reasoningContent: String? = null
)

fun extractTokenUsage(json: String): TokenUsage? {
  return runCatching {
    val root = JsonParser.parseString(json)
    val usage = findUsageObject(root) ?: return@runCatching null
    val prompt = usage.findInt("prompt_tokens") ?: usage.findInt("input_tokens")
    val completion = usage.findInt("completion_tokens") ?: usage.findInt("output_tokens")
    val total = usage.findInt("total_tokens") ?: listOfNotNull(prompt, completion).takeIf { it.size == 2 }?.sum()
    if (prompt == null && completion == null && total == null) {
      null
    } else {
      TokenUsage(
        promptTokens = prompt,
        completionTokens = completion,
        totalTokens = total,
        raw = usage.toString()
      )
    }
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

private fun JsonObject.findInt(name: String): Int? {
  if (has(name) && !get(name).isJsonNull) return runCatching { get(name).asInt }.getOrNull()
  entrySet().forEach { entry ->
    val value = entry.value
    if (value.isJsonObject) {
      val nested = value.asJsonObject.findInt(name)
      if (nested != null) return nested
    }
    if (value.isJsonArray) {
      value.asJsonArray.forEach { item ->
        if (item.isJsonObject) {
          val nested = item.asJsonObject.findInt(name)
          if (nested != null) return nested
        }
      }
    }
  }
  return null
}

private fun findUsageObject(element: JsonElement): JsonObject? {
  if (element.isJsonObject) {
    val obj = element.asJsonObject
    obj.get("usage")
      ?.takeIf { it.isJsonObject }
      ?.let { return it.asJsonObject }
    obj.entrySet().forEach { entry ->
      val nested = findUsageObject(entry.value)
      if (nested != null) return nested
    }
  } else if (element.isJsonArray) {
    element.asJsonArray.forEach { item ->
      val nested = findUsageObject(item)
      if (nested != null) return nested
    }
  }
  return null
}

private fun compatibleToolDefinitions(): List<Map<String, Any>> {
  return listOf(webSearchToolDefinition(), openPageToolDefinition(), webFetchToolDefinition())
}

private fun webSearchToolDefinition(): Map<String, Any> = functionToolDefinition(
  name = "web_search",
  description = "Search the web for current information relevant to the user's question.",
  properties = mapOf(
    "query" to mapOf(
      "type" to "string",
      "description" to "A concise search query."
    )
  ),
  required = listOf("query")
)

private fun openPageToolDefinition(): Map<String, Any> = functionToolDefinition(
  name = "open",
  description = "Open a specific web page URL returned by web_search and extract readable page text.",
  properties = mapOf(
    "url" to mapOf(
      "type" to "string",
      "description" to "The absolute http or https URL to open."
    )
  ),
  required = listOf("url")
)

private fun webFetchToolDefinition(): Map<String, Any> = functionToolDefinition(
  name = "web_fetch",
  description = "Fetch a specific web page URL returned by web_search and extract readable page text.",
  properties = mapOf(
    "url" to mapOf(
      "type" to "string",
      "description" to "The absolute http or https URL to fetch."
    )
  ),
  required = listOf("url")
)

private fun functionToolDefinition(
  name: String,
  description: String,
  properties: Map<String, Any>,
  required: List<String>
): Map<String, Any> {
  return mapOf(
    "type" to "function",
    "function" to mapOf(
      "name" to name,
      "description" to description,
      "parameters" to mapOf(
        "type" to "object",
        "properties" to properties,
        "required" to required
      )
    )
  )
}

fun extractCompatibleToolCalls(json: String): List<CompatibleToolCall> {
  return extractCompatibleToolDecision(json).toolCalls
}

fun extractCompatibleToolDecision(json: String): CompatibleToolDecision {
  return runCatching {
    val root = JsonParser.parseString(json).asJsonObject
    val firstMessage = root.getAsJsonArray("choices")
      ?.firstOrNull()
      ?.asJsonObject
      ?.getAsJsonObject("message")
    val calls = root.getAsJsonArray("choices")
      ?.flatMap { choice ->
        val message = choice.asJsonObject.getAsJsonObject("message")
        message?.getAsJsonArray("tool_calls")?.mapNotNull { item ->
          val tool = item.asJsonObject
          val function = tool.getAsJsonObject("function") ?: return@mapNotNull null
          val name = normalizeCompatibleToolName(function.findString("name") ?: return@mapNotNull null)
            ?: return@mapNotNull null
          CompatibleToolCall(
            id = tool.findString("id") ?: return@mapNotNull null,
            name = name,
            arguments = function.findString("arguments") ?: "{}"
          )
        } ?: emptyList()
      }
      .orEmpty()
    CompatibleToolDecision(
      toolCalls = calls,
      content = firstMessage?.findString("content"),
      usage = extractTokenUsage(json),
      reasoningContent = firstMessage?.directString("reasoning_content")
        ?: firstMessage?.directString("reasoning")
    )
  }.getOrDefault(CompatibleToolDecision(emptyList(), null, null))
}

fun parseToolQuery(arguments: String): String? {
  return runCatching {
    JsonParser.parseString(arguments).asJsonObject.findString("query")
  }.getOrNull()?.trim()
}

fun parseToolUrl(arguments: String): String? {
  return runCatching {
    JsonParser.parseString(arguments).asJsonObject.findString("url")
  }.getOrNull()?.trim()
}

fun extractCompatibleMarkupToolCall(text: String): CompatibleToolCall? {
  return extractCompatibleMarkupToolCalls(text).firstOrNull()
}

fun extractCompatibleMarkupToolCalls(text: String): List<CompatibleToolCall> {
  val normalized = normalizeCompatibleMarkup(text)
  return Regex(
    """(?is)<\s*invoke\s+name\s*=\s*["']([^"']+)["']\s*>(.*?)</\s*invoke\s*>"""
  ).findAll(normalized).mapIndexedNotNull { index, match ->
    val name = normalizeCompatibleToolName(match.groupValues[1])
      ?: return@mapIndexedNotNull null
    val body = match.groupValues[2]
    val args = mutableMapOf<String, String>()
    Regex(
      """(?is)<\s*parameter\s+name\s*=\s*["']([^"']+)["'][^>]*>(.*?)</\s*parameter\s*>"""
    ).findAll(body).forEach { parameterMatch ->
      args[parameterMatch.groupValues[1].trim()] = decodeXmlText(parameterMatch.groupValues[2].trim())
    }
    if (args.isEmpty()) return@mapIndexedNotNull null
    CompatibleToolCall(
      id = "markup_${System.nanoTime()}_$index",
      name = name,
      arguments = Gson().toJson(args)
    )
  }.toList()
}

private fun normalizeCompatibleMarkup(text: String): String {
  return text
    .replace("\uFF5C\uFF5CDSML\uFF5C\uFF5C", "")
    .replace("｜｜DSML｜｜", "")
    .replace("锝滐綔DSML锝滐綔", "")
    .trim()
}

private val CompatibleToolNames = setOf("web_search", "open", "open_page", "web_fetch")

private fun normalizeCompatibleToolName(name: String): String? {
  val normalized = name.trim().lowercase()
  return when (normalized) {
    "open_url", "open_url_page" -> "open"
    in CompatibleToolNames -> normalized
    else -> null
  }
}

private fun looksLikeCompatibleToolMarkup(text: String): Boolean {
  val normalized = normalizeCompatibleMarkup(text)
  return normalized.startsWith("<")
}

private fun decodeXmlText(value: String): String {
  return value
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
}

/*
fun extractCompatibleMarkupToolCall(text: String): CompatibleToolCall? {
  val normalized = normalizeCompatibleMarkup(text)
  val invoke = Regex("""<\s*invoke\s+name\s*=\s*["']([^"']+)["']\s*>""", RegexOption.IGNORE_CASE)
    .find(normalized) ?: return null
  val name = invoke.groupValues[1].trim()
  if (name !in CompatibleToolNames) return null
  val body = normalized.substring(invoke.range.last + 1)
  val args = mutableMapOf<String, String>()
  Regex(
    """(?is)<\s*parameter\s+name\s*=\s*["']([^"']+)["'][^>]*>(.*?)</\s*parameter\s*>"""
  ).findAll(body).forEach { match ->
    args[match.groupValues[1].trim()] = decodeXmlText(match.groupValues[2].trim())
  }
  if (args.isEmpty()) return null
  return CompatibleToolCall(
    id = "markup_${System.nanoTime()}",
    name = name,
    arguments = Gson().toJson(args)
  )
}

private val CompatibleToolNames = setOf("web_search", "open", "open_page")

private fun looksLikeCompatibleToolMarkup(text: String): Boolean {
  val normalized = normalizeCompatibleMarkup(text)
  return normalized.startsWith("<") &&
    ("<tool_calls" in normalized || "<invoke" in normalized || "<parameter" in normalized)
}

private fun normalizeCompatibleMarkup(text: String): String {
  return text.replace("｜｜DSML｜｜", "").trim()
}

private fun decodeXmlText(value: String): String {
  return value
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
}
*/

fun extractResponseToolCall(event: String?, json: String): ChatStreamEvent.ToolCall? {
  return runCatching {
    val root = JsonParser.parseString(json).asJsonObject
    val item = root.getAsJsonObject("item") ?: root
    val type = item.findString("type").orEmpty()
    val eventName = event.orEmpty()
    val isWebSearch = type.contains("web_search", ignoreCase = true) ||
      eventName.contains("web_search", ignoreCase = true) ||
      json.contains("web_search_call", ignoreCase = true)
    if (!isWebSearch) return@runCatching null
    val status = item.findString("status") ?: root.findString("status") ?: eventName.substringAfterLast('.', "")
    val citations = extractOpenAiUrlCitations(json)
    val output = if (status.equals("searching", ignoreCase = true) || status.equals("in_progress", ignoreCase = true)) {
      null
    } else if (citations.isNotEmpty()) {
      formatUrlCitations(citations.associate { it.url to it.title })
    } else {
      null
    }
    ChatStreamEvent.ToolCall(
      id = item.findString("id") ?: root.findString("item_id") ?: "openai-web-search",
      name = "web_search",
      input = item.findString("query") ?: item.findString("action") ?: eventName.takeIf { it.isNotBlank() },
      output = output
    )
  }.getOrNull()
}

data class OpenAiWebSearchUpdate(
  val queries: List<String>,
  val urls: List<OpenAiUrlCitation>
)

fun extractOpenAiWebSearchUpdate(event: String?, json: String): OpenAiWebSearchUpdate? {
  return runCatching {
    val root = JsonParser.parseString(json).asJsonObject
    val item = root.getAsJsonObject("item") ?: root
    val type = item.findString("type").orEmpty()
    val eventName = event.orEmpty()
    val isWebSearch = type.contains("web_search", ignoreCase = true) ||
      eventName.contains("web_search", ignoreCase = true) ||
      json.contains("web_search_call", ignoreCase = true)
    if (!isWebSearch) return@runCatching null
    val action = item.getAsJsonObject("action") ?: root.getAsJsonObject("action")
    val queries = action?.getAsJsonArray("queries")
      ?.mapNotNull { query -> runCatching { query.asString.trim() }.getOrNull() }
      ?.filter { it.isNotBlank() }
      .orEmpty()
    val urls = buildList {
      addAll(extractOpenAiUrlCitations(json))
      action?.directString("url")?.trimUrlPunctuation()?.takeIf { it.isNotBlank() }?.let { url ->
        add(OpenAiUrlCitation(url = url, title = action.directString("title") ?: url))
      }
    }.distinctBy { it.url }
    OpenAiWebSearchUpdate(queries = queries, urls = urls)
  }.getOrNull()
}

data class OpenAiUrlCitation(
  val url: String,
  val title: String
)

fun extractOpenAiUrlCitations(json: String): List<OpenAiUrlCitation> {
  return runCatching {
    val root = JsonParser.parseString(json)
    collectOpenAiUrlCitations(root)
      .distinctBy { it.url }
  }.getOrDefault(emptyList())
}

fun extractUrlCitationsFromText(text: String): List<OpenAiUrlCitation> {
  return PlainUrlRegex.findAll(text)
    .mapNotNull { match ->
      val url = match.value.trimUrlPunctuation()
      if (url.isBlank()) {
        null
      } else {
        OpenAiUrlCitation(url = url, title = url)
      }
    }
    .distinctBy { it.url }
    .toList()
}

private fun collectOpenAiUrlCitations(element: JsonElement): List<OpenAiUrlCitation> {
  val citations = mutableListOf<OpenAiUrlCitation>()
  if (element.isJsonObject) {
    val obj = element.asJsonObject
    val type = obj.findString("type").orEmpty()
    val url = obj.directString("url")
    if (url != null && (type.contains("url_citation", ignoreCase = true) || obj.has("title"))) {
      citations += OpenAiUrlCitation(
        url = url,
        title = obj.directString("title") ?: url
      )
    }
    obj.entrySet().forEach { entry ->
      citations += collectOpenAiUrlCitations(entry.value)
    }
  } else if (element.isJsonArray) {
    element.asJsonArray.forEach { item ->
      citations += collectOpenAiUrlCitations(item)
    }
  }
  return citations
}

private fun String.trimUrlPunctuation(): String {
  return trimEnd(
    '.', ',', ';', ':', ')', ']', '}', '>',
    '\u3002', '\uFF0C', '\uFF1B', '\uFF1A', '\uFF09'
  )
}

private fun JsonObject.directString(name: String): String? {
  return if (has(name) && !get(name).isJsonNull) {
    runCatching { get(name).asString }.getOrNull()
  } else {
    null
  }
}

private val PlainUrlRegex = Regex("https?://[^\\s<>\"'`\\]\\)\\}]+")

private fun formatUrlCitations(citations: Map<String, String>): String {
  return citations.entries.joinToString("\n") { (url, title) ->
    if (title.isBlank() || title == url) url else "$title\n$url"
  }
}

private fun formatWebSearchOutput(
  queries: Set<String>,
  citations: Map<String, String>
): String {
  return buildString {
    if (queries.isNotEmpty()) {
      appendLine("查询：")
      queries.forEach { appendLine(it) }
    }
    if (citations.isNotEmpty()) {
      if (isNotEmpty()) appendLine()
      appendLine("网址：")
      append(formatUrlCitations(citations))
    }
  }.trim()
}
