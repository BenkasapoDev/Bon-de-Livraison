package com.infosetgroup.delivery

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // Filters all layout imports
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.repository.DeliveryRepository
import com.infosetgroup.delivery.ui.PendingScreen
import com.infosetgroup.delivery.ui.SyncBottomBar
import com.infosetgroup.delivery.ui.theme.DeliveryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

// --- 1. DESIGN SYSTEM & COLORS ---

object DeliveryColors {
    val PrimaryDark = Color(0xFF1E293B)     // Slate 800 (Header)
    val PrimaryCorral = Color(0xFFFF6B4A) // The "Coral" orange-red color
    val PrimaryLight = Color(0xFF334155)    // Slate 700
    val Accent = Color(0xFFF97316)          // Orange 500 (Action Buttons)
    val Background = Color(0xFFF1F5F9)      // Slate 100 (App Background)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val InputBg = Color(0xFFF8FAFC)
    val BorderSubtle = Color(0xFFE2E8F0)
}

// --- 2. DATA MODELS ---

data class DeliveryItem(
    val item: String,
    val serialNumber: String,
    val sim: String,
    val merchant: String,
    val shop: String,
    val receiver: String,
    val deliveryAgent: String,
    val code: String
)

sealed class Screen(val title: String) {
    object Form : Screen("Livraison")
    object History : Screen("Historique")
    object Offline : Screen("Hors-ligne")
}

sealed class MainTab {
    object FormTab : MainTab()
    object HistoryTab : MainTab()
    object OfflineTab : MainTab()
}

// --- 3. REUSABLE UI COMPONENTS ---

@Composable
fun FormSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = DeliveryColors.TextSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 24.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DeliveryColors.TextPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = DeliveryColors.TextSecondary)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = DeliveryColors.InputBg,
                disabledContainerColor = DeliveryColors.InputBg,
                focusedIndicatorColor = DeliveryColors.Accent,
                unfocusedIndicatorColor = Color.Transparent, // No underline
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = DeliveryColors.TextPrimary,
                unfocusedTextColor = DeliveryColors.TextPrimary
            ),
            singleLine = true
        )
    }
}

// --- 4. MAIN ACTIVITY ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeliveryTheme {
                MainScreen()
            }
        }
    }

    // Fetch history Logic
    fun fetchHistory(callback: (List<DeliveryItem>) -> Unit) {
        val endpoint = "https://deliveries.devi7.in/api/rest/v1/deliveries/history"
        Thread {
            try {
                val url = java.net.URL(endpoint)
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                val input = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val arr = org.json.JSONArray(input)
                val list = mutableListOf<DeliveryItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        DeliveryItem(
                            item = obj.optString("item"),
                            serialNumber = obj.optString("serialNumber"),
                            sim = obj.optString("sim"),
                            merchant = obj.optString("merchant"),
                            shop = obj.optString("shop"),
                            receiver = obj.optString("receiver"),
                            deliveryAgent = obj.optString("deliveryAgent"),
                            code = obj.optString("code")
                        )
                    )
                }
                runOnUiThread { callback(list) }
            } catch (_: Exception) {
                runOnUiThread {
                    runOnUiThread {
                        val userFriendlyMessage = "Impossible de récupérer l'historique."
                        Toast.makeText(this, "📴 $userFriendlyMessage", Toast.LENGTH_LONG).show()
                        callback(emptyList())
                    }
                }
            }
        }.start()
    }
}

// --- 5. MAIN SCREEN & NAVIGATION ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current

    // State Hoisting: Form data sits here so History can update it
    val objet = remember { mutableStateOf("") }
    val serial = remember { mutableStateOf("") }
    val sim = remember { mutableStateOf("") }
    val marchand = remember { mutableStateOf("") }
    val magasin = remember { mutableStateOf("") }
    val responsable = remember { mutableStateOf("") }
    val livreur = remember { mutableStateOf("") }
    val imageBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val imagePath = remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateOf<Screen>(Screen.Form) }
    val currentTab = remember { mutableStateOf<MainTab>(MainTab.FormTab) }
    // --- NEW: State for selected history item ---
    val selectedHistoryItem = remember { mutableStateOf<DeliveryItem?>(null) }

    // Repository & Sync State
    val repo = DeliveryRepository.getInstance(context)
    val pendingCountFlow = repo.observePendingCount()
    val pendingCount by pendingCountFlow.collectAsState(initial = 0)
    val syncing = remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Column {
                // Custom Sync Bar from your existing code
                SyncBottomBar(
                    pendingCount = pendingCount,
                    onOpenPending = {
                        currentTab.value = MainTab.OfflineTab
                        selected.value = Screen.Offline
                    },
                    onSync = {
                        syncing.value = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val res = repo.syncPending()
                            syncing.value = false
                            withContext(Dispatchers.Main) {
                                when (res) {
                                    is com.infosetgroup.delivery.repository.SyncResult.Success ->
                                        Toast.makeText(context, "Synchronisé ${res.syncedCount}", Toast.LENGTH_SHORT).show()
                                    is com.infosetgroup.delivery.repository.SyncResult.NothingToSync ->
                                        Toast.makeText(context, "Rien à synchroniser", Toast.LENGTH_SHORT).show()
                                    is com.infosetgroup.delivery.repository.SyncResult.Failure ->
                                        Toast.makeText(context, "Échec: ${res.error}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    syncing = syncing.value
                )

                // Navigation Bar
                // --- PROFESSIONAL NAVIGATION BAR ---
                NavigationBar(
                    containerColor = Color.White.copy(alpha = 0.95f), // Modern glass effect
                    tonalElevation = 0.dp, // Removes the muddy grey shadow
                    modifier = Modifier
                        .graphicsLayer {
                            // High-definition diffused shadow
                            shadowElevation = 24f
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            clip = true
                        }
                        .border(
                            width = 1.dp,
                            color = DeliveryColors.BorderSubtle.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                    // Define items in a list for clean iteration
                    val navItems = listOf(
                        Triple(MainTab.FormTab, Screen.Form, Icons.AutoMirrored.Rounded.Assignment),
                        Triple(MainTab.HistoryTab, Screen.History, Icons.Rounded.History),
                        Triple(MainTab.OfflineTab, Screen.Offline, Icons.Rounded.CloudOff)
                    )

                    navItems.forEach { (tab, screen, icon) ->
                        val isSelected = currentTab.value == tab

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                currentTab.value = tab
                                selected.value = screen
                            },
                            icon = {
                                // Stack icon and a custom dot indicator
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        // Selected icon is slightly larger for visual "depth"
                                        modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                                    )

                                    // The "Active Dot" - a hallmark of professional UI
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(DeliveryColors.Accent, CircleShape)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) DeliveryColors.Accent else DeliveryColors.TextSecondary
                                )
                            },
                            // Logic: only show the label of the active tab for a cleaner "Apple-style" look
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeliveryColors.Accent,
                                selectedTextColor = DeliveryColors.Accent,
                                unselectedIconColor = DeliveryColors.TextSecondary.copy(alpha = 0.6f),
                                unselectedTextColor = DeliveryColors.TextSecondary.copy(alpha = 0.6f),
                                // Hide the default heavy M3 background pill
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // --- NEW: Show detail if selectedHistoryItem is not null ---
        if (selected.value is Screen.History && selectedHistoryItem.value != null) {
            HistoryDetailScreen(
                item = selectedHistoryItem.value!!,
                onBack = { selectedHistoryItem.value = null }
            )
        } else {
            // Screen Content
            when (selected.value) {
                is Screen.Form -> {
                    FormScreen(
                        objet = objet,
                        serial = serial,
                        sim = sim,
                        marchand = marchand,
                        magasin = magasin,
                        responsable = responsable,
                        livreur = livreur,
                        imageBitmap = imageBitmap,
                        imagePath = imagePath,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                is Screen.History -> {
                    HistoryScreen(
                        onShowDetail = { selectedHistoryItem.value = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                is Screen.Offline -> {
                    PendingScreen(
                        onBack = {
                            currentTab.value = MainTab.FormTab
                            selected.value = Screen.Form
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// --- 6. HISTORY SCREEN (Redesigned) ---

@Composable
fun HistoryScreen(onShowDetail: (DeliveryItem) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<DeliveryItem>() }
    val isLoading = remember { mutableStateOf(true) }
    var query by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) {
        val activity = context as? MainActivity
        activity?.fetchHistory { list ->
            items.clear()
            items.addAll(list)
            isLoading.value = false
        } ?: run { isLoading.value = false }
    }

    Column(modifier = modifier.background(DeliveryColors.Background).fillMaxSize()) {
        // Simple Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeliveryColors.PrimaryDark)
                .padding(24.dp)
        ) {
            Text(
                text = "Historique Récent",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Search Bar (Floating over header/content junction)
        Box(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-24).dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "search") },
                placeholder = { Text("Rechercher un n° série...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeliveryColors.Accent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeliveryColors.Accent)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp)
            ) {
                items(items) { delivery ->
                    // Ticket Style Item
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onShowDetail(delivery) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            // Status Strip
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(6.dp)
                                    .background(if(delivery.code.isNotEmpty()) DeliveryColors.Accent else DeliveryColors.PrimaryLight)
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
    }
}

// --- NEW: HISTORY DETAIL SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(item: DeliveryItem, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeliveryColors.Background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Modern Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(DeliveryColors.PrimaryDark, DeliveryColors.Accent)
                        )
                    )
                    .height(180.dp)
            ) {
                // Large Icon
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 24.dp)
                )
                // Title
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp, end = 100.dp)
                ) {
                    Text(
                        text = item.item,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Détail de la livraison",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp
                    )
                }
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            // --- Details List ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                DetailRowModern(Icons.Filled.QrCode, "N° Série", item.serialNumber)
                DetailRowModern(Icons.Filled.SimCard, "SIM", item.sim)
                DetailRowModern(Icons.Filled.Store, "Marchand", item.merchant)
                DetailRowModern(Icons.Filled.Store, "Magasin", item.shop)
                DetailRowModern(Icons.Filled.Person, "Responsable", item.receiver)
                DetailRowModern(Icons.Filled.LocalShipping, "Livreur", item.deliveryAgent)
                DetailRowModern(Icons.Filled.VpnKey, "Code", item.code)
            }
        }
    }
}

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

// --- 7. FORM SCREEN (Redesigned) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    objet: MutableState<String>,
    serial: MutableState<String>,
    sim: MutableState<String>,
    marchand: MutableState<String>,
    magasin: MutableState<String>,
    responsable: MutableState<String>,
    livreur: MutableState<String>,
    imageBitmap: MutableState<Bitmap?>,
    imagePath: MutableState<String?>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deliveryViewModel: com.infosetgroup.delivery.ui.DeliveryViewModel = viewModel()
    val isSubmitting by deliveryViewModel.isSubmitting.collectAsState()
    val lastSubmitResult by deliveryViewModel.lastSubmitResult.collectAsState()

    // Camera Logic
    val cameraPermissionGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        cameraPermissionGranted.value = granted
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap.value = bitmap
            try {
                val dir = File(context.filesDir, "images")
                if (!dir.exists()) dir.mkdirs()
                val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
                val file = File(dir, "IMG_${time}.jpg")
                FileOutputStream(file).use { fos -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos) }
                imagePath.value = file.absolutePath
            } catch (e: Exception) { Toast.makeText(context, "Erreur image", Toast.LENGTH_SHORT).show() }
        }
    }

    LaunchedEffect(lastSubmitResult) {
        lastSubmitResult?.let { res ->
            when (res) {
                is com.infosetgroup.delivery.repository.SubmitResult.Sent ->
                    Toast.makeText(context, "Envoyé avec succès", Toast.LENGTH_SHORT).show()
                is com.infosetgroup.delivery.repository.SubmitResult.Queued ->
                    Toast.makeText(context, "Sauvegardé hors-ligne", Toast.LENGTH_SHORT).show()
                is com.infosetgroup.delivery.repository.SubmitResult.Failure ->
                    Toast.makeText(context, "Erreur: ${res.error}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeliveryColors.Background)
    ) {
        // 1. Header
        Surface(
            color = DeliveryColors.PrimaryDark,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().height(110.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nouvelle Livraison",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Remplissez les détails ci-dessous",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(48.dp).background(Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, null, tint = DeliveryColors.Accent)
                }
            }
        }

        // 2. Form Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {

            FormSectionTitle("Détails du Colis")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModernTextField(
                        value = objet.value,
                        onValueChange = { objet.value = it },
                        label = "Objet / Produit",
                        icon = Icons.Filled.Inventory2 // Ensure material-icons-extended dependency or change to Box
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernTextField(
                            value = serial.value,
                            onValueChange = { serial.value = it },
                            label = "N° Série",
                            icon = Icons.Filled.QrCode,
                            modifier = Modifier.weight(1f)
                        )
                        // SIM field with visible +243 prefix
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SIM / ICCID",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DeliveryColors.TextPrimary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            TextField(
                                value = sim.value.removePrefix("+243"),
                                onValueChange = { newValue ->
                                    // Always enforce the +243 prefix
                                    sim.value = "+243" + newValue.filter { it.isDigit() }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.SimCard, contentDescription = null, tint = DeliveryColors.TextSecondary)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            "+243",
                                            color = DeliveryColors.TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = DeliveryColors.InputBg,
                                    disabledContainerColor = DeliveryColors.InputBg,
                                    focusedIndicatorColor = DeliveryColors.Accent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedTextColor = DeliveryColors.TextPrimary,
                                    unfocusedTextColor = DeliveryColors.TextPrimary
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            FormSectionTitle("Point de Vente")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModernTextField(
                        value = marchand.value,
                        onValueChange = { marchand.value = it },
                        label = "Marchand",
                        icon = Icons.Filled.Store
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernTextField(
                        value = magasin.value,
                        onValueChange = { magasin.value = it },
                        label = "Magasin / Zone",
                        icon = Icons.Filled.Store
                    )
                }
            }

            FormSectionTitle("Intervenants")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModernTextField(
                        value = responsable.value,
                        onValueChange = { responsable.value = it },
                        label = "Responsable Réception",
                        icon = Icons.Filled.Person
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernTextField(
                        value = livreur.value,
                        onValueChange = { livreur.value = it },
                        label = "Agent de Livraison",
                        icon = Icons.Filled.LocalShipping
                    )
                }
            }

            FormSectionTitle("Preuve Photo")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, DeliveryColors.BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable {
                        if (cameraPermissionGranted.value) launcher.launch(null)
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap.value != null) {
                    Image(
                        bitmap = imageBitmap.value!!.asImageBitmap(),
                        contentDescription = "Preuve",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)))
                    Icon(Icons.Filled.Edit, "Change", tint = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(60.dp).background(DeliveryColors.InputBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PhotoCamera, null, tint = DeliveryColors.Accent, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Appuyer pour photographier", color = DeliveryColors.TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    if (objet.value.isBlank() || serial.value.isBlank()) {
                        Toast.makeText(context, "Données incomplètes (Objet/Série)", Toast.LENGTH_SHORT).show()
                    } else {
                        val entity = DeliveryEntity(
                            item = objet.value, serialNumber = serial.value, sim = sim.value,
                            merchant = marchand.value, shop = magasin.value,
                            receiver = responsable.value, deliveryAgent = livreur.value,
                            receiverProofPath = imagePath.value ?: "",

                        )
                        deliveryViewModel.submitDelivery(entity)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeliveryColors.Accent),
                elevation = ButtonDefaults.buttonElevation(8.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("VALIDER LA LIVRAISON", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// 8. PREVIEW
@Preview(showBackground = true)
@Composable
fun FormPreview() {
    DeliveryTheme {
        // Dummy preview, won't fully work without ViewModel context but shows layout
        Text("Preview requires Mock VM")
    }
}