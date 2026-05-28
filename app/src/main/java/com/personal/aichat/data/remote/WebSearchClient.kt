package com.personal.aichat.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WebSearchResult(
  val title: String,
  val url: String,
  val snippet: String
)

data class WebSearchResponse(
  val query: String,
  val results: List<WebSearchResult>
)

interface WebSearchClient {
  suspend fun search(query: String): WebSearchResponse
}

class DuckDuckGoWebSearchClient(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()
) : WebSearchClient {
  override suspend fun search(query: String): WebSearchResponse = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim().take(180)
    if (cleanQuery.isBlank()) return@withContext WebSearchResponse(query, emptyList())
    val request = Request.Builder()
      .url("https://duckduckgo.com/html/?q=${URLEncoder.encode(cleanQuery, "UTF-8")}")
      .header("User-Agent", "Mozilla/5.0 AIChatApp")
      .get()
      .build()
    val results = runCatching {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) emptyList() else parseDuckDuckGoHtml(response.body?.string().orEmpty()).take(5)
      }
    }.getOrDefault(emptyList())
    WebSearchResponse(cleanQuery, results)
  }
}

fun WebSearchResponse.toToolOutput(): String {
  if (results.isEmpty()) return "没有找到可用的网页搜索结果。"
  return buildString {
    appendLine("搜索关键词：$query")
    results.forEachIndexed { index, result ->
      appendLine()
      appendLine("${index + 1}. ${result.title}")
      if (result.snippet.isNotBlank()) appendLine(result.snippet)
      appendLine(result.url)
    }
  }.trim()
}

private fun parseDuckDuckGoHtml(html: String): List<WebSearchResult> {
  if (html.isBlank()) return emptyList()
  val itemRegex = Regex("""(?s)<div[^>]*class="result[^"]*"[^>]*>(.*?)</div>\s*</div>""")
  return itemRegex.findAll(html)
    .mapNotNull { parseDuckDuckGoResult(it.value) }
    .distinctBy { it.url }
    .toList()
}

private fun parseDuckDuckGoResult(block: String): WebSearchResult? {
  val link = Regex("""(?s)<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""")
    .find(block) ?: return null
  val title = stripHtml(link.groupValues[2])
  val url = normalizeDuckDuckGoUrl(decodeHtml(link.groupValues[1]))
  val snippet = Regex("""(?s)<a[^>]*class="result__snippet"[^>]*>(.*?)</a>""")
    .find(block)
    ?.groupValues
    ?.get(1)
    ?.let(::stripHtml)
    .orEmpty()
  if (title.isBlank() || url.isBlank()) return null
  return WebSearchResult(title = title, url = url, snippet = snippet)
}

private fun normalizeDuckDuckGoUrl(rawUrl: String): String {
  val uddg = Regex("""[?&]uddg=([^&]+)""").find(rawUrl)?.groupValues?.get(1)
  return if (uddg != null) {
    runCatching { URLDecoder.decode(uddg, "UTF-8") }.getOrDefault(uddg)
  } else {
    rawUrl
  }
}

private fun stripHtml(value: String): String {
  return decodeHtml(value.replace(Regex("<[^>]+>"), " "))
    .replace(Regex("\\s+"), " ")
    .trim()
}

private fun decodeHtml(value: String): String {
  val named = value
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
  return Regex("""&#(\d+);""").replace(named) { match ->
    match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
  }
}
