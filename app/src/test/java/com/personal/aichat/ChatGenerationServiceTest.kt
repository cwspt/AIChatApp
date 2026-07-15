package com.personal.aichat

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.personal.aichat.ui.isStreamingConnectionInterruption
import com.personal.aichat.ui.requestUnrestrictedBackgroundGeneration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatGenerationServiceTest {
  @Test
  fun backgroundGenerationRequestTargetsCurrentPackage() {
    val application = ApplicationProvider.getApplicationContext<Application>()

    requestUnrestrictedBackgroundGeneration(application)

    val intent = Shadows.shadowOf(application).nextStartedActivity
    assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
    assertEquals("package:${application.packageName}", intent.dataString)
  }

  @Test
  fun streamingConnectionErrorShowsBackgroundPermissionAction() {
    assertTrue(isStreamingConnectionInterruption("流式连接意外中断。请重试"))
    assertFalse(isStreamingConnectionInterruption("HTTP 401"))
  }

  @Test
  fun generationWakeLockIsHeldOnlyWhileServiceIsActive() {
    val controller = Robolectric.buildService(ChatGenerationService::class.java).create()
    val service = controller.get()

    service.onStartCommand(Intent(service, ChatGenerationService::class.java), 0, 1)

    assertTrue(service.isCpuWakeLockHeld)
    assertTrue(service.isWifiLockHeld)

    controller.destroy()

    assertFalse(service.isCpuWakeLockHeld)
    assertFalse(service.isWifiLockHeld)
  }
}
