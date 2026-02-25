package com.trasparenza.app.ui.receipt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trasparenza.app.data.model.ReceiptData
import com.trasparenza.app.data.model.ReceiptProduct

/**
 * Screen to import a receipt (image, PDF, or video) and show extracted products.
 * From here the user can proceed to the full sustainability report.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onNavigateToReport: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    // File picker: images, PDFs, videos
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧾 Scontrino") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val state = uiState) {
                is ReceiptUiState.Idle -> {
                    IdleContent(
                        selectedUri = selectedUri,
                        onPickFile = { filePickerLauncher.launch("*/*") },
                        onAnalyze = { uri -> viewModel.analyzeReceiptUri(uri) },
                        onFullReport = { uri -> viewModel.generateReportFromUri(uri) },
                    )
                }

                is ReceiptUiState.LoadingOcr -> {
                    LoadingCard(message = state.message, icon = Icons.Filled.DocumentScanner)
                }

                is ReceiptUiState.ReceiptReady -> {
                    ReceiptReadyContent(
                        data = state.data,
                        onGenerateReport = { viewModel.generateReportFromReceiptData(state.data) },
                        onReset = { viewModel.reset(); selectedUri = null },
                    )
                }

                is ReceiptUiState.LoadingReport -> {
                    LoadingCard(message = state.message, icon = Icons.Filled.Eco)
                }

                is ReceiptUiState.ReportReady -> {
                    // Navigate to report screen
                    LaunchedEffect(Unit) { onNavigateToReport() }
                    LoadingCard(message = "Apertura rapporto…", icon = Icons.Filled.Eco)
                }

                is ReceiptUiState.Error -> {
                    ErrorCard(
                        message = state.message,
                        onRetry = { viewModel.reset(); selectedUri = null },
                    )
                }
            }
        }
    }
}

// ─── Idle Content ─────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    selectedUri: Uri?,
    onPickFile: () -> Unit,
    onAnalyze: (Uri) -> Unit,
    onFullReport: (Uri) -> Unit,
) {
    // Header
    Text(
        text = "Analizza il tuo scontrino",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Importa un'immagine, un PDF o un video dello scontrino per scoprire l'impatto ecologico del tuo acquisto.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(8.dp))

    // Supported formats
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormatChip(icon = Icons.Filled.Image, label = "Immagine")
        FormatChip(icon = Icons.Filled.PictureAsPdf, label = "PDF")
        FormatChip(icon = Icons.Filled.Videocam, label = "Video")
    }

    Spacer(Modifier.height(8.dp))

    // File pick area
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 2.dp,
                color = if (selectedUri != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onPickFile() },
        contentAlignment = Alignment.Center,
    ) {
        if (selectedUri != null) {
            AsyncImage(
                model = selectedUri,
                contentDescription = "Scontrino selezionato",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Tocca per selezionare un file", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    AnimatedVisibility(visible = selectedUri != null, enter = fadeIn(), exit = fadeOut()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Only OCR
            OutlinedButton(
                onClick = { onAnalyze(selectedUri!!) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.DocumentScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Solo lista prodotti")
            }
            // Full pipeline
            Button(
                onClick = { onFullReport(selectedUri!!) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Eco, null)
                Spacer(Modifier.width(8.dp))
                Text("Rapporto sostenibilità completo")
            }
        }
    }
}

// ─── Receipt Ready (OCR only result) ─────────────────────────────────────────

@Composable
private fun ReceiptReadyContent(
    data: ReceiptData,
    onGenerateReport: () -> Unit,
    onReset: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🏪 ${data.storeName ?: "Negozio non identificato"}", fontWeight = FontWeight.Bold)
            if (data.receiptDate != null) Text("📅 ${data.receiptDate}")
            if (data.totalAmount != null) Text("💰 Totale: ${data.currency} ${"%.2f".format(data.totalAmount)}")
            Text("🛒 ${data.products.size} prodotti trovati")
        }
    }

    // Product list
    data.products.forEach { product ->
        ReceiptProductRow(product)
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = onGenerateReport,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Eco, null)
        Spacer(Modifier.width(8.dp))
        Text("Genera rapporto sostenibilità")
    }

    TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text("Nuovo scontrino")
    }
}

@Composable
private fun ReceiptProductRow(product: ReceiptProduct) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                if (product.brand != null) {
                    Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (product.totalPrice != null) {
                Text(
                    "€${"%.2f".format(product.totalPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─── Loading Card ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingCard(message: String, icon: ImageVector) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text(message, textAlign = TextAlign.Center)
        }
    }
}

// ─── Error Card ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Errore", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Text(message)
            Button(onClick = onRetry) { Text("Riprova") }
        }
    }
}

// ─── Format Chip ─────────────────────────────────────────────────────────────

@Composable
private fun FormatChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
