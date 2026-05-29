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

enum class ConversationType {
  CHAT,
  IMAGE
}

enum class ImageGenerationSize(val apiValue: String) {
  AUTO("auto"),
  SQUARE("1024x1024"),
  LANDSCAPE("1536x1024"),
  PORTRAIT("1024x1536")
}

enum class ImageGenerationQuality(val apiValue: String) {
  AUTO("auto"),
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high")
}

enum class ImageGenerationApiMode {
  RESPONSES_TOOL,
  IMAGES_API
}

enum class ImageGenerationOutputFormat(val apiValue: String, val mimeType: String) {
  PNG("png", "image/png"),
  JPEG("jpeg", "image/jpeg"),
  WEBP("webp", "image/webp")
}

enum class ImageGenerationBackground(val apiValue: String) {
  AUTO("auto"),
  OPAQUE("opaque"),
  TRANSPARENT("transparent")
}

data class ImageGenerationOptions(
  val size: ImageGenerationSize = ImageGenerationSize.AUTO,
  val quality: ImageGenerationQuality = ImageGenerationQuality.AUTO,
  val count: Int = 1,
  val outputFormat: ImageGenerationOutputFormat = ImageGenerationOutputFormat.PNG,
  val background: ImageGenerationBackground = ImageGenerationBackground.AUTO,
  val captureRawResponseLog: Boolean = false
)

data class ChatProviderConfig(
  val id: String,
  val displayName: String,
  val type: ProviderType,
  val baseUrl: String,
  val defaultModel: String,
  val enabled: Boolean,
  val supportsStreaming: Boolean,
  val supportsAttachments: Boolean = false,
  val supportsImageGeneration: Boolean = false,
  val imageGenerationApiMode: ImageGenerationApiMode = ImageGenerationApiMode.RESPONSES_TOOL,
  val imageGenerationModel: String = "",
  val extraHeadersJson: String,
  val secretRef: String?,
  val reasoningEffort: ReasoningEffort = ReasoningEffort.AUTO
)

data class ChatConversation(
  val id: String,
  val title: String,
  val providerId: String,
  val model: String,
  val type: ConversationType = ConversationType.CHAT,
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

data class FavoriteSnippet(
  val id: String,
  val title: String,
  val description: String,
  val tags: List<String>,
  val messages: List<FavoriteSnippetMessage>,
  val searchText: String,
  val sourceConversationId: String,
  val sourceConversationTitle: String,
  val sourceProviderId: String?,
  val sourceProviderName: String?,
  val sourceModel: String?,
  val sourceGroupName: String?,
  val sourceFirstMessageId: String?,
  val sourceLastMessageId: String?,
  val messageCount: Int,
  val createdAt: Long,
  val updatedAt: Long
)

data class FavoriteSnippetMessage(
  val id: String,
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
  val attachments: List<ChatAttachment> = emptyList()
)

enum class GroupMessageSenderType {
  USER,
  BOT,
  SYSTEM,
  TOOL
}

enum class GroupTurnTrigger {
  MANUAL,
  AUTO,
  SUMMARY,
  UNKNOWN
}

data class AiBot(
  val id: String,
  val name: String,
  val providerId: String,
  val model: String,
  val systemPrompt: String,
  val bubbleColorKey: String = "AUTO",
  val enabled: Boolean,
  val createdAt: Long,
  val updatedAt: Long
)

data class GroupChatRoom(
  val id: String,
  val title: String,
  val topic: String,
  val summary: String,
  val createdAt: Long,
  val updatedAt: Long,
  val isArchived: Boolean,
  val isDeleted: Boolean
)

data class GroupChatMember(
  val groupId: String,
  val botId: String,
  val sortOrder: Int,
  val enabled: Boolean,
  val createdAt: Long,
  val updatedAt: Long
)

data class GroupChatMessage(
  val id: String,
  val groupId: String,
  val senderType: GroupMessageSenderType,
  val botId: String?,
  val senderName: String,
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
  val attachments: List<ChatAttachment> = emptyList(),
  val turnTrigger: GroupTurnTrigger = GroupTurnTrigger.UNKNOWN,
  val turnRound: Int? = null,
  val turnIndex: Int? = null,
  val turnMemberCount: Int? = null
)

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
  data class ImageGenerated(
    val base64Data: String,
    val mimeType: String = "image/png",
    val revisedPrompt: String? = null
  ) : ChatStreamEvent
  data object Completed : ChatStreamEvent
  data class Failed(val message: String) : ChatStreamEvent
}
