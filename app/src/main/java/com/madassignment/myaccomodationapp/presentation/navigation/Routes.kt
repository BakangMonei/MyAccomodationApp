package com.madassignment.myaccomodationapp.presentation.navigation

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val LISTING_DETAIL = "listing/{listingId}"
    const val PAYMENT = "pay/{listingId}"
    const val CHAT = "chat/{chatId}/{peerId}"

    fun listingDetail(listingId: String) = "listing/$listingId"
    fun payment(listingId: String) = "pay/$listingId"
    fun chat(chatId: String, peerId: String) = "chat/$chatId/$peerId"
}
