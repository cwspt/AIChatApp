package com.personal.aichat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ProviderAdapter {
  fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent>

  fun generateImages(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ImageGenerationOptions
  ): Flow<ChatStreamEvent> = flowOf(ChatStreamEvent.Failed("当前 Provider 不支持图片生成"))
}
