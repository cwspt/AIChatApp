package com.personal.aichat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val CHAT_DATABASE_VERSION = 17

@Database(
  entities = [
    ProviderEntity::class,
    ConversationEntity::class,
    MessageEntity::class,
    FavoriteSnippetEntity::class,
    AiBotEntity::class,
    GroupChatRoomEntity::class,
    GroupChatMemberEntity::class,
    GroupMessageEntity::class
  ],
  version = CHAT_DATABASE_VERSION,
  exportSchema = true
)
abstract class ChatDatabase : RoomDatabase() {
  abstract fun chatDao(): ChatDao

  companion object {
    @Volatile private var instance: ChatDatabase? = null

    fun getInstance(context: Context): ChatDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          ChatDatabase::class.java,
          "ai-chat.db"
        ).addMigrations(*ALL_MIGRATIONS).build().also { instance = it }
      }
    }

    private val Migration1To2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE conversations ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE conversations ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
      }
    }

    private val Migration2To3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN groupName TEXT NOT NULL DEFAULT ''")
      }
    }

    private val Migration3To4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE providers ADD COLUMN reasoningEffort TEXT NOT NULL DEFAULT 'AUTO'")
      }
    }

    private val Migration4To5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN forkedFromConversationId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE conversations ADD COLUMN forkedFromMessageId TEXT DEFAULT NULL")
      }
    }

    private val Migration5To6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN totalDurationMs INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN firstTokenDurationMs INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN promptTokens INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN completionTokens INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN totalTokens INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE messages ADD COLUMN rawResponseLog TEXT DEFAULT NULL")
      }
    }

    private val Migration6To7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT ''")
      }
    }

    private val Migration7To8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE providers ADD COLUMN supportsAttachments INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE providers SET supportsAttachments = 1 WHERE type IN ('OPENAI_RESPONSES', 'TOKENHUB_PROXY')")
      }
    }

    private val Migration8To9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS favorite_snippets (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            tagsJson TEXT NOT NULL,
            messagesJson TEXT NOT NULL,
            searchText TEXT NOT NULL,
            sourceConversationId TEXT NOT NULL,
            sourceConversationTitle TEXT NOT NULL,
            sourceProviderId TEXT,
            sourceProviderName TEXT,
            sourceModel TEXT,
            sourceGroupName TEXT,
            sourceFirstMessageId TEXT,
            sourceLastMessageId TEXT,
            messageCount INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    private val Migration9To10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS ai_bots (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            providerId TEXT NOT NULL,
            model TEXT NOT NULL,
            systemPrompt TEXT NOT NULL,
            enabled INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS group_chat_rooms (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            topic TEXT NOT NULL,
            summary TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            isArchived INTEGER NOT NULL,
            isDeleted INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS group_chat_members (
            groupId TEXT NOT NULL,
            botId TEXT NOT NULL,
            sortOrder INTEGER NOT NULL,
            enabled INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            PRIMARY KEY(groupId, botId)
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS group_messages (
            id TEXT NOT NULL PRIMARY KEY,
            groupId TEXT NOT NULL,
            senderType TEXT NOT NULL,
            botId TEXT,
            senderName TEXT NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL,
            status TEXT NOT NULL,
            providerId TEXT,
            model TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            errorMessage TEXT,
            totalDurationMs INTEGER,
            firstTokenDurationMs INTEGER,
            promptTokens INTEGER,
            completionTokens INTEGER,
            totalTokens INTEGER,
            attachmentsJson TEXT NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    private val Migration10To11 = object : Migration(10, 11) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ai_bots ADD COLUMN bubbleColorKey TEXT NOT NULL DEFAULT 'AUTO'")
      }
    }

    private val Migration11To12 = object : Migration(11, 12) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE group_messages ADD COLUMN turnTrigger TEXT NOT NULL DEFAULT 'UNKNOWN'")
        db.execSQL("ALTER TABLE group_messages ADD COLUMN turnRound INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE group_messages ADD COLUMN turnIndex INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE group_messages ADD COLUMN turnMemberCount INTEGER DEFAULT NULL")
      }
    }

    private val Migration12To13 = object : Migration(12, 13) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN type TEXT NOT NULL DEFAULT 'CHAT'")
        db.execSQL("ALTER TABLE providers ADD COLUMN supportsImageGeneration INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE providers SET supportsImageGeneration = 1 WHERE type = 'OPENAI_RESPONSES'")
      }
    }

    private val Migration13To14 = object : Migration(13, 14) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE providers ADD COLUMN imageGenerationApiMode TEXT NOT NULL DEFAULT 'RESPONSES_TOOL'")
        db.execSQL("ALTER TABLE providers ADD COLUMN imageGenerationModel TEXT NOT NULL DEFAULT ''")
      }
    }

    private val Migration14To15 = object : Migration(14, 15) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE providers ADD COLUMN contextWindowTokensOverride INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE conversations ADD COLUMN contextSummary TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE conversations ADD COLUMN contextSummaryCutoffMessageId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE conversations ADD COLUMN contextSummaryUpdatedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE group_chat_rooms ADD COLUMN contextSummary TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE group_chat_rooms ADD COLUMN contextSummaryCutoffMessageId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE group_chat_rooms ADD COLUMN contextSummaryUpdatedAt INTEGER DEFAULT NULL")
      }
    }

    private val Migration15To16 = object : Migration(15, 16) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN contentPartsJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE group_messages ADD COLUMN contentPartsJson TEXT NOT NULL DEFAULT ''")
      }
    }

    private val Migration16To17 = object : Migration(16, 17) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN reasoningContent TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE group_messages ADD COLUMN reasoningContent TEXT NOT NULL DEFAULT ''")
      }
    }

    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
      Migration1To2,
      Migration2To3,
      Migration3To4,
      Migration4To5,
      Migration5To6,
      Migration6To7,
      Migration7To8,
      Migration8To9,
      Migration9To10,
      Migration10To11,
      Migration11To12,
      Migration12To13,
      Migration13To14,
      Migration14To15,
      Migration15To16,
      Migration16To17
    )
  }
}
