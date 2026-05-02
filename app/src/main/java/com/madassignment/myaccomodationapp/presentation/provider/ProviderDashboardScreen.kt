package com.madassignment.myaccomodationapp.presentation.provider

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madassignment.myaccomodationapp.domain.model.GaboroneRegion
import java.time.Instant

@Composable
fun ProviderDashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
) {
    val listings by viewModel.myListings.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("1500") }
    var deposit by rememberSaveable { mutableStateOf("750") }
    var location by rememberSaveable { mutableStateOf(GaboroneRegion.CBD) }
    var type by rememberSaveable { mutableStateOf("Single Room") }
    var amenities by rememberSaveable { mutableStateOf("Wi-Fi, Study desk") }
    var images by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("Host") }
    val availability by remember { mutableLongStateOf(Instant.now().toEpochMilli()) }

    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        item {
            Text("Provider dashboard", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("How students see your name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Listing title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Monthly rent (BWP)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = deposit,
                onValueChange = { deposit = it },
                label = { Text("Deposit (BWP)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Region") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amenities,
                onValueChange = { amenities = it },
                label = { Text("Amenities (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = images,
                onValueChange = { images = it },
                label = { Text("Image URLs (comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.saveDraft(
                        displayName = displayName,
                        title = title,
                        price = price.toDoubleOrNull() ?: 0.0,
                        deposit = deposit.toDoubleOrNull() ?: 0.0,
                        location = location,
                        type = type,
                        amenitiesText = amenities,
                        imageUrlsText = images,
                        availabilityMillis = availability,
                        existingId = null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Publish listing") }
            Spacer(Modifier.height(24.dp))
            Text("Your listings", style = MaterialTheme.typography.titleMedium)
        }
        items(listings, key = { it.id }) { l ->
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(l.title, style = MaterialTheme.typography.titleSmall)
                Text("${l.location} · P${l.price.toInt()}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

