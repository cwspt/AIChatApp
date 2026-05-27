package com.personal.aichat.domain

import kotlinx.coroutines.flow.Flow

interface ProviderAdapter {
  fun streamChat(
    config: ChatProviderConfig,
    apiKey: String?,
    messages: List<ChatMessage>,
    options: ChatCompletionOptions
  ): Flow<ChatStreamEvent>
}
