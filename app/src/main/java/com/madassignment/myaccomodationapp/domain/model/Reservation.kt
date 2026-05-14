package com.madassignment.myaccomodationapp.domain.model

import java.time.Instant

data class Reservation(
    val id: String,
    val listingId: String,
    val userId: String,
    /** Total amount paid so far (deposit, then deposit + balance after balance is paid). */
    val amount: Double,
    val receiptNumber: String,
    val timestamp: Instant,
    val providerId: String? = null,
    val payerEmail: String? = null,
    val listingTitle: String = "",
    val depositAmount: Double = amount,
    val balanceAmount: Double = 0.0,
    val balanceReceiptNumber: String? = null,
    val balancePaidAt: Instant? = null,
) {
    val isFullyPaid: Boolean get() = balanceReceiptNumber != null || balanceAmount <= 0.0
}
