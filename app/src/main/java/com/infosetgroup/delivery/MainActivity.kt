package com.infosetgroup.delivery

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import com.infosetgroup.delivery.data.AppDatabaseHolder
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.ui.DeliveryDetailScreen
import com.infosetgroup.delivery.ui.PendingScreen
import com.infosetgroup.delivery.ui.theme.DeliveryTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import com.infosetgroup.delivery.util.mapNetworkErrorToFrench
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

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
    val code: String,
    val receiverProofPath: String = "",
    // optional ISO createdAt from server (e.g. 2025-12-22T11:22:01+00:00)
    val createdAt: String? = null
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
)  {
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
                focusedIndicatorColor = DeliveryColors.Accent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        }
    }


@Composable
fun CollapsedHeader(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        color = DeliveryColors.PrimaryDark,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            if (trailing != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

// --- 4. MAIN ACTIVITY ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize important libraries synchronously to avoid race conditions where
        // ViewModels or other components open the Room DB before our safe init runs.
        try {
            AndroidThreeTen.init(applicationContext)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "AndroidThreeTen.init failed: ${e.message}")
        }

        try {
            // AppDatabaseHolder will attempt a safe init and delete a stale DB file if needed.
            AppDatabaseHolder.init(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "AppDatabaseHolder.init failed", e)
        }

        enableEdgeToEdge()
        setContent {
            DeliveryTheme {
                MainScreen()
            }
        }
    }
}

// --- 5. MAIN SCREEN & NAVIGATION ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
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
    val selectedHistoryItem = remember { mutableStateOf<DeliveryItem?>(null) }

    // NEW: central SnackbarHostState for the whole screen
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                NavigationBar(
                    containerColor = Color.White.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .graphicsLayer {
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
                    val navItems = listOf(
                        // AutoMirrored.Rounded.Assignment isn't available with current icon set; use a stable icon.
                        Triple(MainTab.FormTab, Screen.Form, Icons.Filled.Inventory2),
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                                    )

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
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeliveryColors.Accent,
                                selectedTextColor = DeliveryColors.Accent,
                                unselectedIconColor = DeliveryColors.TextSecondary.copy(alpha = 0.6f),
                                unselectedTextColor = DeliveryColors.TextSecondary.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (selected.value is Screen.History && selectedHistoryItem.value != null) {
            HistoryDetailScreen(
                item = selectedHistoryItem.value!!,
                onBack = { selectedHistoryItem.value = null },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (selected.value) {
                is Screen.Form -> {
                    // Pass snackbarHostState to FormScreen so it can show consistent French snackbars
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
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                is Screen.History -> {
                    HistoryScreen(
                        onShowDetail = { selectedHistoryItem.value = it },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                is Screen.Offline -> {
                    PendingScreen(
                        onBack = {
                            currentTab.value = MainTab.FormTab
                            selected.value = Screen.Form
                        },
                        onAdd = {
                            // Navigate to the Form screen and select Form tab
                            currentTab.value = MainTab.FormTab
                            selected.value = Screen.Form
                        },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// --- 6. HISTORY SCREEN ---

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onShowDetail: (DeliveryItem) -> Unit, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val vm: com.infosetgroup.delivery.ui.HistoryViewModel = viewModel()
    var query by remember { mutableStateOf("") }
    val lazyPagingItems = vm.historyFlow.collectAsLazyPagingItems()
    val currentPageSize by vm.pageSize.collectAsState()

    // Show paging errors as French snackbars
    LaunchedEffect(lazyPagingItems.loadState) {
        val refresh = lazyPagingItems.loadState.refresh
        if (refresh is LoadState.Error) {
            val err = refresh.error
            snackbarHostState.showSnackbar(mapNetworkErrorToFrench(err.message ?: err.toString()))
        }
        val append = lazyPagingItems.loadState.append
        if (append is LoadState.Error) {
            val err = append.error
            snackbarHostState.showSnackbar(mapNetworkErrorToFrench(err.message ?: err.toString()))
        }
    }

    Column(modifier = modifier.background(DeliveryColors.Background).fillMaxSize()) {
        CollapsedHeader(title = "Historique Récent")

        Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-24).dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    vm.setKeyword(q.ifBlank { null })
                },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Show active query context to the user.
            if (query.isNotBlank()) {
                Text(
                    text = "Résultats pour '$query'",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeliveryColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Afficher par:",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeliveryColors.TextSecondary
                )
                listOf(10, 25, 50, 100).forEach { size ->
                    FilterChip(
                        selected = currentPageSize == size,
                        onClick = { vm.setPageSize(size) },
                        label = { Text(size.toString()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeliveryColors.Accent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        when (lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeliveryColors.Accent)
            }

            is LoadState.Error -> {
                // Map the underlying error to a friendly French message and offer retry
                val err = (lazyPagingItems.loadState.refresh as? LoadState.Error)?.error
                val friendly = mapNetworkErrorToFrench(err?.message ?: err.toString())
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = friendly, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { lazyPagingItems.retry() }) {
                            Text("Réessayer")
                        }
                    }
                }
            }

            else -> {
                if (lazyPagingItems.itemCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = DeliveryColors.TextSecondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (query.isNotBlank()) "Aucun résultat pour '$query'" else "Aucun résultat trouvé",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DeliveryColors.TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index ->
                                val it = lazyPagingItems[index]
                                it?.code?.takeIf { c -> c.isNotBlank() } ?: index
                            }
                        ) { index ->
                            val d = lazyPagingItems[index]
                            if (d != null) {
                                // Cache formatted date per item to avoid repeated parsing on each recomposition
                                val formattedCreatedAt = remember(d.createdAt) { formatIsoCreatedAt(d.createdAt) }

                                com.infosetgroup.delivery.ui.TicketCard(
                                    title = d.item,
                                    shop = d.shop,
                                    serialNumber = d.serialNumber,
                                    deliveryAgent = d.deliveryAgent,
                                    code = d.code,
                                    imagePath = d.receiverProofPath,
                                    createdAt = formattedCreatedAt,
                                    onClick = { onShowDetail(d) }
                                )
                            }
                        }

                        item {
                            when (lazyPagingItems.loadState.append) {
                                is LoadState.Loading -> Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = DeliveryColors.Accent)
                                }

                                is LoadState.Error -> Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(onClick = { lazyPagingItems.retry() }) { Text("Réessayer") }
                                }

                                else -> Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDetailScreen(item: DeliveryItem, onBack: () -> Unit, modifier: Modifier = Modifier) {
    // Fetch full detail from server by code and display centralized DeliveryDetailScreen.
    val code = item.code
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var fetched by remember { mutableStateOf<DeliveryItem?>(null) }

    LaunchedEffect(code) {
        isLoading = true
        errorMsg = null
        try {
            val res = com.infosetgroup.delivery.network.NetworkClient.getHistoryDetail(code)
            when (res) {
                is com.infosetgroup.delivery.network.NetworkResult.Success -> {
                    val body = res.body
                    if (!body.isNullOrBlank()) {
                        try {
                            val jo = org.json.JSONObject(body)
                            // log raw body for debug (prefix only)
                            android.util.Log.d("HistoryDetail", "detail response body length=${body.length} prefix=${body.take(400)}")

                            // parse fields defensively
                            val itemName = jo.optString("item", item.item)
                            val serial = jo.optString("serialNumber", item.serialNumber)
                            val sim = jo.optString("sim", item.sim)
                            val merchant = jo.optString("merchant", item.merchant)
                            val shop = jo.optString("shop", item.shop)
                            val receiver = jo.optString("receiver", item.receiver)
                            val deliveryAgent = jo.optString("deliveryAgent", item.deliveryAgent)
                            val codeResp = jo.optString("code", code)
                            val receiverProof = jo.optString("receiverProof", item.receiverProofPath)

                            android.util.Log.d("HistoryDetail", "raw receiverProof length=${receiverProof.length} prefix=${receiverProof.take(120)}")

                            // Normalize receiverProof: remove quotes/whitespace and convert raw base64 to a data URL
                            val normalizedReceiverProof = run {
                                var candidate = receiverProof ?: ""
                                candidate = candidate.trim()

                                // If server returned an empty string but the list item had a value, use that as fallback
                                if (candidate.isBlank() && item.receiverProofPath.isNotBlank()) {
                                    android.util.Log.d("HistoryDetail", "receiverProof detail was blank; falling back to list item receiverProofPath (length=${item.receiverProofPath.length})")
                                    candidate = item.receiverProofPath
                                }

                                // If blank -> return null (no image)
                                if (candidate.isBlank()) {
                                    android.util.Log.d("HistoryDetail", "receiverProof is blank after fallback checks for code=$codeResp")
                                    return@run null
                                }

                                // strip surrounding quotes if present
                                if ((candidate.startsWith("\"") && candidate.endsWith("\"")) || (candidate.startsWith("'") && candidate.endsWith("'"))) {
                                    candidate = candidate.substring(1, candidate.length - 1)
                                }

                                // compact whitespace/newlines that may be embedded in base64 from the backend
                                val compact = candidate.replace(Regex("\\s+"), "")
                                val base64Regex = Regex("^[A-Za-z0-9+/=]+$")

                                // allow common base64 payloads that may start with a leading '/'
                                val looksLikeBase64 = compact.length > 40 && (base64Regex.matches(compact) || (compact.startsWith("/") && base64Regex.matches(compact.substring(1))))

                                if (looksLikeBase64) {
                                    val dataUrl = "data:image/jpeg;base64,$compact"
                                    android.util.Log.d("HistoryDetail", "Detected base64 receiverProof for code=$codeResp, dataUrlPrefix=${dataUrl.take(80)}")
                                    dataUrl
                                } else {
                                    // if already a data: URL or http/file path, keep as-is
                                    if (candidate.startsWith("data:", ignoreCase = true) || candidate.startsWith("http", ignoreCase = true) || candidate.startsWith("file:", ignoreCase = true)) {
                                        android.util.Log.d("HistoryDetail", "receiverProof seems to be a URL or data: for code=$codeResp, prefix=${candidate.take(80)}")
                                        candidate
                                    } else {
                                        // unknown/unsupported format -> treat as absent
                                        android.util.Log.d("HistoryDetail", "receiverProof present but not recognized as base64 or URL for code=$codeResp, prefix=${candidate.take(80)}")
                                        null
                                    }
                                }
                            }

                            fetched = DeliveryItem(
                                item = itemName,
                                serialNumber = serial,
                                sim = sim,
                                merchant = merchant,
                                shop = shop,
                                receiver = receiver,
                                deliveryAgent = deliveryAgent,
                                code = codeResp,
                                receiverProofPath = normalizedReceiverProof ?: ""
                            )
                        } catch (je: Exception) {
                            // include exception message in error for better diagnostics
                            errorMsg = "Erreur de lecture du détail: ${je.message ?: "format inattendu"}"
                        }
                    } else {
                        errorMsg = "Aucune donnée reçue"
                    }
                }
                is com.infosetgroup.delivery.network.NetworkResult.Failure -> {
                    errorMsg = mapNetworkErrorToFrench(res.throwable?.message ?: "Erreur réseau")
                }
            }
        } catch (t: Throwable) {
            errorMsg = mapNetworkErrorToFrench(t.message ?: "Erreur inconnue")
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeliveryColors.Accent)
            }
        }
        errorMsg != null -> {
            Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = errorMsg ?: "Erreur", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { /* retry by re-triggering effect */ }, colors = ButtonDefaults.buttonColors(containerColor = DeliveryColors.Accent)) {
                    Text("Réessayer")
                }
            }
        }
        fetched != null -> {
            val it = fetched!!
            val details = listOf(
                Triple(Icons.Filled.QrCode, "N° Série", it.serialNumber),
                Triple(Icons.Filled.SimCard, "SIM", it.sim),
                Triple(Icons.Filled.Store, "Marchand", it.merchant),
                Triple(Icons.Filled.Store, "Magasin", it.shop),
                Triple(Icons.Filled.Person, "Responsable", it.receiver),
                Triple(Icons.Filled.LocalShipping, "Livreur", it.deliveryAgent),
                Triple(Icons.Filled.VpnKey, "Code", it.code)
            )

            DeliveryDetailScreen(
                title = it.item,
                subtitle = "Détail de la livraison",
                details = details,
                imagePath = it.receiverProofPath,
                onBack = onBack,
                modifier = modifier
            )
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun détail")
            }
        }
    }
}

// --- 7. FORM SCREEN ---

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
    snackbarHostState: SnackbarHostState, // NEW param
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deliveryViewModel: com.infosetgroup.delivery.ui.DeliveryViewModel = viewModel()
    val isSubmitting by deliveryViewModel.isSubmitting.collectAsState()
    val lastSubmitResult by deliveryViewModel.lastSubmitResult.collectAsState()

    val scope = rememberCoroutineScope()

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
            } catch (e: Exception) {
                // show snackbar instead of Toast; use scope.launch to call suspend function
                scope.launch {
                    snackbarHostState.showSnackbar(message = "Erreur image: ${e.message ?: "Erreur inconnue"}")
                }
            }
        }
    }

    // small helper to map common network / exception messages to friendly French
    // (removed local implementation in favor of shared util mapNetworkErrorToFrench)

    LaunchedEffect(lastSubmitResult) {
        lastSubmitResult?.let { res ->
            when (res) {
                is com.infosetgroup.delivery.repository.SubmitResult.Sent -> {
                    // show snackbar instead of Toast
                    snackbarHostState.showSnackbar(message = "Envoyé avec succès")
                    // Clear form fields after successful send
                    objet.value = ""
                    serial.value = ""
                    sim.value = ""
                    marchand.value = ""
                    magasin.value = ""
                    responsable.value = ""
                    livreur.value = ""
                    imageBitmap.value = null
                    imagePath.value = null
                    // acknowledge submission so it doesn't repeat
                    deliveryViewModel.clearLastSubmitResult()
                }
                is com.infosetgroup.delivery.repository.SubmitResult.Queued -> {
                    snackbarHostState.showSnackbar(message = "Sauvegardé hors-ligne")
                    // Clear form fields after queued save as well
                    objet.value = ""
                    serial.value = ""
                    sim.value = ""
                    marchand.value = ""
                    magasin.value = ""
                    responsable.value = ""
                    livreur.value = ""
                    imageBitmap.value = null
                    imagePath.value = null
                    // acknowledge
                    deliveryViewModel.clearLastSubmitResult()
                }
                is com.infosetgroup.delivery.repository.SubmitResult.Failure -> {
                    val friendly = mapNetworkErrorToFrench(res.error)
                    snackbarHostState.showSnackbar(message = "Erreur: $friendly")
                    // acknowledge failure so it doesn't repeat; keep fields to let user retry
                    deliveryViewModel.clearLastSubmitResult()
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(DeliveryColors.Background)) {
        CollapsedHeader(title = "Nouvelle Livraison", subtitle = "Remplissez les détails ci-dessous")

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

            FormSectionTitle("Détails du Colis")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModernTextField(
                        value = objet.value,
                        onValueChange = { objet.value = it },
                        label = "Objet / Produit",
                        icon = Icons.Filled.Inventory2
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
                                    sim.value = "+243" + newValue.filter { it.isDigit() }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.SimCard, contentDescription = null, tint = DeliveryColors.TextSecondary)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+243", color = DeliveryColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = DeliveryColors.InputBg,
                                    focusedIndicatorColor = DeliveryColors.Accent,
                                    unfocusedIndicatorColor = Color.Transparent
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
                    ModernTextField(value = marchand.value, onValueChange = { marchand.value = it }, label = "Marchand", icon = Icons.Filled.Store)
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernTextField(value = magasin.value, onValueChange = { magasin.value = it }, label = "Magasin / Zone", icon = Icons.Filled.Store)
                }
            }

            FormSectionTitle("Intervenants")
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ModernTextField(value = responsable.value, onValueChange = { responsable.value = it }, label = "Responsable Réception", icon = Icons.Filled.Person)
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernTextField(value = livreur.value, onValueChange = { livreur.value = it }, label = "Agent de Livraison", icon = Icons.Filled.LocalShipping)
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
                    Image(bitmap = imageBitmap.value!!.asImageBitmap(), contentDescription = "Preuve", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)))
                    Icon(Icons.Filled.Edit, "Change", tint = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(60.dp).background(DeliveryColors.InputBg, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PhotoCamera, null, tint = DeliveryColors.Accent, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Appuyer pour photographier", color = DeliveryColors.TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (objet.value.isBlank() || serial.value.isBlank()) {
                        // replaced Toast with snackbar via coroutine scope
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Données incomplètes (Objet/Série)")
                        }
                    } else {
                        val entity = DeliveryEntity(
                            item = objet.value, serialNumber = serial.value, sim = sim.value,
                            merchant = marchand.value, shop = magasin.value,
                            receiver = responsable.value, deliveryAgent = livreur.value,
                            receiverProofPath = imagePath.value ?: ""
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

@Preview(showBackground = true)
@Composable
fun FormPreview() {
    DeliveryTheme {
        Text("Preview requires Mock VM")
    }
}

// helper to format ISO createdAt strings into friendly French date/time
fun formatIsoCreatedAt(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val odt = OffsetDateTime.parse(iso)
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH)
        odt.format(fmt)
    } catch (_: DateTimeParseException) {
        // fallback: return raw
        iso
    } catch (_: Throwable) {
        iso
    }
}
