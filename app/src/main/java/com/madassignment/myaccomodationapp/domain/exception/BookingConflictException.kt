package com.madassignment.myaccomodationapp.domain.exception

class BookingConflictException(message: String = "This listing is no longer available.") : Exception(message)

class ReAuthRequiredException(message: String = "Please sign in again to continue.") : Exception(message)
