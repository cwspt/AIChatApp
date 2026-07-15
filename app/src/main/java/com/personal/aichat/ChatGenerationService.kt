package com.personal.aichat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ChatGenerationService : Service() {
  private var generationWakeLock: PowerManager.WakeLock? = null
  private var generationWifiLock: WifiManager.WifiLock? = null
  private val lockTimeoutHandler = Handler(Looper.getMainLooper())
  private val releaseLocksAfterTimeout = Runnable(::releaseGenerationLocks)

  internal val isCpuWakeLockHeld: Boolean
    get() = generationWakeLock?.isHeld == true
  internal val isWifiLockHeld: Boolean
    get() = generationWifiLock?.isHeld == true

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ensureChannel()
    when (intent?.action) {
      ActionStop -> {
        releaseGenerationLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
      ActionComplete -> {
        releaseGenerationLocks()
        showCompletedNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
      else -> {
        startForeground(OngoingNotificationId, ongoingNotification())
        acquireGenerationLocks()
      }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    releaseGenerationLocks()
    super.onDestroy()
  }

  @Suppress("DEPRECATION")
  private fun acquireGenerationLocks() {
    lockTimeoutHandler.removeCallbacks(releaseLocksAfterTimeout)
    lockTimeoutHandler.postDelayed(releaseLocksAfterTimeout, MaxWakeLockDurationMs)
    val wakeLock = generationWakeLock ?: getSystemService(PowerManager::class.java)
      .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:ChatGeneration")
      .apply { setReferenceCounted(false) }
      .also { generationWakeLock = it }
    if (!wakeLock.isHeld) {
      wakeLock.acquire(MaxWakeLockDurationMs)
    }

    val wifiLock = generationWifiLock ?: getSystemService(WifiManager::class.java)
      .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:ChatGenerationWifi")
      .apply { setReferenceCounted(false) }
      .also { generationWifiLock = it }
    if (!wifiLock.isHeld) {
      wifiLock.acquire()
    }
  }

  private fun releaseGenerationLocks() {
    lockTimeoutHandler.removeCallbacks(releaseLocksAfterTimeout)
    generationWifiLock?.takeIf { it.isHeld }?.release()
    generationWifiLock = null
    generationWakeLock?.takeIf { it.isHeld }?.release()
    generationWakeLock = null
  }

  private fun ongoingNotification() = NotificationCompat.Builder(this, ChannelId)
    .setSmallIcon(R.mipmap.ic_launcher)
    .setContentTitle("AI 正在回复")
    .setContentText("回复完成前会尽量保持后台任务运行")
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setContentIntent(openAppIntent())
    .build()

  private fun showCompletedNotification() {
    val notification = NotificationCompat.Builder(this, ChannelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("AI 回复已完成")
      .setContentText("点击返回查看结果")
      .setAutoCancel(true)
      .setContentIntent(openAppIntent())
      .build()
    getSystemService(NotificationManager::class.java).notify(CompleteNotificationId, notification)
  }

  private fun openAppIntent(): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun ensureChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
      ChannelId,
      "AI 回复",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "AI 回复进行中和完成提醒"
    }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  companion object {
    private const val ChannelId = "ai_chat_generation"
    private const val OngoingNotificationId = 1001
    private const val CompleteNotificationId = 1002
    private const val ActionStart = "com.personal.aichat.action.START_GENERATION"
    private const val ActionStop = "com.personal.aichat.action.STOP_GENERATION"
    private const val ActionComplete = "com.personal.aichat.action.COMPLETE_GENERATION"
    private const val MaxWakeLockDurationMs = 2 * 60 * 60 * 1000L

    fun start(context: Context) {
      val intent = Intent(context, ChatGenerationService::class.java).setAction(ActionStart)
      ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
      context.startService(Intent(context, ChatGenerationService::class.java).setAction(ActionStop))
    }

    fun complete(context: Context) {
      context.startService(Intent(context, ChatGenerationService::class.java).setAction(ActionComplete))
    }
  }
}
