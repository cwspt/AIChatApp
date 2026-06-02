package com.personal.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class AppChangelogEntry(
  val date: String,
  val changes: List<String>
)

private val AppChangelogEntries = listOf(
  AppChangelogEntry(
    date = "2026-06-02",
    changes = listOf(
      "设置页新增更新日志，按日期汇总最近新增能力、修正和维护改进。",
      "Provider 配置增加能力矩阵、二维码导入导出、扫码导入和更清晰的管理入口。",
      "附件支持 app 内 PDF/文本预览，导出内容会包含附件索引。",
      "收藏夹支持搜索高亮、批量删除、批量追加标签、排序和消息批量移除。",
      "群聊支持自动播放偏好、摘要刷新提示、机器人头像色标、背景预设组合和 Markdown 导出。",
      "工具调用卡片按工具类型显示不同视觉样式，并支持清理历史 DSML 工具标记。",
      "持续拆分聊天、设置、Provider 和滚动相关 UI 组件，降低后续维护复杂度。"
    )
  ),
  AppChangelogEntry(
    date = "2026-05-30",
    changes = listOf(
      "增强上下文压缩和流式输出状态提示。",
      "修复兼容接口工具标记处理，让旧格式工具调用更稳定地呈现。"
    )
  ),
  AppChangelogEntry(
    date = "2026-05-29",
    changes = listOf(
      "新增系统分享入口、群聊管理、群聊播放器和 Web 搜索相关修正。",
      "完善附件大小限制、图片压缩上传和 Provider 错误提示。",
      "新增收藏导入导出、收藏标签管理和工具调用详情展示。",
      "新增生图接口模式，并优化聊天 UI 分层与文件夹能力。"
    )
  ),
  AppChangelogEntry(
    date = "2026-05-28",
    changes = listOf(
      "新增多 AI 群聊。",
      "扩展聊天应用基础能力，并建立项目 Tracker。"
    )
  ),
  AppChangelogEntry(
    date = "2026-05-27",
    changes = listOf(
      "创建 AIChatApp 核心聊天体验。",
      "建立 Android 项目结构和 Jenkins 发布流水线。"
    )
  )
)

@Composable
internal fun AppChangelogSection() {
  SettingsSection(title = "更新日志") {
    Text(
      text = "根据 Git 提交记录整理",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    AppChangelogEntries.forEachIndexed { index, entry ->
      ChangelogEntryBlock(entry)
      if (index < AppChangelogEntries.lastIndex) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
      }
    }
  }
}

@Composable
private fun ChangelogEntryBlock(entry: AppChangelogEntry) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = entry.date,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold
    )
    entry.changes.forEach { change ->
      ChangelogBullet(change)
    }
  }
}

@Composable
private fun ChangelogBullet(text: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "-",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(top = 1.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f)
    )
  }
}
