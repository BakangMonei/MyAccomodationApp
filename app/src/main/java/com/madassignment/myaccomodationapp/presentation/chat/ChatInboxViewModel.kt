package com.madassignment.myaccomodationapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveChatThreadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChatPreview(
    val chatId: String,
    val peerId: String,
    val peerLabel: String,
    val lastMessageText: String,
    val unreadCount: Int,
    val lastMessageEpochMs: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatInboxViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeChatThreads: ObserveChatThreadsUseCase,
) : ViewModel() {

    val authUid: StateFlow<String?> = observeAuthState()
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val chats: StateFlow<List<ChatPreview>> = authUid
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                observeChatThreads(uid).map { threads ->
                    threads.map { thread ->
                        val peerId =
                            thread.participantIds.firstOrNull { participant -> participant != uid } ?: uid
                        ChatPreview(
                            chatId = thread.chatId,
                            peerId = peerId,
                            peerLabel = "User ${peerId.take(6)}",
                            lastMessageText = thread.lastMessageText.ifBlank { "Start the conversation" },
                            unreadCount = thread.unreadCount,
                            lastMessageEpochMs = thread.lastMessageAt.toEpochMilli(),
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
