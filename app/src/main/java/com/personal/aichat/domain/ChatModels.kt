package com.personal.aichat.domain

enum class ProviderType {
  OPENAI_RESPONSES,
  OPENAI_COMPATIBLE_CHAT,
  TOKENHUB_PROXY,
  ANTHROPIC_MESSAGES,
  GEMINI_GENERATE_CONTENT
}

enum class ReasoningEffort(val apiValue: String?) {
  AUTO(null),
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  XHIGH("xhigh")
}

enum class MessageRole {
  USER,
  ASSISTANT,
  SYSTEM,
  TOOL
}

enum class MessageStatus {
  COMPLETE,
  STREAMING,
  FAILED
}

data class ChatProviderConfig(
  val id: String,
  val displayName: String,
  val type: ProviderType,
  val baseUrl: String,
  val defaultModel: String,
  val enabled: Boolean,
  val supportsStreaming: Boolean,
  val supportsAttachments: Boolean = false,
  val extraHeadersJson: String,
  val secretRef: String?,
  val reasoningEffort: ReasoningEffort = ReasoningEffort.AUTO
)

data class ChatConversation(
  val id: String,
  val title: String,
  val providerId: String,
  val model: String,
  val groupName: String,
  val forkedFromConversationId: String?,
  val forkedFromMessageId: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val isArchived: Boolean,
  val isDeleted: Boolean,
  val isPinned: Boolean
)

data class ChatConversationGroup(
  val name: String,
  val conversations: List<ChatConversation>
)

data class ChatMessage(
  val id: String,
  val conversationId: String,
  val role: MessageRole,
  val content: String,
  val status: MessageStatus,
  val providerId: String?,
  val model: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val errorMessage: String?,
  val totalDurationMs: Long? = null,
  val firstTokenDurationMs: Long? = null,
  val promptTokens: Int? = null,
  val completionTokens: Int? = null,
  val totalTokens: Int? = null,
  val rawResponseLog: String? = null,
  val attachments: List<ChatAttachment> = emptyList()
)

data class ChatAttachment(
  val id: String,
  val displayName: String,
  val mimeType: String,
  val sizeBytes: Long,
  val localPath: String
) {
  val isImage: Boolean
    get() = mimeType.startsWith("image/")
}

data class ChatCompletionOptions(
  val model: String,
  val stream: Boolean = true,
  val captureRawResponseLog: Boolean = false,
  val webSearchMode: WebSearchMode = WebSearchMode.OFF
)

sealed interface ChatStreamEvent {
  data object Started : ChatStreamEvent
  data class TextDelta(val text: String) : ChatStreamEvent
  data class ToolCall(
    val id: String? = null,
    val name: String,
    val input: String? = null,
    val output: String? = null
  ) : ChatStreamEvent
  data class Usage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val raw: String? = null
  ) : ChatStreamEvent
  data class RawFrame(val event: String?, val data: String) : ChatStreamEvent
  data object Completed : ChatStreamEvent
  data class Failed(val message: String) : ChatStreamEvent
}
