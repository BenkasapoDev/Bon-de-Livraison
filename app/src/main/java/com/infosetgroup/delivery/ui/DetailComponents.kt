package com.infosetgroup.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
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
import androidx.compose.material3.CircularProgressIndicator
import com.infosetgroup.delivery.CollapsedHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import com.infosetgroup.delivery.R
import android.util.Log
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

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

fun formatMillisToFrench(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", Locale.FRENCH)
        sdf.format(java.util.Date(millis))
    } catch (_: Throwable) {
        ""
    }
}

/**
 * Centralized detail screen used by History and Pending.
 * - title: main title (item name)
 * - modifier: modifier for the screen
 * - subtitle: small subtitle
 * - details: list of (icon,label,value)
 * - imagePath: optional local path or remote URL of the proof image
 */
@Composable
fun DeliveryDetailScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "Détail de la livraison",
    details: List<Triple<ImageVector, String, String>>,
    imagePath: String?,
    onBack: () -> Unit,
    // optional sync action shown as a trailing button in the header
    onSync: (() -> Unit)? = null,
    syncing: Boolean = false
) {
    Surface(modifier = modifier.fillMaxSize(), color = DeliveryColors.Background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CollapsedHeader(
                title = title,
                subtitle = subtitle,
                showBack = true,
                onBack = onBack,
                trailing = {
                    // If a sync action is provided show a sync icon (or progress) next to the decorative icon.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onSync != null) {
                            // show progress or sync button
                            if (syncing) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                androidx.compose.material3.IconButton(onClick = { onSync() }) {
                                    Icon(imageVector = Icons.Filled.Sync, contentDescription = "synchroniser", tint = Color.White)
                                }
                            }
                        }

                        // Decorative trailing icon (dimmed)
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
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
                    Spacer(modifier = Modifier.height(30.dp))


                }

                item {
                    // show image if available
                    val raw = imagePath

                    // Normalize and detect base64 or data URL
                    val imageData = remember(raw) {
                        // Log only length and prefix to avoid huge output when base64 is present
                        Log.d("DetailComponents", "compute imageData rawLength=${raw?.length ?: 0}")

                        if (raw.isNullOrBlank()) {
                            Log.d("DetailComponents", "raw is null or blank (no receiverProof provided)")
                            return@remember null
                        }

                        var candidate = raw.trim()
                        Log.d("DetailComponents", "candidate after trim length=${candidate.length}")

                        // If already a data URL, use as-is
                        if (candidate.startsWith("data:", ignoreCase = true)) {
                            Log.d("DetailComponents", "candidate is data URL (startsWith data:), prefix=${candidate.take(64)}...")
                            return@remember candidate
                        }

                        // Remove surrounding quotes if any
                        if ((candidate.startsWith("\"") && candidate.endsWith("\"")) || (candidate.startsWith("'") && candidate.endsWith("'"))) {
                            candidate = candidate.substring(1, candidate.length - 1)
                            Log.d("DetailComponents", "stripped surrounding quotes, new length=${candidate.length}")
                        }

                        // Remove whitespace/newlines that may exist inside base64
                        val compact = candidate.replace(Regex("\\s+"), "")
                        Log.d("DetailComponents", "compact length=${compact.length}")

                        // If it looks like base64 (contains only base64 chars and reasonably long) -> prefix
                        val base64Regex = Regex("^[A-Za-z0-9+/=]+$")
                        val isBase64 = compact.length > 40 && (base64Regex.matches(compact) || (compact.startsWith("/") && base64Regex.matches(compact.substring(1))))
                        if (isBase64) {
                            val dataUrl = "data:image/jpeg;base64,$compact"
                            Log.d("DetailComponents", "detected base64, compactLength=${compact.length}, dataUrlPrefix=${dataUrl.take(64)}...")
                            return@remember dataUrl
                        }

                        // HTTP/HTTPS or file paths
                        if (candidate.startsWith("http", ignoreCase = true)) {
                            Log.d("DetailComponents", "candidate is http url: $candidate")
                            return@remember candidate
                        }
                        if (candidate.startsWith("file:", ignoreCase = true)) {
                            Log.d("DetailComponents", "candidate is file url: $candidate")
                            return@remember candidate
                        }

                        // fallback: maybe it's a local absolute path
                        if (candidate.contains("/")) {
                            val f = "file://$candidate"
                            Log.d("DetailComponents", "candidate contains '/', returning file:// path: $f")
                            return@remember f
                        }

                        Log.d("DetailComponents", "no matching image pattern, returning null (raw prefix=${candidate.take(64)})")
                        null
                    }

                    // Log the final computed imageData for visibility
                    if (imageData == null) {
                        Log.d("DetailComponents", "imageData computed = null -> will show fallback placeholder")
                    } else {
                        Log.d("DetailComponents", "imageData computed length=${imageData.length} prefix=${imageData.take(120)}...")
                    }

                    if (imageData != null) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(text = "Preuve de Réception", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeliveryColors.TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Prepare model for Coil: if data URL, decode base64 and write to a temp cache file
                            val context = LocalContext.current
                            val modelForCoil = remember(imageData) {
                                if (imageData.startsWith("data:", ignoreCase = true)) {
                                    try {
                                        val comma = imageData.indexOf(',')
                                        val b64 = if (comma >= 0) imageData.substring(comma + 1) else imageData
                                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                                        val cacheDir = File(context.cacheDir, "proofs")
                                        if (!cacheDir.exists()) cacheDir.mkdirs()
                                        val tmp = File(cacheDir, "proof_${System.currentTimeMillis()}.jpg")
                                        FileOutputStream(tmp).use { it.write(bytes) }
                                        Log.d("DetailComponents", "wrote base64 image to temp file: ${tmp.absolutePath}")
                                        tmp.absolutePath
                                    } catch (_: Throwable) {
                                        Log.d("DetailComponents", "failed to decode/write base64 image: (exception)")
                                        imageData
                                    }
                                } else imageData
                            }

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(modelForCoil)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Preuve de réception",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_launcher_background),
                                error = painterResource(id = R.drawable.ic_launcher_background)
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Pas de preuve", tint = DeliveryColors.TextSecondary, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Aucune preuve fournie", color = DeliveryColors.TextSecondary)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun TicketCard(
    title: String,
    shop: String,
    serialNumber: String,
    deliveryAgent: String,
    code: String,
    imagePath: String?,
    // optional ISO createdAt string for History items or formatted createdAt for Pending
    createdAt: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(if (code.isNotEmpty()) DeliveryColors.Accent else DeliveryColors.PrimaryLight)
            )

            // Thumbnail logic
            val thumbData = remember(imagePath) {
                val path = imagePath
                if (path == null) return@remember null

                var candidate = path.trim()
                if (candidate.startsWith("data:", ignoreCase = true)) return@remember candidate
                if ((candidate.startsWith("\"") && candidate.endsWith("\"")) || (candidate.startsWith("'") && candidate.endsWith("'"))) {
                    candidate = candidate.substring(1, candidate.length - 1)
                }
                val compact = candidate.replace(Regex("\\s+"), "")
                val base64Regex = Regex("^[A-Za-z0-9+/=]+$")
                val isBase64 = compact.length > 40 && (base64Regex.matches(compact) || (compact.startsWith("/") && base64Regex.matches(compact.substring(1))))
                if (isBase64) {
                    Log.d("DetailComponents", "thumb detected base64 (including leading '/'), returning data url")
                    return@remember "data:image/jpeg;base64,$compact"
                }
                if (candidate.startsWith("http", ignoreCase = true)) return@remember candidate
                if (candidate.startsWith("file:", ignoreCase = true)) return@remember candidate
                if (candidate.contains("/")) return@remember "file://$candidate"
                null
            }

            if (thumbData != null) {
                val ctx = LocalContext.current
                // if thumbData is a data URL, decode and write to temp file eagerly
                val thumbModelForCoil = remember(thumbData) {
                    if (thumbData.startsWith("data:", ignoreCase = true)) {
                        try {
                            val comma = thumbData.indexOf(',')
                            val b64 = if (comma >= 0) thumbData.substring(comma + 1) else thumbData
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            val cacheDir = File(ctx.cacheDir, "proofs")
                            if (!cacheDir.exists()) cacheDir.mkdirs()
                            val tmp = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(tmp).use { it.write(bytes) }
                            tmp.absolutePath
                        } catch (_: Throwable) {
                            Log.d("DetailComponents", "thumb decode/write failed: (exception)")
                            thumbData
                        }
                    } else thumbData
                }

                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(thumbModelForCoil).crossfade(true).build(),
                    contentDescription = "thumb",
                    modifier = Modifier.padding(12.dp).size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
                    error = painterResource(id = R.drawable.ic_launcher_background)
                )
             } else {
                 Box(
                     modifier = Modifier.padding(12.dp).size(56.dp).clip(RoundedCornerShape(8.dp)).background(DeliveryColors.InputBg),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(Icons.Filled.CameraAlt, null, Modifier.size(22.dp), DeliveryColors.TextSecondary)
                 }
             }

             Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Column(modifier = Modifier.weight(1f)) {
                         Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeliveryColors.TextPrimary)
                         // show createdAt if provided (smaller, secondary)
                         if (!createdAt.isNullOrBlank()) {
                             Text(text = createdAt, style = MaterialTheme.typography.labelSmall, color = DeliveryColors.TextSecondary)
                         }
                     }

                     Spacer(modifier = Modifier.weight(1f))
                     Box(modifier = Modifier.background(DeliveryColors.InputBg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                         Text(text = shop, style = MaterialTheme.typography.labelSmall, color = DeliveryColors.TextSecondary)
                     }
                 }
                 Spacer(modifier = Modifier.height(8.dp))
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Filled.QrCode, null, Modifier.size(14.dp), DeliveryColors.Accent)
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(text = serialNumber, style = MaterialTheme.typography.bodySmall, color = DeliveryColors.TextSecondary)
                     Spacer(modifier = Modifier.width(16.dp))
                     Icon(Icons.Filled.Person, null, Modifier.size(14.dp), DeliveryColors.Accent)
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(text = deliveryAgent, style = MaterialTheme.typography.bodySmall, color = DeliveryColors.TextSecondary)
                 }
             }

             Box(modifier = Modifier.fillMaxHeight().padding(end = 12.dp), contentAlignment = Alignment.Center) {
                 Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = DeliveryColors.BorderSubtle)
             }
         }
     }
 }
