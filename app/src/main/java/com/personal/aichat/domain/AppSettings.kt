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

data class AppSettings(
  val palette: AppThemePalette = AppThemePalette.MOSS,
  val themeMode: AppThemeMode = AppThemeMode.LIGHT,
  val fontScale: Float = 1.0f,
  val debugResponseLogging: Boolean = false,
  val webSearchMode: WebSearchMode = WebSearchMode.OFF
)
