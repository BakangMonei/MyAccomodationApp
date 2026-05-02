package com.madassignment.myaccomodationapp.presentation.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRoute(
    onBack: () -> Unit,
    onReceiptDone: () -> Unit,
    paymentViewModel: PaymentViewModel = hiltViewModel(),
) {
    val listing by paymentViewModel.listing.collectAsStateWithLifecycle()
    val ui by paymentViewModel.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sandbox payment") },
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
                Spacer(Modifier.height(24.dp))
                when (val state = ui) {
                    PaymentUiState.Idle -> {
                        Button(
                            onClick = { paymentViewModel.pay(l.depositAmount) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Pay deposit (sandbox)")
                        }
                    }
                    PaymentUiState.Processing -> {
                        CircularProgressIndicator()
                    }
                    is PaymentUiState.Success -> {
                        val r = state.reservation
                        Text("Payment successful", style = MaterialTheme.typography.titleMedium)
                        Text("Receipt: ${r.receiptNumber}", style = MaterialTheme.typography.bodyLarge)
                        Text("Amount: P${r.amount.toInt()}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onReceiptDone) { Text("Done") }
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
