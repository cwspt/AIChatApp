package com.personal.aichat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.aichat.data.ChatPreferencesRepository
import com.personal.aichat.data.ChatRepository
import com.personal.aichat.domain.ChatProviderConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
  private val repository: ChatRepository,
  private val preferencesRepository: ChatPreferencesRepository
) : ViewModel() {
  private val localState = MutableStateFlow(ChatUiState())
  private var sendJob: Job? = null

  @OptIn(ExperimentalCoroutinesApi::class)
  val uiState = combine(
    repository.providers,
    repository.conversations,
    preferencesRepository.selectedConversationId,
    preferencesRepository.selectedProviderId,
    localState
  ) { providers, conversations, selectedConversationId, selectedProviderId, local ->
    val effectiveConversationId = selectedConversationId ?: conversations.firstOrNull()?.id
    val effectiveProviderId = selectedProviderId
      ?: conversations.firstOrNull { it.id == effectiveConversationId }?.providerId
      ?: providers.firstOrNull()?.id
    local.copy(
      providers = providers,
      conversations = conversations,
      selectedConversationId = effectiveConversationId,
      selectedProviderId = effectiveProviderId
    )
  }.flatMapLatest { state ->
    val conversationId = state.selectedConversationId
    if (conversationId == null) {
      flowOf(state)
    } else {
      repository.observeMessages(conversationId).combine(flowOf(state)) { messages, current ->
        current.copy(messages = messages)
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

  init {
    viewModelScope.launch {
      repository.bootstrapDefaults()
      val conversation = repository.ensureConversation()
      preferencesRepository.setSelectedConversation(conversation.id)
      preferencesRepository.setSelectedProvider(conversation.providerId)
    }
  }

  fun updateInput(value: String) {
    localState.update { it.copy(input = value) }
  }

  fun send() {
    val state = uiState.value
    val conversationId = state.selectedConversationId ?: return
    val text = state.input
    localState.update { it.copy(input = "") }
    sendJob = viewModelScope.launch {
      repository.sendMessage(conversationId, text)
    }
  }

  fun retryLast() {
    val conversationId = uiState.value.selectedConversationId ?: return
    sendJob = viewModelScope.launch {
      repository.retryLast(conversationId)
    }
  }

  fun selectConversation(id: String) {
    viewModelScope.launch {
      preferencesRepository.setSelectedConversation(id)
    }
  }

  fun selectProvider(id: String) {
    viewModelScope.launch {
      preferencesRepository.setSelectedProvider(id)
    }
  }

  fun newConversation() {
    val provider = uiState.value.selectedProvider ?: return
    viewModelScope.launch {
      val conversation = repository.createConversation(provider.id, provider.defaultModel)
      preferencesRepository.setSelectedProvider(provider.id)
      preferencesRepository.setSelectedConversation(conversation.id)
    }
  }

  fun openSettings(provider: ChatProviderConfig? = uiState.value.selectedProvider) {
    localState.update { it.copy(settingsOpen = true, editingProvider = provider) }
  }

  fun closeSettings() {
    localState.update { it.copy(settingsOpen = false, editingProvider = null) }
  }

  fun saveProvider(provider: ChatProviderConfig, apiKey: String?) {
    viewModelScope.launch {
      repository.saveProvider(provider, apiKey)
      preferencesRepository.setSelectedProvider(provider.id)
      closeSettings()
    }
  }

  companion object {
    fun factory(
      repository: ChatRepository,
      preferencesRepository: ChatPreferencesRepository
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(repository, preferencesRepository) as T
      }
    }
  }
}
