package com.personal.aichat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    ProviderEntity::class,
    ConversationEntity::class,
    MessageEntity::class
  ],
  version = 1,
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
        ).build().also { instance = it }
      }
    }
  }
}
