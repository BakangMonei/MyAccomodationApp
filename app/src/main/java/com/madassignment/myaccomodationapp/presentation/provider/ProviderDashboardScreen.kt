package com.madassignment.myaccomodationapp.presentation.provider

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.madassignment.myaccomodationapp.domain.model.GaboroneRegion
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
) {
    val listings by viewModel.myListings.collectAsStateWithLifecycle()
    val publishing by viewModel.publishing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("1500") }
    var deposit by rememberSaveable { mutableStateOf("750") }
    var location by rememberSaveable { mutableStateOf(GaboroneRegion.CBD) }
    var type by rememberSaveable { mutableStateOf("Single Room") }
    var amenities by rememberSaveable { mutableStateOf("Wi-Fi, Study desk") }
    var displayName by rememberSaveable { mutableStateOf("Host") }
    val availability by remember { mutableLongStateOf(Instant.now().toEpochMilli()) }
    val selectedUris = remember { mutableStateListOf<android.net.Uri>() }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS),
    ) { uris ->
        selectedUris.clear()
        selectedUris.addAll(uris.take(MAX_PHOTOS))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbar.showSnackbar(message)
            if (message == "Listing published") {
                selectedUris.clear()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Provider dashboard", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Upload photos — they are stored in Firebase Storage and shown to students in real time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("How students see your name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Listing title") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Rent (BWP)") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = deposit,
                        onValueChange = { deposit = it },
                        label = { Text("Deposit") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Region") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = amenities,
                    onValueChange = { amenities = it },
                    label = { Text("Amenities (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Photos (max $MAX_PHOTOS)",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            pickImages.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Choose images")
                    }
                    if (selectedUris.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedUris.clear()
                            },
                            label = { Text("Clear (${selectedUris.size})") },
                        )
                    }
                }
            }
            item {
                if (selectedUris.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No photos selected yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(selectedUris.size) { idx ->
                            val uri = selectedUris[idx]
                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .height(132.dp)
                                            .aspectRatio(3f / 4f)
                                            .clip(RoundedCornerShape(12.dp)),
                                    )
                                    IconButton(
                                        onClick = { selectedUris.removeAt(idx) },
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        viewModel.publishListing(
                            displayName = displayName,
                            title = title,
                            price = price.toDoubleOrNull() ?: 0.0,
                            deposit = deposit.toDoubleOrNull() ?: 0.0,
                            location = location,
                            type = type,
                            amenitiesText = amenities,
                            imageUris = selectedUris.toList(),
                            availabilityMillis = availability,
                            existingId = null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !publishing && selectedUris.isNotEmpty(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (publishing) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(if (publishing) "Uploading…" else "Publish listing")
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Your listings", style = MaterialTheme.typography.titleLarge)
            }
            items(listings, key = { it.id }) { listing ->
                ProviderListingCard(listing = listing)
            }
        }
    }
}

@Composable
private fun ProviderListingCard(
    listing: com.madassignment.myaccomodationapp.domain.model.Listing,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = listing.imageUrls.first(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(listing.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${listing.location} · ${listing.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "P${listing.price.toInt()} / mo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val MAX_PHOTOS = 8
