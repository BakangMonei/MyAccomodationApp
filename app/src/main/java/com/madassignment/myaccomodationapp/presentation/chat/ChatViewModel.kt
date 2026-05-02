package com.madassignment.myaccomodationapp.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.model.AuthUser
import com.madassignment.myaccomodationapp.domain.model.ChatMessage
import com.madassignment.myaccomodationapp.domain.usecase.ClearChatUnreadUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveChatMessagesUseCase
import com.madassignment.myaccomodationapp.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeChatMessages: ObserveChatMessagesUseCase,
    observeAuthState: ObserveAuthStateUseCase,
    private val sendChatMessage: SendChatMessageUseCase,
    private val clearChatUnread: ClearChatUnreadUseCase,
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val peerId: String = checkNotNull(savedStateHandle["peerId"])

    private val authFlow = observeAuthState()

    val authUid: StateFlow<AuthUser?> = authFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<ChatMessage>> = observeChatMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val me = authFlow.first { it != null } ?: return@launch
            clearChatUnread(chatId, me.uid)
        }
    }

    fun send(text: String) {
        val me = authUid.value ?: return
        viewModelScope.launch {
            sendChatMessage(chatId, me.uid, peerId, text)
        }
    }
}
