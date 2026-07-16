package com.personal.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
internal fun ImageExportChoiceDialog(
  choice: ImageExportChoiceState,
  onExportPaged: () -> Unit,
  onExportSingle: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("选择长图导出方式") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("内容较长，预计分成 ${choice.pageCount} 张。")
        Text(
          text = if (choice.singleImageAllowed) {
            "也可以合成一张长图；单图较长，部分分享应用可能压缩或无法预览。"
          } else {
            "内容已超过安全单图高度，为避免导出失败，只支持分图。"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(onClick = onExportPaged) {
        Text("分成 ${choice.pageCount} 张")
      }
    },
    dismissButton = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (choice.singleImageAllowed) {
          TextButton(onClick = onExportSingle) {
            Text("合成 1 张")
          }
        }
        TextButton(onClick = onDismiss) {
          Text("取消")
        }
      }
    }
  )
}
