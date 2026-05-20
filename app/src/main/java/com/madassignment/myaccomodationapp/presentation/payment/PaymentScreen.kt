package com.madassignment.myaccomodationapp.presentation.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRoute(
    onBack: () -> Unit,
    onFlowFinishedNavigateToProfile: () -> Unit,
    paymentViewModel: PaymentViewModel = hiltViewModel(),
) {
    val listing by paymentViewModel.listing.collectAsStateWithLifecycle()
    val ui by paymentViewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            listing?.let { l ->
                Text(l.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Deposit due: P${l.depositAmount.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (ui !is PaymentUiState.AwaitingBalance && ui !is PaymentUiState.Complete) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Balance after deposit: P${(l.price - l.depositAmount).coerceAtLeast(0.0).toInt()} (first month remainder)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                if (l.status != ListingStatus.Available &&
                    ui !is PaymentUiState.AwaitingBalance &&
                    ui !is PaymentUiState.Complete &&
                    ui !is PaymentUiState.ProcessingBalance
                ) {
                    Text(
                        "This room is already reserved. Please choose another listing.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Go back")
                    }
                } else when (val state = ui) {
                    PaymentUiState.Idle -> {
                        Button(
                            onClick = { paymentViewModel.payDeposit(l.depositAmount) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = l.status == ListingStatus.Available,
                        ) {
                            Text("Pay deposit (sandbox)")
                        }
                    }
                    PaymentUiState.ProcessingDeposit -> {
                        CircularProgressIndicator()
                    }
                    is PaymentUiState.AwaitingBalance -> {
                        val r = state.reservation
                        Text(
                            "Deposit paid · Receipt: ${r.receiptNumber}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text("Amount paid: P${r.depositAmount.toInt()}")
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Balance due: P${r.balanceAmount.toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { paymentViewModel.payBalance() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Pay balance (sandbox)")
                        }
                    }
                    PaymentUiState.ProcessingBalance -> {
                        CircularProgressIndicator()
                    }
                    is PaymentUiState.Complete -> {
                        val r = state.reservation
                        LaunchedEffect(r.id, r.balanceReceiptNumber) {
                            delay(3000)
                            onFlowFinishedNavigateToProfile()
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "All set!",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            "Your booking is complete.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Deposit receipt: ${r.receiptNumber}", style = MaterialTheme.typography.bodyMedium)
                        if (r.balanceReceiptNumber != null) {
                            Text(
                                "Balance receipt: ${r.balanceReceiptNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text("Total paid: P${r.amount.toInt()}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Opening your profile…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is PaymentUiState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onBack) { Text("Go back") }
                    }
                }
            } ?: Text("Loading…")
        }
    }
}
