package com.personal.aichat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatPreferencesDataStore by preferencesDataStore("chat_preferences")

class ChatPreferencesRepository(private val context: Context) {
  private val selectedProviderKey = stringPreferencesKey("selected_provider_id")
  private val selectedConversationKey = stringPreferencesKey("selected_conversation_id")

  val selectedProviderId: Flow<String?> = context.chatPreferencesDataStore.data.map { preferences ->
    preferences[selectedProviderKey]
  }

  val selectedConversationId: Flow<String?> = context.chatPreferencesDataStore.data.map { preferences ->
    preferences[selectedConversationKey]
  }

  suspend fun setSelectedProvider(id: String) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[selectedProviderKey] = id
    }
  }

  suspend fun setSelectedConversation(id: String) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[selectedConversationKey] = id
    }
  }
}
