package com.madassignment.myaccomodationapp.data.repository

import com.madassignment.myaccomodationapp.data.mapper.toChatMessageOrNull
import com.madassignment.myaccomodationapp.data.mapper.toChatThreadOrNull
import com.madassignment.myaccomodationapp.domain.model.ChatMessage
import com.madassignment.myaccomodationapp.domain.model.ChatThread
import com.madassignment.myaccomodationapp.domain.repository.ChatRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ChatRepository {

    override fun observeThreadsForUser(userId: String): Flow<List<ChatThread>> = callbackFlow {
        val reg = firestore.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toChatThreadOrNull(userId) }
                    .sortedByDescending { it.lastMessageAt }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        // Single-field index on sentAt is created automatically for subcollection queries.
        val reg = messagesCollection(chatId)
            .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().mapNotNull { it.toChatMessageOrNull() }
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun sendMessage(
        chatId: String,
        senderId: String,
        peerId: String,
        text: String,
    ): Result<Unit> = runCatching {
        val participants = listOf(senderId, peerId).sorted()
        val senderEmail = firestore.collection("users").document(senderId).get().await().getString("email").orEmpty()
        val peerEmail = firestore.collection("users").document(peerId).get().await().getString("email").orEmpty()
        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.set(
            mapOf(
                "chatId" to chatId,
                "participantIds" to participants,
                "participantEmails" to mapOf(
                    senderId to senderEmail,
                    peerId to peerEmail,
                ),
                "lastMessageText" to text,
                "lastSenderId" to senderId,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastActivityAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
        val payload = mapOf(
            "chatId" to chatId,
            "senderId" to senderId,
            "text" to text,
            "sentAt" to FieldValue.serverTimestamp(),
            "readBy" to listOf(senderId),
        )
        messagesCollection(chatId).add(payload).await()
        chatRef.update(FieldPath.of("unread", peerId), FieldValue.increment(1)).await()
    }

    override suspend fun markMessagesRead(
        chatId: String,
        messageId: String,
        readerId: String,
    ): Result<Unit> = runCatching {
        messagesCollection(chatId).document(messageId)
            .update("readBy", FieldValue.arrayUnion(readerId))
            .await()
    }

    override suspend fun clearUnread(chatId: String, readerId: String): Result<Unit> = runCatching {
        firestore.collection("chats").document(chatId)
            .update(FieldPath.of("unread", readerId), 0)
            .await()
    }

    override fun observeUnreadCount(chatId: String, myUserId: String): Flow<Int> =
        observeMessages(chatId).map { messages ->
            messages.count { message ->
                message.senderId != myUserId && !message.readBy.contains(myUserId)
            }
        }

    override fun observeTotalUnreadForUser(userId: String): Flow<Int> = callbackFlow {
        // INDEX REQUIRED: chats (participantIds CONTAINS, __name__)
        val reg = firestore.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                val total = snapshot?.documents.orEmpty().sumOf { doc ->
                    val unread = doc.get("unread") as? Map<*, *> ?: emptyMap<String, Any>()
                    (unread[userId] as? Number)?.toInt() ?: 0
                }
                trySend(total)
            }
        awaitClose { reg.remove() }
    }

    private fun messagesCollection(chatId: String) =
        firestore.collection("chats").document(chatId).collection("messages")
}
