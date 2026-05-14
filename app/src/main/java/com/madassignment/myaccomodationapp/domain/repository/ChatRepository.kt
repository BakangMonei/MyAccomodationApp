package com.madassignment.myaccomodationapp.domain.repository

import com.madassignment.myaccomodationapp.domain.model.ChatMessage
import com.madassignment.myaccomodationapp.domain.model.ChatThread
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeThreadsForUser(userId: String): Flow<List<ChatThread>>

    fun observeMessages(chatId: String): Flow<List<ChatMessage>>

    suspend fun sendMessage(chatId: String, senderId: String, peerId: String, text: String): Result<Unit>

    suspend fun markMessagesRead(chatId: String, messageId: String, readerId: String): Result<Unit>

    suspend fun clearUnread(chatId: String, readerId: String): Result<Unit>

    fun observeUnreadCount(chatId: String, myUserId: String): Flow<Int>

    fun observeTotalUnreadForUser(userId: String): Flow<Int>
}
