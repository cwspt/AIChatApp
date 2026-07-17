package com.personal.aichat

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.aichat.data.local.CHAT_DATABASE_VERSION
import com.personal.aichat.data.local.ChatDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatDatabaseMigrationRobolectricTest {
  @get:Rule
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    ChatDatabase::class.java,
    emptyList(),
    FrameworkSQLiteOpenHelperFactory()
  )

  @Test
  fun migratesSeededSchema1DatabaseToLatest() {
    helper.createDatabase(SEEDED_DB, 1).apply {
      execSQL(
        """
        INSERT INTO providers (
          id, displayName, type, baseUrl, defaultModel, enabled,
          supportsStreaming, extraHeadersJson, secretRef, sortOrder
        ) VALUES (
          'provider-1', 'GPT', 'OPENAI_RESPONSES', 'https://api.openai.com/v1',
          'gpt-4.1-mini', 1, 1, '', 'provider_provider-1', 0
        )
        """.trimIndent()
      )
      execSQL(
        """
        INSERT INTO conversations (
          id, title, providerId, model, createdAt, updatedAt
        ) VALUES (
          'conversation-1', 'Hello', 'provider-1', 'gpt-4.1-mini', 10, 20
        )
        """.trimIndent()
      )
      execSQL(
        """
        INSERT INTO messages (
          id, conversationId, role, content, status, providerId, model,
          createdAt, updatedAt, errorMessage
        ) VALUES (
          'message-1', 'conversation-1', 'USER', 'hi', 'COMPLETE',
          'provider-1', 'gpt-4.1-mini', 11, 12, NULL
        )
        """.trimIndent()
      )
      close()
    }

    val db = helper.runMigrationsAndValidate(
      SEEDED_DB,
      CHAT_DATABASE_VERSION,
      true,
      *ChatDatabase.ALL_MIGRATIONS
    )

    assertEquals("GPT", db.stringValue("SELECT displayName FROM providers WHERE id = 'provider-1'"))
    assertEquals("AUTO", db.stringValue("SELECT reasoningEffort FROM providers WHERE id = 'provider-1'"))
    assertEquals(1L, db.longValue("SELECT supportsAttachments FROM providers WHERE id = 'provider-1'"))
    assertEquals(1L, db.longValue("SELECT supportsImageGeneration FROM providers WHERE id = 'provider-1'"))
    assertEquals("RESPONSES_TOOL", db.stringValue("SELECT imageGenerationApiMode FROM providers WHERE id = 'provider-1'"))
    assertEquals("", db.stringValue("SELECT imageGenerationModel FROM providers WHERE id = 'provider-1'"))
    assertNull(db.stringValue("SELECT contextWindowTokensOverride FROM providers WHERE id = 'provider-1'"))

    assertEquals("Hello", db.stringValue("SELECT title FROM conversations WHERE id = 'conversation-1'"))
    assertEquals("", db.stringValue("SELECT groupName FROM conversations WHERE id = 'conversation-1'"))
    assertEquals("CHAT", db.stringValue("SELECT type FROM conversations WHERE id = 'conversation-1'"))
    assertEquals(0L, db.longValue("SELECT isArchived FROM conversations WHERE id = 'conversation-1'"))
    assertEquals(0L, db.longValue("SELECT isDeleted FROM conversations WHERE id = 'conversation-1'"))
    assertEquals(0L, db.longValue("SELECT isPinned FROM conversations WHERE id = 'conversation-1'"))
    assertEquals("", db.stringValue("SELECT contextSummary FROM conversations WHERE id = 'conversation-1'"))
    assertNull(db.stringValue("SELECT contextSummaryCutoffMessageId FROM conversations WHERE id = 'conversation-1'"))
    assertNull(db.stringValue("SELECT contextSummaryUpdatedAt FROM conversations WHERE id = 'conversation-1'"))

    assertEquals("hi", db.stringValue("SELECT content FROM messages WHERE id = 'message-1'"))
    assertEquals("", db.stringValue("SELECT attachmentsJson FROM messages WHERE id = 'message-1'"))
    assertEquals("", db.stringValue("SELECT contentPartsJson FROM messages WHERE id = 'message-1'"))
    assertTrue(db.hasColumn("group_messages", "contentPartsJson"))
    assertNull(db.stringValue("SELECT rawResponseLog FROM messages WHERE id = 'message-1'"))
    assertEquals(0L, db.longValue("SELECT COUNT(*) FROM favorite_snippets"))
    assertEquals(0L, db.longValue("SELECT COUNT(*) FROM ai_bots"))
    assertEquals(0L, db.longValue("SELECT COUNT(*) FROM group_chat_rooms"))
    db.close()
  }

  @Test
  fun allExportedSchemasMigrateToLatest() {
    for (version in 1 until CHAT_DATABASE_VERSION) {
      val dbName = "migration-empty-$version"
      helper.createDatabase(dbName, version).close()
      helper.runMigrationsAndValidate(
        dbName,
        CHAT_DATABASE_VERSION,
        true,
        *ChatDatabase.ALL_MIGRATIONS
      ).close()
    }
  }

  private fun SupportSQLiteDatabase.stringValue(query: String): String? {
    val cursor = query(query)
    cursor.use {
      assertTrue(it.moveToFirst())
      return if (it.isNull(0)) null else it.getString(0)
    }
  }

  private fun SupportSQLiteDatabase.longValue(query: String): Long {
    val cursor = query(query)
    cursor.use {
      assertTrue(it.moveToFirst())
      return it.getLong(0)
    }
  }

  private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
      val nameIndex = cursor.getColumnIndexOrThrow("name")
      while (cursor.moveToNext()) {
        if (cursor.getString(nameIndex) == column) return true
      }
    }
    return false
  }

  private companion object {
    const val SEEDED_DB = "robolectric-migration-seeded"
  }
}
