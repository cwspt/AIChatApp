package com.personal.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.aichat.data.ChatPreferencesRepository
import com.personal.aichat.data.ChatRepository
import com.personal.aichat.data.local.ChatDatabase
import com.personal.aichat.data.security.EncryptedApiKeyStore
import com.personal.aichat.ui.AIChatAppRoot
import com.personal.aichat.ui.ChatViewModel
import com.personal.aichat.ui.theme.AIChatTheme

class MainActivity : ComponentActivity() {
  private val database by lazy { ChatDatabase.getInstance(applicationContext) }
  private val preferencesRepository by lazy { ChatPreferencesRepository(applicationContext) }
  private val apiKeyStore by lazy { EncryptedApiKeyStore(applicationContext) }
  private val chatRepository by lazy {
    ChatRepository(
      dao = database.chatDao(),
      preferencesRepository = preferencesRepository,
      apiKeyStore = apiKeyStore
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AIChatTheme {
        val chatViewModel: ChatViewModel = viewModel(
          factory = ChatViewModel.factory(
            repository = chatRepository,
            preferencesRepository = preferencesRepository
          )
        )
        AIChatAppRoot(viewModel = chatViewModel)
      }
    }
  }
}
