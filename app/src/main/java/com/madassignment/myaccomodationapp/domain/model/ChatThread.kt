package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

data class ChatThread(
    val chatId: String,
    val participantIds: List<String>,
    val participantEmails: Map<String, String>,
    val lastMessageText: String,
    val lastSenderId: String?,
    val lastMessageAt: Instant,
    val unreadCount: Int,
)
