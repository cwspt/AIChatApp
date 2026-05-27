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
  SYSTEM
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
  val errorMessage: String?
)

data class ChatCompletionOptions(
  val model: String,
  val stream: Boolean = true
)

sealed interface ChatStreamEvent {
  data object Started : ChatStreamEvent
  data class TextDelta(val text: String) : ChatStreamEvent
  data object Completed : ChatStreamEvent
  data class Failed(val message: String) : ChatStreamEvent
}
