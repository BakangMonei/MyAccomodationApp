package com.madassignment.myaccomodationapp.domain.model

data class ListingPage(
    val items: List<Listing>,
    val nextCursor: String?,
)

data class AuthUser(
    val uid: String,
    val email: String?,
)
