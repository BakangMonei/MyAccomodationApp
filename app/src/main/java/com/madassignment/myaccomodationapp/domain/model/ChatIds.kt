package com.madassignment.myaccomodationapp.domain.model

import java.security.MessageDigest

object ChatIds {
    fun forStudentAndProvider(studentId: String, providerId: String): String {
        val ordered = listOf(studentId, providerId).sorted().joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(ordered.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
