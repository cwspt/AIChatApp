package com.personal.aichat

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatGenerationServiceTest {
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
