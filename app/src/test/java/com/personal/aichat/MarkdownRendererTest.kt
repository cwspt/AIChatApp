package com.personal.aichat

import com.personal.aichat.ui.renderInlineMarkdown
import com.personal.aichat.ui.MarkdownBlock
import com.personal.aichat.ui.MarkdownInlineStyle
import com.personal.aichat.ui.parseMarkdownBlocks
import com.personal.aichat.ui.parseInlineMarkdown
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRendererTest {
  @Test
  fun blockMarkdownParserKeepsBubbleAndImageExportStructuresAligned() {
    val blocks = parseMarkdownBlocks(
      """
      Paragraph

      > Quoted **bold** text
      > second line

      ---

      ***

      ___

      ###### Heading

      - bullet
      - [ ] todo
      - [x] done
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
        MarkdownBlock.BlockQuote::class,
        MarkdownBlock.Divider::class,
        MarkdownBlock.Divider::class,
        MarkdownBlock.Divider::class,
        MarkdownBlock.Heading::class,
        MarkdownBlock.ListItem::class,
        MarkdownBlock.ListItem::class,
        MarkdownBlock.ListItem::class,
        MarkdownBlock.ListItem::class,
        MarkdownBlock.Table::class,
        MarkdownBlock.Code::class
      ),
      blocks.map { it::class }
    )
    assertEquals("Quoted **bold** text\nsecond line", (blocks[1] as MarkdownBlock.BlockQuote).text)
    assertEquals(6, (blocks[5] as MarkdownBlock.Heading).level)
    assertEquals("•", (blocks[6] as MarkdownBlock.ListItem).marker)
    assertEquals("☐", (blocks[7] as MarkdownBlock.ListItem).marker)
    assertEquals("☑", (blocks[8] as MarkdownBlock.ListItem).marker)
    assertEquals("1.", (blocks[9] as MarkdownBlock.ListItem).marker)
  }

  @Test
  fun inlineMarkdownStylesAreSharedAndAppliedToBubbleText() {
    val source = "**bold** __strong__ *italic* _also italic_ ***both*** ~~removed~~ `code`"
    val tokens = parseInlineMarkdown(source)

    assertEquals(
      "bold strong italic also italic both removed code",
      tokens.joinToString("") { it.text }
    )
    assertEquals(
      listOf(
        MarkdownInlineStyle.BOLD,
        MarkdownInlineStyle.BODY,
        MarkdownInlineStyle.BOLD,
        MarkdownInlineStyle.BODY,
        MarkdownInlineStyle.ITALIC,
        MarkdownInlineStyle.BODY,
        MarkdownInlineStyle.ITALIC,
        MarkdownInlineStyle.BODY,
        MarkdownInlineStyle.BOLD_ITALIC,
        MarkdownInlineStyle.BODY,
        MarkdownInlineStyle.STRIKETHROUGH,
        MarkdownInlineStyle.BODY,
        MarkdownInlineStyle.INLINE_CODE
      ),
      tokens.map { it.style }
    )

    val rendered = renderInlineMarkdown(source)
    val italicStart = rendered.text.indexOf("italic")
    val bothStart = rendered.text.indexOf("both")
    val removedStart = rendered.text.indexOf("removed")
    assertTrue(rendered.spanStyles.any { it.start == 0 && it.item.fontWeight == FontWeight.Bold })
    assertTrue(rendered.spanStyles.any { it.start == italicStart && it.item.fontStyle == FontStyle.Italic })
    assertTrue(
      rendered.spanStyles.any {
        it.start == bothStart && it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic
      }
    )
    assertTrue(
      rendered.spanStyles.any {
        it.start == removedStart && it.item.textDecoration == TextDecoration.LineThrough
      }
    )
  }

  @Test
  fun inlineMarkdownAnnotatesRawAndMarkdownUrls() {
    val rendered = renderInlineMarkdown(
      "Docs: https://example.com/pricing\u3002 Also see [help](https://example.com/help) and <https://example.com/about>."
    )

    assertEquals(
      listOf("https://example.com/pricing", "https://example.com/help", "https://example.com/about"),
      rendered.getStringAnnotations("markdown_url", 0, rendered.length).map { it.item }
    )
    assertEquals(
      "Docs: https://example.com/pricing\u3002 Also see help and https://example.com/about.",
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
