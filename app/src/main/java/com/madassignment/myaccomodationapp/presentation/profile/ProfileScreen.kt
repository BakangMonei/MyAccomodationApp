package com.madassignment.myaccomodationapp.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val activeReservations by viewModel.activeReservations.collectAsStateWithLifecycle()
    val cancellingIds by viewModel.cancellingIds.collectAsStateWithLifecycle()
    var deletePassword by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbar.showSnackbar(it) }
    }

    if (!isSignedIn) {
        Column(
            modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to see your reservations, save preferences, and chat with landlords.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNavigateToAuth, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in / Register")
            }
        }
        return
    }

    if (profile == null) {
        Column(
            modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                "Loading your profile...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            profile?.let { p ->
                Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                Text(p.email, style = MaterialTheme.typography.bodyMedium)
                Text("Role: ${p.role.name}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                val dateLabel = p.preferences.availabilityOnOrBefore?.let { instant ->
                    DateTimeFormatter.ofPattern("d MMM yyyy")
                        .withZone(ZoneId.systemDefault())
                        .format(instant)
                }
                Text(
                    buildString {
                        append("Saved filters: BWP ${p.preferences.minPriceBwp.toInt()}–${p.preferences.maxPriceBwp.toInt()}")
                        if (p.preferences.locations.isNotEmpty()) {
                            append(" · ${p.preferences.locations.joinToString()}")
                        }
                        dateLabel?.let { append(" · Available by $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Active reservations", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        if (activeReservations.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No reservations yet.")
                        Text(
                            "Browse listings and secure a place with the sandbox deposit flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(activeReservations, key = { it.id }) { res ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        val title = res.listingTitle.ifBlank { "Listing" }
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (res.isFullyPaid) "Fully paid" else "Balance due: P${res.balanceAmount.toInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (res.isFullyPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        )
                        Text("Deposit receipt: ${res.receiptNumber} · P${res.depositAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                        if (res.balanceReceiptNumber != null) {
                            Text(
                                "Balance receipt: ${res.balanceReceiptNumber} · P${res.balanceAmount.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text("Total paid: P${res.amount.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        val isCancelling = res.id in cancellingIds
                        OutlinedButton(
                            onClick = { viewModel.cancelReservation(res.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCancelling,
                        ) {
                            if (isCancelling) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Cancelling…")
                                }
                            } else {
                                Text("Cancel reservation (undo)")
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
            Spacer(Modifier.height(24.dp))
            Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = deletePassword,
                onValueChange = { deletePassword = it },
                label = { Text("Enter password to confirm deletion") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.deleteAccount(deletePassword) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete account permanently") }
        }
    }
    }
}
