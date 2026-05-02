package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val sentAt: Instant,
    val readBy: List<String>,
)
