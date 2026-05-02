package com.madassignment.myaccomodationapp.data.util

import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ReceiptNumbers {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE.withZone(ZoneOffset.UTC)
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray()

    fun next(now: Instant): String {
        val day = dateFormatter.format(now)
        val rnd = SecureRandom()
        val suffix = CharArray(6) { alphabet[rnd.nextInt(alphabet.size)] }.concatToString()
        return "RES-$day-$suffix"
    }
}
