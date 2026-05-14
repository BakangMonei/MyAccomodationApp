package com.madassignment.myaccomodationapp.presentation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madassignment.myaccomodationapp.domain.exception.BookingConflictException
import com.madassignment.myaccomodationapp.domain.model.AuthUser
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.Reservation
import com.madassignment.myaccomodationapp.domain.usecase.ObserveAuthStateUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ObserveListingUseCase
import com.madassignment.myaccomodationapp.domain.usecase.PayReservationBalanceUseCase
import com.madassignment.myaccomodationapp.domain.usecase.ReserveListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PaymentUiState {
    data object Idle : PaymentUiState
    data object ProcessingDeposit : PaymentUiState
    data class AwaitingBalance(val reservation: Reservation) : PaymentUiState
    data object ProcessingBalance : PaymentUiState
    data class Complete(val reservation: Reservation) : PaymentUiState
    data class Error(val message: String) : PaymentUiState
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reserveListing: ReserveListingUseCase,
    private val payReservationBalance: PayReservationBalanceUseCase,
    observeAuthState: ObserveAuthStateUseCase,
    observeListing: ObserveListingUseCase,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    private val authUser: StateFlow<AuthUser?> = observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val listing: StateFlow<Listing?> = observeListing(listingId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val ui: StateFlow<PaymentUiState> = _ui.asStateFlow()

    fun payDeposit(depositAmount: Double) {
        val user = authUser.value ?: return
        viewModelScope.launch {
            _ui.value = PaymentUiState.ProcessingDeposit
            val result = reserveListing(listingId, user.uid, depositAmount)
            _ui.value = result.fold(
                onSuccess = { reservation ->
                    if (reservation.isFullyPaid) {
                        PaymentUiState.Complete(reservation)
                    } else {
                        PaymentUiState.AwaitingBalance(reservation)
                    }
                },
                onFailure = { e -> e.toPaymentError() },
            )
        }
    }

    fun payBalance() {
        val user = authUser.value ?: return
        val awaiting = _ui.value as? PaymentUiState.AwaitingBalance ?: return
        val reservation = awaiting.reservation
        viewModelScope.launch {
            _ui.value = PaymentUiState.ProcessingBalance
            val result = payReservationBalance(reservation.id, user.uid)
            _ui.value = result.fold(
                onSuccess = { PaymentUiState.Complete(it) },
                onFailure = { e -> e.toPaymentError() },
            )
        }
    }

    private fun Throwable.toPaymentError(): PaymentUiState.Error =
        if (this is BookingConflictException) {
            PaymentUiState.Error("Someone else just reserved this room. Please pick another listing.")
        } else {
            PaymentUiState.Error(message ?: "Could not complete reservation.")
        }
}
