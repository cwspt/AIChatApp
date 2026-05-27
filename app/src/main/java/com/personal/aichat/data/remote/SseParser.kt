package com.personal.aichat.data.remote

data class SseFrame(
  val event: String?,
  val data: String
)

class SseParser {
  private val dataLines = mutableListOf<String>()
  private var event: String? = null

  fun accept(line: String): SseFrame? {
    if (line.isBlank()) {
      if (dataLines.isEmpty()) {
        event = null
        return null
      }
      val frame = SseFrame(event = event, data = dataLines.joinToString("\n"))
      dataLines.clear()
      event = null
      return frame
    }

    when {
      line.startsWith("event:") -> event = line.removePrefix("event:").trim()
      line.startsWith("data:") -> dataLines += line.removePrefix("data:").trimStart()
    }
    return null
  }
}
