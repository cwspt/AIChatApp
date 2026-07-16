package com.personal.aichat

import com.personal.aichat.ui.renderInlineMarkdown
import com.personal.aichat.ui.MarkdownBlock
import com.personal.aichat.ui.parseMarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownRendererTest {
  @Test
  fun blockMarkdownParserKeepsBubbleAndImageExportStructuresAligned() {
    val blocks = parseMarkdownBlocks(
      """
      Paragraph

      ---

      ***

      ___

      ## Heading

      - bullet
      1. ordered

      | Name | Value |
      | --- | --- |
      | A | B |

      ```
      code
      ```
      """.trimIndent()
    )

    assertEquals(
      listOf(
        MarkdownBlock.Paragraph::class,
        MarkdownBlock.Divider::class,
        MarkdownBlock.Divider::class,
        MarkdownBlock.Divider::class,
        MarkdownBlock.Heading::class,
        MarkdownBlock.ListItem::class,
        MarkdownBlock.ListItem::class,
        MarkdownBlock.Table::class,
        MarkdownBlock.Code::class
      ),
      blocks.map { it::class }
    )
    assertEquals("•", (blocks[5] as MarkdownBlock.ListItem).marker)
    assertEquals("1.", (blocks[6] as MarkdownBlock.ListItem).marker)
  }

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

  @Test
  fun inlineMarkdownAnnotatesBareDomainsWithCanonicalHttpsUrls() {
    val rendered = renderInlineMarkdown(
      "Read help.openai.com/docs\u3002 [site](www.example.org/guide) and email team@example.com."
    )

    assertEquals(
      listOf("https://help.openai.com/docs", "https://www.example.org/guide"),
      rendered.getStringAnnotations("markdown_url", 0, rendered.length).map { it.item }
    )
    assertEquals(
      "Read help.openai.com/docs\u3002 site and email team@example.com.",
      rendered.text
    )
    val bareDomainStart = rendered.text.indexOf("help.openai.com")
    assertEquals(
      listOf("https://help.openai.com/docs"),
      rendered.getStringAnnotations("markdown_url", bareDomainStart, bareDomainStart + 1).map { it.item }
    )
  }
}
