@file:Suppress("ModifierParameter")
package com.infosetgroup.delivery.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infosetgroup.delivery.R
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.ui.theme.cardGradientEnd
import com.infosetgroup.delivery.ui.theme.cardGradientStart
import com.infosetgroup.delivery.ui.theme.primaryGradientEnd
import com.infosetgroup.delivery.ui.theme.primaryGradientStart
import com.infosetgroup.delivery.ui.theme.success
import com.infosetgroup.delivery.ui.theme.failure
import com.infosetgroup.delivery.ui.theme.md_grey_200
import com.infosetgroup.delivery.ui.theme.md_grey_300
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun PendingScreen(
    viewModel: PendingViewModel = viewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // fetch once when screen composes
    LaunchedEffect(Unit) { viewModel.fetchDeliveries() }

    val isLoading by viewModel.isLoading.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val list by viewModel.list.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()

    var selectedDelivery by remember { mutableStateOf<DeliveryEntity?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = {
            // Taller curved header with decorative blobs to better match the Dribbble inspiration — UI-only
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(brush = Brush.horizontalGradient(listOf(primaryGradientStart, primaryGradientEnd)))
            ) {
                // decorative blobs (vector drawables)
                Icon(painter = painterResource(id = R.drawable.blob_purple), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(160.dp).offset(x = (-30).dp, y = (-20).dp).alpha(0.95f))
                Icon(painter = painterResource(id = R.drawable.blob_teal), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = 20.dp).alpha(0.9f))

                // curved bottom overlay using a Box with rounded corners to simulate arc
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp)
                    .background(color = Color.Transparent)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                )

                // AppBar content aligned inside header
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp), verticalAlignment = Alignment.Top) {
                    IconButton(onClick = { onBack() }, modifier = Modifier.size(36.dp).background(color = Color.White.copy(alpha = 0.08f), shape = CircleShape)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pending Deliveries", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp), color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$pendingCount waiting to sync", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                    }

                    // small circular avatar as an accent on right
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    isLoading -> ShimmerLoading()
                    list.isEmpty() -> EmptyStateWithCTA(onAdd = {})
                    else -> AnimatedDeliveryList(list = list, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), onItemClick = { selectedDelivery = it })
                }

                // separate bottom sync bar pinned to bottom with stronger prominence
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    HorizontalDivider()
                    PendingNavBar(pendingCount = pendingCount, syncing = syncing, onSync = { viewModel.syncDeliveries() })
                }
                // Delivery details sheet
                if (selectedDelivery != null) {
                    ModalBottomSheet(
                        onDismissRequest = { selectedDelivery = null },
                        sheetState = sheetState
                    ) {
                        DeliveryDetailsSheet(delivery = selectedDelivery!!, onClose = { selectedDelivery = null })
                    }
                }
            }
        }
    )
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

@Composable
fun EmptyStateWithCTA(onAdd: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No pending deliveries", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap the form to add a delivery or sync when online.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onAdd) { Text("Add delivery") }
        }
    }
}

@Composable
fun AnimatedDeliveryList(list: List<DeliveryEntity>, modifier: Modifier = Modifier, onItemClick: (DeliveryEntity) -> Unit = {}) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
        items(list, key = { it.id }) { delivery ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(300, easing = FastOutSlowInEasing))
            ) {
                DeliveryItemCard(delivery = delivery, onClick = { onItemClick(delivery) })
            }
        }
    }
}

@Composable
fun DeliveryItemCard(delivery: DeliveryEntity, onClick: () -> Unit = {}) {
    val local = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        // Card with extra vertical padding so the overlapping image can sit comfortably
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier
                .background(brush = Brush.verticalGradient(listOf(cardGradientStart, cardGradientEnd)))
                .clickable { onClick() }
            ) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 120.dp, top = 18.dp, bottom = 18.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = delivery.item, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = delivery.shop, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Agent: ${delivery.deliveryAgent}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = formatTime(delivery.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // small action or chevron (kept visual only)
                    Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Overlapping image on the left — stands out from the card
        val imageModifier = Modifier
            .size(104.dp)
            .offset(x = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .shadow(10.dp, RoundedCornerShape(14.dp))

        val path = delivery.receiverProofPath
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(local).data(file).crossfade(true).build(),
                    contentDescription = "proof",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                    Text("No image", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                Text("—", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Status pill positioned at top-end of the card
        val statusText = delivery.status ?: "PENDING"
        val statusColor by animateColorAsState(targetValue = when (statusText.uppercase(Locale.getDefault())) {
            "SENT" -> success
            "FAILED" -> failure
            else -> MaterialTheme.colorScheme.secondary
        }, animationSpec = tween(durationMillis = 360))

        Box(modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = (-12).dp, y = 8.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.95f), tonalElevation = 6.dp) {
                Text(text = statusText.uppercase(Locale.getDefault()), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PendingNavBar(pendingCount: Int, syncing: Boolean, onSync: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // pending badge
            Box(modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(text = "$pendingCount pending", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.weight(1f))

            // sync button
            Button(onClick = { if (!syncing) onSync() }, enabled = !syncing, shape = RoundedCornerShape(12.dp)) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.Sync, contentDescription = "sync", tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = "Envoyer", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeliveryDetailsSheet(delivery: DeliveryEntity, onClose: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Delivery details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Item: ${delivery.item}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Serial: ${delivery.serialNumber}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Shop: ${delivery.shop}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Receiver: ${delivery.receiver}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Agent: ${delivery.deliveryAgent}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onClose) { Text("Close") }
        }
    }
}

private fun formatTime(epoch: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(epoch))
    } catch (_: Exception) {
        ""
    }
}
