package com.personal.aichat.data.security

interface ApiKeyStore {
  fun read(secretRef: String?): String?
  fun write(secretRef: String, apiKey: String)
  fun delete(secretRef: String)
}
