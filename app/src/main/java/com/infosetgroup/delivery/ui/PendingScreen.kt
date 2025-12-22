@file:Suppress("ModifierParameter")
package com.infosetgroup.delivery.ui

// NEW imports to reuse the MainActivity design utils and colors

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.infosetgroup.delivery.CollapsedHeader
import com.infosetgroup.delivery.DeliveryColors
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.ui.theme.md_grey_200
import com.infosetgroup.delivery.ui.theme.md_grey_300
import com.infosetgroup.delivery.util.SnackbarHelper
import com.infosetgroup.delivery.util.mapNetworkErrorToFrench
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun PendingScreen(
    viewModel: PendingViewModel = viewModel(),
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // fetch once when screen composes
    LaunchedEffect(Unit) { viewModel.fetchDeliveries() }

    val isLoading by viewModel.isLoading.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val singleSyncing by viewModel.singleSyncing.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    // use shared SnackbarHostState passed from MainActivity
    val coroutineScope = rememberCoroutineScope()

    // collect one-shot sync events and show French messages (map common network errors to friendlier text)
    LaunchedEffect(Unit) {
        viewModel.syncEvents.collect { res ->
            val msg = when (res) {
                is com.infosetgroup.delivery.repository.SyncResult.Success -> "${res.syncedCount} livraisons synchronisées"
                is com.infosetgroup.delivery.repository.SyncResult.Failure -> mapNetworkErrorToFrench(res.error)
                com.infosetgroup.delivery.repository.SyncResult.NothingToSync -> "Aucune livraison à synchroniser"
            }

            // use centralized snackbar helper to avoid duplicates across the app
            // We're already inside a suspend lambda (LaunchedEffect) so call directly
            SnackbarHelper.showIfUnique(snackbarHostState, msg, SnackbarDuration.Long)
        }
    }

    var selectedDelivery by remember { mutableStateOf<DeliveryEntity?>(null) }

    // Collect PagingData from ViewModel
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()

    // Instead of returning early which can be fragile across recompositions, render the Scaffold
    // and place the header inside the content so we can fully control when it appears. This
    // prevents the situation where both the parent topBar and the centralized detail header
    // are visible at the same time.
    Scaffold(
        modifier = modifier,
        // Move the bulk sync bar into Scaffold.bottomBar so it is always visible above the
        // application bottom area. Hide it while viewing a single item's detail.
        bottomBar = {
            if (selectedDelivery == null) {
                PendingNavBar(pendingCount = pendingCount, syncing = syncing, onBulkSync = {
                    coroutineScope.launch { viewModel.syncDeliveries() }
                })
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(DeliveryColors.Background)
            .padding(padding)) {

            // Render the header only when not showing details. When a detail is selected
            // the centralized `DeliveryDetailScreen` will render its own header, so we must
            // not display the parent header in that state.
            if (selectedDelivery == null) {
                CollapsedHeader(title = "Livraisons en attente", subtitle = "$pendingCount en attente", showBack = true, onBack = onBack)

                // push the list content below the header by adding a spacer at top of content area
                Column(modifier = Modifier.fillMaxSize().padding(top = 64.dp)) {
                    when {
                        // show top-level loading when first loading or when paging initial load
                        isLoading -> ShimmerLoading()
                        pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading -> EmptyStateWithCTA(onAdd = onAdd)
                        else -> PagingTicketList(pagingItems = pagingItems, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), onItemClick = { selectedDelivery = it })
                    }

                    // keep a flexible spacer to push list above the bottomBar (Scaffold bottomBar handles the visible bar)
                    Spacer(modifier = Modifier.weight(1f))

                    // removed inline HorizontalDivider and PendingNavBar to avoid duplicates; the bar is now shown
                    // via Scaffold.bottomBar
                }

            } else {
                // If an item is selected show the centralized detail screen as full-screen replacement.
                val d = selectedDelivery!!
                val details: List<Triple<ImageVector, String, String>> = listOf(
                    Triple(Icons.Filled.QrCode, "N° Série", d.serialNumber),
                    Triple(Icons.Filled.SimCard, "SIM", d.sim),
                    Triple(Icons.Filled.Store, "Marchand", d.merchant),
                    Triple(Icons.Filled.Store, "Magasin", d.shop),
                    Triple(Icons.Filled.Person, "Responsable", d.receiver),
                    Triple(Icons.Filled.LocalShipping, "Livreur", d.deliveryAgent),
                    Triple(Icons.Filled.VpnKey, "Statut", mapStatusToFrench(d.status))
                )

                DeliveryDetailScreen(
                    title = d.item,
                    subtitle = "Détail de la livraison",
                    details = details,
                    imagePath = d.receiverProofPath,
                    onBack = { selectedDelivery = null },
                    // forward pending screen's sync action so user can sync from details as well
                    onSync = {
                        coroutineScope.launch {
                            // single-item sync is handled by the ViewModel which will emit a result
                            // back to the UI through `syncEvents` (keeps a single snackbar source).
                            viewModel.syncSingle(d.id)
                        }
                    },
                    // if the single item is currently syncing show the spinner, otherwise false
                    syncing = (singleSyncing != null && singleSyncing == d.id),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // removed floating bulk overlay to avoid duplicate sync controls
        }
    }
}

@Composable
fun PagingTicketList(pagingItems: LazyPagingItems<DeliveryEntity>, modifier: Modifier = Modifier, onItemClick: (DeliveryEntity) -> Unit = {}) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 100.dp)) {
        // handle refresh/load states
        when (pagingItems.loadState.refresh) {
            is LoadState.Loading -> item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DeliveryColors.Accent) } }
            is LoadState.Error -> item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(text = "Erreur lors du chargement", color = MaterialTheme.colorScheme.error) } }
            else -> {}
        }

        items(count = pagingItems.itemCount) { index ->
            val delivery = pagingItems[index] ?: return@items

            // Use centralized TicketCard for consistent visuals
            TicketCard(
                title = delivery.item,
                shop = delivery.shop,
                serialNumber = delivery.serialNumber,
                deliveryAgent = delivery.deliveryAgent,
                // DeliveryEntity doesn't have `code`; TicketCard expects a string flag used for styling.
                // Use `status` (nullable) converted to a non-null String to keep visual behavior stable.
                code = delivery.status ?: "",
                imagePath = delivery.receiverProofPath,
                // NEW: format the createdAt (millis) to French string for display
                createdAt = formatMillisToFrench(delivery.createdAt),
                onClick = { onItemClick(delivery) }
            )
        }

        // footer: show loading or retry
        item {
            when (pagingItems.loadState.append) {
                is LoadState.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DeliveryColors.Accent) }
                is LoadState.Error -> Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { Button(onClick = { pagingItems.retry() }) { Text("Réessayer") } }
                else -> Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ShimmerLoading() {
    // Simple shimmer skeleton for list
    val shimmerColors = listOf(
        md_grey_200.copy(alpha = 0.6f),
        md_grey_300.copy(alpha = 0.3f),
        md_grey_200.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing))
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 1000f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
            )
        }
    }
}

//
@Composable
fun EmptyStateWithCTA(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Aucune livraison en attente", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Utilisez le formulaire pour ajouter une livraison ou synchronisez lorsque vous êtes en ligne.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onAdd) { Text("Ajouter une livraison") }
        }
    }
}


// New ticket-style list to match HistoryScreen visual
@Deprecated("Use PagingTicketList instead")
@Composable
fun TicketDeliveryList(list: List<DeliveryEntity>, modifier: Modifier = Modifier, onItemClick: (DeliveryEntity) -> Unit = {}) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 100.dp)) {
        items(list) { delivery ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onItemClick(delivery) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    // Status strip
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(
                                when (delivery.status?.uppercase(Locale.getDefault())) {
                                    "SENT" -> DeliveryColors.PrimaryCorral
                                    "FAILED" -> MaterialTheme.colorScheme.error
                                    else -> DeliveryColors.PrimaryLight
                                }
                            )
                    )

                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = delivery.item,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeliveryColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(DeliveryColors.InputBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = delivery.shop,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeliveryColors.TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.QrCode, null, Modifier.size(14.dp), DeliveryColors.Accent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = delivery.serialNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = DeliveryColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Filled.Person, null, Modifier.size(14.dp), DeliveryColors.Accent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = delivery.deliveryAgent,
                                style = MaterialTheme.typography.bodySmall,
                                color = DeliveryColors.TextSecondary
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxHeight().padding(end = 12.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DeliveryColors.BorderSubtle)
                    }
                }
            }
        }
    }
}

@Composable
fun PendingNavBar(pendingCount: Int, syncing: Boolean, onBulkSync: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // pending badge
            Box(modifier = Modifier
                .background(color = DeliveryColors.InputBg, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(text = "$pendingCount en attente", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = DeliveryColors.TextPrimary)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bulk sync button (main affordance for Pending screen). Labelled clearly
            // as 'Envoyer tout' to indicate a bulk operation. Disabled when syncing or
            // when there is nothing to sync.
            Button(onClick = { if (!syncing) onBulkSync() }, enabled = !syncing && pendingCount > 0, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DeliveryColors.Accent)) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.Sync, contentDescription = "synchroniser tout", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = "Envoyer tout", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// PendingDetailScreen removed. Use DeliveryDetailScreen (in DetailComponents.kt) to display full details for both History and Pending items.

private fun mapStatusToFrench(status: String?): String {
    return when (status?.uppercase(Locale.getDefault())) {
        "SENT" -> "ENVOYÉ"
        "FAILED" -> "ÉCHOUÉ"
        "PENDING" -> "EN ATTENTE"
        null -> "EN ATTENTE"
        else -> status.uppercase(Locale.getDefault())
    }
}
