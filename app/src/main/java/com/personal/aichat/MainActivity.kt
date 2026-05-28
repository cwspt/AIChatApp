package com.personal.aichat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
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
    requestNotificationPermissionIfNeeded()
    setContent {
      val settings by preferencesRepository.appSettings.collectAsState(com.personal.aichat.domain.AppSettings())
      AIChatTheme(settings = settings) {
        val chatViewModel: ChatViewModel = viewModel(
          factory = ChatViewModel.factory(
            repository = chatRepository,
            preferencesRepository = preferencesRepository,
            appContext = applicationContext
          )
        )
        AIChatAppRoot(viewModel = chatViewModel)
      }
    }
  }

  override fun onStart() {
    super.onStart()
    AppForegroundTracker.isForeground = true
  }

  override fun onStop() {
    AppForegroundTracker.isForeground = false
    super.onStop()
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
  }
}
