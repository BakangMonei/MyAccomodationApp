package com.madassignment.myaccomodationapp.domain.usecase

import com.madassignment.myaccomodationapp.domain.model.ChatMessage
import com.madassignment.myaccomodationapp.domain.model.ChatThread
import com.madassignment.myaccomodationapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveChatThreadsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(userId: String): Flow<List<ChatThread>> =
        chatRepository.observeThreadsForUser(userId)
}

class ObserveChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(chatId: String): Flow<List<ChatMessage>> = chatRepository.observeMessages(chatId)
}

class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(
        chatId: String,
        senderId: String,
        peerId: String,
        text: String,
    ): Result<Unit> = chatRepository.sendMessage(chatId, senderId, peerId, text)
}

class ObserveUnreadChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(chatId: String, myUserId: String): Flow<Int> =
        chatRepository.observeUnreadCount(chatId, myUserId)
}

class MarkChatReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(chatId: String, messageId: String, readerId: String): Result<Unit> =
        chatRepository.markMessagesRead(chatId, messageId, readerId)
}

class ClearChatUnreadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(chatId: String, readerId: String): Result<Unit> =
        chatRepository.clearUnread(chatId, readerId)
}

class ObserveTotalChatUnreadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(userId: String): Flow<Int> = chatRepository.observeTotalUnreadForUser(userId)
}
