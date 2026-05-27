package com.personal.aichat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
  @Query("SELECT * FROM providers ORDER BY sortOrder ASC, displayName ASC")
  fun observeProviders(): Flow<List<ProviderEntity>>

  @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
  suspend fun providerById(id: String): ProviderEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProvider(provider: ProviderEntity)

  @Query("UPDATE providers SET enabled = :enabled WHERE id = :id")
  suspend fun setProviderEnabled(id: String, enabled: Boolean)

  @Query("SELECT COUNT(*) FROM providers")
  suspend fun providerCount(): Int

  @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
  fun observeConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
  fun observeArchivedConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT 1")
  suspend fun latestConversation(): ConversationEntity?

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  suspend fun conversationById(id: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConversation(conversation: ConversationEntity)

  @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateConversationTitle(id: String, title: String, updatedAt: Long)

  @Query("UPDATE conversations SET providerId = :providerId, model = :model, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateConversationProvider(id: String, providerId: String, model: String, updatedAt: Long)

  @Query("UPDATE conversations SET title = :title, groupName = :groupName, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateConversationMeta(id: String, title: String, groupName: String, updatedAt: Long)

  @Query("UPDATE conversations SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setConversationPinned(id: String, isPinned: Boolean, updatedAt: Long)

  @Query("UPDATE conversations SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
  suspend fun archiveConversation(id: String, updatedAt: Long)

  @Query("UPDATE conversations SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
  suspend fun restoreConversation(id: String, updatedAt: Long)

  @Query("UPDATE conversations SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
  suspend fun deleteConversation(id: String, updatedAt: Long)

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
  fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
  suspend fun messagesForConversation(conversationId: String): List<MessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMessage(message: MessageEntity)

  @Query("UPDATE messages SET content = :content, status = :status, updatedAt = :updatedAt, errorMessage = :errorMessage WHERE id = :id")
  suspend fun updateMessage(id: String, content: String, status: String, updatedAt: Long, errorMessage: String?)

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND role = 'USER' ORDER BY createdAt DESC LIMIT 1")
  suspend fun lastUserMessage(conversationId: String): MessageEntity?
}
