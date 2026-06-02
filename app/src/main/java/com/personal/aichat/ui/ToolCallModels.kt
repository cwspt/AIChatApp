package com.personal.aichat.ui

import java.util.Locale

internal enum class ToolCallVisualKind {
  SEARCH,
  PAGE,
  FILE,
  TOOL
}

internal fun toolCallVisualKind(name: String): ToolCallVisualKind {
  val normalized = name.trim().lowercase(Locale.US)
  return when {
    normalized == "web_search" || normalized.contains("search") -> ToolCallVisualKind.SEARCH
    normalized in setOf("open", "open_page", "open_url", "open_url_page") -> ToolCallVisualKind.PAGE
    normalized == "web_fetch" || normalized.contains("fetch") || normalized.contains("file") -> ToolCallVisualKind.FILE
    else -> ToolCallVisualKind.TOOL
  }
}

internal fun toolCallGroupVisualKind(details: List<ToolCallDetails>): ToolCallVisualKind {
  val kinds = details.map { toolCallVisualKind(it.name) }.toSet()
  return when {
    ToolCallVisualKind.SEARCH in kinds -> ToolCallVisualKind.SEARCH
    ToolCallVisualKind.PAGE in kinds -> ToolCallVisualKind.PAGE
    ToolCallVisualKind.FILE in kinds -> ToolCallVisualKind.FILE
    else -> ToolCallVisualKind.TOOL
  }
}

internal fun groupToolSummary(details: List<ToolCallDetails>, count: Int, streaming: Boolean): String {
  val names = details.map { it.name }.distinct().joinToString("、").ifBlank { "tool" }
  val summary = details.asSequence()
    .mapNotNull { it.summary }
    .firstOrNull { it.isNotBlank() }
  return buildString {
    append(if (streaming) "正在调用" else "已调用")
    append(" $count 次工具")
    append("：")
    append(names)
    summary?.let {
      append(" · ")
      append(it)
    }
  }
}

internal data class ToolCallCitation(
  val title: String?,
  val url: String
)

internal data class ToolCallDetails(
  val name: String,
  val input: String?,
  val output: String?,
  val query: String? = null,
  val openedUrls: List<String> = emptyList(),
  val citations: List<ToolCallCitation> = emptyList()
) {
  val summary: String?
    get() {
      query?.takeIf { it.isNotBlank() }?.let { return "查询：${it.take(100)}" }
      openedUrls.firstOrNull()?.let { return "打开：${it.take(100)}" }
      citations.firstOrNull()?.let { citation ->
        return listOfNotNull(citation.title?.takeIf { it.isNotBlank() }, citation.url)
          .joinToString(" · ")
          .take(120)
      }
      val outputLines = output?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.toList()
        .orEmpty()
      val outputUrl = outputLines.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
      val outputFirst = outputLines.firstOrNull()
      val inputFirst = input?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
      return (outputUrl ?: outputFirst ?: inputFirst)?.take(120)
    }
}

internal fun parseToolCallDetails(content: String): ToolCallDetails {
  val name = content.substringAfter("工具：", "").lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "tool" }
  val input = content.sectionAfter("输入：", "输出：")
  val output = content.sectionAfter("输出：", null)
  val inputUrls = extractPlainUrls(input.orEmpty())
  val outputUrls = extractPlainUrls(output.orEmpty())
  val query = extractToolQuery(input) ?: extractToolQuery(output) ?: extractQueryFromToolOutput(output)
  val openedUrls = when (name) {
    "open", "open_page", "web_fetch" -> (inputUrls + outputUrls.take(1)).distinct()
    else -> inputUrls
  }
  val citations = extractToolCitations(output.orEmpty(), openedUrls.toSet())
  return ToolCallDetails(
    name = name,
    input = input,
    output = output,
    query = query,
    openedUrls = openedUrls,
    citations = citations
  )
}

private fun extractToolQuery(text: String?): String? {
  if (text.isNullOrBlank()) return null
  Regex(""""(?:query|queries|q)"\s*:\s*(\[[^\]]*]|"[^"]*")""").find(text)?.let { match ->
    val raw = match.groupValues[1].trim()
    if (raw.startsWith("[")) {
      return Regex(""""([^"]+)"""").find(raw)?.groupValues?.getOrNull(1)?.jsonUnescape()
    }
    return raw.trim('"').jsonUnescape().takeIf { it.isNotBlank() }
  }
  Regex("""(?im)^(?:查询|搜索关键词|搜索|query)\s*[:：]\s*(.+)$""").find(text)?.let { match ->
    return match.groupValues[1].trim().takeIf { it.isNotBlank() }
  }
  return null
}

private fun extractQueryFromToolOutput(output: String?): String? {
  if (output.isNullOrBlank()) return null
  val lines = output.lineSequence().map { it.trim() }.toList()
  val labelIndex = lines.indexOfFirst {
    it.contains("查询") ||
      it.contains("搜索关键词") ||
      it.contains(LegacyToolQueryLabel) ||
      it.contains(LegacyToolSearchKeywordLabel)
  }
  if (labelIndex < 0) return null
  return lines.drop(labelIndex + 1)
    .firstOrNull { line ->
      line.isNotBlank() &&
        !line.startsWith("http://") &&
        !line.startsWith("https://") &&
        !line.contains("网址") &&
        !line.contains(LegacyToolUrlLabel)
    }
    ?.take(120)
}

private fun extractPlainUrls(text: String): List<String> {
  return PlainToolUrlRegex.findAll(text)
    .map { it.value.trimEnd('.', ',', ';', '，', '。', '；') }
    .distinct()
    .toList()
}

private fun extractToolCitations(output: String, openedUrls: Set<String>): List<ToolCallCitation> {
  if (output.isBlank()) return emptyList()
  val lines = output.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
  val citations = mutableListOf<ToolCallCitation>()
  lines.forEachIndexed { index, line ->
    extractPlainUrls(line).forEach { url ->
      if (url in openedUrls) return@forEach
      val title = nearestCitationTitle(lines, index)
      citations += ToolCallCitation(title = title, url = url)
    }
  }
  return citations.distinctBy { it.url }
}

private fun nearestCitationTitle(lines: List<String>, urlLineIndex: Int): String? {
  lines.take(urlLineIndex).takeLast(4).asReversed().forEach { line ->
    Regex("""^\d+[.)]\s+(.+)$""").matchEntire(line)?.let { match ->
      return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }
  }
  val previous = lines.take(urlLineIndex).asReversed().firstOrNull { line ->
    !line.startsWith("http://") &&
      !line.startsWith("https://") &&
      !line.contains("：") &&
      !line.contains(":") &&
      line.length <= 120
  } ?: return null
  return previous
    .replace(Regex("""^\d+[.)]\s*"""), "")
    .trim()
    .takeIf { it.isNotBlank() }
}

private fun String.jsonUnescape(): String {
  return replace("\\n", "\n")
    .replace("\\\"", "\"")
    .replace("\\/", "/")
    .replace("\\\\", "\\")
}

private val PlainToolUrlRegex = Regex("https?://[^\\s<>\"'`\\]\\)\\}]+")

// Historical mojibake labels emitted by older tool output.
private const val LegacyToolQueryLabel = "\u93cc\u30e8"
private const val LegacyToolSearchKeywordLabel = "\u93bc\u6ec5\u50a8\u934f\u62bd\u656d"
private const val LegacyToolUrlLabel = "\u7f03\u621d\u6f43"

private fun String.sectionAfter(label: String, until: String?): String? {
  val start = indexOf(label)
  if (start < 0) return null
  val contentStart = start + label.length
  val end = until?.let { marker ->
    indexOf(marker, startIndex = contentStart).takeIf { it >= 0 }
  } ?: length
  return substring(contentStart, end).trim().takeIf { it.isNotBlank() }
}
