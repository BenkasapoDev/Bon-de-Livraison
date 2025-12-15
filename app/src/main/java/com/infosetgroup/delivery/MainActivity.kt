package com.infosetgroup.delivery

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.infosetgroup.delivery.repository.DeliveryRepository
import com.infosetgroup.delivery.data.DeliveryEntity
import com.infosetgroup.delivery.ui.SyncBottomBar
import com.infosetgroup.delivery.ui.PendingScreen
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import org.json.JSONArray
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.height
import com.infosetgroup.delivery.ui.theme.DeliveryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Enhanced color palette for modern logistics branding
object DeliveryColors {
    val PrimaryDark = Color(0xFF0F1B3C) // Deep navy blue
    val PrimaryCorral = Color(0xFFFF6B4A) // Coral accent
    val SurfaceLight = Color(0xFFF8F9FB) // Soft white
    val TextPrimary = Color(0xFF1A2847) // Dark text
    val TextSecondary = Color(0xFF687483) // Muted text
    val BorderLight = Color(0xFFE5E9F0) // Light border
}

// Data model for history items
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

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeliveryTheme {
                MainScreen()
            }
        }
    }

    // Fetch history from provided API and return parsed list via callback
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
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Erreur fetch historique: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(emptyList())
                }
            }
        }.start()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current

    // Lifted form state so both Form and History screens can access/update it
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

    // repository + pending state
    val repo = DeliveryRepository.getInstance(context)
    val pendingCountFlow = repo.observePendingCount()
    val pendingCount by pendingCountFlow.collectAsState(initial = 0)
    val syncing = remember { mutableStateOf(false) }
    val showPendingScreen = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selected.value.title, fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DeliveryColors.PrimaryDark),
                modifier = Modifier.height(72.dp)
            )
        },
        bottomBar = {
            Column {
                SyncBottomBar(pendingCount = pendingCount, onOpenPending = { showPendingScreen.value = true }, onSync = {
                    syncing.value = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val res = repo.syncPending()
                        syncing.value = false
                        withContext(Dispatchers.Main) {
                            when (res) {
                                is com.infosetgroup.delivery.repository.SyncResult.Success -> Toast.makeText(context, "Synchronisé ${res.syncedCount}", Toast.LENGTH_SHORT).show()
                                is com.infosetgroup.delivery.repository.SyncResult.NothingToSync -> Toast.makeText(context, "Rien à synchroniser", Toast.LENGTH_SHORT).show()
                                is com.infosetgroup.delivery.repository.SyncResult.Failure -> Toast.makeText(context, "Échec de la synchronisation: ${res.error}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }, syncing = syncing.value)

                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Edit, contentDescription = "Form") },
                        label = { Text("Formulaire") },
                        selected = selected.value is Screen.Form,
                        onClick = { selected.value = Screen.Form },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeliveryColors.PrimaryCorral,
                            unselectedIconColor = DeliveryColors.TextSecondary,
                            selectedTextColor = DeliveryColors.PrimaryCorral,
                            unselectedTextColor = DeliveryColors.TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = "History") },
                        label = { Text("Historique") },
                        selected = selected.value is Screen.History,
                        onClick = { selected.value = Screen.History },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeliveryColors.PrimaryCorral,
                            unselectedIconColor = DeliveryColors.TextSecondary,
                            selectedTextColor = DeliveryColors.PrimaryCorral,
                            unselectedTextColor = DeliveryColors.TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->

        if (showPendingScreen.value) {
            PendingScreen(onBack = { showPendingScreen.value = false }, modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        when (selected.value) {
            is Screen.Form -> {
                // pass lifted state into FormScreen
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
                HistoryScreen(onPick = { item ->
                    // fill lifted form state and navigate to form
                    objet.value = item.item
                    serial.value = item.serialNumber
                    sim.value = item.sim
                    marchand.value = item.merchant
                    magasin.value = item.shop
                    responsable.value = item.receiver
                    livreur.value = item.deliveryAgent
                    selected.value = Screen.Form
                    Toast.makeText(context, "Élément chargé dans le formulaire", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

// Refactored FormScreen to accept external state (lifted)
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

    val cameraPermissionGranted = remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = RequestPermission()
    ) { granted: Boolean ->
        cameraPermissionGranted.value = granted
        if (!granted) {
            Toast.makeText(context, "La permission d'appareil photo est requise", Toast.LENGTH_SHORT).show()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            imageBitmap.value = bitmap
            // save image to app files directory and store its path
            try {
                val dir = File(context.filesDir, "images")
                if (!dir.exists()) dir.mkdirs()
                val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
                val file = File(dir, "IMG_${time}.jpg")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                imagePath.value = file.absolutePath
                Toast.makeText(context, "Photo prise", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Toast.makeText(context, "Erreur lors de l'enregistrement de la photo: ${t.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Aucune photo", Toast.LENGTH_SHORT).show()
        }
    }

    // submitting state to disable the send button while the request is in flight
    val submitting = remember { mutableStateOf(false) }

    // UI is mostly unchanged, but using passed-in states
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Section: Détails de livraison
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📋 Détails de livraison",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DeliveryColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Enhanced form fields with improved styling
                    StyledTextField(
                        value = objet.value,
                        onValueChange = { objet.value = it },
                        label = "Objet",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StyledTextField(
                            value = serial.value,
                            onValueChange = { serial.value = it },
                            label = "N° de série",
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            value = sim.value,
                            onValueChange = { sim.value = it },
                            label = "SIM",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StyledTextField(
                            value = marchand.value,
                            onValueChange = { marchand.value = it },
                            label = "Marchand",
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            value = magasin.value,
                            onValueChange = { magasin.value = it },
                            label = "Magasin",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StyledTextField(
                            value = responsable.value,
                            onValueChange = { responsable.value = it },
                            label = "Responsable",
                            modifier = Modifier.weight(1f)
                        )
                        StyledTextField(
                            value = livreur.value,
                            onValueChange = { livreur.value = it },
                            label = "Livreur",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Signature / Preuve
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📸 Signature / Preuve",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DeliveryColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Instruction text: allow wrapping and full width
                    Text(
                        text = "Prenez une photo de la signature ou de la preuve de livraison.",
                        color = DeliveryColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        overflow = TextOverflow.Clip
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StyledButton(
                            onClick = {
                                if (cameraPermissionGranted.value) {
                                    launcher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            text = "📷 Prendre une photo",
                            modifier = Modifier.weight(1f),
                            isPrimary = true,
                            enabled = true
                        )
                        StyledButton(
                            onClick = {
                                imageBitmap.value = null
                                imagePath.value = null
                            },
                            text = "Effacer",
                            modifier = Modifier.weight(1f),
                            isPrimary = false,
                            enabled = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced image preview styling; make placeholder clickable to open camera
                    if (imageBitmap.value != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, DeliveryColors.BorderLight, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DeliveryColors.SurfaceLight)
                        ) {
                            Image(
                                bitmap = imageBitmap.value!!.asImageBitmap(),
                                contentDescription = "Signature capturée",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            colors = CardDefaults.cardColors(containerColor = DeliveryColors.SurfaceLight)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        // also open camera when tapping placeholder
                                        if (cameraPermissionGranted.value) {
                                            launcher.launch(null)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(text = "📷", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "📷 Prendre une photo",
                                        color = DeliveryColors.TextSecondary,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ou touchez ici pour ouvrir la caméra",
                                        color = DeliveryColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StyledButton(
                    onClick = {
                        if (objet.value.isBlank()) {
                            Toast.makeText(context, "Veuillez renseigner l'objet", Toast.LENGTH_SHORT).show()
                            return@StyledButton
                        }
                        // build entity and submit via repository
                        val repo = DeliveryRepository.getInstance(context)
                        val entity = DeliveryEntity(
                            item = objet.value,
                            serialNumber = serial.value,
                            sim = sim.value,
                            merchant = marchand.value,
                            shop = magasin.value,
                            receiver = responsable.value,
                            deliveryAgent = livreur.value,
                            receiverProofPath = imagePath.value
                        )
                        submitting.value = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val res = repo.submitDelivery(entity)
                            withContext(Dispatchers.Main) {
                                when (res) {
                                    is com.infosetgroup.delivery.repository.SubmitResult.Sent -> Toast.makeText(context, "Envoyé", Toast.LENGTH_SHORT).show()
                                    is com.infosetgroup.delivery.repository.SubmitResult.Queued -> Toast.makeText(context, "Sauvegardé hors ligne (id=${res.id})", Toast.LENGTH_SHORT).show()
                                    is com.infosetgroup.delivery.repository.SubmitResult.Failure -> Toast.makeText(context, "Erreur: ${res.error}", Toast.LENGTH_LONG).show()
                                }
                                submitting.value = false
                            }
                        }
                    },
                    text = "✓ Envoyer",
                    modifier = Modifier.weight(1f),
                    isPrimary = true,
                    enabled = true
                )

                StyledButton(
                    onClick = {
                        objet.value = ""
                        serial.value = ""
                        sim.value = ""
                        marchand.value = ""
                        magasin.value = ""
                        responsable.value = ""
                        livreur.value = ""
                        imageBitmap.value = null
                        imagePath.value = null
                    },
                    text = "↻ Réinitialiser",
                    modifier = Modifier.weight(1f),
                    isPrimary = false,
                    enabled = true
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// New HistoryScreen composable shows list full-screen and allows picking an item
@Composable
fun HistoryScreen(onPick: (DeliveryItem) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<DeliveryItem>() }
    // Loading state shown while fetchHistory is in progress
    val isLoading = remember { mutableStateOf(true) }

    // load once when this composable enters composition
    LaunchedEffect(Unit) {
        val activity = context as? MainActivity
        activity?.fetchHistory { list ->
            items.clear()
            items.addAll(list)
            isLoading.value = false
        } ?: run {
            Toast.makeText(context, "Impossible de charger historique : activité introuvable", Toast.LENGTH_SHORT).show()
            isLoading.value = false
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.height(6.dp))

        // Show loader while fetching
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeliveryColors.PrimaryCorral)
            }
            return@Column
        }

        // Show empty state if no items
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Aucun historique disponible", color = DeliveryColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Optional: small guidance
                    Text(text = "Essayez de rafraîchir plus tard.", color = DeliveryColors.TextSecondary, fontSize = 12.sp)
                }
            }
            return@Column
        }

        LazyColumn {
            items(items) { it ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onPick(it) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = it.item, fontWeight = FontWeight.SemiBold)
                        Text(text = "N° série: ${it.serialNumber}", fontSize = 12.sp, color = DeliveryColors.TextSecondary)
                        Text(text = "Magasin: ${it.shop}", fontSize = 12.sp, color = DeliveryColors.TextSecondary)
                    }
                }
            }
        }
    }
}

// New sealed class to represent the two screens in the app
sealed class Screen(val title: String) {
    object Form : Screen("Bon de Livraison")
    object History : Screen("Historique")
}

// New reusable styled text field component for consistency
@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier.height(56.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeliveryColors.PrimaryCorral,
            unfocusedBorderColor = DeliveryColors.BorderLight,
            focusedLabelColor = DeliveryColors.PrimaryCorral,
            unfocusedLabelColor = DeliveryColors.TextSecondary,
            focusedTextColor = DeliveryColors.TextPrimary,
            unfocusedTextColor = DeliveryColors.TextPrimary,
            cursorColor = DeliveryColors.PrimaryCorral
        ),
        shape = RoundedCornerShape(8.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
    )
}

// Updated StyledButton: don't force Text to fill width so emoji/text remain visible and can wrap
@Composable
fun StyledButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    // Precompute objects with remember to avoid creating stateful objects during composition
    val btnModifier = remember(modifier) { modifier.heightIn(min = 52.dp) }
    // ButtonDefaults.buttonColors is itself @Composable, call it directly
    val btnColors = ButtonDefaults.buttonColors(
        containerColor = if (isPrimary) DeliveryColors.PrimaryCorral else DeliveryColors.BorderLight,
        contentColor = if (isPrimary) Color.White else DeliveryColors.TextPrimary
    )
    val btnShape = remember { RoundedCornerShape(10.dp) }
    val btnPadding = remember { androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp) }

    Button(
        onClick = onClick,
        modifier = btnModifier,
        colors = btnColors,
        shape = btnShape,
        enabled = enabled,
        contentPadding = btnPadding
    ) {
        Text(
            text,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            // do not force fillMaxWidth here to avoid hiding emoji
            modifier = Modifier,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FormScreenPreview() {
    DeliveryTheme {
        FormScreen(
            objet = remember { mutableStateOf("Colis urgent") },
            serial = remember { mutableStateOf("SN123456") },
            sim = remember { mutableStateOf("SIM987654321") },
            marchand = remember { mutableStateOf("Marchand Test") },
            magasin = remember { mutableStateOf("Magasin Central") },
            responsable = remember { mutableStateOf("Jean Dupont") },
            livreur = remember { mutableStateOf("Pierre Martin") },
            imageBitmap = remember { mutableStateOf(null) },
            imagePath = remember { mutableStateOf(null) }
        )
    }
}