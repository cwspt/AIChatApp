package com.personal.aichat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

internal fun isBatteryOptimizationIgnored(context: Context): Boolean {
  return context.getSystemService(PowerManager::class.java)
    .isIgnoringBatteryOptimizations(context.packageName)
}

internal fun requestUnrestrictedBackgroundGeneration(context: Context) {
  val directRequest = Intent(
    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    Uri.parse("package:${context.packageName}")
  ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  runCatching { context.startActivity(directRequest) }
    .onFailure {
      context.startActivity(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }
}

internal fun shouldKeepScreenOnForGeneration(
  enabled: Boolean,
  streamingConversationIds: Set<String>,
  streamingGroupIds: Set<String>
): Boolean {
  return enabled && (streamingConversationIds.isNotEmpty() || streamingGroupIds.isNotEmpty())
}

internal fun isStreamingConnectionInterruption(message: String?): Boolean {
  return message?.contains("流式连接意外中断", ignoreCase = true) == true
}
