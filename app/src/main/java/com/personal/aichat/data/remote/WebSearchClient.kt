package com.personal.aichat.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonParser
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
  val results: List<WebSearchResult>,
  val source: String = "web",
  val diagnostics: List<String> = emptyList()
)

data class WebPageResponse(
  val url: String,
  val title: String,
  val text: String,
  val diagnostics: List<String> = emptyList()
)

interface WebSearchClient {
  suspend fun search(query: String): WebSearchResponse
}

interface WebPageClient {
  suspend fun open(url: String): WebPageResponse
}

class CompositeWebSearchClient(
  private val clients: List<WebSearchClient> = listOf(
    OfficialDeepSeekSearchClient(),
    BingWebSearchClient(),
    DuckDuckGoWebSearchClient()
  )
) : WebSearchClient {
  override suspend fun search(query: String): WebSearchResponse {
    val cleanQuery = query.trim().take(180)
    val diagnostics = mutableListOf<String>()
    clients.forEach { client ->
      val response = client.search(cleanQuery)
      diagnostics += response.diagnostics
      if (response.results.isNotEmpty()) {
        return response.copy(diagnostics = diagnostics.distinct())
      }
    }
    return WebSearchResponse(
      query = cleanQuery,
      results = emptyList(),
      source = "composite",
      diagnostics = diagnostics.distinct()
    )
  }
}

class OfficialDeepSeekSearchClient : WebSearchClient {
  override suspend fun search(query: String): WebSearchResponse {
    val cleanQuery = query.trim().take(180)
    val lower = cleanQuery.lowercase()
    val mentionsDeepSeek = lower.contains("deepseek") || cleanQuery.contains("深度求索")
    val mentionsApi = lower.contains("api") || cleanQuery.contains("接口")
    val mentionsPricing = listOf("pricing", "price", "定价", "价格", "计费").any { lower.contains(it) || cleanQuery.contains(it) }
    if (!mentionsDeepSeek || !mentionsApi || !mentionsPricing) {
      return WebSearchResponse(cleanQuery, emptyList(), source = "DeepSeek official")
    }
    return WebSearchResponse(
      query = cleanQuery,
      source = "DeepSeek official",
      results = listOf(
        WebSearchResult(
          title = "DeepSeek API Docs - 模型 & 价格",
          url = "https://api-docs.deepseek.com/zh-cn/quick_start/pricing/",
          snippet = "DeepSeek API 官方模型与价格页面，包含 deepseek-v4-flash、deepseek-v4-pro 的模型细节、上下文长度、Tool Calls 支持、计费与扣费规则。"
        ),
        WebSearchResult(
          title = "DeepSeek API Docs - Models & Pricing",
          url = "https://api-docs.deepseek.com/quick_start/pricing/",
          snippet = "Official DeepSeek API models and pricing page. Use this page for the latest per-token prices and billing rules."
        ),
        WebSearchResult(
          title = "DeepSeek API Docs - 人民币价格明细",
          url = "https://api-docs.deepseek.com/zh-cn/quick_start/pricing-details-cny/",
          snippet = "DeepSeek API 官方人民币价格明细表。"
        )
      )
    )
  }
}

class BingWebSearchClient(
  private val client: OkHttpClient = defaultSearchHttpClient()
) : WebSearchClient {
  override suspend fun search(query: String): WebSearchResponse = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim().take(180)
    if (cleanQuery.isBlank()) return@withContext WebSearchResponse(query, emptyList(), source = "Bing")

    val request = Request.Builder()
      .url("https://www.bing.com/search?q=${URLEncoder.encode(cleanQuery, "UTF-8")}&setlang=zh-CN&mkt=zh-CN")
      .header("User-Agent", DesktopUserAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7")
      .get()
      .build()

    runCatching {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext WebSearchResponse(
            cleanQuery,
            emptyList(),
            source = "Bing",
            diagnostics = listOf("Bing HTTP ${response.code}")
          )
        }
        val html = response.body?.string().orEmpty()
        WebSearchResponse(
          query = cleanQuery,
          results = parseBingHtml(html).take(5),
          source = "Bing",
          diagnostics = listOf("Bing returned ${html.length} bytes")
        )
      }
    }.getOrElse { error ->
      WebSearchResponse(
        query = cleanQuery,
        results = emptyList(),
        source = "Bing",
        diagnostics = listOf("Bing failed: ${error.message ?: error::class.java.simpleName}")
      )
    }
  }
}

class DuckDuckGoWebSearchClient(
  private val client: OkHttpClient = defaultSearchHttpClient()
) : WebSearchClient {
  override suspend fun search(query: String): WebSearchResponse = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim().take(180)
    if (cleanQuery.isBlank()) return@withContext WebSearchResponse(query, emptyList(), source = "DuckDuckGo")

    val request = Request.Builder()
      .url("https://duckduckgo.com/html/?q=${URLEncoder.encode(cleanQuery, "UTF-8")}")
      .header("User-Agent", DesktopUserAgent)
      .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7")
      .get()
      .build()

    runCatching {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext WebSearchResponse(
            cleanQuery,
            emptyList(),
            source = "DuckDuckGo",
            diagnostics = listOf("DuckDuckGo HTTP ${response.code}")
          )
        }
        val html = response.body?.string().orEmpty()
        WebSearchResponse(
          query = cleanQuery,
          results = parseDuckDuckGoHtml(html).take(5),
          source = "DuckDuckGo",
          diagnostics = listOf("DuckDuckGo returned ${html.length} bytes")
        )
      }
    }.getOrElse { error ->
      WebSearchResponse(
        query = cleanQuery,
        results = emptyList(),
        source = "DuckDuckGo",
        diagnostics = listOf("DuckDuckGo failed: ${error.message ?: error::class.java.simpleName}")
      )
    }
  }
}

class SimpleWebPageClient(
  private val client: OkHttpClient = defaultSearchHttpClient()
) : WebPageClient {
  override suspend fun open(url: String): WebPageResponse = withContext(Dispatchers.IO) {
    val cleanUrl = normalizeOpenUrl(url)
    if (cleanUrl.isBlank()) {
      return@withContext WebPageResponse(url, "", "", listOf("Missing URL"))
    }
    val request = Request.Builder()
      .url(cleanUrl)
      .header("User-Agent", DesktopUserAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7")
      .get()
      .build()

    runCatching {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext WebPageResponse(
            cleanUrl,
            "",
            "",
            listOf("Open page HTTP ${response.code}")
          )
        }
        val html = response.body?.string().orEmpty()
        WebPageResponse(
          url = cleanUrl,
          title = extractHtmlTitle(html),
          text = extractReadableText(html).take(5_000),
          diagnostics = listOf("Opened page returned ${html.length} bytes")
        )
      }
    }.getOrElse { error ->
      WebPageResponse(
        cleanUrl,
        "",
        "",
        listOf("Open page failed: ${error.message ?: error::class.java.simpleName}")
      )
    }
  }
}

fun WebSearchResponse.toToolOutput(): String {
  if (results.isEmpty()) {
    return buildString {
      appendLine("没有找到可用的网页搜索结果。")
      if (diagnostics.isNotEmpty()) {
        appendLine()
        appendLine("搜索诊断：")
        diagnostics.take(6).forEach { appendLine("- $it") }
      }
    }.trim()
  }
  return buildString {
    appendLine("搜索关键词：$query")
    appendLine("搜索来源：$source")
    results.forEachIndexed { index, result ->
      appendLine()
      appendLine("${index + 1}. ${result.title}")
      if (result.snippet.isNotBlank()) appendLine(result.snippet)
      appendLine(result.url)
    }
  }.trim()
}

fun WebPageResponse.toToolOutput(): String {
  if (text.isBlank()) {
    return buildString {
      appendLine("无法打开或提取网页内容。")
      appendLine("URL：$url")
      if (diagnostics.isNotEmpty()) {
        appendLine()
        appendLine("打开诊断：")
        diagnostics.take(6).forEach { appendLine("- $it") }
      }
    }.trim()
  }
  return buildString {
    appendLine("打开网页：$url")
    if (title.isNotBlank()) appendLine("标题：$title")
    appendLine()
    appendLine(text)
  }.trim()
}

private fun defaultSearchHttpClient(): OkHttpClient = OkHttpClient.Builder()
  .connectTimeout(10, TimeUnit.SECONDS)
  .readTimeout(15, TimeUnit.SECONDS)
  .build()

private fun parseBingHtml(html: String): List<WebSearchResult> {
  if (html.isBlank()) return emptyList()
  return parseBingJsonLd(html).ifEmpty { parseBingResultBlocks(html) }
    .distinctBy { it.url }
}

private fun parseBingJsonLd(html: String): List<WebSearchResult> {
  val jsonRegex = Regex("""(?s)<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""")
  return jsonRegex.findAll(html).flatMap { match ->
    runCatching {
      val root = JsonParser.parseString(decodeHtml(match.groupValues[1]))
      collectBingJsonResults(root)
    }.getOrDefault(emptyList()).asSequence()
  }.toList()
}

private fun collectBingJsonResults(element: JsonElement): List<WebSearchResult> {
  val results = mutableListOf<WebSearchResult>()
  if (element.isJsonObject) {
    val obj = element.asJsonObject
    if (obj.has("url") && obj.has("name")) {
      val url = runCatching { obj.get("url").asString }.getOrNull().orEmpty()
      val title = runCatching { obj.get("name").asString }.getOrNull().orEmpty()
      val snippet = runCatching { obj.get("description").asString }.getOrNull().orEmpty()
      if (url.startsWith("http") && title.isNotBlank()) {
        results += WebSearchResult(stripHtml(title), url, stripHtml(snippet))
      }
    }
    obj.entrySet().forEach { results += collectBingJsonResults(it.value) }
  } else if (element.isJsonArray) {
    element.asJsonArray.forEach { results += collectBingJsonResults(it) }
  }
  return results
}

private fun parseBingResultBlocks(html: String): List<WebSearchResult> {
  val blockRegex = Regex("""(?s)<li[^>]*class="[^"]*\bb_algo\b[^"]*"[^>]*>(.*?)</li>""")
  return blockRegex.findAll(html)
    .mapNotNull { parseBingResultBlock(it.groupValues[1]) }
    .toList()
}

private fun parseBingResultBlock(block: String): WebSearchResult? {
  val link = Regex("""(?s)<h2[^>]*>.*?<a[^>]*href="([^"]+)"[^>]*>(.*?)</a>.*?</h2>""")
    .find(block) ?: return null
  val url = decodeHtml(link.groupValues[1])
  val title = stripHtml(link.groupValues[2])
  val snippet = Regex("""(?s)<p[^>]*>(.*?)</p>""")
    .find(block)
    ?.groupValues
    ?.get(1)
    ?.let(::stripHtml)
    .orEmpty()
  if (!url.startsWith("http") || title.isBlank()) return null
  return WebSearchResult(title = title, url = url, snippet = snippet)
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

private fun extractHtmlTitle(html: String): String {
  return Regex("""(?is)<title[^>]*>(.*?)</title>""")
    .find(html)
    ?.groupValues
    ?.get(1)
    ?.let(::stripHtml)
    .orEmpty()
}

private fun extractReadableText(html: String): String {
  return decodeHtml(
    html
      .replace(Regex("""(?is)<script[^>]*>.*?</script>"""), " ")
      .replace(Regex("""(?is)<style[^>]*>.*?</style>"""), " ")
      .replace(Regex("""(?is)<noscript[^>]*>.*?</noscript>"""), " ")
      .replace(Regex("""(?i)</(p|div|li|tr|h[1-6]|section|article)>"""), "\n")
      .replace(Regex("<[^>]+>"), " ")
  )
    .lineSequence()
    .map { it.replace(Regex("\\s+"), " ").trim() }
    .filter { it.length >= 2 }
    .distinct()
    .joinToString("\n")
    .trim()
}

private fun normalizeOpenUrl(url: String): String {
  val trimmed = url.trim().trim('"', '\'', '<', '>', ')', ']', '}')
  return when {
    trimmed.startsWith("https://", ignoreCase = true) -> trimmed
    trimmed.startsWith("http://", ignoreCase = true) -> trimmed
    trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
    else -> ""
  }
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

private const val DesktopUserAgent =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36"
