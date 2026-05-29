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

  @Query("DELETE FROM providers WHERE id = :id")
  suspend fun deleteProvider(id: String)

  @Query("SELECT * FROM ai_bots WHERE providerId = :providerId ORDER BY enabled DESC, updatedAt DESC, name ASC")
  suspend fun aiBotsByProviderId(providerId: String): List<AiBotEntity>

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

  @Query("UPDATE conversations SET groupName = :newGroupName, updatedAt = :updatedAt WHERE groupName = :oldGroupName AND isDeleted = 0")
  suspend fun renameConversationGroup(oldGroupName: String, newGroupName: String, updatedAt: Long)

  @Query("UPDATE conversations SET groupName = '', updatedAt = :updatedAt WHERE groupName = :groupName AND isDeleted = 0")
  suspend fun clearConversationGroup(groupName: String, updatedAt: Long)

  @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
  suspend fun touchConversation(id: String, updatedAt: Long)

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

  @Query("UPDATE messages SET attachmentsJson = :attachmentsJson, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateMessageAttachments(id: String, attachmentsJson: String, updatedAt: Long)

  @Query(
    """
    UPDATE messages SET
      content = :content,
      status = :status,
      updatedAt = :updatedAt,
      errorMessage = :errorMessage,
      totalDurationMs = :totalDurationMs,
      firstTokenDurationMs = :firstTokenDurationMs,
      promptTokens = :promptTokens,
      completionTokens = :completionTokens,
      totalTokens = :totalTokens,
      rawResponseLog = :rawResponseLog
    WHERE id = :id
    """
  )
  suspend fun updateMessageWithMetadata(
    id: String,
    content: String,
    status: String,
    updatedAt: Long,
    errorMessage: String?,
    totalDurationMs: Long?,
    firstTokenDurationMs: Long?,
    promptTokens: Int?,
    completionTokens: Int?,
    totalTokens: Int?,
    rawResponseLog: String?
  )

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND role = 'USER' ORDER BY createdAt DESC LIMIT 1")
  suspend fun lastUserMessage(conversationId: String): MessageEntity?

  @Query("SELECT * FROM favorite_snippets ORDER BY updatedAt DESC")
  fun observeFavoriteSnippets(): Flow<List<FavoriteSnippetEntity>>

  @Query("SELECT * FROM favorite_snippets WHERE id = :id LIMIT 1")
  suspend fun favoriteSnippetById(id: String): FavoriteSnippetEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFavoriteSnippet(favorite: FavoriteSnippetEntity)

  @Query("DELETE FROM favorite_snippets WHERE id = :id")
  suspend fun deleteFavoriteSnippet(id: String)

  @Query("SELECT * FROM ai_bots ORDER BY enabled DESC, updatedAt DESC, name ASC")
  fun observeAiBots(): Flow<List<AiBotEntity>>

  @Query("SELECT * FROM ai_bots WHERE id = :id LIMIT 1")
  suspend fun aiBotById(id: String): AiBotEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAiBot(bot: AiBotEntity)

  @Query("UPDATE ai_bots SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setAiBotEnabled(id: String, enabled: Boolean, updatedAt: Long)

  @Query("DELETE FROM ai_bots WHERE id = :id")
  suspend fun deleteAiBot(id: String)

  @Query("SELECT * FROM group_chat_rooms WHERE isDeleted = 0 AND isArchived = 0 ORDER BY updatedAt DESC")
  fun observeGroupChatRooms(): Flow<List<GroupChatRoomEntity>>

  @Query("SELECT * FROM group_chat_rooms WHERE id = :id LIMIT 1")
  suspend fun groupChatRoomById(id: String): GroupChatRoomEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGroupChatRoom(room: GroupChatRoomEntity)

  @Query("UPDATE group_chat_rooms SET title = :title, topic = :topic, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateGroupChatRoomMeta(id: String, title: String, topic: String, updatedAt: Long)

  @Query("UPDATE group_chat_rooms SET summary = :summary, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateGroupChatSummary(id: String, summary: String, updatedAt: Long)

  @Query("UPDATE group_chat_rooms SET updatedAt = :updatedAt WHERE id = :id")
  suspend fun touchGroupChatRoom(id: String, updatedAt: Long)

  @Query("UPDATE group_chat_rooms SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
  suspend fun archiveGroupChatRoom(id: String, updatedAt: Long)

  @Query("UPDATE group_chat_rooms SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
  suspend fun deleteGroupChatRoom(id: String, updatedAt: Long)

  @Query("SELECT * FROM group_chat_members WHERE groupId = :groupId AND enabled = 1 ORDER BY sortOrder ASC")
  fun observeGroupChatMembers(groupId: String): Flow<List<GroupChatMemberEntity>>

  @Query("SELECT * FROM group_chat_members WHERE groupId = :groupId AND enabled = 1 ORDER BY sortOrder ASC")
  suspend fun groupChatMembers(groupId: String): List<GroupChatMemberEntity>

  @Query("SELECT * FROM group_chat_members WHERE groupId = :groupId ORDER BY sortOrder ASC")
  suspend fun allGroupChatMembers(groupId: String): List<GroupChatMemberEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGroupChatMember(member: GroupChatMemberEntity)

  @Query("UPDATE group_chat_members SET enabled = 0, updatedAt = :updatedAt WHERE groupId = :groupId AND botId = :botId")
  suspend fun removeGroupChatMember(groupId: String, botId: String, updatedAt: Long)

  @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY createdAt ASC")
  fun observeGroupMessages(groupId: String): Flow<List<GroupMessageEntity>>

  @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY createdAt ASC")
  suspend fun groupMessages(groupId: String): List<GroupMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGroupMessage(message: GroupMessageEntity)

  @Query(
    """
    UPDATE group_messages SET
      content = :content,
      status = :status,
      updatedAt = :updatedAt,
      errorMessage = :errorMessage,
      totalDurationMs = :totalDurationMs,
      firstTokenDurationMs = :firstTokenDurationMs,
      promptTokens = :promptTokens,
      completionTokens = :completionTokens,
      totalTokens = :totalTokens
    WHERE id = :id
    """
  )
  suspend fun updateGroupMessageWithMetadata(
    id: String,
    content: String,
    status: String,
    updatedAt: Long,
    errorMessage: String?,
    totalDurationMs: Long?,
    firstTokenDurationMs: Long?,
    promptTokens: Int?,
    completionTokens: Int?,
    totalTokens: Int?
  )
}
