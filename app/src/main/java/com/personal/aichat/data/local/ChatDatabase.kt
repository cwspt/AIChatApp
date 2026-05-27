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
    MessageEntity::class
  ],
  version = 4,
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
        ).addMigrations(Migration1To2, Migration2To3, Migration3To4).build().also { instance = it }
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
  }
}
