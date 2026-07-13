package com.personal.aichat

import com.personal.aichat.ui.renderInlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownRendererTest {
  @Test
  fun inlineMarkdownAnnotatesRawAndMarkdownUrls() {
    val rendered = renderInlineMarkdown(
      "Docs: https://example.com/pricing\u3002 Also see [help](https://example.com/help)."
    )

    assertEquals(
      listOf("https://example.com/pricing", "https://example.com/help"),
      rendered.getStringAnnotations("markdown_url", 0, rendered.length).map { it.item }
    )
    assertEquals(
      "Docs: https://example.com/pricing\u3002 Also see help.",
      rendered.text
    )
  }
}
