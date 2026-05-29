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
import com.personal.aichat.domain.ChatBackgroundPreset
import com.personal.aichat.domain.WebSearchMode
import com.personal.aichat.domain.defaultBackgroundPresets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
  suspend fun setBackgroundPresets(presets: List<ChatBackgroundPreset>)
}

class ChatPreferencesRepository(private val context: Context) : ChatSelectionStore {
  private val gson = Gson()
  private val backgroundPresetListType = object : TypeToken<List<ChatBackgroundPreset>>() {}.type
  private val selectedProviderKey = stringPreferencesKey("selected_provider_id")
  private val selectedConversationKey = stringPreferencesKey("selected_conversation_id")
  private val themePaletteKey = stringPreferencesKey("theme_palette")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val fontScaleKey = floatPreferencesKey("font_scale")
  private val debugResponseLoggingKey = booleanPreferencesKey("debug_response_logging")
  private val webSearchModeKey = stringPreferencesKey("web_search_mode")
  private val backgroundPresetsKey = stringPreferencesKey("background_presets_json")

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
        ?: WebSearchMode.OFF,
      backgroundPresets = parseBackgroundPresets(preferences[backgroundPresetsKey])
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

  override suspend fun setBackgroundPresets(presets: List<ChatBackgroundPreset>) {
    context.chatPreferencesDataStore.edit { preferences ->
      preferences[backgroundPresetsKey] = gson.toJson(normalizeBackgroundPresets(presets))
    }
  }

  private fun parseBackgroundPresets(json: String?): List<ChatBackgroundPreset> {
    if (json.isNullOrBlank()) return defaultBackgroundPresets()
    return runCatching {
      gson.fromJson<List<ChatBackgroundPreset>>(json, backgroundPresetListType)
    }.getOrNull()
      ?.let(::normalizeBackgroundPresets)
      ?.takeIf { it.isNotEmpty() }
      ?: defaultBackgroundPresets()
  }

  private fun normalizeBackgroundPresets(presets: List<ChatBackgroundPreset>): List<ChatBackgroundPreset> {
    return presets
      .filter { it.title.isNotBlank() || it.content.isNotBlank() }
      .sortedWith(compareBy<ChatBackgroundPreset> { it.sortOrder }.thenBy { it.createdAt }.thenBy { it.title })
      .mapIndexed { index, preset ->
        preset.copy(
          title = preset.title.trim().ifBlank { "未命名背景" },
          content = preset.content.trim(),
          sortOrder = index
        )
      }
  }
}
