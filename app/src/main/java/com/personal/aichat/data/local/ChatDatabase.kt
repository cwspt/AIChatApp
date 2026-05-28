package com.personal.aichat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
  version = 10,
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
        ).addMigrations(
          Migration1To2,
          Migration2To3,
          Migration3To4,
          Migration4To5,
          Migration5To6,
          Migration6To7,
          Migration7To8,
          Migration8To9,
          Migration9To10
        ).build().also { instance = it }
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
  }
}
