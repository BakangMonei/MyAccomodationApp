package com.madassignment.myaccomodationapp.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    var deletePassword by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier
            .fillMaxSize()
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
                Text(
                    "Saved filters: BWP ${p.preferences.minPriceBwp.toInt()}–${p.preferences.maxPriceBwp.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Active reservations", style = MaterialTheme.typography.titleMedium)
        }
        if (reservations.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
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
            items(reservations, key = { it.id }) { res ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Receipt ${res.receiptNumber}", style = MaterialTheme.typography.titleSmall)
                        Text("Listing ${res.listingId}")
                        Text("P${res.amount.toInt()}")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
            Spacer(Modifier.height(16.dp))
            Text("Delete account (requires password)", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = deletePassword,
                onValueChange = { deletePassword = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.deleteAccount(deletePassword) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete account permanently") }
        }
    }
}
