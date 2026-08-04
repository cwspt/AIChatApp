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
    date = "2026-08-04",
    changes = listOf(
      "DeepSeek 配置选择 deepseek-v4-flash 时自动使用官方 Responses API，旧模型继续使用 Chat Completions。",
      "适配 Responses 语义化流式结束、未完成和失败事件，并按 DeepSeek v4 的推理档位发送参数。",
      "DeepSeek Responses 的思考过程会单独折叠展示，不再混入回答正文；展开后可查看完整推理文本。",
      "DeepSeek v4-flash 保留联网搜索能力，同时隐藏不受支持的图片/文件输入和生图工具，避免请求被服务端替换或忽略。"
    )
  ),
  AppChangelogEntry(
    date = "2026-07-17",
    changes = listOf(
      "聊天输入区改为宽文本框和动态加号、发送、停止按钮，低频功能统一收纳到内联工具面板。",
      "普通单聊、独立生图和群聊会按能力显示相册、拍照、文件、重试、回复插图或生图设置。",
      "普通 GPT 单聊可按次允许生成插图，由模型在回答中按内容需要混排 0-3 张图片。",
      "图文回复支持图片生成状态、失败后单图重试、图片预览，并在收藏和分享导出中保留原始顺序。",
      "长图分享可按真实比例绘制回复中的本地插图，分页时不会裁切图片。"
    )
  ),
  AppChangelogEntry(
    date = "2026-07-16",
    changes = listOf(
      "长图导出会先测量真实高度，较长内容可选择安全分图或合成单张图片。",
      "聊天气泡和导出图片补齐 Markdown 分割线、引用、H1-H6、任务列表、斜体、粗斜体、删除线和自动链接，并共用语法解析。",
      "AI 生成在息屏后继续保持 CPU 和网络活动，静默断连可在未输出正文时安全重试。"
    )
  ),
  AppChangelogEntry(
    date = "2026-07-14",
    changes = listOf(
      "AI 回复中的 Markdown 链接、原始网址和常见裸域名可直接打开或复制。",
      "长图导出保留标题、粗体、行内代码、链接、列表、代码块和表格等 Markdown 层次。"
    )
  ),
  AppChangelogEntry(
    date = "2026-07-13",
    changes = listOf(
      "超长聊天图片按安全高度连续分页，不再因 Bitmap 上限截断内容。",
      "分享长图增大正文和 Markdown 字号，并隐藏工具调用记录，提高手机端可读性。",
      "无可用机器人时，群聊创建页可直接进入机器人管理。"
    )
  ),
  AppChangelogEntry(
    date = "2026-07-11",
    changes = listOf(
      "Provider 能力矩阵支持折叠，展开后与配置列表分别滚动。",
      "删除默认 Provider 配置后不再自动恢复，最后一个配置会受到删除保护。",
      "修正本地签名发布脚本的 JDK、Android SDK 和 Gradle 路径。"
    )
  ),
  AppChangelogEntry(
    date = "2026-07-05",
    changes = listOf(
      "减少聊天列表滚动期间的状态读取和浮动按钮计算，改善长对话滑动流畅度。",
      "缩小选择、回到底部和上下文容量控件，限制顶部元信息宽度，减少窄屏遮挡。"
    )
  ),
  AppChangelogEntry(
    date = "2026-06-03",
    changes = listOf(
      "将长气泡导航、回到底部和滚动条计算移出滚动热路径。"
    )
  ),
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
