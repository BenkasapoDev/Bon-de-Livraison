package com.infosetgroup.delivery.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infosetgroup.delivery.data.DeliveryEntity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingScreen(viewModel: PendingViewModel = viewModel(), onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val isLoading by viewModel.isLoading.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val list by viewModel.list.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pending Deliveries",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading pending deliveries...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                        )
                    }
                } else {
                    if (list.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending deliveries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(list) { delivery ->
                                DeliveryCard(delivery)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.syncDeliveries() }) {
                if (syncing) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Sync, contentDescription = "Sync")
                }
            }
        }
    )
}

@Composable
fun DeliveryCard(delivery: DeliveryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val context = LocalContext.current
            val imageModifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))

            if (!delivery.receiverProofPath.isNullOrBlank()) {
                val f = File(delivery.receiverProofPath!!)
                if (f.exists()) {
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    if (bmp != null) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "proof", modifier = imageModifier, contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                            Text("No image")
                        }
                    }
                } else {
                    Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                        Text("No image")
                    }
                }
            } else {
                Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
                    Text("—")
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = delivery.item, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Serial: ${delivery.serialNumber}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Shop: ${delivery.shop}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // status badge
            val statusText = delivery.status ?: "PENDING"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (statusText.uppercase()) {
                    "SENT" -> MaterialTheme.colorScheme.primary
                    "FAILED" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
