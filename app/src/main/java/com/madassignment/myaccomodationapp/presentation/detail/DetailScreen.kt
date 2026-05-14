package com.madassignment.myaccomodationapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.madassignment.myaccomodationapp.domain.model.ChatIds
import com.madassignment.myaccomodationapp.domain.model.ListingStatus
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DetailRoute(
    authUid: String?,
    onBack: () -> Unit,
    onReserve: (listingId: String) -> Unit,
    onOpenChat: (chatId: String, peerId: String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listing?.title ?: "Listing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val item = listing
        if (item == null) {
            Column(
                Modifier
                    .padding(padding)
                    .padding(24.dp),
            ) {
                repeat(4) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val pagerState = rememberPagerState(pageCount = { item.imageUrls.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp)),
            ) { page ->
                AsyncImage(
                    model = item.imageUrls[page],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(item.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${item.location} · ${item.type}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "P${item.price.toInt()} / month · deposit P${item.depositAmount.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Available from ${dateFmt.format(item.availabilityDate.atZone(java.time.ZoneId.systemDefault()))}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Landlord: ${item.providerDisplayName}", style = MaterialTheme.typography.bodyMedium)
            Text("Amenities: ${item.amenities.joinToString()}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            if (item.status == ListingStatus.Available) {
                Button(
                    onClick = { 
                        if (authUid != null) onReserve(item.id) 
                        else onOpenChat("auth", "") // Hack to trigger auth, or better, just check in NavHost
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true
                ) {
                    Text(if (authUid != null) "Reserve (sandbox deposit)" else "Sign in to reserve")
                }
            } else {
                Text("Currently reserved.", color = MaterialTheme.colorScheme.error)
            }
            if (authUid != null) {
                if (authUid != item.providerId) {
                    Button(
                        onClick = {
                            val chatId = ChatIds.forStudentAndProvider(authUid, item.providerId)
                            onOpenChat(chatId, item.providerId)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Message landlord")
                    }
                }
            } else {
                Text(
                    "Sign in to chat with the landlord.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
