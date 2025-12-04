package com.infosetgroup.delivery

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.infosetgroup.delivery.ui.theme.DeliveryTheme
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// Enhanced color palette for modern logistics branding
object DeliveryColors {
    val PrimaryDark = Color(0xFF0F1B3C) // Deep navy blue
    val PrimaryCorral = Color(0xFFFF6B4A) // Coral accent
    val SurfaceLight = Color(0xFFF8F9FB) // Soft white
    val TextPrimary = Color(0xFF1A2847) // Dark text
    val TextSecondary = Color(0xFF687483) // Muted text
    val BorderLight = Color(0xFFE5E9F0) // Light border
    val SuccessGreen = Color(0xFF10B981) // Success state
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeliveryTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // Upgraded top bar with dark navy background and better styling
                        CenterAlignedTopAppBar(
                            title = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Bon de Livraison",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = DeliveryColors.PrimaryDark
                            ),
                            modifier = Modifier.height(72.dp)
                        )
                    }
                ) { innerPadding ->
                    FormScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // Updated to accept all fields and build a JSON payload in French keys
    fun sendToApi(
        objet: String,
        serial: String,
        sim: String,
        marchand: String,
        magasin: String,
        responsable: String,
        livreur: String,
        imageBase64: String?
    ) {
        val endpoint = "https://example.com/api/upload"
        thread {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val safeImage = imageBase64 ?: ""
                val json = "{" +
                        "\"objet\":\"${escapeJson(objet)}\"," +
                        "\"numero_serie\":\"${escapeJson(serial)}\"," +
                        "\"sim\":\"${escapeJson(sim)}\"," +
                        "\"marchand\":\"${escapeJson(marchand)}\"," +
                        "\"magasin\":\"${escapeJson(magasin)}\"," +
                        "\"responsable\":\"${escapeJson(responsable)}\"," +
                        "\"livreur\":\"${escapeJson(livreur)}\"," +
                        "\"image_base64\":\"${escapeJson(safeImage)}\"" +
                        "}"

                OutputStreamWriter(conn.outputStream).use { it.write(json) }

                val code = conn.responseCode
                runOnUiThread {
                    val msg = if (code in 200..299) "Envoyé avec succès (code=$code)" else "Échec de l'envoi (code=$code)"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Erreur réseau: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun escapeJson(s: String): String {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // form fields
    val objet = remember { mutableStateOf("") }
    val serial = remember { mutableStateOf("") }
    val sim = remember { mutableStateOf("") }
    val marchand = remember { mutableStateOf("") }
    val magasin = remember { mutableStateOf("") }
    val responsable = remember { mutableStateOf("") }
    val livreur = remember { mutableStateOf("") }

    val imageBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val imageBase64 = remember { mutableStateOf<String?>(null) }

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

    // Launcher for taking picture
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            imageBitmap.value = bitmap
            // convert to base64
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            imageBase64.value = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Toast.makeText(context, "Photo prise", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Aucune photo", Toast.LENGTH_SHORT).show()
        }
    }

    // Updated surface background to match new color scheme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DeliveryColors.SurfaceLight
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Section: Détails de livraison
            // Enhanced card styling with shadow and better spacing
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
                    Spacer(modifier = Modifier.height(16.dp))

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
                    Text(
                        text = "Prenez une photo de la signature ou de la preuve de livraison.",
                        color = DeliveryColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // Updated buttons with coral accent and improved styling
                        StyledButton(
                            onClick = {
                                if (cameraPermissionGranted.value) {
                                    launcher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            text = "📷 Photo",
                            modifier = Modifier.weight(1f),
                            isPrimary = true
                        )
                        StyledButton(
                            onClick = {
                                imageBitmap.value = null
                                imageBase64.value = null
                            },
                            text = "Effacer",
                            modifier = Modifier.weight(1f),
                            isPrimary = false
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced image preview styling
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
                                .height(140.dp),
                            colors = CardDefaults.cardColors(containerColor = DeliveryColors.SurfaceLight)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(text = "📭", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aucune photo",
                                        color = DeliveryColors.TextSecondary,
                                        fontSize = 13.sp
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
                        val activity = context as? MainActivity
                        activity?.sendToApi(
                            objet.value,
                            serial.value,
                            sim.value,
                            marchand.value,
                            magasin.value,
                            responsable.value,
                            livreur.value,
                            imageBase64.value
                        ) ?: Toast.makeText(context, "Impossible d'envoyer : activité introuvable", Toast.LENGTH_SHORT).show()
                    },
                    text = "✓ Envoyer",
                    modifier = Modifier.weight(1f),
                    isPrimary = true
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
                        imageBase64.value = null
                    },
                    text = "↻ Réinitialiser",
                    modifier = Modifier.weight(1f),
                    isPrimary = false
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
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

// New reusable styled button component for consistency
@Composable
fun StyledButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    Button(
        onClick = onClick,
        // use heightIn to avoid forcing exact height which can conflict with weight
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) DeliveryColors.PrimaryCorral else DeliveryColors.BorderLight,
            contentColor = if (isPrimary) Color.White else DeliveryColors.TextPrimary
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            // ensure long text or emojis don't overflow vertically
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FormScreenPreview() {
    DeliveryTheme {
        FormScreen()
    }
}