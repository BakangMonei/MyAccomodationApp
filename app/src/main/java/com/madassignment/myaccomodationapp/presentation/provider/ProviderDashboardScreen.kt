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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import com.madassignment.myaccomodationapp.domain.model.Reservation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
) {
    val listings by viewModel.myListings.collectAsStateWithLifecycle()
    val activeReservations by viewModel.activeReservations.collectAsStateWithLifecycle()
    val reservationByListingId by viewModel.reservationByListingId.collectAsStateWithLifecycle()
    val editingListing by viewModel.editingListing.collectAsStateWithLifecycle()
    val publishing by viewModel.publishing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("1500") }
    var deposit by rememberSaveable { mutableStateOf("750") }
    var location by rememberSaveable { mutableStateOf(GaboroneRegion.CBD) }
    var type by rememberSaveable { mutableStateOf("Single Room") }
    var amenities by rememberSaveable { mutableStateOf("Wi-Fi, Study desk") }
    var displayName by rememberSaveable { mutableStateOf("Host") }
    var availabilityMillis by rememberSaveable { mutableLongStateOf(Instant.now().toEpochMilli()) }
    val selectedUris = remember { mutableStateListOf<android.net.Uri>() }
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var listingToDelete by remember { mutableStateOf<Listing?>(null) }

    val isEditMode = editingListing != null
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())
    }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS),
    ) { uris ->
        selectedUris.clear()
        selectedUris.addAll(uris.take(MAX_PHOTOS))
    }

    LaunchedEffect(editingListing?.id) {
        val listing = editingListing
        if (listing != null) {
            title = listing.title
            price = listing.price.toInt().toString()
            deposit = listing.depositAmount.toInt().toString()
            location = listing.location
            type = listing.type
            amenities = listing.amenities.joinToString(", ")
            displayName = listing.providerDisplayName
            availabilityMillis = listing.availabilityDate.toEpochMilli()
            existingImageUrls = listing.imageUrls
            selectedUris.clear()
        }
    }

    fun resetCreateForm() {
        title = ""
        price = "1500"
        deposit = "750"
        location = GaboroneRegion.CBD
        type = "Single Room"
        amenities = "Wi-Fi, Study desk"
        displayName = "Host"
        availabilityMillis = Instant.now().toEpochMilli()
        existingImageUrls = emptyList()
        selectedUris.clear()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbar.showSnackbar(message)
            when (message) {
                "Listing published" -> resetCreateForm()
                "Listing updated" -> {
                    selectedUris.clear()
                    existingImageUrls = emptyList()
                }
                "Listing deleted" -> Unit
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = availabilityMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { availabilityMillis = it }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    listingToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { listingToDelete = null },
            title = { Text("Delete listing?") },
            text = {
                Text(
                    "Delete \"${target.title}\"? This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteListing(target.id)
                        listingToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { listingToDelete = null }) { Text("Cancel") }
            },
        )
    }

    val hasPhotos = selectedUris.isNotEmpty() || existingImageUrls.isNotEmpty()
    val canSave = !publishing && hasPhotos && title.isNotBlank()

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
                    if (isEditMode) "Edit your listing — reservation status is kept unchanged."
                    else "Create a listing — photos upload to Firebase Storage and appear for students in real time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Text(
                    if (isEditMode) "Edit listing" else "New listing",
                    style = MaterialTheme.typography.titleMedium,
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
                Text("Region", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GaboroneRegion.ALL.size) { index ->
                        val region = GaboroneRegion.ALL[index]
                        FilterChip(
                            selected = location == region,
                            onClick = { location = region },
                            label = { Text(region, maxLines = 1) },
                        )
                    }
                }
            }
            item {
                val typeOptions = listOf("Single Room", "Sharing", "Flat", "Bachelor")
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeOptions.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) },
                        )
                    }
                }
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
                Text("Available from", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(dateFormatter.format(Instant.ofEpochMilli(availabilityMillis)))
                }
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
                        Text(if (isEditMode) "Replace photos" else "Choose images")
                    }
                    if (selectedUris.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = { selectedUris.clear() },
                            label = { Text("Clear new (${selectedUris.size})") },
                        )
                    }
                }
            }
            item {
                if (selectedUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                } else if (isEditMode && existingImageUrls.isNotEmpty()) {
                    Text(
                        "Current photos (choose new images to replace)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(existingImageUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(132.dp)
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                } else if (!isEditMode) {
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
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isEditMode) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearEditor()
                                resetCreateForm()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !publishing,
                        ) {
                            Text("Cancel edit")
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.saveListing(
                                displayName = displayName,
                                title = title,
                                price = price.toDoubleOrNull() ?: 0.0,
                                deposit = deposit.toDoubleOrNull() ?: 0.0,
                                location = location,
                                type = type,
                                amenitiesText = amenities,
                                imageUris = selectedUris.toList(),
                                availabilityMillis = availabilityMillis,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = canSave,
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
                            Text(
                                when {
                                    publishing -> "Saving…"
                                    isEditMode -> "Save changes"
                                    else -> "Publish listing"
                                },
                            )
                        }
                    }
                }
            }
            if (activeReservations.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Active bookings", style = MaterialTheme.typography.titleLarge)
                }
                items(activeReservations, key = { it.id }) { booking ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                booking.listingTitle.ifBlank { "Listing" },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "Reserved by ${booking.studentLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Deposit receipt: ${booking.receiptNumber} · P${booking.depositAmount.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (!booking.isFullyPaid) {
                                Text(
                                    "Balance due: P${booking.balanceAmount.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Your listings", style = MaterialTheme.typography.titleLarge)
            }
            if (listings.isEmpty()) {
                item {
                    Text(
                        "No listings yet. Publish one above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(listings, key = { it.id }) { listing ->
                ProviderListingCard(
                    listing = listing,
                    reservation = reservationByListingId[listing.id],
                    hasActiveReservation = reservationByListingId.containsKey(listing.id),
                    onEdit = { viewModel.loadForEdit(listing) },
                    onDelete = { listingToDelete = listing },
                )
            }
        }
    }
}

@Composable
private fun ProviderListingCard(
    listing: Listing,
    reservation: Reservation?,
    hasActiveReservation: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (listing.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = listing.imageUrls.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Text(listing.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${listing.location} · ${listing.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "P${listing.price.toInt()} / mo · deposit P${listing.depositAmount.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                listing.status.wireValue,
                style = MaterialTheme.typography.labelMedium,
                color = if (listing.status == ListingStatus.Reserved) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            if (listing.status == ListingStatus.Reserved) {
                val reservedLabel = reservation?.studentLabel ?: listing.reservedBy
                Text(
                    "Reserved by ${reservedLabel ?: "a student"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                reservation?.payerEmail?.let { email ->
                    Text(
                        email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !hasActiveReservation,
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
            if (hasActiveReservation) {
                Text(
                    "Delete disabled while a student has an active reservation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val MAX_PHOTOS = 8
