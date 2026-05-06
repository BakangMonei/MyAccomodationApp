package com.madassignment.myaccomodationapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.madassignment.myaccomodationapp.domain.model.GaboroneRegion
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val applied by viewModel.appliedFilters.collectAsStateWithLifecycle()
    val listings by viewModel.listings.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BadgedBox(badge = { Badge { Text("Live") } }) {
                AssistChip(
                    onClick = { sheetOpen = true },
                    label = { Text("Filters") },
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Near campus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "BWP ${applied.minPrice.toInt()}–${applied.maxPrice.toInt()} · updates instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            listings == null -> {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(6) {
                        ListingSkeleton()
                    }
                }
            }
            listings.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No listings match these filters",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Widen the price range or clear region filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                val data = listings
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "${data.size} places · Firestore realtime",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(
                        count = data.size,
                        key = { data[it].id },
                    ) { index ->
                        val item = data[index]
                        ListingCard(item) { onOpenListing(item.id) }
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        FilterBottomSheet(
            initial = applied,
            onDismiss = { sheetOpen = false },
            onApply = {
                viewModel.applyFilters(it)
                sheetOpen = false
            },
        )
    }
}

@Composable
private fun ListingSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth(0.55f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
private fun ListingCard(
    listing: com.madassignment.myaccomodationapp.domain.model.Listing,
    onClick: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                AsyncImage(
                    model = listing.imageUrls.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                                ),
                            ),
                        ),
                )
                Text(
                    listing.type,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "P${listing.price.toInt()} / mo",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${listing.location} · by ${listing.providerDisplayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    initial: ListingFilters,
    onDismiss: () -> Unit,
    onApply: (ListingFilters) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var min by remember { mutableStateOf(initial.minPrice.toString()) }
    var max by remember { mutableStateOf(initial.maxPrice.toString()) }
    val locs = remember { mutableStateListOf<String>().apply { addAll(initial.locations) } }
    val types = remember { mutableStateListOf<String>().apply { addAll(initial.types) } }
    var dateMillis by remember {
        mutableStateOf(
            initial.availabilityOnOrBefore?.toEpochMilli()?.toString().orEmpty(),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = min,
                    onValueChange = { min = it },
                    label = { Text("Min BWP") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = max,
                    onValueChange = { max = it },
                    label = { Text("Max BWP") },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Regions", style = MaterialTheme.typography.labelLarge)
            GaboroneRegion.ALL.forEach { region ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = locs.contains(region),
                        onClick = {
                            if (!locs.remove(region)) locs.add(region)
                        },
                        label = { Text(region, maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Type", style = MaterialTheme.typography.labelLarge)
            val typeOptions = listOf("Single Room", "Sharing", "Flat", "Bachelor")
            typeOptions.forEach { t ->
                FilterChip(
                    selected = types.contains(t),
                    onClick = {
                        if (!types.remove(t)) types.add(t)
                    },
                    label = { Text(t) },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dateMillis,
                onValueChange = { dateMillis = it },
                label = { Text("Availability on/before (epoch ms, optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { scope.launch { sheetState.hide() }; onDismiss() },
                    modifier = Modifier.weight(1f),
                ) { Text("Close") }
                Button(
                    onClick = {
                        val filters = ListingFilters(
                            minPrice = min.toDoubleOrNull() ?: 0.0,
                            maxPrice = max.toDoubleOrNull() ?: 20_000.0,
                            locations = locs.toList(),
                            types = types.toList(),
                            availabilityOnOrBefore = dateMillis.toLongOrNull()
                                ?.let { java.time.Instant.ofEpochMilli(it) },
                        )
                        onApply(filters)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }
    }
}
