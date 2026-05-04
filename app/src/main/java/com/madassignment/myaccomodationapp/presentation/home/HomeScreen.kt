package com.madassignment.myaccomodationapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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
    val paging = viewModel.listings.collectAsLazyPagingItems()
    var sheetOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = false,
                onClick = { sheetOpen = true },
                label = { Text("Filters") },
            )
            Text(
                "BWP ${applied.minPrice.toInt()}–${applied.maxPrice.toInt()}",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        when (paging.loadState.refresh) {
            is LoadState.Loading -> {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(6) {
                        ListingSkeleton()
                    }
                }
            }
            is LoadState.Error -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Could not load listings. Check connection and try again.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { paging.retry() }) { Text("Retry") }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        count = paging.itemCount,
                        key = paging.itemKey { it.id },
                    ) { index ->
                        val item = paging[index]
                        if (item == null) {
                            ListingSkeleton()
                        } else {
                            ListingRow(item) { onOpenListing(item.id) }
                        }
                    }
                    item {
                        if (paging.loadState.append is LoadState.Loading) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
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
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
private fun ListingRow(
    listing: com.madassignment.myaccomodationapp.domain.model.Listing,
    onClick: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = listing.imageUrls.first(),
                contentDescription = null,
                modifier = Modifier
                    .height(88.dp)
                    .fillMaxWidth(0.35f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
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
