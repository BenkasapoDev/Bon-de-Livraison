package com.infosetgroup.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infosetgroup.delivery.DeliveryColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.infosetgroup.delivery.CollapsedHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.PhotoCamera

@Composable
fun DetailRowModern(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(DeliveryColors.InputBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DeliveryColors.Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = DeliveryColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = DeliveryColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )
        }
    }
    HorizontalDivider(color = DeliveryColors.BorderSubtle, thickness = 1.dp)
}

/**
 * Centralized detail screen used by History and Pending.
 * - title: main title (item name)
 * - subtitle: small subtitle
 * - details: list of (icon,label,value)
 * - imagePath: optional local path or remote URL of the proof image
 */
@Composable
fun DeliveryDetailScreen(
    title: String,
    subtitle: String = "Détail de la livraison",
    details: List<Triple<ImageVector, String, String>>,
    imagePath: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = DeliveryColors.Background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CollapsedHeader(
                title = title,
                subtitle = subtitle,
                showBack = true,
                onBack = onBack,
                trailing = {
                    // decorative trailing icon
                    // use standard filled assignment icon to avoid unresolved imports
                    Icon(imageVector = Icons.Filled.Assignment, contentDescription = null, tint = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(44.dp))
                }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp)
            ) {
                items(details) { (icon, label, value) ->
                    DetailRowModern(icon, label, value)
                }

                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(text = "Données affichées localement", color = DeliveryColors.TextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                item {
                    // show image if available
                    val raw = imagePath
                    val imageData = when {
                        raw == null -> null
                        raw.startsWith("http", ignoreCase = true) -> raw
                        raw.startsWith("file:", ignoreCase = true) -> raw
                        raw.isNotBlank() -> "file://$raw"
                        else -> null
                    }

                    if (imageData != null) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(text = "Preuve de Réception", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeliveryColors.TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageData)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Preuve de réception",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        // fallback
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(text = "Preuve de Réception", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeliveryColors.TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeliveryColors.InputBg), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = "Pas de preuve", tint = DeliveryColors.TextSecondary, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
