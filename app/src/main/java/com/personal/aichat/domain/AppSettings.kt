package com.personal.aichat.domain

enum class AppThemePalette(val label: String) {
  MOSS("松林绿"),
  OCEAN("海盐蓝"),
  SAKURA("樱花粉"),
  AMBER("暖琥珀")
}

enum class AppThemeMode(val label: String) {
  LIGHT("浅色"),
  DARK("深色")
}

enum class WebSearchMode(val label: String) {
  OFF("关闭"),
  AUTO("自动搜索"),
  REQUIRED("强制搜索")
}

enum class StreamingBubbleMotion(val label: String) {
  STANDARD("明显"),
  SUBTLE("轻量"),
  OFF("关闭")
}

const val DEFAULT_ATTACHMENT_MAX_FILE_MB = 20
const val DEFAULT_ATTACHMENT_MAX_PENDING_MB = 50
const val DEFAULT_ATTACHMENT_MAX_IMAGE_SOURCE_MB = 80

data class ChatBackgroundPreset(
  val id: String,
  val title: String,
  val content: String,
  val sortOrder: Int,
  val createdAt: Long,
  val updatedAt: Long
)

data class AppSettings(
  val palette: AppThemePalette = AppThemePalette.MOSS,
  val themeMode: AppThemeMode = AppThemeMode.LIGHT,
  val fontScale: Float = 1.0f,
  val debugResponseLogging: Boolean = false,
  val webSearchMode: WebSearchMode = WebSearchMode.OFF,
  val streamingBubbleMotion: StreamingBubbleMotion = StreamingBubbleMotion.STANDARD,
  val attachmentMaxFileMb: Int = DEFAULT_ATTACHMENT_MAX_FILE_MB,
  val attachmentMaxPendingMb: Int = DEFAULT_ATTACHMENT_MAX_PENDING_MB,
  val attachmentMaxImageSourceMb: Int = DEFAULT_ATTACHMENT_MAX_IMAGE_SOURCE_MB,
  val backgroundPresets: List<ChatBackgroundPreset> = defaultBackgroundPresets()
)

fun defaultBackgroundPresets(now: Long = 0L): List<ChatBackgroundPreset> = listOf(
  ChatBackgroundPreset(
    id = "default_family",
    title = "家庭背景",
    content = "家庭成员、年龄、健康情况、作息、饮食偏好、预算和出行限制等背景：\n- \n\n讨论时请优先考虑安全性、可执行性和家庭成员的真实约束。",
    sortOrder = 0,
    createdAt = now,
    updatedAt = now
  ),
  ChatBackgroundPreset(
    id = "default_personal",
    title = "个人偏好",
    content = "我的个人偏好、目标、禁忌、预算、时间安排和决策风格：\n- \n\n讨论时请在给出建议前先检查是否符合这些偏好。",
    sortOrder = 1,
    createdAt = now,
    updatedAt = now
  ),
  ChatBackgroundPreset(
    id = "default_project",
    title = "项目/工作背景",
    content = "项目目标、当前进度、相关角色、技术/业务约束、截止时间和已知风险：\n- \n\n讨论时请围绕目标拆解方案、风险和下一步行动。",
    sortOrder = 2,
    createdAt = now,
    updatedAt = now
  )
)
