package com.personal.aichat.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedApiKeyStore(context: Context) : ApiKeyStore {
  private val preferences by lazy {
    val masterKey = MasterKey.Builder(context.applicationContext)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()
    EncryptedSharedPreferences.create(
      context.applicationContext,
      "ai_provider_secrets",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  override fun read(secretRef: String?): String? {
    if (secretRef.isNullOrBlank()) return null
    return preferences.getString(secretRef, null)?.takeIf { it.isNotBlank() }
  }

  override fun write(secretRef: String, apiKey: String) {
    preferences.edit().putString(secretRef, apiKey).apply()
  }

  override fun delete(secretRef: String) {
    preferences.edit().remove(secretRef).apply()
  }
}
