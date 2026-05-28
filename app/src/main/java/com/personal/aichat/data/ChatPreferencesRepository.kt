package com.personal.aichat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.aichat.domain.AppSettings
import com.personal.aichat.domain.AppThemeMode
import com.personal.aichat.domain.AppThemePalette
import com.personal.aichat.domain.WebSearchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatPreferencesDataStore by preferencesDataStore("chat_preferences")

interface ChatSelectionStore {
  val selectedProviderId: Flow<String?>
  val selectedConversationId: Flow<String?>
  val appSettings: Flow<AppSettings>
  suspend fun setSelectedProvider(id: String)
  suspend fun setSelectedConversation(id: String)
  suspend fun setThemePalette(palette: AppThemePalette)
  suspend fun setThemeMode(mode: AppThemeMode)
  suspend fun setFontScale(scale: Float)
  suspend fun setDebugResponseLogging(enabled: Boolean)
  suspend fun setWebSearchMode(mode: WebSearchMode)
}

class ChatPreferencesRepository(private val context: Context) : ChatSelectionStore {
  private val selectedProviderKey = stringPreferencesKey("selected_provider_id")
  private val selectedConversationKey = stringPreferencesKey("selected_conversation_id")
  private val themePaletteKey = stringPreferencesKey("theme_palette")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val fontScaleKey = floatPreferencesKey("font_scale")
  private val debugResponseLoggingKey = booleanPreferencesKey("debug_response_logging")
  private val webSearchModeKey = stringPreferencesKey("web_search_mode")

  override val selectedProviderId: Flow<String?> = context.chatPreferencesDataStore.data.map { preferences ->
    preferences[selectedProviderKey]
  }

  override val selectedConversationId: Flow<String?> = context.chatPreferencesDataStore.data.map { preferences ->
    preferences[selectedConversationKey]
  }

  override val appSettings: Flow<AppSettings> = context.chatPreferencesDataStore.data.map { preferences ->
    AppSettings(
      palette = preferences[themePaletteKey]
        ?.let { runCatching { AppThemePalette.valueOf(it) }.getOrNull() }
        ?: AppThemePalette.MOSS,
      themeMode = preferences[themeModeKey]
        ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
        ?: AppThemeMode.LIGHT,
      fontScale = (preferences[fontScaleKey] ?: 1.0f).coerceIn(0.85f, 1.25f),
      debugResponseLogging = preferences[debugResponseLoggingKey] ?: false,
      webSearchMode = preferences[webSearchModeKey]
        ?.let { runCatching { WebSearchMode.valueOf(it) }.getOrNull() }
        ?: WebSearchMode.OFF
    )
  }

  override suspend fun setSelectedProvider(id: String) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[selectedProviderKey] = id
    }
  }

  override suspend fun setSelectedConversation(id: String) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[selectedConversationKey] = id
    }
  }

  override suspend fun setThemePalette(palette: AppThemePalette) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[themePaletteKey] = palette.name
    }
  }

  override suspend fun setThemeMode(mode: AppThemeMode) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[themeModeKey] = mode.name
    }
  }

  override suspend fun setFontScale(scale: Float) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[fontScaleKey] = scale.coerceIn(0.85f, 1.25f)
    }
  }

  override suspend fun setDebugResponseLogging(enabled: Boolean) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[debugResponseLoggingKey] = enabled
    }
  }

  override suspend fun setWebSearchMode(mode: WebSearchMode) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[webSearchModeKey] = mode.name
    }
  }
}
